# Cómo armar el Figma navegable con estos archivos

Las 10 pantallas, los 4 modales y el tablero de componentes ya están dibujados como SVG. Figma los importa como **frames editables**: cada texto queda como capa de texto y cada caja como vector, así que podés cambiar lo que quieras. Lo único que falta hacer a mano es conectar los flujos, que es la parte rápida.

Calculá unos 40 minutos.

---

## Paso 1 — Importar

1. Creá un archivo nuevo en Figma: **New design file**.
2. Abrí la carpeta `docs/preentrega/figma` en el explorador.
3. Seleccioná los **15 archivos .svg** y arrastralos sobre el lienzo de Figma de una vez.
4. Figma crea un frame por archivo, con el nombre del archivo.

Si al soltar los archivos Figma los pone encimados, no importa: en el paso 2 los acomodás.

> **Si algún frame entra como grupo y no como frame:** seleccionalo y usá `Ctrl + Alt + G` (Frame selection). Para prototipar hace falta que sean frames.

---

## Paso 2 — Ordenar y renombrar

Renombrá los frames en el panel de capas con estos nombres (son los que vas a usar en el mapa de navegación):

| Archivo | Nombre del frame |
|---|---|
| `01-seleccion-robot.svg` | `01 Selección de robot` |
| `04b-control-en-espera.svg` | `04b Control — en espera` |
| `02-supervision.svg` | `02 Supervisión` |
| `02b-supervision-enlace-perdido.svg` | `02b Supervisión — sin enlace` |
| `03-mision.svg` | `03 Misión` |
| `03b-mision-finalizada.svg` | `03b Misión — finalizada` |
| `04-control.svg` | `04 Control` |
| `05-alertas.svg` | `05 Alertas` |
| `05b-historial.svg` | `05b Historial` |
| `06-modo-demo.svg` | `06 Modo demo` |
| `07-modal-decision-anomalia.svg` | `M1 Decisión de anomalía` |
| `08-modal-autorizacion.svg` | `M2 Autorización` |
| `08b-modal-autorizacion-rechazada.svg` | `M2b Autorización rechazada` |
| `09-modal-confirmacion.svg` | `M3 Confirmación` |
| `10-componentes-y-estados.svg` | `Componentes y estados` |

Acomodalos en el lienzo en tres filas: flujo normal arriba, flujo con falla en el medio, estados y contingencias abajo. Dejá el tablero de componentes en una página aparte (botón **+** al lado de *Page 1*, llamala `Componentes`).

---

## Paso 3 — Componentes reutilizables

El enunciado pide componentes reutilizables con sus estados. El tablero `Componentes y estados` ya tiene los seis en sus cuatro variantes. Para que cuenten como componentes de Figma:

1. Andá a la página `Componentes`.
2. Seleccioná una celda (por ejemplo el botón azul de la columna NORMAL).
3. `Ctrl + Alt + K` → **Create component**.
4. Repetí con las otras tres celdas de esa fila.
5. Seleccioná los cuatro componentes de la fila → clic derecho → **Combine as variants**.
6. En el panel derecho renombrá la propiedad a `Estado` y los valores a `Normal`, `Deshabilitado`, `Advertencia`, `Error`.

Hacelo al menos con **Botón de comando** y **Chip de estado de orden**, que son los dos que más se repiten. Si te queda tiempo, seguí con los otros cuatro. Con dos componentes bien hechos ya mostrás que entendiste el mecanismo; con seis apurados y rotos, no.

---

## Paso 4 — Flujo normal

Pasá a la pestaña **Prototype** del panel derecho.

Poné el punto de inicio en `01 Selección de robot`: seleccionalo y hacé clic en el ícono de bandera (*Flow starting point*). Renombrá el flujo a **Flujo normal**.

Después conectá, arrastrando el círculo azul desde el elemento de origen hasta el frame de destino:

| # | Desde | Elemento que se toca | Hacia |
|---|---|---|---|
| 1 | `01 Selección de robot` | Botón **Seleccionar** de G1-01 | `04b Control — en espera` |
| 2 | `04b Control — en espera` | Botón **Iniciar ronda de verificación** | `02 Supervisión` |
| 3 | `02 Supervisión` | Pestaña **Misión** de la barra inferior | `03 Misión` |
| 4 | `03 Misión` | Tarjeta **P3 · Sala de UPS** | `03b Misión — finalizada` |
| 5 | `03b Misión — finalizada` | Botón **Ver en el historial** | `05b Historial` |
| 6 | `05b Historial` | Pestaña **Supervisión** | `02 Supervisión` |

