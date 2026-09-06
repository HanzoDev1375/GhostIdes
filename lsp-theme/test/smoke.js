'use strict';

/**
 * Smoke test for ghost-theme-lsp: spawns the server via stdio and exercises
 * initialize / didOpen / diagnostics / completion / hover / definition /
 * documentColor / colorPresentation.
 * Exit code 0 on success, 1 on failure.
 */

const { spawn } = require('child_process');
const path = require('path');

const SERVER = process.env.GTH_LSP_SERVER || path.join(__dirname, '..', 'src', 'server.js');
const child = spawn(process.execPath, [SERVER], { stdio: ['pipe', 'pipe', 'inherit'] });

let buf = Buffer.alloc(0);
let nextId = 1;
const pending = new Map();
const notifications = [];
let failed = 0;

function frame(obj) {
  const body = JSON.stringify(obj);
  return Buffer.from(`Content-Length: ${Buffer.byteLength(body)}\r\n\r\n${body}`, 'utf8');
}

function send(obj) {
  child.stdin.write(frame(obj));
}

function request(method, params) {
  return new Promise((resolve, reject) => {
    const id = nextId++;
    pending.set(id, { resolve, reject });
    send({ jsonrpc: '2.0', id, method, params });
  });
}

function notify(method, params) {
  send({ jsonrpc: '2.0', method, params });
}

child.stdout.on('data', (d) => {
  buf = Buffer.concat([buf, d]);
  for (;;) {
    const h = buf.indexOf('\r\n\r\n');
    if (h < 0) return;
    const header = buf.slice(0, h).toString('utf8');
    const m = /Content-Length:\s*(\d+)/i.exec(header);
    if (!m) {
      buf = Buffer.alloc(0);
      return;
    }
    const len = parseInt(m[1], 10);
    const start = h + 4;
    if (buf.length < start + len) return;
    const body = buf.slice(start, start + len).toString('utf8');
    buf = buf.slice(start + len);
    let msg;
    try {
      msg = JSON.parse(body);
    } catch (e) {
      continue;
    }
    if (msg.id !== undefined && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) reject(new Error(JSON.stringify(msg.error)));
      else resolve(msg.result);
    } else {
      notifications.push(msg);
    }
  }
});

const SAMPLE = `{
  "activity": {
    "background": "#282c34",
    "statusBar": "@editor.lineNumber"
  },
  "editor": {
    "lineNumber": "#5c6370",
    "keyword": "@activity.background",
    "badkey": "#ffffff"
  },
  "widget": {
    "blursize": 2.5,
    "imagepath": "themes/bg.png"
  },
  "material3": {
    "surface": "#121318"
  }
}
`;

function pos(offsets, wanted) {
  let line = 0;
  while (line < offsets.length - 1 && offsets[line + 1] <= wanted) line++;
  return { line, character: wanted - offsets[line] };
}

