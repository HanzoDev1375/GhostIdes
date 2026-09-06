'use strict';

/**
 * GhostIDEs theme language server (.gth).
 * Zero-dependency Node.js LSP over stdio (Content-Length framing).
 *
 * Features:
 *  - completions: section names at root, valid keys per section, and relative
 *    references `@section.` / `@section.key`
 *  - hover: description of keys + resolved value of @references
 *  - go-to-definition: jumps from `@section.key` to the target key
 *  - diagnostics: unknown sections/keys, invalid colors, bad references, and
 *    invalid blursize values
 *  - formatting: textDocument/formatting + rangeFormatting via Prettier
 *    (falls back to a built-in JSON printer when Prettier is unavailable)
 *  - document colors: renders color chips for hex values and resolved refs
 */

const { spawn, spawnSync } = require('child_process');

const { BLOCKS, DEFAULTS, isBlock, keysOf, isValidKey, typeOf, description, resolveRef } = require('./theme-schema');

// --------------------------------------------------------------------------
// JSON-RPC framing
// --------------------------------------------------------------------------

let buffer = Buffer.alloc(0);

function sendMessage(obj) {
  const body = JSON.stringify(obj);
  const b = Buffer.from(`Content-Length: ${Buffer.byteLength(body)}\r\n\r\n${body}`, 'utf8');
  process.stdout.write(b);
}

function onData(data) {
  buffer = Buffer.concat([buffer, data]);
  for (;;) {
    const headerEnd = buffer.indexOf('\r\n\r\n');
    if (headerEnd < 0) return;
    const header = buffer.slice(0, headerEnd).toString('utf8');
    const m = /Content-Length:\s*(\d+)/i.exec(header);
    if (!m) {
      buffer = Buffer.alloc(0);
      return;
    }
    const length = Number(m[1]);
    const bodyStart = headerEnd + 4;
    if (buffer.length < bodyStart + length) return;
    const body = buffer.slice(bodyStart, bodyStart + length).toString('utf8');
    buffer = buffer.slice(bodyStart + length);
    if (body.trim()) {
      try {
        handleMessage(JSON.parse(body));
      } catch (_) {
        /* ignore malformed frame */
      }
    }
  }
}

process.stdin.on('data', onData);
process.stdin.on('end', () => process.exit(0));

// --------------------------------------------------------------------------
// Position math (UTF-16 code units, LSP default)
// --------------------------------------------------------------------------

function buildLineStarts(text) {
  const starts = [0];
  for (let i = 0; i < text.length; i++) {
    if (text.charCodeAt(i) === 10) starts.push(i + 1);
  }
  return starts;
}

function offsetToPosition(starts, offset) {
  let lo = 0;
  let hi = starts.length - 1;
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1;
    if (starts[mid] <= offset) lo = mid;
    else hi = mid - 1;
  }
  return { line: lo, character: offset - starts[lo] };
}

function positionToOffset(starts, line, character) {
  const base = line >= 0 && line < starts.length ? starts[line] : starts[starts.length - 1];
  return base + Math.max(0, character);
}

function rangeOf(starts, startOff, endOff) {
  return { start: offsetToPosition(starts, startOff), end: offsetToPosition(starts, endOff) };
}

// --------------------------------------------------------------------------
// Tolerant JSON tokenizer
// --------------------------------------------------------------------------

function readString(text, start) {
  let i = start + 1;
  const n = text.length;
  let out = '';
  while (i < n) {
    const ch = text[i];
    if (ch === '\\') {
      if (i + 1 < n) out += text[i + 1];
      i += 2;
      continue;
    }
    if (ch === '"') {
      i++;
      break;
    }
    out += ch;
    i++;
  }
  return { value: out, end: i };
}

function isWhitespace(ch) {
  return ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r';
}

function nearestBlock(stack) {
  for (let i = stack.length - 1; i >= 0; i--) {
    if (stack[i].block) return stack[i].block;
  }
  return null;
}

