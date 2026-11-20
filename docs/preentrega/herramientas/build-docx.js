const fs = require('fs');
const path = require('path');
const d = require('docx');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  WidthType, HeadingLevel, ImageRun, AlignmentType, BorderStyle, ShadingType
} = d;

const base = process.argv[2];
const lineas = fs.readFileSync(path.join(base, 'preentrega.md'), 'utf8')
  .replace(/\r\n/g, '\n').split('\n');

const AZUL = '15293F';
const AZUL2 = '1F3A5F';
const GRIS = '5F6B76';
const BORDE = 'DDE3E9';

// ---------- formato dentro de una línea ----------
function runs(texto, extra = {}) {
  const out = [];
  // **negrita**, `código`, *cursiva*
  const re = /(\*\*[^*]+\*\*|`[^`]+`|\*[^*]+\*)/g;
  let i = 0, m;
  while ((m = re.exec(texto)) !== null) {
    if (m.index > i) out.push(new TextRun({ text: texto.slice(i, m.index), ...extra }));
    const t = m[0];
    if (t.startsWith('**')) {
      out.push(new TextRun({ text: t.slice(2, -2), bold: true, color: AZUL, ...extra }));
    } else if (t.startsWith('`')) {
      out.push(new TextRun({ text: t.slice(1, -1), font: 'Consolas', size: 18, color: AZUL2, ...extra }));
    } else {
      out.push(new TextRun({ text: t.slice(1, -1), italics: true, ...extra }));
    }
    i = m.index + t.length;
  }
  if (i < texto.length) out.push(new TextRun({ text: texto.slice(i), ...extra }));
  return out.length ? out : [new TextRun({ text: '', ...extra })];
}

function celda(texto, encabezado) {
  return new TableCell({
    width: { size: 100, type: WidthType.AUTO },
    shading: encabezado ? { type: ShadingType.CLEAR, fill: AZUL2 } : undefined,
    margins: { top: 60, bottom: 60, left: 110, right: 110 },
    borders: {
      top: { style: BorderStyle.SINGLE, size: 2, color: BORDE },
      bottom: { style: BorderStyle.SINGLE, size: 2, color: BORDE },
      left: { style: BorderStyle.SINGLE, size: 2, color: BORDE },
      right: { style: BorderStyle.SINGLE, size: 2, color: BORDE }
    },
    children: [new Paragraph({
      spacing: { before: 0, after: 0 },
      children: encabezado
        ? [new TextRun({ text: texto.replace(/\*\*/g, ''), bold: true, color: 'FFFFFF', size: 18 })]
        : runs(texto, { size: 18 })
    })]
  });
}

function filasDeTabla(bloque) {
  const filas = bloque
    .filter(l => !/^\|[\s|:-]+\|$/.test(l.trim()))
    .map(l => l.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(c => c.trim()));
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: filas.map((f, i) => new TableRow({
      tableHeader: i === 0,
      children: f.map(c => celda(c, i === 0))
    }))
  });
}

// ---------- recorrido del documento ----------
const hijos = [];
let instanciaLista = 0;
let enLista = false;

for (let i = 0; i < lineas.length; i++) {
  const l = lineas[i];
  const t = l.trim();

  if (t === '' ) { enLista = false; continue; }
  if (t === '---') { enLista = false; continue; }

  // tabla
  if (t.startsWith('|')) {
    const bloque = [];
    while (i < lineas.length && lineas[i].trim().startsWith('|')) bloque.push(lineas[i]), i++;
    i--;
    hijos.push(filasDeTabla(bloque));
    hijos.push(new Paragraph({ text: '', spacing: { after: 120 } }));
    enLista = false;
    continue;
  }

  // imagen
  const img = t.match(/^!\[.*\]\((.+)\)$/);
  if (img) {
    const png = path.join(base, 'esquema-recorrido.png');
    if (fs.existsSync(png)) {
      hijos.push(new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 160, after: 120 },
        children: [new ImageRun({
          type: 'png',
          data: fs.readFileSync(png),
          transformation: { width: 620, height: 410 }
        })]
      }));
    }
    continue;
  }

  // encabezados
  if (t.startsWith('### ')) {
    hijos.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: runs(t.slice(4)) }));
    continue;
  }
  if (t.startsWith('## ')) {
    hijos.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: runs(t.slice(3)) }));
    continue;
  }
  if (t.startsWith('# ')) {
    hijos.push(new Paragraph({ heading: HeadingLevel.TITLE, children: runs(t.slice(2)) }));
    continue;
  }

  // viñetas
  if (t.startsWith('- ')) {
    hijos.push(new Paragraph({ bullet: { level: 0 }, spacing: { after: 60 }, children: runs(t.slice(2)) }));
    enLista = true;
    continue;
  }

  // lista numerada
  const num = t.match(/^(\d+)\.\s+(.*)$/);
  if (num) {
    if (!enLista) instanciaLista++;
    enLista = true;
    hijos.push(new Paragraph({
      numbering: { reference: 'lista-num', level: 0, instance: instanciaLista },
      spacing: { after: 60 },
      children: runs(num[2])
    }));
    continue;
  }

  // párrafo (el encabezado del documento usa <br>)
  enLista = false;
  for (const parte of t.split(/<br>\s*/)) {
    if (parte === '') continue;
    hijos.push(new Paragraph({
      alignment: AlignmentType.JUSTIFIED,
      spacing: { after: 120 },
      children: runs(parte)
    }));
  }
}

const doc = new Document({
  creator: 'Fabricio Boccardo',
  title: 'Preentrega — HMI/HRI para verificación de salas técnicas con Unitree G1',
  description: 'Desarrollo de Aplicaciones I — UADE',
  numbering: {
    config: Array.from({ length: 12 }, (_, n) => ({
      reference: 'lista-num',
      levels: [{ level: 0, format: 'decimal', text: '%1.', alignment: AlignmentType.START,
                 style: { paragraph: { indent: { left: 460, hanging: 260 } } } }]
    })).slice(0, 1)
  },
  styles: {
    default: {
      document: { run: { font: 'Calibri', size: 21, color: '1A1D21' },
                  paragraph: { spacing: { line: 288 } } }
    },
    paragraphStyles: [
      { id: 'Title', name: 'Title', basedOn: 'Normal', next: 'Normal',
        run: { size: 38, bold: true, color: AZUL },
        paragraph: { spacing: { after: 160 } } },
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal',
        run: { size: 27, bold: true, color: AZUL },
        paragraph: { spacing: { before: 360, after: 140 },
                     border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: 'C9D2DB', space: 4 } } } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal',
        run: { size: 23, bold: true, color: AZUL2 },
        paragraph: { spacing: { before: 240, after: 90 } } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal',
        run: { size: 21, bold: true, color: AZUL2 },
        paragraph: { spacing: { before: 180, after: 80 } } }
    ]
  },
  sections: [{
    properties: { page: { margin: { top: 1020, bottom: 1020, left: 910, right: 910 } } },
    children: hijos
  }]
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync(path.join(base, 'preentrega.docx'), buf);
  console.log('docx listo:', buf.length, 'bytes');
});
