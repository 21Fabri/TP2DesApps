# Interfaz HMI/HRI para verificación previa de salas técnicas con Unitree G1

**TP Grupal — Desarrollo de Aplicaciones I — UADE, Facultad de Ingeniería y Cs. Exactas**
Fecha de diseño: 2026-11-17

---

## 1. Problema y contexto

### El problema

Antes de que la cuadrilla de mantenimiento intervenga un tablero eléctrico o una sala de máquinas del edificio, alguien tiene que entrar primero a verificar las condiciones del recinto: ausencia de tensión, temperatura de barras y borneras, estado de instrumentos, presencia de humo u olor a quemado, y que no haya obstáculos ni daños visibles.

### Cómo se resuelve hoy

Un técnico de mantenimiento entra con EPP a hacer esa verificación a mano. Queda expuesto a:

- **Arco eléctrico**, que es la contingencia grave: puede ser fatal y ocurre en el momento de abrir o accionar.
- Atmósferas calientes o con productos de combustión.
- Espacios confinados con poca ventilación.

La verificación además depende de la atención de una persona al final de un turno, y no deja registro estructurado: queda en una planilla o en la memoria del técnico.

### La propuesta

El Unitree G1 entra primero. Recorre los puntos de verificación de una ronda, toma lecturas, acciona lo que haga falta bajo autorización explícita, y devuelve un informe de condiciones. La cuadrilla entra después, sabiendo qué va a encontrar. Un operador supervisa la ronda a distancia desde la app.

### Justificación frente a no usar robot

El costo del equipo (USD 60.000 aproximados) se compara contra la exposición de una persona a un evento de arco eléctrico, que es un riesgo de consecuencia catastrófica y baja probabilidad. El robot no elimina la necesidad de la cuadrilla: **elimina la exposición en el momento de mayor riesgo**, que es la verificación a ciegas de un recinto cuyo estado se desconoce. Como beneficio secundario, cada ronda deja un registro estructurado y con timestamp que hoy no existe.

### Justificación de G1 frente a Go2

El cuadrúpedo no tiene manos. En este servicio hay que:

- Abrir puertas de tablero y accionar llaves y seccionadores, que son controles diseñados para manos humanas.
- Leer instrumentos analógicos montados a la altura del rostro de una persona de pie.

La altura de 1,32 m del G1 coincide con la franja de montaje de instrumentos y mandos de un tablero. El argumento central es que **el G1 interactúa con infraestructura diseñada para humanos**, que es exactamente lo que un cuadrúpedo o un robot con ruedas no puede hacer.

---

## 2. Plataforma: Unitree G1 EDU

### Datos técnicos

| Característica | Valor |
|---|---|
| Peso | 35 kg |
| Altura | 1,32 m |
| Velocidad máxima | 2 m/s |
| Articulaciones | Más de 35 |
| Capacidad de carga | 5 kg por brazo |
| Autonomía | 2 h 30 min a 3 h según uso |
| Tiempo de carga | 1 h 30 min |
| Costo aproximado | USD 60.000 |

### Capacidades disponibles de fábrica

- **Mapeo 3D por lidar.** Genera un mapa del entorno. Mira hacia abajo en un cono triangular; lo que queda alrededor se representa como mapa de calor.
- **Arreglo de micrófonos.** Varios micrófonos agrupados que permiten ubicar la dirección de origen de un sonido.
- **Sensor inercial de equilibrio.** Giroscopio y acelerómetro que revisan la postura muchas veces por segundo.
- **Manos con sensor en la palma**, con 5 kg de capacidad por brazo.
- **Arquitectura de dos cómputos**: una parte blindada por el fabricante y una parte abierta a programación.
- **Integración con asistentes de IA.** En la demostración observada estaba integrado con Otto, el asistente de UADE.

### Restricciones relevantes para este caso

Estas limitaciones no son defectos a ocultar: son el fundamento del diseño de seguridad de la interfaz.