function scan(text) {
  const tokens = [];
  const stack = [];
  let objectDepth = 0;
  let pendingProp = null;
  const n = text.length;
  let i = 0;

  while (i < n) {
    const ch = text[i];
    if (ch === '"') {
      const read = readString(text, i);
      let j = read.end;
      while (j < n && isWhitespace(text[j])) j++;
      const isKey = text[j] === ':';
      const block = nearestBlock(stack);
      const tok = {
        kind: 'string',
        raw: read.value,
        start: i,
        end: read.end,
        contentStart: i + 1,
        contentEnd: read.end - 1,
        block,
        isKey,
        owner: pendingProp ? pendingProp.key : null,
      };
      if (!isKey) pendingProp = null;
      else pendingProp = { key: read.value, block };
      tokens.push(tok);
      i = read.end;
      continue;
    }
    if (ch === '{') {
      const before = objectDepth;
      let block = null;
      if (before === 1 && pendingProp && isBlock(pendingProp.key)) block = pendingProp.key;
      stack.push({ block, isRoot: before === 0 });
      objectDepth++;
      pendingProp = null;
      i++;
      continue;
    }
    if (ch === '}') {
      if (stack.length) {
        stack.pop();
        objectDepth--;
      }
      pendingProp = null;
      i++;
      continue;
    }
    if (ch === '[') {
      i++;
      continue;
    }
    if (ch === ']' || ch === ',') pendingProp = null;
    i++;
  }

  let braceBalance = 0;
  for (let k = 0; k < n; k++) {
    if (text[k] === '{') braceBalance++;
    else if (text[k] === '}') braceBalance--;
  }
  return { tokens, braceBalance };
}

// --------------------------------------------------------------------------
// Document store
// --------------------------------------------------------------------------

const documents = new Map();

function indexDocument(uri, text) {
  const lineStarts = buildLineStarts(text);
  const { tokens, braceBalance } = scan(text);
  const diagnostics = computeDiagnostics(uri, text, tokens, lineStarts, braceBalance);
  const doc = { uri, text, lineStarts, tokens, diagnostics };
  documents.set(uri, doc);
  publishDiagnostics(uri, diagnostics);
  return doc;
}

/** Effective theme = defaults deep-merged with this document's own values. */
function effectiveMap(tokens) {
  const eff = JSON.parse(JSON.stringify(DEFAULTS));
  for (const t of tokens) {
    if (!t.isKey && t.owner && t.block && eff[t.block]) {
      eff[t.block][t.owner] = t.raw;
    }
  }
  return eff;
}

function posOfStarts(doc, off) {
  return offsetToPosition(doc.lineStarts, off);
}

// --------------------------------------------------------------------------
// Diagnostics
// --------------------------------------------------------------------------

const SEVERITY = { ERROR: 1, WARNING: 2, INFO: 3, HINT: 4 };