async function main() {
  const t0 = Date.now();
  const init = await request('initialize', {
    processId: null, rootUri: null, capabilities: {},
  });
  require('assert').deepStrictEqual(
    Object.keys(init.capabilities || {}).sort(),
    ['colorProvider', 'completionProvider', 'definitionProvider', 'documentFormattingProvider', 'documentRangeFormattingProvider', 'hoverProvider', 'positionEncoding', 'textDocumentSync'],
  );
  console.log('  [ok] initialize capabilities');

  notify('initialized', {});
  notify('textDocument/didOpen', {
    textDocument: { uri: 'file:///t.gth', languageId: 'gth', version: 1, text: SAMPLE },
  });

  // Wait for publishDiagnostics
  await new Promise((r) => setTimeout(r, 150));
  const diagNotifs = notifications.filter((n) => n.method === 'textDocument/publishDiagnostics');
  const diags = diagNotifs[diagNotifs.length - 1]?.params?.diagnostics || [];
  const messages = diags.map((d) => d.message);
  console.log(`  [ok] received ${diags.length} diagnostics`);
  require('assert').strictEqual(diags.length, 1, 'expected exactly 1 diagnostic (badkey)');
  const sampleLines = SAMPLE.split('\n');
  const wantDiag = diags[0];
  const lineText = sampleLines[wantDiag.range.start.line];
  require('assert').ok(lineText.includes('badkey'), 'diagnostic must point at the "badkey" line');
  console.log(`  [ok] diagnostic points at key "${'badkey'}" -> ${wantDiag.message}`);

  // hover: position inside "@editor.lineNumber" value (activity/statusBar)
  const statusBarIndex = SAMPLE.indexOf('@editor.lineNumber');
  const offsets = [];
  let o = 0;
  for (const l of SAMPLE.split('\n')) { offsets.push(o); o += l.length + 1; }
  const hover = await request('textDocument/hover', {
    textDocument: { uri: 'file:///t.gth' },
    position: pos(offsets, statusBarIndex + 2),
  });
  require('assert').ok(hover && hover.contents && hover.contents.value.includes('lineNumber'));
  console.log(`  [ok] hover -> ${hover.contents.value.replace(/\n/g, ' | ')}`);

  // definition from "@activity.background" -> editor.keyword key
  const refIndex = SAMPLE.indexOf('@activity.background');
  const def = await request('textDocument/definition', {
    textDocument: { uri: 'file:///t.gth' },
    position: pos(offsets, refIndex + 2),
  });
  require('assert').ok(def && def.uri === 'file:///t.gth');
  const defLine = sampleLines[def.range.start.line];
  require('assert').ok(defLine.includes('background'), `definition must hit background, got: ${defLine.trim()}`);
  console.log(`  [ok] definition -> ${defLine.trim()}`);

  // completion INSIDE a value string: typo "@material3.sur" -> expects surface
  // wrote position right after 's' inside value; we emulate by a didChange edit
  const changed = SAMPLE.replace('"statusBar": "@editor.lineNumber"', '"statusBar": "@material3.sur"');
  notify('textDocument/didChange', {
    textDocument: { uri: 'file:///t.gth', version: 2 },
    contentChanges: [{ text: changed }],
  });
  await new Promise((r) => setTimeout(r, 50));
  const compPos = pos(offsets, SAMPLE.indexOf('@editor.lineNumber') + '@material3.sur'.length);
  const comp = await request('textDocument/completion', {
    textDocument: { uri: 'file:///t.gth' },
    position: compPos,
  });
  const labels = (comp?.items || []).map((i) => i.label);
  require('assert').ok(labels.includes('material3.surface'), `expected material3.surface, got ${labels.join(',')}`);
  console.log(`  [ok] completion (${labels.length} items) includes material3.surface`);

  // key completion: type "acc" inside widget -> acc**...** hints? we test root key too
  const keyChanged = changed.replace('"blursize": 2.5,', '"ac": 2.5,');
  notify('textDocument/didChange', {
    textDocument: { uri: 'file:///t.gth', version: 3 },
    contentChanges: [{ text: keyChanged }],
  });
  await new Promise((r) => setTimeout(r, 50));
  const keyPosIndex = keyChanged.indexOf('"ac"') + 1;
  const keyOffsets = [];
  let ko = 0;
  for (const l of keyChanged.split('\n')) { keyOffsets.push(ko); ko += l.length + 1; }
  const comp2 = await request('textDocument/completion', {
    textDocument: { uri: 'file:///t.gth' },
    position: pos(keyOffsets, keyPosIndex + 1),
  });
  const keyLabels = (comp2?.items || []).map((i) => i.label);
  require('assert').ok(keyLabels.includes('accent'), `expected accent, got ${keyLabels.join(',')}`);
  console.log('  [ok] key completion includes widget.accent');

  // documentColor
  const colors = await request('textDocument/documentColor', {
    textDocument: { uri: 'file:///t.gth' },
  });
  require('assert').ok(Array.isArray(colors) && colors.length >= 4, `expected >=4 colors, got ${colors?.length}`);
  const rendered = await request('textDocument/colorPresentation', {
    textDocument: { uri: 'file:///t.gth' },
    color: colors[0].color,
    range: colors[0].range,
  });
  require('assert').ok(Array.isArray(rendered) && rendered.length >= 2);
  console.log(`  [ok] documentColor (${colors.length} colors) + colorPresentation`);

  // formatting: request an edit that prettifies the (already valid) sample.
  // Indentation is forced to a single space to prove the server reformats.
  const messy = SAMPLE.split('\n')
    .map((l) => (l.trim() ? l.replace(/^(.*:).*$/, (m) => l.replace(/^ +/, ' ')) : ''))
    .join('\n');
  if (!messy.includes('\n ')) {
    console.log('  [!] skip formatting: could not build a messy sample');
  } else {
    notify('textDocument/didChange', {
      textDocument: { uri: 'file:///t.gth', version: 4 },
      contentChanges: [{ text: messy }],
    });
    await new Promise((r) => setTimeout(r, 50));
    const edits = await request('textDocument/formatting', {
      textDocument: { uri: 'file:///t.gth' },
      options: { tabSize: 2, insertSpaces: true },
    });
    require('assert').ok(Array.isArray(edits) && edits.length === 1, 'expected one formatting edit');
    const newText = edits[0].newText;
    require('assert').ok(newText.includes('  "activity"'), 'formatted output must be indented with 2 spaces');
    require('assert').ok(newText.includes('  "material3"'), 'formatted output must keep the material3 section');
    const range = edits[0].range;
    require('assert').ok(range.start.line === 0 && range.start.character === 0, 'format must start at 0,0');
    console.log('  [ok] textDocument/formatting returned a 2-space indented edit');
  }

  await request('shutdown', null);
  notify('exit', null);

  const ms = Date.now() - t0;
  console.log(`\nALL TESTS PASSED in ${ms} ms (${SERVER})`);
  child.stdin.end();
  process.exit(0);
}

main().catch((e) => {
  console.error('TEST FAILED:', e && e.message ? e.message : e);
  console.error(
    'notifications:',
    notifications.map((n) => n.method),
  );
  process.exit(1);
});

setTimeout(() => {
  console.error('TEST TIMEOUT (server did not reply)');
  process.exit(1);
}, 8000).unref();
child.on('exit', () => {});