- **No cuenta con corte físico de emergencia (kill-switch).** Es la restricción que domina todo el diseño.
- **La estabilidad es su punto débil.** Pisa con fuerza y, en modo manual, la teleoperación no consulta sus sensores de entorno.
- **Zona ciega perimetral.** El cono de visión apunta hacia abajo; el entorno lateral y trasero se representa como mapa de calor y no como detección confiable.
- **No actúa por iniciativa propia.** Necesita un objetivo dado. Reconoce patrones aprendidos de datos; no comprende la situación como una persona. Lo que se percibe como personalidad es diseño de interacción, no estado emocional.
- **Autonomía acotada.** Entre 2 h 30 min y 3 h, con 1 h 30 min de recarga, lo que obliga a planificar la ronda y el regreso.

### Capacidades que requerirían equipamiento o desarrollo adicional

| Capacidad necesaria | Estado |
|---|---|
| Medición de temperatura de tableros | Requiere **cámara térmica** adicional; no viene de fábrica |
| Detección de humo, CO o gases | Requiere sensores adicionales |
| Lectura de instrumentos analógicos | Requiere visión por computadora entrenada para este caso |
| Accionamiento de seccionadores | Requiere herramienta específica, validación de torque y certificación eléctrica |
| Corte físico de emergencia | **No existe**; requiere desarrollo o un dispositivo externo |
| Navegación autónoma robusta en sala técnica | El lidar mapea, pero la navegación confiable sin GPS en estos recintos requiere desarrollo |
| Operación en ambiente con polvo o calor elevado | Grado de protección no garantizado para el recinto |

El informe debe declarar explícitamente que la variable de temperatura del servicio depende de equipamiento adicional. Es un dato que juega a favor: muestra que el grupo distinguió lo que el robot hace de lo que haría falta construir.

### Decisión de diseño: sin joystick

**La app no ofrece control manual continuo.** La razón no es simplificar: en modo manual el G1 no consulta sus sensores de entorno, de modo que un joystick remoto operado por alguien sin línea de vista convierte al operador en la única barrera contra una colisión, con 35 kg a 2 m/s y una zona ciega perimetral. Todo el control es por **órdenes discretas**, que es lo que el enunciado permite expresamente.

---

## 3. Entorno y recorrido

### Ubicación

Salas técnicas del edificio de UADE. El esquema del entorno se releva en sitio.

### Ronda de verificación (5 puntos)

| # | Punto | Qué verifica |
|---|---|---|
| P1 | Sala de tablero general | Ausencia de tensión, temperatura de barras, olor a quemado |
| P2 | Sala de bombas y presurización | Estado de bombas, temperatura, pérdidas |
| P3 | Sala de UPS y racks | Temperatura, estado de baterías, ventilación |
| P4 | Sala de máquinas de ascensores | Temperatura, estado de maniobra |
| P5 | Grupo electrógeno | Nivel de combustible, temperatura, estado de arranque |

### Punto seguro

Antesala del pasillo técnico, donde está la base de carga. Es el destino del comando de regreso y del regreso autónomo por pérdida de enlace.

---

## 4. Actores y roles

| Actor | Usa la app | Qué necesita |
|---|---|---|
| **Operador de mantenimiento** | Sí, es el único usuario | Estado del robot y de la ronda, alertas, capacidad de intervenir |
| **Supervisor de mantenimiento** | No directamente | Autoriza las órdenes críticas mediante código; queda registrado nominalmente |
| **Cuadrilla de mantenimiento** | No | El informe de condiciones antes de entrar |
| **Acompañante presencial** | No | Está en el sitio con autoridad para intervenir físicamente y cortar la alimentación de la sala |
| **Terceros** (alumnos, limpieza) | No | Señalización y previsibilidad del movimiento del robot |

### Ubicación del operador

En la oficina de mantenimiento, **sin línea de vista** al robot. La telemetría es su única fuente de verdad. Esa condición es la que justifica el panel de supervisión completo y la que vuelve grave, y no molesta, la pérdida de enlace.

### Cuándo debe intervenir un humano

1. Cuando un punto de verificación da resultado de falla: el robot no decide, se detiene y espera.
2. Cuando una orden crítica requiere autorización de supervisor.
3. Cuando la batería no alcanza para completar la ronda y el regreso.
4. Cuando se detecta presencia en el entorno y el avance queda bloqueado.
5. Cuando una orden de detención no se verifica como ejecutada: ahí interviene el acompañante presencial, porque la app ya no alcanza.