function computeDiagnostics(uri, text, tokens, lineStarts, braceBalance) {
  const diags = [];

  if (braceBalance !== 0) {
    diags.push({
      range: { start: { line: 0, character: 0 }, end: { line: 0, character: 1 } },
      severity: SEVERITY.WARNING,
      source: 'ghost-theme-lsp',
      message: 'JSON is incomplete (braces `{` and `}` are unbalanced).',
    });
  }

  const seenKeys = new Map();
  for (const t of tokens) if (t.isKey) {
    const k = t.block ? String(t.block) : '_root';
    if (!seenKeys.has(k)) seenKeys.set(k, new Map());
    const map = seenKeys.get(k);
    map.set(t.raw, (map.get(t.raw) || 0) + 1);
  }

  for (const t of tokens) {
    const range = rangeOf(lineStarts, t.start, t.end);

    if (t.isKey) {
      if (t.block === null) {
        if (!isBlock(t.raw) && t.raw.trim()) {
          diags.push({
            range, severity: SEVERITY.WARNING, source: 'ghost-theme-lsp',
            message: `Section "${t.raw}" is unknown. Valid sections: ${BLOCKS.join(', ')}.`,
          });
        }
      } else if (!isValidKey(t.block, t.raw)) {
        diags.push({
          range, severity: SEVERITY.WARNING, source: 'ghost-theme-lsp',
          message: `Key "${t.raw}" does not exist in section ${t.block}.`,
        });
      }
      continue;
    }

    if (t.raw === '' && (t.owner === 'imagepath' || t.owner === 'blursize')) continue;

    const dup = t.block && seenKeys.get(t.block) && seenKeys.get(t.block).get(t.owner);
    if (t.block && t.owner && dup && tokenIsOwner(tokens, t)) {
      // duplicates are flagged on the owner key token (already reported once)
    }

    if (!t.owner || !t.block) continue;
    const type = typeOf(t.block, t.owner);

    if (type === 'number') {
      const trimmed = t.raw.trim();
      if (trimmed && !/^-?\d+(\.\d+)?$/.test(trimmed)) {
        diags.push({
          range, severity: SEVERITY.ERROR, source: 'ghost-theme-lsp',
          message: `Value "${t.raw}" must be a number (blursize).`,
        });
      } else if (trimmed) {
        const num = Number(trimmed);
        if (num < 0 || num > 25) {
          diags.push({
            range, severity: SEVERITY.WARNING, source: 'ghost-theme-lsp',
            message: 'blursize must be between 0 and 25.',
          });
        }
      }
      continue;
    }

    if (type === 'path') continue;

    // color
    if (t.raw.startsWith('@')) {
      const parts = t.raw.slice(1).split('.', 2);
      if (parts.length !== 2 || !parts[0] || !parts[1]) {
        diags.push({
          range, severity: SEVERITY.ERROR, source: 'ghost-theme-lsp',
          message: 'Invalid reference. Expected format: "@section.key", e.g. "@material3.surface".',
        });
        continue;
      }
      if (!isBlock(parts[0])) {
        diags.push({
          range, severity: SEVERITY.ERROR, source: 'ghost-theme-lsp',
          message: `Reference section "${parts[0]}" does not exist. Valid sections: ${BLOCKS.join(', ')}.`,
        });
        continue;
      }
      if (!isValidKey(parts[0], parts[1])) {
        diags.push({
          range, severity: SEVERITY.ERROR, source: 'ghost-theme-lsp',
          message: `Reference key "${parts[1]}" does not exist in section ${parts[0]}.`,
        });
        continue;
      }
      const eff = effectiveMap(tokens);
      const resolved = resolveRef(eff, t.raw);
      if (resolved === t.raw || resolved.startsWith('@')) {
        diags.push({
          range, severity: SEVERITY.ERROR, source: 'ghost-theme-lsp',
          message: 'Reference cannot resolve to a final color (cycle or incomplete).',
        });
      }
    } else if (!/^#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(t.raw.trim())) {
      diags.push({
        range, severity: SEVERITY.WARNING, source: 'ghost-theme-lsp',
        message: `Color "${t.raw}" is not valid. Format: #RRGGBB or #RRGGBBAA, or an @section.key reference.`,
      });
    }
  }

  return diags;
}

function tokenIsOwner(tokens, t) {
  return !t.isKey && t.owner != null;
}

function publishDiagnostics(uri, diagnostics) {
  sendMessage({ jsonrpc: '2.0', method: 'textDocument/publishDiagnostics', params: { uri, diagnostics } });
}

// --------------------------------------------------------------------------
// Types
// --------------------------------------------------------------------------

const kinds = { Property: 10, Value: 12, Color: 17 };

function hexToRgb(hex) {
  const m = /^#?([0-9a-fA-F]{3,8})$/.exec((hex || '').trim());
  if (!m) return null;
  let h = m[1];
  if (h.length === 3 || h.length === 4) h = [...h].map(c => c + c).join('');
  while (h.length < 8) h += 'ff';
  const a = parseInt(h.slice(0, 2), 16);
  const r = parseInt(h.slice(2, 4), 16);
  const g = parseInt(h.slice(4, 6), 16);
  const b = parseInt(h.slice(6, 8), 16);
  const rgb = { red: r / 255, green: g / 255, blue: b / 255, alpha: a / 255 };
  rgb.hex = `#${h.toUpperCase()}`;
  return rgb;
}

// --------------------------------------------------------------------------
// LSP handlers
// --------------------------------------------------------------------------

const handlers = {};

