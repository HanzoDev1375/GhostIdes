'use strict';

/**
 * Builds a single self-contained dist/ghost-theme-lsp.js from src/ so the
 * server can run on Android without npm install (source + bundle ship side
 * by side in assets). The bundle keeps only the runtime require() for the
 * Node built-in 'child_process' (used by the optional Prettier formatter,
 * which falls back to the built-in JSON printer when Prettier is absent).
 */

const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const pkg = require('../package.json');

const schema = fs.readFileSync(path.join(root, 'src', 'theme-schema.js'), 'utf8');
let server = fs.readFileSync(path.join(root, 'src', 'server.js'), 'utf8');
server = server.replace("require('./theme-schema')", '__schema');
server = server.replace("require('../package.json').version", JSON.stringify(pkg.version));

const bundle = `#!/usr/bin/env node
'use strict';
const __schemaModule = { exports: {} };
(function (module, exports) {
${schema}
})(__schemaModule, __schemaModule.exports);
const __schema = __schemaModule.exports;

${server}
`;

const distDir = path.join(root, 'dist');
fs.mkdirSync(distDir, { recursive: true });
const out = path.join(distDir, 'ghost-theme-lsp.js');
fs.writeFileSync(out, bundle);
console.log(`built ${path.relative(root, out)} (${fs.statSync(out).size} bytes)`);