---

## 5. Modelo de misión

### Estructura

La misión es una **ronda ordenada de puntos de verificación**. En cada punto el robot toma lecturas y produce un resultado: `OK`, `OBSERVACION` o `FALLA`. El progreso es puntos completados sobre el total.

### Subproceso de anomalía

Cuando un punto da `FALLA`, el robot se detiene y la app exige una decisión del operador entre tres caminos:

1. **Marcar y continuar** — se registra la falla y la ronda sigue con el punto siguiente.
2. **Aislar** — se solicita accionar el seccionador, lo que exige autorización de supervisor.
3. **Abortar y regresar** — la ronda se cierra como incompleta y el robot vuelve al punto seguro.

Sin decisión del operador el robot no avanza. Este subproceso es el que cubre el requisito de mostrar un flujo normal y otro con falla.

### Consecuencia operativa de una ronda incompleta

Si la ronda no se completa, **la cuadrilla no puede entrar**. El historial registra qué puntos quedaron sin verificar. Esto le da peso real a los comandos de cancelación y de regreso.

### Estados operativos

`EN_ESPERA`, `EN_TRASLADO`, `VERIFICANDO_PUNTO`, `ESPERANDO_DECISION`, `PAUSADO`, `DETENIDO`, `RETORNANDO`, `EN_FALLA`

---

## 6. Telemetría

### Las ocho obligatorias

| Dato | Representación en este caso |
|---|---|
| Conectividad | Estado del enlace (`OK`, `DEGRADADO`, `PERDIDO`) y latencia en ms |
| Batería | Porcentaje y autonomía estimada restante en minutos |
| Estado operativo | Uno de los estados de la sección anterior |
| Misión y progreso | Ronda activa, punto actual, completados sobre total |
| Ubicación | Sala y punto de verificación actual |
| Movimiento | Velocidad en m/s y bandera de caminando |
| Alertas | Lista con severidad (`INFO`, `ADVERTENCIA`, `CRITICA`) y marca de reconocimiento |
| Última actualización | Timestamp y antigüedad en segundos, con marca de dato antiguo |

### Las tres variables del servicio

Criterio de selección: tienen que ser **dinámicas** y estar ancladas en una capacidad o una limitación real del G1, para que cada una se pueda fundamentar en la defensa.

1. **Temperatura máxima detectada en el punto** (°C). Es el dato que justifica la misión. Requiere cámara térmica adicional, lo cual se declara.
2. **Cobertura del entorno y presencias detectadas** (porcentaje del perímetro cubierto por el lidar, y conteo de personas). Anclada en la zona ciega perimetral.
3. **Margen de estabilidad** (inclinación del torso, derivada del sensor inercial). Anclada en que la estabilidad es el punto débil del equipo, lo cual es crítico en salas con bandejas de cables y desniveles.

### Nota de criterio

**La ausencia de tensión confirmada no es telemetría.** Es el dato más importante del servicio, pero es un **resultado por punto** y no un valor que cambia segundo a segundo. Va en el modelo de resultado del punto de verificación. La distinción conviene explicarla en el informe.

---

## 7. Comandos

### Tabla de comandos

| Comando | Condiciones de habilitación | Confirmación | Rechazo típico |
|---|---|---|---|
| **Iniciar misión** | `EN_ESPERA`, enlace `OK`, autonomía ≥ ronda + regreso + 15 % de margen, sin alertas `CRITICA` sin reconocer | Un paso | "Batería insuficiente: ronda 48 min + regreso 9 min; autonomía 41 min" |
| **Pausar** | Misión en curso y no en medio de un accionamiento crítico | Un paso | "No se puede pausar durante el accionamiento del seccionador" |
| **Reanudar** | `PAUSADO`, enlace `OK`, sin presencias en zona ciega | Un paso | "Presencia detectada a 1,8 m en sector no cubierto" |
| **Cancelar** | Misión activa o pausada | **Dos pasos** | — |
| **Regreso a punto seguro** | Robot capaz de caminar y con ruta conocida | Un paso | "Sin localización válida desde la posición actual" |
| **Detención operativa** | **Siempre habilitado, sin condiciones** | Un paso | Nunca se rechaza; puede quedar sin respuesta |
| **Reconocer alertas** | Hay alertas sin reconocer | Un paso | — |
| **Accionar seccionador** (crítico) | Dentro del subproceso, autorización de supervisor vigente (120 s desde su ingreso), enlace `OK` | **Dos pasos** | "Código de supervisor inválido" / "Autorización vencida" / "Enlace degradado" |