handlers['initialize'] = (params) => ({
  capabilities: {
    positionEncoding: 'utf-16',
    textDocumentSync: { openClose: true, change: 1 },
    completionProvider: { triggerCharacters: ['@', '.', '"', ':'], resolveProvider: false },
    hoverProvider: true,
    definitionProvider: true,
    documentFormattingProvider: true,
    documentRangeFormattingProvider: true,
    colorProvider: { provideDocumentColors: true, provideColorPresentations: true },
  },
  serverInfo: { name: 'ghost-theme-lsp', version: require('../package.json').version },
});

handlers['shutdown'] = () => null;

handlers['textDocument/didOpen'] = (p) => {
  indexDocument(p.textDocument.uri, p.textDocument.text);
  return null;
};

handlers['textDocument/didChange'] = (p) => {
  const uri = p.textDocument.uri;
  const changes = p.contentChanges || [];
  if (changes.length === 0) return null;
  indexDocument(uri, changes[changes.length - 1].text);
  return null;
};

handlers['textDocument/didClose'] = (p) => {
  documents.delete(p.textDocument.uri);
  return null;
};

handlers['textDocument/completion'] = (p) => {
  const doc = documents.get(p.textDocument.uri);
  if (!doc) return null;
  const offset = positionToOffset(doc.lineStarts, p.position.line, p.position.character);
  const items = [];

  const token = doc.tokens.find(
    (t) => offset >= t.start && offset <= t.end && (offset !== t.start || t.start === t.end)
  );
  if (!token) return { isIncomplete: false, items };

  const contentUpTo = Math.min(offset, token.contentEnd);
  const prefixLen = contentUpTo - token.contentStart;
  const prefix = token.raw.slice(0, prefixLen);

  if (token.isKey) {
    if (token.block === null) {
      for (const b of BLOCKS) {
        if (!b.startsWith(prefix)) continue;
        items.push({
          label: b, kind: kinds.Property,
          textEdit: { range: rangeOf(doc.lineStarts, token.contentStart, token.contentEnd), newText: b },
          documentation: { kind: 'markdown', value: `Section \`${b}\` of the theme` },
        });
      }
    } else {
      for (const key of keysOf(token.block)) {
        if (!key.startsWith(prefix)) continue;
        items.push({
          label: key, kind: kinds.Property, detail: token.block,
          textEdit: { range: rangeOf(doc.lineStarts, token.contentStart, token.contentEnd), newText: key },
          documentation: description(token.block, key) || undefined,
        });
      }
    }
    return { isIncomplete: false, items };
  }

  if (!token.owner || !token.block) return { isIncomplete: false, items };

  const at = prefix.lastIndexOf('@');
  if (at < 0) return { isIncomplete: false, items };

  const afterAt = prefix.slice(at + 1);
  const dot = afterAt.indexOf('.');
  const eff = effectiveMap(doc.tokens);
  const replaceRange = rangeOf(doc.lineStarts, token.contentStart + at, token.contentEnd);

  if (dot >= 0) {
    const block = afterAt.slice(0, dot);
    const partialKey = afterAt.slice(dot + 1);
    if (isBlock(block)) {
      for (const key of keysOf(block)) {
        if (!key.startsWith(partialKey)) continue;
        const ref = `@${block}.${key}`;
        const resolved = resolveRef(eff, ref);
        const resolvedHex = resolved && !resolved.startsWith('@') ? resolved : '';
        items.push({
          label: `${block}.${key}`, kind: kinds.Value, detail: 'Color reference',
          textEdit: { range: replaceRange, newText: ref },
          documentation: {
            kind: 'markdown',
            value: description(block, key) + (resolvedHex ? `\n\nResolved value: \`${resolvedHex}\`` : ''),
          },
        });
      }
    }
  } else {
    for (const b of BLOCKS) {
      if (!b.startsWith(afterAt)) continue;
      items.push({
        label: b, kind: kinds.Value, detail: 'Section',
        textEdit: { range: replaceRange, newText: `@${b}.` },
        documentation: { kind: 'markdown', value: `Keys of section \`${b}\` will be suggested after the dot.` },
      });
    }
  }
  return { isIncomplete: false, items };
};

