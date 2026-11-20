# HMI/HRI para verificación previa de salas técnicas — Unitree G1

TP de Desarrollo de Aplicaciones I — UADE, Facultad de Ingeniería y Cs. Exactas.

## Qué es

App Android de supervisión y operación de un Unitree G1 que recorre las salas
técnicas del edificio verificando condiciones antes de que la cuadrilla de
mantenimiento intervenga un tablero.

El robot es simulado. **La app funciona sin robot, sin servidor y sin internet**,
y no declara ningún permiso.

## Entregas

| Entrega | Dónde está |
|---|---|
| **Preentrega** (6.1) | [`docs/preentrega/`](docs/preentrega/) — documento en PDF y Word, esquema del recorrido y las pantallas en SVG para armar el Figma |
| **Entrega final** (6.2) | Este repositorio: APK, código y tests. Faltan el Figma publicado, las evidencias y el video |

## Cómo correrla

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para correr los tests:

```bash
./gradlew test
```

## Cómo probar los cinco escenarios

La app tiene una pantalla **Modo demo** que no formaría parte de la app real:
es una herramienta de prueba para reproducir cada escenario a voluntad.

1. **Misión normal** — reinicia todo; iniciar la ronda desde Control.
2. **Batería baja** — ronda en curso al 40 %: se pausa sola y avisa, y
   "Reanudar" se deshabilita cuando ya no alcanza para el regreso.
3. **Desconexión** — ronda sin enlace: datos antiguos, órdenes sin respuesta y
   reconciliación de eventos al restablecer.
4. **Incidente** — el primer punto mide 78,4 °C: falla y decisión obligatoria.
5. **Orden confirmada que no se ejecuta** — la detención responde confirmada y
   el robot sigue caminando.

Código de supervisor para las órdenes críticas: **2048**.

El simulador corre a **1 segundo real = 1 minuto simulado**, así que una ronda
completa de 48 minutos tarda unos 48 segundos.

## Arquitectura

```
UI (Compose)  →  RobotViewModel  →  RobotRepository  →  RobotSimulator
                                          ↓
                                    Room (tabla eventos)
```

- La UI observa un único `StateFlow<UiState>` y **nunca** accede al simulador.
- `RobotRepository` es la única frontera: cambiar la implementación simulada por
  una real no obligaría a tocar la UI ni el ViewModel.
- El simulador es **determinista**: no usa `Random` ni el reloj del sistema, por
  lo que los tests son confiables y la demo se comporta siempre igual.
- Room guarda **una sola tabla de eventos**; el historial de rondas se deriva
  por consulta, agrupando por misión.

### Estructura

```
model/      dominio y Parametros con todas las constantes
sim/        Rondas (los 5 puntos) y RobotSimulator
data/       RobotRepository, su implementación simulada, HistorialMapper
data/local/ entidad, DAO y base de Room
viewmodel/  UiState y el único RobotViewModel
ui/         6 pantallas, 3 modales, 6 componentes reutilizables
```

## Decisiones que conviene saber para la defensa

- **No hay joystick.** En modo manual el G1 no consulta sus sensores de entorno,
  así que un control remoto continuo operado sin línea de vista sería la
  principal fuente de riesgo. Todo el control es por órdenes discretas.
- **La detención operativa es el único comando sin condiciones de habilitación.**
  Rechazar una parada por batería baja sería inaceptable.
- **Orden confirmada no es orden ejecutada.** Cada orden lleva, además de su
  estado, un indicador de si su efecto se verificó en la telemetría. El G1 real
  no tiene corte físico de emergencia.
- **Reconocer una alerta no la resuelve.** Sólo registra que el operador la vio.
- **Un solo ViewModel.** La app tiene un único dominio, el estado del robot;
  partirlo en varios obligaría a sincronizar copias del mismo estado.
- **Una sola tabla en Room.** El enunciado pide persistir el historial de
  misiones *o* de eventos; con eventos alcanza y las rondas se deducen.

## Notas técnicas del entorno

El proyecto usa **AGP 9.3.3 con Kotlin 2.2.10**, donde AGP trae Kotlin
integrado. En ese modo KSP no puede registrar sus fuentes generadas por la vía
habitual, así que `gradle.properties` incluye:

```properties
android.disallowKotlinSourceSets=false
```

Es la salida documentada por Android para usar Room con KSP sobre AGP 9.

## Documentos

- Diseño: [`docs/superpowers/specs/2026-11-17-hmi-g1-verificacion-salas-tecnicas-design.md`](docs/superpowers/specs/2026-11-17-hmi-g1-verificacion-salas-tecnicas-design.md)
- Plan de implementación: [`docs/superpowers/plans/2026-11-17-apk-hmi-salas-tecnicas.md`](docs/superpowers/plans/2026-11-17-apk-hmi-salas-tecnicas.md)
- Evidencias: [`docs/evidencias/`](docs/evidencias/)

## Limitaciones declaradas

- La medición de temperatura requiere una cámara térmica que el G1 no trae de
  fábrica. Lo mismo la detección de gases y la lectura de instrumentos
  analógicos.
- El accionamiento de seccionadores requeriría validación de torque y
  certificación eléctrica, fuera del alcance del trabajo.
- El contrato de mensajes es una propuesta propia, **no** el protocolo oficial
  de Unitree.
- No hay validación con hardware físico: todo el comportamiento del robot es
  simulado.
- El prototipo navegable en Figma y el relevamiento en sitio de las salas son
  entregables aparte, no cubiertos por este repositorio.
