const fs = require('fs');
const path = require('path');
const { marked } = require('marked');

const base = process.argv[2];                 // carpeta docs/preentrega
const md = fs.readFileSync(path.join(base, 'preentrega.md'), 'utf8');
const svg = fs.readFileSync(path.join(base, 'esquema-recorrido.svg'), 'utf8');

let cuerpo = marked.parse(md, { mangle: false, headerIds: false });

// La figura del recorrido se incrusta: el PDF tiene que viajar solo.
cuerpo = cuerpo.replace(
  /<img[^>]*src="esquema-recorrido\.svg"[^>]*>/,
  '<div class="esquema">' + svg.replace(/<\?xml[^>]*\?>/, '') + '</div>'
);

const html = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Preentrega — HMI/HRI para verificación de salas técnicas</title>
<style>
  @page { size: A4; margin: 18mm 16mm 18mm 16mm; }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font: 10.5pt/1.55 "Segoe UI", Calibri, Helvetica, Arial, sans-serif;
    color: #1A1D21;
  }
  h1 {
    font-size: 19pt; line-height: 1.25; margin: 0 0 4pt;
    color: #15293F;
  }
  h1 + p { color: #5F6B76; font-size: 10pt; margin: 0 0 4pt; }
  h2 {
    font-size: 13.5pt; margin: 20pt 0 7pt; color: #15293F;
    border-bottom: 1.2pt solid #C9D2DB; padding-bottom: 3pt;
    page-break-after: avoid;
  }
  h3 {
    font-size: 11pt; margin: 13pt 0 4pt; color: #1F3A5F;
    page-break-after: avoid;
  }
  p { margin: 0 0 7pt; text-align: justify; }
  ul, ol { margin: 0 0 8pt; padding-left: 16pt; }
  li { margin-bottom: 3pt; }
  strong { color: #15293F; }
  code {
    font-family: Consolas, "Courier New", monospace;
    font-size: 9pt; background: #EEF1F4; padding: 0.5pt 3pt;
    border-radius: 3px; color: #1F3A5F;
  }
  hr { border: none; border-top: 1pt solid #DDE3E9; margin: 16pt 0; }
  table {
    width: 100%; border-collapse: collapse; margin: 6pt 0 11pt;
    font-size: 9.2pt; page-break-inside: avoid;
  }
  th {
    background: #1F3A5F; color: #fff; text-align: left;
    padding: 5pt 7pt; font-weight: 600; font-size: 9pt;
  }
  td { padding: 5pt 7pt; border-bottom: 0.7pt solid #DDE3E9; vertical-align: top; }
  tr:nth-child(even) td { background: #F6F8FA; }
  .esquema { margin: 10pt 0 6pt; text-align: center; page-break-inside: avoid; }
  .esquema svg { width: 100%; height: auto; }
  em { color: #5F6B76; }
  h2, h3 { break-after: avoid; }
</style>
</head>
<body>
${cuerpo}
</body>
</html>`;

fs.writeFileSync(path.join(base, 'preentrega.html'), html, 'utf8');
console.log('html listo');