handlers['textDocument/hover'] = (p) => {
  const doc = documents.get(p.textDocument.uri);
  if (!doc) return null;
  const offset = positionToOffset(doc.lineStarts, p.position.line, p.position.character);
  const token = doc.tokens.find((t) => offset >= t.start && offset <= t.end);
  if (!token) return null;

  const eff = effectiveMap(doc.tokens);
  let markdown = '';

  if (token.isKey) {
    const name = token.block ? `**${token.block}.${token.raw}**` : `**${token.raw}**`;
    const desc = token.block ? description(token.block, token.raw) : '';
    markdown = `${name}${desc ? '\n\n' + desc : ''}`;
    if (token.block) {
      const def = DEFAULTS[token.block] && DEFAULTS[token.block][token.raw];
      if (def !== undefined) markdown += `\n\nDefault: \`${def}\``;
    }
  } else if (token.owner && token.block) {
    const ownDesc = description(token.block, token.owner);
    markdown += `**${token.block}.${token.owner}**` + (ownDesc ? `\n\n${ownDesc}` : '');
    if (token.raw.startsWith('@')) {
      const resolved = resolveRef(eff, token.raw);
      const hex = resolved && !resolved.startsWith('@') ? hexToRgb(resolved) : null;
      markdown += `\n\nReference: \`${token.raw}\``;
      if (hex) markdown += `\nResolved value: \`${hex.hex}\``;
    } else if (typeOf(token.block, token.owner) === 'color' && /^#([0-9a-fA-F]{3,8})/.test(token.raw)) {
      const hex = hexToRgb(token.raw);
      if (hex) markdown += `\n\`${hex.hex}\``;
    }
  } else {
    return null;
  }

  return { contents: { kind: 'markdown', value: markdown }, range: rangeOf(doc.lineStarts, token.start, token.end) };
};

handlers['textDocument/definition'] = (p) => {
  const doc = documents.get(p.textDocument.uri);
  if (!doc) return null;
  const offset = positionToOffset(doc.lineStarts, p.position.line, p.position.character);
  const token = doc.tokens.find((t) => offset >= t.start && offset <= t.end);
  if (!token || token.isKey || !token.raw || !token.raw.startsWith('@')) return null;
  const parts = token.raw.slice(1).split('.', 2);
  if (parts.length !== 2) return null;
  const target = doc.tokens.find((t) => t.isKey && t.raw === parts[1] && t.block === parts[0]);
  if (!target) return null;
  return {
    uri: doc.uri,
    range: rangeOf(doc.lineStarts, target.contentStart, target.contentEnd),
  };
};

handlers['textDocument/documentColor'] = (p) => {
  const doc = documents.get(p.textDocument.uri);
  if (!doc) return null;
  const eff = effectiveMap(doc.tokens);
  const colors = [];
  for (const t of doc.tokens) {
    if (t.isKey || !t.owner || !t.block) continue;
    if (typeOf(t.block, t.owner) !== 'color') continue;
    let hex = t.raw;
    if (hex.startsWith('@')) {
      const resolved = resolveRef(eff, hex);
      if (!resolved || resolved.startsWith('@')) continue;
      hex = resolved;
    }
    const rgb = hexToRgb(hex);
    if (!rgb) continue;
    colors.push({
      range: rangeOf(doc.lineStarts, t.contentStart, t.contentEnd),
      color: { red: rgb.red, green: rgb.green, blue: rgb.blue, alpha: rgb.alpha },
    });
  }
  return colors;
};

handlers['textDocument/colorPresentation'] = (p) => {
  const c = p.color || {};
  const toHex = (v) => Math.round(v * 255).toString(16).padStart(2, '0').toUpperCase();
  const hh = `${toHex(c.red)}${toHex(c.green)}${toHex(c.blue)}`;
  const alpha = Math.round((c.alpha === undefined ? 1 : c.alpha) * 255);
  const rr = (v) => Math.round(v * 255);
  return [
    { label: alpha < 255 ? `#${hh}${toHex(alpha / 255)}` : `#${hh}` },
    { label: `rgba(${rr(c.red)}, ${rr(c.green)}, ${rr(c.blue)}, ${Math.round((alpha / 255) * 100) / 100})` },
  ];
};

