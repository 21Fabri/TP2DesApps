# Preentrega

Lo que pide el punto 6.1 del enunciado:

> Documento breve con problema, entorno, actores, misión, elección del robot y
> fuentes técnicas; esquema del recorrido, telemetría, comandos, riesgos y
> Figma navegable (flujo normal y falla).

## Qué entregar

| Archivo | Para qué |
|---|---|
| `preentrega.pdf` | **El documento a subir.** 11 secciones, con el esquema del recorrido incrustado |
| `preentrega.docx` | La misma versión en Word, por si el campus la pide editable |
| `preentrega.md` | La fuente. Si hay que corregir algo, se corrige acá y se regenera |
| `esquema-recorrido.svg` / `.png` | El esquema del entorno y la ronda, por separado |
| `figma/` | Las 10 pantallas, los 4 modales y el tablero de componentes en SVG para importar, más la guía para armar el prototipo |

El link del prototipo ya está en el documento, en la sección 9.4.

## Cómo regenerar el PDF y el Word

Si editás `preentrega.md`, hay que volver a generar los dos archivos. Necesita
Node y Chrome, los dos ya instalados.

```bash
node docs/preentrega/herramientas/build-html.js docs/preentrega
```

```bash
node docs/preentrega/herramientas/build-docx.js docs/preentrega
```

El PDF sale de imprimir el HTML con Chrome en modo headless:

```bash
"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf="docs/preentrega/preentrega.pdf" "docs/preentrega/preentrega.html"
```

Antes de correr los scripts por primera vez hace falta instalar las dos
dependencias dentro de `docs/preentrega/herramientas`:

```bash
npm install marked docx
```

## Qué falta para la entrega final (punto 6.2)

Esto **no** va en la preentrega, pero ya está casi todo hecho en el repositorio:

- Figma definitivo, APK instalable y código fuente → los tres están; al Figma le
  faltan los componentes convertidos en variantes.
- Diagrama de arquitectura, Repository simulado y ejemplos de mensajes →
  están en las secciones 11 y 12 del documento de diseño.
- Evidencias de los cinco escenarios → falta correr la app y capturar.
- Registro de decisiones y limitaciones → sección 13 del documento de diseño.
- Video de demostración → falta.