### Decisiones de diseño

**La detención operativa es el único comando sin condiciones de habilitación.** Sería absurdo rechazar una parada por batería baja. Conviene dejarlo explícito en el informe.

**Cancelar no es regresar.** Cancelar cierra la ronda como incompleta; regresar mueve al robot a la base. Son dos efectos distintos con dos consecuencias distintas.

**Reconocer una alerta no la resuelve.** Solo registra que el operador la vio. La interfaz las muestra distinto.

### Estados de la orden

Los cuatro que pide el enunciado:

`PENDIENTE` → `CONFIRMADA` | `RECHAZADA` | `SIN_RESPUESTA`

Una orden pasa a `SIN_RESPUESTA` si no llega respuesta en **8 s** desde el envío. No se reintenta sola.

Más un segundo indicador, independiente:

**`ejecucionVerificada`**: booleano, con el criterio de verificación asociado.

El robot puede acusar recibo de una orden de detención y seguir caminando. `CONFIRMADA` significa que el robot recibió la orden; la verificación de ejecución significa que el efecto esperado se observó en la telemetría. Son dos cosas distintas y la interfaz las muestra como dos cosas distintas. El enunciado advierte justamente esto: una orden enviada no equivale a una orden ejecutada.

Criterios de verificación por orden:

| Orden | Criterio de ejecución verificada |
|---|---|
| Detención operativa | `velocidadMs == 0` sostenido durante 2 s |
| Pausar | `estadoOperativo == PAUSADO` |
| Regreso a punto seguro | `estadoOperativo == RETORNANDO` |
| Accionar seccionador | Lectura de ausencia de tensión en el punto |

---

## 8. Los cinco escenarios demostrables

### 1. Misión normal

Inicio, traslado, verificación de los 5 puntos con resultado `OK`, informe generado, regreso al punto seguro, ronda finalizada.

### 2. Batería baja

En el punto 3 la autonomía restante baja del umbral de advertencia (ronda restante + regreso + 15 % de margen). Se emite advertencia con dos acciones disponibles: regresar ahora, o continuar con la ronda reducida a los puntos críticos. La autonomía estimada se recalcula en vivo y **la acción de continuar se deshabilita sola** cuando ya no alcanza para el regreso. El operador ve desaparecer la opción y entiende por qué.

### 3. Desconexión

Al entrar a la sala de tablero general, blindada, se pierde el enlace. La telemetría queda marcada como dato antiguo con la antigüedad en segundos y degradado visual progresivo. Las órdenes enviadas pasan a `SIN_RESPUESTA` y no se reintentan solas. Al recuperarse el enlace llega el backlog de eventos y hay que **reconciliarlo**: el robot informa que completó el punto 1 mientras nadie lo veía, con timestamps anteriores a la reconexión.

La reconciliación es lo que justifica la persistencia en Room y no un arreglo en memoria: hay que absorber eventos cuyo `ts` de origen es anterior al `recibidoTs`.

### 4. Incidente

En el tablero general la temperatura máxima da 78,4 °C. El punto se marca `FALLA` y se dispara el subproceso de anomalía. El operador decide entre marcar y continuar, aislar (con autorización de supervisor) o abortar. Sin decisión, el robot no avanza.

### 5. Escenario propio: orden confirmada que no se ejecuta

El operador envía **detención operativa**. El robot responde `CONFIRMADA`. Pero la telemetría sigue mostrando 1,2 m/s y la ubicación cambiando: el criterio de verificación no se cumple.

La interfaz escala: avisa que la detención por software no surtió efecto, deja de presentar la orden como resuelta e indica el procedimiento de intervención presencial.

Este escenario cierra el círculo entre la restricción real del equipo (no hay corte físico de emergencia), la advertencia del enunciado (una orden enviada no equivale a una orden ejecutada) y la decisión de modelar la ejecución verificada por separado.

---