En todas usá **Navigate to** con animación **Instant**. La conexión 4 es un atajo de prototipo: representa el paso del tiempo hasta que la ronda termina. Decilo en la defensa, no lo escondas.

---

## Paso 5 — Flujo con falla

Poné un segundo punto de inicio en `02 Supervisión` y renombrá ese flujo a **Flujo con falla**.

| # | Desde | Elemento que se toca | Hacia |
|---|---|---|---|
| 1 | `02 Supervisión` | Banner **Temperatura sobre umbral en P1** | `03 Misión` |
| 2 | `03 Misión` | Tarjeta **P1 · FALLA** | `M1 Decisión de anomalía` |
| 3 | `M1 Decisión de anomalía` | Opción **Aislar: accionar seccionador** | `M2 Autorización` |
| 4 | `M2 Autorización` | Botón **Autorizar** | `M3 Confirmación` |
| 5 | `M2 Autorización` | Primer casillero del código | `M2b Autorización rechazada` |
| 6 | `M2b Autorización rechazada` | Botón **Cancelar** | `M1 Decisión de anomalía` |
| 7 | `M3 Confirmación` | Botón rojo **Cancelar ronda** | `04 Control` |
| 8 | `M1 Decisión de anomalía` | Opción **Marcar y continuar** | `02 Supervisión` |
| 9 | `M1 Decisión de anomalía` | Opción **Abortar y regresar** | `04 Control` |

La conexión 5 es la que muestra el estado de error del componente: el evaluador toca el campo y ve el rechazo. Vale la pena tenerla.

---

## Paso 6 — Contingencias

Estas tres conexiones cubren la desconexión y las alertas, que el enunciado pide como gestión de contingencias:

| Desde | Elemento | Hacia |
|---|---|---|
| `02 Supervisión` | Indicador **Dato actual** de arriba | `02b Supervisión — sin enlace` |
| `02b Supervisión — sin enlace` | Banner **Detención operativa: SIN RESPUESTA** | `05 Alertas` |
| `05 Alertas` | Pestaña **Historial** | `05b Historial` |
| `05b Historial` | Pestaña **Alertas** | `05 Alertas` |
| `02 Supervisión` | Pestaña **Demo** | `06 Modo demo` |
| `06 Modo demo` | Escenario **3 · Desconexión** | `02b Supervisión — sin enlace` |
| `06 Modo demo` | Escenario **4 · Incidente en P1** | `M1 Decisión de anomalía` |
| `06 Modo demo` | Escenario **5 · Orden confirmada…** | `04 Control` |

Las conexiones desde el Modo demo son las más útiles en la defensa: te dejan saltar a cualquier escenario sin recorrer todo el flujo.

---

## Paso 7 — Revisar antes de entregar

- Abrí el **Present** (botón de play arriba a la derecha) y recorré los dos flujos de punta a punta. Fijate que no haya pantalla sin salida.
- Verificá que la barra de navegación inferior funcione en al menos Supervisión, Misión, Control y Alertas: si el evaluador toca una pestaña y no pasa nada, parece roto.
- Compartí con **Anyone with the link → can view**, y pegá el link en el documento de la preentrega.

---

## Qué podés defender de estas pantallas

Son los detalles que no se ven a simple vista pero están puestos a propósito:

- **Cada botón deshabilitado dice por qué.** Un gris sin explicación obliga al operador a adivinar; acá siempre hay una línea con el motivo.
- **El estado de la orden y su ejecución son dos indicadores distintos.** En `04 Control` hay una orden `CONFIRMADA` con «Ejecución NO verificada»: es el escenario propio del grupo y el que responde a la advertencia del enunciado.
- **Durante la desconexión los datos del robot se congelan, pero el estado del enlace se informa al día.** Es lo único que la app mide por sí misma; sin eso el operador no sabría que está mirando datos viejos.
- **El color nunca es el único portador del estado.** Cada variante lleva texto que la nombra.
- **La acción destructiva no queda en la posición del botón por defecto** (modal de confirmación).
- **El modo demo está declarado como herramienta de prueba**, no escondido como si fuera parte del producto.