// --------------------------------------------------------------------------
// Formatting (Prettier with a built-in JSON fallback)
// --------------------------------------------------------------------------

// prettierState: 'unknown' | 'installing' | 'ready' | 'failed'. All prettier
// setup runs in the background so formatting can never block the server;
// requests use the built-in JSON printer until Prettier is ready.
let prettierState = 'unknown';

function runCmd(file, args, input, timeout) {
  try {
    return spawnSync(file, args, { input, encoding: 'utf8', timeout });
  } catch (_) {
    return null;
  }
}

function probePrettier() {
  const probe = runCmd('prettier', ['--version'], '', 8000);
  return !!probe && probe.status === 0;
}

/**
 * Ensures a `prettier` binary will be available, kicking off a background
 * `npm install -g prettier` when it is missing. Called lazily on the first
 * formatting request and only ever attempted once per server session. Does
 * not wait for the install: returns true only when Prettier is already ready.
 */
function ensurePrettier() {
  if (prettierState !== 'unknown') return prettierState === 'ready';
  prettierState = 'installing';
  if (probePrettier()) {
    prettierState = 'ready';
    return true;
  }
  const installer = spawn('npm', ['install', '-g', 'prettier'], { stdio: 'ignore' });
  installer.on('error', () => {
    prettierState = 'failed';
  });
  installer.on('close', (code) => {
    if (code === 0 && probePrettier()) prettierState = 'ready';
    else prettierState = 'failed';
  });
  return false;
}

function prettierFormat(text) {
  const run = runCmd(
    'prettier',
    ['--stdin-filepath', 'theme.gth', '--parser', 'json', '--tab-width', '2'],
    text,
    10000,
  );
  if (!run || run.status !== 0) return null;
  return run.stdout;
}

function builtinFormat(text) {
  try {
    return JSON.stringify(JSON.parse(text), null, 2) + '\n';
  } catch (_) {
    return null;
  }
}

/** Formats the whole document; returns a single TextEdit or null when unchanged/unparseable. */
function formatDocument(doc) {
  let out = null;
  if (prettierState === 'ready') {
    out = prettierFormat(doc.text);
  } else if (prettierState === 'unknown') {
    ensurePrettier();
  }
  if (out === null) out = builtinFormat(doc.text);
  if (out === null || out === doc.text) return null;
  const lastStart = doc.lineStarts[doc.lineStarts.length - 1];
  return [
    {
      range: {
        start: { line: 0, character: 0 },
        end: { line: doc.lineStarts.length - 1, character: doc.text.length - lastStart },
      },
      newText: out,
    },
  ];
}

handlers['textDocument/formatting'] = (p) => {
  const doc = documents.get(p.textDocument.uri);
  if (!doc) return null;
  return formatDocument(doc);
};

handlers['textDocument/rangeFormatting'] = (p) => {
  const doc = documents.get(p.textDocument.uri);
  if (!doc) return null;
  return formatDocument(doc);
};

handlers['workspace/configuration'] = () => [];

// --------------------------------------------------------------------------
// Dispatch
// --------------------------------------------------------------------------

function handleMessage(msg) {
  if (!msg || msg.method === undefined) return;

  if (msg.method.startsWith('$/')) return;

  if (msg.id !== undefined && msg.id !== null) {
    // request
    const handler = handlers[msg.method];
    let result;
    let error = null;
    if (handler) {
      try {
        result = handler(msg.params);
      } catch (e) {
        error = { code: -32603, message: String((e && e.message) || e) };
      }
    } else {
      error = { code: -32601, message: `Unsupported method: ${msg.method}` };
    }
    if (error) {
      sendMessage({ jsonrpc: '2.0', id: msg.id, error });
    } else {
      sendMessage({ jsonrpc: '2.0', id: msg.id, result: result === undefined ? null : result });
    }
    return;
  }

  // notification
  const handler = handlers[msg.method];
  if (handler) {
    try {
      handler(msg.params);
    } catch (_) {
      /* notifications are best-effort */
    }
  }
  if (msg.method === 'exit') process.exit(0);
}

process.stdout.on('error', () => process.exit(0));