## 9. Los cinco riesgos

### Riesgo 1 — No hay corte físico de emergencia

El G1 no tiene kill-switch y el operador no tiene línea de vista.

- **Interfaz**: la detención se presenta siempre como *solicitud*, nunca como garantía; escala visualmente si no se verifica el efecto.
- **Lógica**: verificación de ejecución contra la telemetría, con criterio explícito.
- **Procedimiento**: acompañante presencial obligatorio durante toda la ronda, con autoridad para intervenir físicamente y cortar la alimentación de la sala.

### Riesgo 2 — Personas en la zona ciega

El cono de visión apunta hacia abajo; 35 kg a 2 m/s.

- **Interfaz**: mostrar el porcentaje de cobertura y marcar el sector no cubierto, para que "no detecto a nadie" no se lea como "no hay nadie".
- **Lógica**: reanudar y avanzar se bloquean con presencia detectada.
- **Procedimiento**: bloqueo de acceso y señalización de las salas durante la ronda.

### Riesgo 3 — Órdenes accidentales sobre infraestructura eléctrica

Un toque mal dado que acciona un seccionador.

- **Interfaz**: las órdenes críticas separadas visualmente del resto y fuera de la zona de alcance del pulgar.
- **Lógica**: autorización de supervisor con ventana de validez corta, más confirmación de dos pasos.
- **Procedimiento**: registro nominal de quién autorizó cada accionamiento.

### Riesgo 4 — Decidir sobre datos antiguos

Durante la desconexión el operador puede actuar creyendo que ve el presente.

- **Interfaz**: antigüedad en segundos y degradado visual progresivo; nunca el último valor mostrado como si fuera actual.
- **Lógica**: las órdenes quedan en `SIN_RESPUESTA` y no se reintentan automáticamente.
- **Procedimiento**: si el enlace se pierde más de 60 s, el robot ejecuta regreso autónomo al punto seguro sin esperar orden.

### Riesgo 5 — Privacidad

El equipo tiene lidar 3D y un arreglo de micrófonos capaz de ubicar el origen de un sonido, recorriendo pasillos de una facultad.

- **Interfaz**: indicador visible de qué se está registrando en cada momento.
- **Lógica**: no persistir audio; la presencia se guarda como conteo, sin identificación.
- **Procedimiento**: cartelería, ventana horaria sin actividad académica y retención limitada del historial.

---

## 10. Diseño en Figma

### Mapa de pantallas

| Pantalla | Contenido |
|---|---|
| **Selección de robot** | Lista de equipos con su estado. Incluye al menos uno no disponible, para mostrar el estado deshabilitado |
| **Panel de supervisión** | Estado del robot, batería, enlace, ronda en curso con progreso, alertas activas, accesos al resto |
| **Telemetría** | Las ocho obligatorias y las tres del servicio, con última actualización |
| **Misiones** | Rondas disponibles y detalle de la ronda con sus 5 puntos y el resultado de cada uno |
| **Control** | Comandos con su condición de habilitación y el estado de cada orden enviada |
| **Alertas** | Lista con severidad, marca de reconocimiento y acción de reconocer |
| **Historial** | Rondas y eventos persistidos, con los puntos sin verificar de las rondas incompletas |
| **Estado de desconexión** | Tratamiento transversal: degradado del panel con antigüedad del dato, más una vista de reconciliación al recuperar |
| **Decisión de anomalía** | Modal del subproceso con los tres caminos |
| **Autorización de supervisor** | Modal de ingreso de código con su vigencia |
| **Modo demo** | Selección de los cinco escenarios e inyección de fallas sueltas |

### Componentes reutilizables

Tarjeta de telemetría, botón de comando, chip de estado de orden, banner de alerta, indicador de antigüedad del dato, barra de progreso de ronda.

### Estados visuales

Cada componente se entrega en los cuatro estados: **normal, deshabilitado, advertencia y error**.

### Flujos a mostrar

- **Flujo normal**: selección de robot → panel → iniciar ronda → progreso → finalización → historial.
- **Flujo con falla**: ronda en curso → punto con `FALLA` → decisión de anomalía → autorización de supervisor → accionamiento → resultado.

### Nota sobre el modo demo

Es una pantalla que no existiría en la app entregada a un cliente. Se justifica como **herramienta de prueba**: permite reproducir cada escenario a voluntad para generar las evidencias y el video, y permite que en la defensa se dispare cualquier escenario con un toque. Conviene marcarla como tal en el Figma y en el informe.

---

## 11. Arquitectura del APK

### Stack

Kotlin, Jetpack Compose, MVVM, ViewModel, StateFlow, Room. Funciona sin robot, sin servidor y sin internet.

### Estructura de paquetes

```
model/      Robot, Telemetry, Mission, CheckPoint, CheckResult,
            Command, CommandState, Alert, Event
data/       RobotRepository (interface)
            SimulatedRobotRepository (implementación)
            room/ MissionHistoryDao, EventDao, entities
sim/        RobotSimulator — máquina de estados + tick de 500 ms
            FaultInjector — desconexión, batería, temperatura, no ejecución
ui/         screens/ y components/ en Compose
viewmodel/  SupervisionViewModel, MissionViewModel, ControlViewModel,
            HistoryViewModel
```

### Flujo de datos

La UI observa `StateFlow` expuestos por el ViewModel. El ViewModel colecta flows del `RobotRepository`. El Repository envuelve al simulador y a Room. Los eventos de comando viajan en sentido inverso: UI → ViewModel → Repository → simulador.

**La UI nunca accede al simulador.** El `RobotRepository` es la única frontera, lo que permitiría reemplazar la implementación simulada por una real sin tocar la UI ni los ViewModels.

### Simulador

Dirigido por reloj, con tick de 500 ms. Responsabilidades:

- Actualizar batería y telemetría de forma continua y coherente.
- Avanzar la máquina de estados de la misión respetando los estados operativos.
- Emitir alertas y eventos.
- Resolver comandos: evaluar condiciones de habilitación, responder confirmación o rechazo con motivo, y aplicar o no el efecto.
- Permitir forzar desconexiones, rechazos y no ejecución de órdenes, desde el `FaultInjector`.

### Parámetros del simulador

| Parámetro | Valor |
|---|---|
| Tick de simulación | 500 ms |
| Vencimiento de orden sin respuesta | 8 s |
| Dato considerado antiguo | > 3 s desde la última actualización |
| Regreso autónomo por pérdida de enlace | 60 s sin enlace |
| Margen de batería para habilitar inicio | 15 % sobre ronda + regreso |
| Vigencia de la autorización de supervisor | 120 s |
| Verificación de detención | `velocidadMs == 0` sostenido 2 s |
| Umbral de temperatura que marca `FALLA` | 70 °C |
| Distancia de presencia que bloquea el avance | 2,5 m |

Estos valores son del simulador y están centralizados para poder ajustarlos sin tocar la UI.

### Persistencia

Room guarda el historial de rondas y de eventos. El esquema de eventos lleva `ts` de origen y `recibidoTs`, para soportar la reconciliación posterior a una desconexión.

---

## 12. Contrato de mensajes

> **Aclaración**: este contrato es una **propuesta propia del grupo** para la comunicación entre la app y el robot simulado. No es el protocolo oficial de Unitree ni pretende representarlo.

### Telemetría

```json
{
  "tipo": "telemetria",
  "robotId": "G1-01",
  "ts": "2026-11-17T22:14:03Z",
  "enlace": { "estado": "OK", "latenciaMs": 84 },
  "bateria": { "porcentaje": 62, "autonomiaMin": 97 },
  "estadoOperativo": "VERIFICANDO_PUNTO",
  "mision": {
    "id": "RND-004",
    "puntoActual": "P3_UPS",
    "completados": 2,
    "total": 5
  },
  "ubicacion": { "sala": "Sala de UPS y racks", "punto": "P3_UPS" },
  "movimiento": { "velocidadMs": 0.0, "caminando": false },
  "servicio": {
    "temperaturaMaxC": 41.2,
    "entorno": { "coberturaPct": 68, "presencias": 0 },
    "margenEstabilidadPct": 83
  },
  "alertasActivas": 1
}
```

### Comando simple

```json
{
  "tipo": "comando",
  "comandoId": "CMD-7f3a",
  "robotId": "G1-01",
  "orden": "DETENCION_OPERATIVA",
  "ts": "2026-11-17T22:14:07Z",
  "autorizacion": null
}
```

### Comando crítico

```json
{
  "tipo": "comando",
  "comandoId": "CMD-7f3b",
  "robotId": "G1-01",
  "orden": "ACCIONAR_SECCIONADOR",
  "parametros": { "punto": "P1_TABLERO_GENERAL" },
  "ts": "2026-11-17T22:16:40Z",
  "autorizacion": {
    "supervisorId": "SUP-204",
    "vigenciaSeg": 120
  }
}
```

### Respuesta confirmada

```json
{
  "tipo": "respuesta",
  "comandoId": "CMD-7f3a",
  "estado": "CONFIRMADA",
  "ts": "2026-11-17T22:14:08Z",
  "ejecucion": {
    "verificada": false,
    "criterio": "velocidadMs == 0 sostenido 2 s"
  }
}
```

### Respuesta rechazada

```json
{
  "tipo": "respuesta",
  "comandoId": "CMD-7f40",
  "estado": "RECHAZADA",
  "motivo": "BATERIA_INSUFICIENTE",
  "detalle": "Ronda estimada 48 min + regreso 9 min; autonomía 41 min",
  "ts": "2026-11-17T22:18:02Z"
}
```

### Evento con reconciliación

```json
{
  "tipo": "evento",
  "eventoId": "EVT-1182",
  "robotId": "G1-01",
  "ts": "2026-11-17T22:09:55Z",
  "recibidoTs": "2026-11-17T22:11:12Z",
  "clase": "PUNTO_COMPLETADO",
  "datos": {
    "punto": "P1_TABLERO_GENERAL",
    "resultado": "FALLA",
    "temperaturaMaxC": 78.4,
    "ausenciaTensionConfirmada": false
  }
}
```

### Nota sobre `SIN_RESPUESTA`

`SIN_RESPUESTA` no es un mensaje que llegue del robot: es un **estado del lado de la app**, derivado de un vencimiento de plazo sin respuesta. Esa distinción conviene explicarla, porque es el estado que importa durante la desconexión.

---

## 13. Decisiones y limitaciones registradas

| Decisión | Fundamento |
|---|---|
| Sin control manual continuo | En modo manual el G1 no consulta sus sensores de entorno; un joystick sin línea de vista sería la principal fuente de riesgo |
| Un solo rol de usuario en la app | El enunciado no exige multi-rol; la autorización de supervisor se modela como condición simulada, no como pantalla aparte |
| Ejecución verificada separada de la confirmación | El enunciado advierte que una orden enviada no equivale a una orden ejecutada |
| La ausencia de tensión no es telemetría | Es un resultado por punto, no un valor continuo |
| La detención operativa no tiene condiciones de habilitación | Rechazar una parada por otra condición sería inaceptable |
| Pantalla de modo demo visible | Permite reproducir los cinco escenarios de forma repetible para evidencias y defensa; se declara como herramienta de prueba |

### Limitaciones del trabajo

- La medición de temperatura, la detección de gases y la lectura de instrumentos analógicos requieren equipamiento o desarrollo que el G1 no trae de fábrica.
- El accionamiento de seccionadores requeriría validación de torque y certificación eléctrica que están fuera del alcance del trabajo.
- El contrato de mensajes es una propuesta propia y no el protocolo del fabricante.
- No hay validación con hardware físico: todo el comportamiento del robot es simulado.

---

## 14. Fuentes técnicas

- **Fuente primaria**: demostración del Unitree G1 EDU en clase, con notas propias sobre sensores, dimensiones, autonomía, capacidad de carga, estabilidad, zona ciega, arquitectura de cómputo y ausencia de corte físico de emergencia.
- Documentación pública del fabricante para la verificación de las especificaciones declaradas.
- Enunciado del TP, Desarrollo de Aplicaciones I, UADE.

---

## 15. Próximos pasos

1. Relevar en sitio las salas técnicas y dibujar el esquema del recorrido.
2. Armar el documento de preentrega a partir de las secciones 1 a 9 de este diseño.
3. Construir el Figma navegable con los dos flujos.
4. Escribir el plan de implementación del APK.
