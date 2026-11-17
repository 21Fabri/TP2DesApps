# APK HMI/HRI Salas Técnicas — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir un APK Android que simule la supervisión y operación de un Unitree G1 haciendo rondas de verificación de salas técnicas, cumpliendo los cinco escenarios demostrables del TP sin hardware, servidor ni internet.

**Architecture:** La UI de Compose observa un único `StateFlow<UiState>` de un único `RobotViewModel`. El ViewModel consume `RobotRepository`, cuya implementación simulada envuelve un `RobotSimulator` determinista y una sola tabla de eventos en Room. El simulador avanza por un bucle de corrutina que llama a `avanzar(TICK_MS)`, pero toda su lógica es sincrónica y se testea sin corrutinas.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), ViewModel + StateFlow, Room, JUnit 4, kotlinx-coroutines-test.

## Global Constraints

- **Paquete base:** `ar.edu.uade.salastecnicas`. Todas las rutas de archivo del plan son relativas a `app/src/main/java/ar/edu/uade/salastecnicas/` salvo que digan `test/`.
- **Idioma del código:** nombres de clases, funciones y propiedades en español, igual que el dominio del TP. Es una decisión deliberada para que el código y la defensa oral usen el mismo vocabulario.
- **El APK funciona sin robot, sin servidor y sin internet.** Ninguna tarea agrega permisos de red ni dependencias de red.
- **La UI nunca accede al simulador.** `RobotRepository` es la única frontera. Si una tarea hace que una pantalla importe algo de `sim/`, está mal.
- **Un solo ViewModel:** `RobotViewModel`. Ninguna tarea crea un segundo ViewModel.
- **Una sola tabla en Room:** `eventos`. El historial de rondas se deriva por consulta, no por una segunda entidad.
- **Sin framework de inyección de dependencias.** El repositorio se crea en la clase `Application` y se entrega por `ViewModelProvider.Factory`.
- **Sin Navigation Compose.** La pantalla activa es un campo `Pantalla` dentro de `UiState` y `AppNavigation` hace un `when` sobre él.
- **El simulador es determinista.** Ninguna tarea introduce `Random`, `System.currentTimeMillis()` ni ninguna otra fuente de no determinismo dentro de `sim/`. El reloj es un `Long` interno que avanza por `avanzar(dtMs)`. Esto es lo que vuelve los tests confiables y la demo repetible.
- **Factor de demo:** `FACTOR_DEMO = 60`, es decir 1 segundo real equivale a 1 minuto simulado. Sin esto una ronda de 48 minutos sería indemostrable.
- **Código explicable:** el trabajo lo desarrolla y lo defiende una sola persona con nivel básico de Kotlin. Preferir un `when` largo y legible antes que una abstracción ingeniosa. Nada de operadores de flows encadenados, delegación por reflexión ni genéricos innecesarios.
- **Cada tarea termina con un commit** cuyo mensaje empieza en minúscula y describe el entregable.

---

## Estructura de archivos

### Producción

| Archivo | Responsabilidad |
|---|---|
| `SalasApp.kt` | `Application`. Crea el repositorio una sola vez |
| `MainActivity.kt` | Monta Compose y el tema |
| `model/Enums.kt` | Todos los enums del dominio |
| `model/Parametros.kt` | Constantes del simulador, centralizadas |
| `model/Robot.kt` | `Robot` |
| `model/Telemetria.kt` | `Telemetria` |
| `model/PuntoVerificacion.kt` | `PuntoVerificacion`, `ResultadoVerificacion` |
| `model/Mision.kt` | `Mision` y sus propiedades derivadas |
| `model/OrdenEnviada.kt` | `OrdenEnviada`, `Habilitacion` |
| `model/Alerta.kt` | `Alerta` |
| `model/Evento.kt` | `Evento` |
| `sim/Rondas.kt` | Datos fijos: los 5 puntos y los 3 robots |
| `sim/RobotSimulator.kt` | Máquina de estados, telemetría, comandos, habilitación |
| `data/RobotRepository.kt` | Interfaz de la frontera |
| `data/SimulatedRobotRepository.kt` | Bucle de corrutina + puente a Room |
| `data/HistorialMapper.kt` | Función pura: eventos a resumen de rondas |
| `data/local/EventoEntity.kt` | Entidad Room |
| `data/local/EventoDao.kt` | DAO |
| `data/local/AppDatabase.kt` | Base de datos |
| `viewmodel/UiState.kt` | Estado único de toda la UI |
| `viewmodel/RobotViewModel.kt` | Único ViewModel + su Factory |
| `ui/AppNavigation.kt` | `when` sobre `Pantalla` |
| `ui/components/*.kt` | 6 componentes reutilizables con sus 4 estados visuales |
| `ui/screens/*.kt` | 6 pantallas |
| `ui/dialogs/*.kt` | 3 modales |

### Tests

| Archivo | Qué cubre |
|---|---|
| `test/sim/AvanceMisionTest.kt` | Progreso de la ronda, telemetría, batería |
| `test/sim/HabilitacionTest.kt` | Condiciones de habilitación y motivos de rechazo |
| `test/sim/EjecucionVerificadaTest.kt` | Confirmada contra ejecutada, escenario 5 |
| `test/sim/DesconexionTest.kt` | Dato antiguo, vencimiento, reconciliación, regreso autónomo |
| `test/sim/AnomaliaTest.kt` | Subproceso, autorización, batería baja |
| `test/data/HistorialMapperTest.kt` | Derivación del historial de rondas |
| `test/viewmodel/RobotViewModelTest.kt` | Mapeo a `UiState` y envío de órdenes |

---

## Task 1: Proyecto base, modelo de dominio y datos de la ronda

**Files:**
- Create: proyecto Android nuevo (ver Step 1)
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Create: `model/Enums.kt`, `model/Parametros.kt`, `model/Robot.kt`, `model/Telemetria.kt`, `model/PuntoVerificacion.kt`, `model/Mision.kt`, `model/OrdenEnviada.kt`, `model/Alerta.kt`, `model/Evento.kt`, `sim/Rondas.kt`
- Test: `test/model/MisionTest.kt`

**Interfaces:**
- Consumes: nada, es la primera tarea.
- Produces: todos los tipos del dominio. Las tareas siguientes usan exactamente estos nombres y tipos. `Mision.completados: Int`, `Mision.total: Int`, `Mision.progresoPct: Int`, `Mision.puntosSinVerificar: List<PuntoVerificacion>`.

- [ ] **Step 1: Crear el proyecto en Android Studio**

En Android Studio: *New Project* → **Empty Activity** (la variante de Compose, no la de Views).

- Name: `Salas Tecnicas`
- Package name: `ar.edu.uade.salastecnicas`
- Language: Kotlin
- Build configuration language: **Kotlin DSL**
- Minimum SDK: **API 26**

Crearlo **dentro de la carpeta del repositorio ya existente**, de modo que `app/` quede al lado de `docs/`. Si Android Studio insiste en crear una subcarpeta, mover el contenido generado a la raíz del repo.

Se usa la plantilla en lugar de escribir el Gradle a mano porque la plantilla fija una combinación de versiones de Kotlin, AGP y Compose que ya es compatible entre sí.

- [ ] **Step 2: Verificar que el proyecto compila antes de tocar nada**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

Si falla acá, el problema es del entorno y no del código. Resolverlo antes de seguir.

- [ ] **Step 3: Agregar las dependencias**

Abrir `gradle/libs.versions.toml` y **leer la versión de Kotlin que generó la plantilla**, en la línea `kotlin = "..."` del bloque `[versions]`. Hace falta para elegir la versión de KSP.

En `[versions]`, agregar:

```toml
room = "2.6.1"
coroutinesTest = "1.8.1"
ksp = "REEMPLAZAR"
```

El valor de `ksp` se arma como `<versionDeKotlin>-1.0.x`. Por ejemplo, si la plantilla generó `kotlin = "2.0.21"`, entonces `ksp = "2.0.21-1.0.25"`. La lista de versiones publicadas está en https://github.com/google/ksp/releases — hay que tomar la que empieza exactamente con la versión de Kotlin del proyecto.

En `[libraries]`, agregar:

```toml
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
```

En `[plugins]`, agregar:

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 4: Aplicar el plugin y las dependencias en el módulo**

En `app/build.gradle.kts`, dentro del bloque `plugins`, agregar como última línea:

```kotlin
alias(libs.plugins.ksp)
```

Dentro del bloque `dependencies`, agregar:

```kotlin
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
implementation(libs.androidx.lifecycle.runtime.compose)
testImplementation(libs.kotlinx.coroutines.test)
```

Si `libs.androidx.lifecycle.runtime.compose` no existe en el catálogo, agregarlo en `[libraries]`:

```toml
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.7" }
```

- [ ] **Step 5: Verificar que sigue compilando**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

Si falla con un error de KSP del tipo "ksp-2.x.x is too old for kotlin-2.y.y", la versión de `ksp` del Step 3 no coincide con la de Kotlin. Corregirla.

- [ ] **Step 6: Escribir los enums**

Create `model/Enums.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

enum class EstadoOperativo {
    EN_ESPERA, EN_TRASLADO, VERIFICANDO_PUNTO, ESPERANDO_DECISION,
    PAUSADO, DETENIDO, RETORNANDO, EN_FALLA
}

enum class EstadoEnlace { OK, DEGRADADO, PERDIDO }

enum class Severidad { INFO, ADVERTENCIA, CRITICA }

enum class ResultadoPunto { OK, OBSERVACION, FALLA }

enum class Orden {
    INICIAR_MISION, PAUSAR, REANUDAR, CANCELAR,
    REGRESO_PUNTO_SEGURO, DETENCION_OPERATIVA,
    RECONOCER_ALERTAS, ACCIONAR_SECCIONADOR
}

enum class EstadoOrden { PENDIENTE, CONFIRMADA, RECHAZADA, SIN_RESPUESTA }

enum class ClaseEvento {
    RONDA_INICIADA, PUNTO_COMPLETADO, ALERTA_EMITIDA,
    ORDEN_ENVIADA, ORDEN_RESUELTA, RONDA_CERRADA,
    ENLACE_PERDIDO, ENLACE_RECUPERADO, DETENCION_NO_EJECUTADA
}

enum class Pantalla {
    SELECCION_ROBOT, SUPERVISION, MISION, CONTROL, ALERTAS_HISTORIAL, MODO_DEMO
}

enum class DecisionAnomalia { MARCAR_Y_CONTINUAR, AISLAR, ABORTAR }

enum class Escenario {
    MISION_NORMAL, BATERIA_BAJA, DESCONEXION, INCIDENTE, ORDEN_NO_EJECUTADA
}
```

- [ ] **Step 7: Escribir los parámetros**

Create `model/Parametros.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

/**
 * Todos los valores que gobiernan el simulador, en un solo lugar,
 * para poder ajustarlos sin tocar la UI.
 */
object Parametros {
    /** Cada tick del bucle de simulación. */
    const val TICK_MS = 500L

    /** 1 segundo real equivale a 1 minuto simulado. Sin esto la demo sería inviable. */
    const val FACTOR_DEMO = 60

    /** Una orden sin respuesta pasa a SIN_RESPUESTA pasado este plazo. */
    const val VENCIMIENTO_ORDEN_MS = 8_000L

    /** Plazo para verificar que el efecto de una orden ocurrió de verdad. */
    const val PLAZO_VERIFICACION_MS = 5_000L

    /** La telemetría se marca como antigua pasado este plazo sin actualizarse. */
    const val DATO_ANTIGUO_MS = 3_000L

    /** Sin enlace por más de este plazo, el robot regresa solo al punto seguro. */
    const val REGRESO_AUTONOMO_MS = 60_000L

    /** Margen de batería exigido para habilitar el inicio de una ronda. */
    const val MARGEN_BATERIA = 0.15

    /** Vigencia de la autorización de supervisor desde su ingreso. */
    const val VIGENCIA_AUTORIZACION_MS = 120_000L

    /** La detención se considera ejecutada si la velocidad se mantiene en 0 este tiempo. */
    const val VERIFICACION_DETENCION_MS = 2_000L

    /** Temperatura que marca un punto como FALLA. */
    const val TEMPERATURA_FALLA_C = 70.0

    /** Una presencia más cerca que esto bloquea el avance. */
    const val DISTANCIA_PRESENCIA_BLOQUEO_M = 2.5

    /** Minutos de traslado entre un punto y el siguiente. */
    const val TRASLADO_MIN = 2

    /** Minutos de regreso desde el último punto al punto seguro. */
    const val REGRESO_MIN = 9

    /** Autonomía total a batería llena, en minutos simulados. */
    const val AUTONOMIA_TOTAL_MIN = 150

    /** Velocidad de marcha, en m/s. */
    const val VELOCIDAD_MARCHA_MS = 1.2

    /** Código que valida la autorización del supervisor. */
    const val CODIGO_SUPERVISOR = "2048"

    /** Duración del accionamiento de un seccionador, en ms simulados de reloj. */
    const val ACCIONAMIENTO_MS = 4_000L
}
```

- [ ] **Step 8: Escribir los data classes del dominio**

Create `model/Robot.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

data class Robot(
    val id: String,
    val modelo: String,
    val disponible: Boolean,
    val motivoNoDisponible: String? = null
)
```

Create `model/PuntoVerificacion.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

data class PuntoVerificacion(
    val id: String,
    val sala: String,
    val queVerifica: String,
    val minutosVerificacion: Int,
    /** Temperatura que mide este punto cuando todo está normal. Fija, para que la demo sea repetible. */
    val temperaturaNormalC: Double
)

data class ResultadoVerificacion(
    val puntoId: String,
    val resultado: ResultadoPunto,
    val temperaturaMaxC: Double,
    val ausenciaTensionConfirmada: Boolean
)
```

Create `model/Mision.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

data class Mision(
    val id: String,
    val nombre: String,
    val puntos: List<PuntoVerificacion>,
    val resultados: List<ResultadoVerificacion> = emptyList(),
    val cerrada: Boolean = false
) {
    val completados: Int get() = resultados.size
    val total: Int get() = puntos.size
    val progresoPct: Int get() = if (total == 0) 0 else completados * 100 / total
    val completa: Boolean get() = completados == total
    val puntosSinVerificar: List<PuntoVerificacion> get() = puntos.drop(completados)
    val puntoActual: PuntoVerificacion? get() = puntos.getOrNull(completados)
}
```

Create `model/Telemetria.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

data class Telemetria(
    val robotId: String,
    val tsMs: Long,
    val enlace: EstadoEnlace,
    val latenciaMs: Int,
    val bateriaPct: Int,
    val autonomiaMin: Int,
    val estadoOperativo: EstadoOperativo,
    val misionId: String?,
    val puntoActualId: String?,
    val puntosCompletados: Int,
    val puntosTotales: Int,
    val sala: String,
    val velocidadMs: Double,
    val caminando: Boolean,
    // Las tres variables propias del servicio
    val temperaturaMaxC: Double,
    val coberturaPct: Int,
    val presencias: Int,
    val distanciaPresenciaM: Double?,
    val margenEstabilidadPct: Int,
    val alertasActivas: Int
)
```

Create `model/OrdenEnviada.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

/**
 * Resultado de evaluar si una orden se puede enviar.
 * Misma función para deshabilitar el botón en la UI y para rechazar en el simulador.
 */
data class Habilitacion(
    val habilitada: Boolean,
    val motivo: String? = null,
    val detalle: String? = null
)

data class OrdenEnviada(
    val comandoId: String,
    val orden: Orden,
    val enviadaTsMs: Long,
    val estado: EstadoOrden,
    val motivoRechazo: String? = null,
    val detalleRechazo: String? = null,
    /** Texto del criterio con el que se verifica que el efecto ocurrió. */
    val criterioEjecucion: String? = null,
    /** Verdadero sólo si el efecto se observó en la telemetría. */
    val ejecucionVerificada: Boolean = false,
    /** Verdadero si venció el plazo de verificación sin que el efecto ocurriera. */
    val ejecucionFallida: Boolean = false
)
```

Create `model/Alerta.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

data class Alerta(
    val id: String,
    val severidad: Severidad,
    val titulo: String,
    val detalle: String,
    val tsMs: Long,
    val reconocida: Boolean = false
)
```

Create `model/Evento.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

data class Evento(
    val id: String,
    val misionId: String?,
    val clase: ClaseEvento,
    /** Momento en que ocurrió, según el reloj del robot. */
    val tsMs: Long,
    /** Momento en que la app lo recibió. Distinto de tsMs si hubo desconexión. */
    val recibidoTsMs: Long,
    val detalle: String
)
```

- [ ] **Step 9: Escribir los datos fijos de la ronda**

Create `sim/Rondas.kt`:

```kotlin
package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.PuntoVerificacion
import ar.edu.uade.salastecnicas.model.Robot

object Rondas {

    const val NOMBRE_RONDA = "Verificación previa de salas técnicas"

    /**
     * Los cinco puntos del recorrido. Los minutos suman 38 y, con 2 minutos de
     * traslado por punto, dan una ronda de 48 minutos simulados.
     */
    val PUNTOS = listOf(
        PuntoVerificacion(
            id = "P1_TABLERO_GENERAL",
            sala = "Sala de tablero general",
            queVerifica = "Ausencia de tensión, temperatura de barras, olor a quemado",
            minutosVerificacion = 8,
            temperaturaNormalC = 38.5
        ),
        PuntoVerificacion(
            id = "P2_BOMBAS",
            sala = "Sala de bombas y presurización",
            queVerifica = "Estado de bombas, temperatura, pérdidas",
            minutosVerificacion = 8,
            temperaturaNormalC = 32.0
        ),
        PuntoVerificacion(
            id = "P3_UPS",
            sala = "Sala de UPS y racks",
            queVerifica = "Temperatura, estado de baterías, ventilación",
            minutosVerificacion = 7,
            temperaturaNormalC = 41.2
        ),
        PuntoVerificacion(
            id = "P4_ASCENSORES",
            sala = "Sala de máquinas de ascensores",
            queVerifica = "Temperatura, estado de maniobra",
            minutosVerificacion = 8,
            temperaturaNormalC = 29.8
        ),
        PuntoVerificacion(
            id = "P5_GRUPO_ELECTROGENO",
            sala = "Grupo electrógeno",
            queVerifica = "Nivel de combustible, temperatura, estado de arranque",
            minutosVerificacion = 7,
            temperaturaNormalC = 27.4
        )
    )

    const val SALA_PUNTO_SEGURO = "Antesala del pasillo técnico"

    /** Tres equipos, dos no disponibles, para poder mostrar el estado deshabilitado. */
    val ROBOTS = listOf(
        Robot(
            id = "G1-01",
            modelo = "Unitree G1 EDU",
            disponible = true
        ),
        Robot(
            id = "G1-02",
            modelo = "Unitree G1 EDU",
            disponible = false,
            motivoNoDisponible = "En carga: 38 %, faltan 55 min"
        ),
        Robot(
            id = "GO2-01",
            modelo = "Unitree Go2 EDU",
            disponible = false,
            motivoNoDisponible = "Fuera de servicio: sin capacidad de accionamiento"
        )
    )
}
```

- [ ] **Step 10: Escribir el test de las propiedades derivadas de Mision**

Create `app/src/test/java/ar/edu/uade/salastecnicas/model/MisionTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.model

import ar.edu.uade.salastecnicas.sim.Rondas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MisionTest {

    private fun misionConResultados(cantidad: Int): Mision {
        val resultados = Rondas.PUNTOS.take(cantidad).map {
            ResultadoVerificacion(it.id, ResultadoPunto.OK, it.temperaturaNormalC, true)
        }
        return Mision("RND-001", Rondas.NOMBRE_RONDA, Rondas.PUNTOS, resultados)
    }

    @Test
    fun `una mision sin resultados esta en cero por ciento`() {
        val mision = misionConResultados(0)
        assertEquals(0, mision.completados)
        assertEquals(5, mision.total)
        assertEquals(0, mision.progresoPct)
        assertFalse(mision.completa)
    }

    @Test
    fun `el progreso se calcula sobre el total de puntos`() {
        assertEquals(40, misionConResultados(2).progresoPct)
    }

    @Test
    fun `una mision con todos los resultados esta completa`() {
        val mision = misionConResultados(5)
        assertTrue(mision.completa)
        assertEquals(100, mision.progresoPct)
        assertTrue(mision.puntosSinVerificar.isEmpty())
    }

    @Test
    fun `los puntos sin verificar son los que siguen al ultimo resultado`() {
        val mision = misionConResultados(3)
        assertEquals(2, mision.puntosSinVerificar.size)
        assertEquals("P4_ASCENSORES", mision.puntosSinVerificar.first().id)
    }

    @Test
    fun `el punto actual es el primero sin resultado`() {
        assertEquals("P3_UPS", misionConResultados(2).puntoActual?.id)
    }
}
```

- [ ] **Step 11: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*MisionTest*"`
Expected: PASS, 5 tests.

Este es el único test del plan que se escribe después del código, porque el código son data classes sin lógica propia. Desde la Task 2 en adelante el test va primero.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "agrega proyecto base, modelo de dominio y datos de la ronda"
```

---

## Task 2: Simulador — avance de la ronda y telemetría

**Files:**
- Create: `sim/RobotSimulator.kt`
- Test: `test/sim/AvanceMisionTest.kt`

**Interfaces:**
- Consumes: todo `model/` y `sim/Rondas.kt` de la Task 1.
- Produces:
  - `class RobotSimulator(robotId: String = "G1-01", puntos: List<PuntoVerificacion> = Rondas.PUNTOS)`
  - `fun avanzar(dtMs: Long)`
  - `fun telemetria(): Telemetria`
  - `fun mision(): Mision?`
  - `fun alertas(): List<Alerta>`
  - `fun ordenes(): List<OrdenEnviada>`
  - `fun drenarEventos(): List<Evento>`
  - `fun relojMs(): Long`
  - `fun iniciarRonda()` — atajo interno que la Task 3 reemplaza por `enviar(Orden.INICIAR_MISION)`

- [ ] **Step 1: Escribir el test del avance de la ronda**

Create `app/src/test/java/ar/edu/uade/salastecnicas/sim/AvanceMisionTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvanceMisionTest {

    /** Avanza el simulador la cantidad de minutos simulados indicada. */
    private fun RobotSimulator.avanzarMinutos(minutos: Int) {
        val ticks = (minutos * 60_000L / Parametros.FACTOR_DEMO) / Parametros.TICK_MS
        repeat(ticks.toInt()) { avanzar(Parametros.TICK_MS) }
    }

    @Test
    fun `el robot arranca en espera y sin mision`() {
        val sim = RobotSimulator()
        assertEquals(EstadoOperativo.EN_ESPERA, sim.telemetria().estadoOperativo)
        assertEquals(null, sim.mision())
        assertEquals(100, sim.telemetria().bateriaPct)
        assertEquals(Rondas.SALA_PUNTO_SEGURO, sim.telemetria().sala)
    }

    @Test
    fun `al iniciar la ronda el robot se traslada al primer punto`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.telemetria().estadoOperativo)
        assertTrue(sim.telemetria().caminando)
        assertEquals(Parametros.VELOCIDAD_MARCHA_MS, sim.telemetria().velocidadMs, 0.001)
        assertNotNull(sim.mision())
        assertEquals(0, sim.mision()!!.completados)
    }

    @Test
    fun `terminado el traslado el robot verifica el punto y deja de caminar`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(Parametros.TRASLADO_MIN)
        assertEquals(EstadoOperativo.VERIFICANDO_PUNTO, sim.telemetria().estadoOperativo)
        assertEquals(0.0, sim.telemetria().velocidadMs, 0.001)
        assertEquals("P1_TABLERO_GENERAL", sim.telemetria().puntoActualId)
        assertEquals("Sala de tablero general", sim.telemetria().sala)
    }

    @Test
    fun `al terminar la verificacion queda un resultado OK con la temperatura del punto`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion)
        val mision = sim.mision()!!
        assertEquals(1, mision.completados)
        val resultado = mision.resultados.first()
        assertEquals("P1_TABLERO_GENERAL", resultado.puntoId)
        assertEquals(ResultadoPunto.OK, resultado.resultado)
        assertEquals(38.5, resultado.temperaturaMaxC, 0.001)
        assertTrue(resultado.ausenciaTensionConfirmada)
    }

    @Test
    fun `la ronda completa los cinco puntos y despues retorna`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        // 38 minutos de verificación + 2 de traslado por cada uno de los 5 puntos
        sim.avanzarMinutos(48)
        assertEquals(5, sim.mision()!!.completados)
        assertEquals(EstadoOperativo.RETORNANDO, sim.telemetria().estadoOperativo)
    }

    @Test
    fun `terminado el regreso la mision queda cerrada y completa`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(48 + Parametros.REGRESO_MIN)
        val mision = sim.mision()!!
        assertTrue(mision.cerrada)
        assertTrue(mision.completa)
        assertEquals(EstadoOperativo.EN_ESPERA, sim.telemetria().estadoOperativo)
        assertEquals(Rondas.SALA_PUNTO_SEGURO, sim.telemetria().sala)
    }

    @Test
    fun `la bateria baja mientras el robot trabaja`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(30)
        // 30 de 150 minutos de autonomía son 20 puntos de batería
        assertEquals(80, sim.telemetria().bateriaPct)
        assertEquals(120, sim.telemetria().autonomiaMin)
    }

    @Test
    fun `la bateria casi no baja mientras el robot espera`() {
        val sim = RobotSimulator()
        sim.avanzarMinutos(30)
        assertEquals(96, sim.telemetria().bateriaPct)
    }

    @Test
    fun `iniciar la ronda registra un evento`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        val eventos = sim.drenarEventos()
        assertEquals(1, eventos.size)
        assertEquals("RONDA_INICIADA", eventos.first().clase.name)
    }

    @Test
    fun `drenar eventos los saca de la cola`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.drenarEventos()
        assertTrue(sim.drenarEventos().isEmpty())
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "*AvanceMisionTest*"`
Expected: FAIL por compilación — `Unresolved reference: RobotSimulator`.

- [ ] **Step 3: Escribir el simulador**

Create `sim/RobotSimulator.kt`:

```kotlin
package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.Alerta
import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.Evento
import ar.edu.uade.salastecnicas.model.Mision
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.PuntoVerificacion
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import ar.edu.uade.salastecnicas.model.ResultadoVerificacion
import ar.edu.uade.salastecnicas.model.Severidad
import ar.edu.uade.salastecnicas.model.Telemetria

/**
 * Simulador del robot. No usa Random ni el reloj del sistema: todo avanza por
 * llamadas a [avanzar], de modo que los tests son deterministas y la demo
 * siempre se comporta igual.
 */
class RobotSimulator(
    private val robotId: String = "G1-01",
    private val puntos: List<PuntoVerificacion> = Rondas.PUNTOS
) {

    // ---- Reloj ----
    private var relojMs = 0L

    // ---- Estado del robot ----
    private var estado = EstadoOperativo.EN_ESPERA
    private var enlace = EstadoEnlace.OK
    private var bateria = 100.0
    private var sala = Rondas.SALA_PUNTO_SEGURO
    private var velocidad = 0.0

    // ---- Progreso de la ronda ----
    private var mision: Mision? = null
    private var minutosEnEtapa = 0.0

    // ---- Variables del servicio ----
    private var temperaturaMaxC = 24.0
    private var coberturaPct = 72
    private var presencias = 0
    private var distanciaPresenciaM: Double? = null
    private var margenEstabilidadPct = 88

    // ---- Colas ----
    private val alertas = mutableListOf<Alerta>()
    private val ordenes = mutableListOf<OrdenEnviada>()
    private val eventos = mutableListOf<Evento>()
    private var contadorIds = 0

    // ---- Última telemetría publicada mientras el enlace estuvo en OK ----
    private var ultimaTelemetria: Telemetria? = null

    // =====================================================================
    // Lectura
    // =====================================================================

    fun relojMs(): Long = relojMs

    fun mision(): Mision? = mision

    fun alertas(): List<Alerta> = alertas.toList()

    fun ordenes(): List<OrdenEnviada> = ordenes.toList()

    /** Devuelve los eventos ya recibidos y los saca de la cola. */
    fun drenarEventos(): List<Evento> {
        val recibidos = eventos.filter { it.recibidoTsMs >= 0 }
        eventos.removeAll(recibidos)
        return recibidos
    }

    /**
     * Si el enlace está perdido devuelve la última telemetría conocida, con su
     * timestamp viejo. Eso es lo que hace que la app muestre datos antiguos.
     */
    fun telemetria(): Telemetria {
        val ultima = ultimaTelemetria
        return if (enlace == EstadoEnlace.PERDIDO && ultima != null) ultima
        else construirTelemetria()
    }

    private fun construirTelemetria() = Telemetria(
        robotId = robotId,
        tsMs = relojMs,
        enlace = enlace,
        latenciaMs = if (enlace == EstadoEnlace.DEGRADADO) 410 else 84,
        bateriaPct = bateria.toInt(),
        autonomiaMin = autonomiaMin(),
        estadoOperativo = estado,
        misionId = mision?.id,
        puntoActualId = mision?.puntoActual?.id,
        puntosCompletados = mision?.completados ?: 0,
        puntosTotales = puntos.size,
        sala = sala,
        velocidadMs = velocidad,
        caminando = velocidad > 0.0,
        temperaturaMaxC = temperaturaMaxC,
        coberturaPct = coberturaPct,
        presencias = presencias,
        distanciaPresenciaM = distanciaPresenciaM,
        margenEstabilidadPct = margenEstabilidadPct,
        alertasActivas = alertas.count { !it.reconocida }
    )

    // =====================================================================
    // Cálculos del servicio
    // =====================================================================

    fun autonomiaMin(): Int = (bateria / 100.0 * Parametros.AUTONOMIA_TOTAL_MIN).toInt()

    /** Minutos que dura la ronda completa, con traslados. */
    fun minutosRonda(): Int =
        puntos.sumOf { it.minutosVerificacion } + puntos.size * Parametros.TRASLADO_MIN

    /** Minutos que faltan de la ronda en curso, con traslados. */
    fun minutosRestantes(): Int {
        val m = mision ?: return minutosRonda()
        val faltan = m.puntosSinVerificar
        return faltan.sumOf { it.minutosVerificacion } + faltan.size * Parametros.TRASLADO_MIN
    }

    /** Minutos exigidos para habilitar el inicio: ronda + regreso + margen. */
    fun minutosNecesarios(): Int =
        ((minutosRonda() + Parametros.REGRESO_MIN) * (1 + Parametros.MARGEN_BATERIA)).toInt()

    fun presenciaBloquea(): Boolean {
        val d = distanciaPresenciaM ?: return false
        return presencias > 0 && d < Parametros.DISTANCIA_PRESENCIA_BLOQUEO_M
    }

    // =====================================================================
    // Avance del tiempo
    // =====================================================================

    fun avanzar(dtMs: Long) {
        relojMs += dtMs
        val minutos = dtMs * Parametros.FACTOR_DEMO / 60_000.0

        consumirBateria(minutos)
        avanzarEtapa(minutos)

        if (enlace != EstadoEnlace.PERDIDO) {
            ultimaTelemetria = construirTelemetria()
        }
    }

    private fun consumirBateria(minutos: Double) {
        val trabajando = estado in listOf(
            EstadoOperativo.EN_TRASLADO,
            EstadoOperativo.VERIFICANDO_PUNTO,
            EstadoOperativo.RETORNANDO
        )
        val factor = if (trabajando) 1.0 else 0.2
        val consumo = minutos / Parametros.AUTONOMIA_TOTAL_MIN * 100.0 * factor
        bateria = (bateria - consumo).coerceAtLeast(0.0)
    }

    private fun avanzarEtapa(minutos: Double) {
        when (estado) {
            EstadoOperativo.EN_TRASLADO -> {
                minutosEnEtapa += minutos
                if (minutosEnEtapa >= Parametros.TRASLADO_MIN) {
                    minutosEnEtapa = 0.0
                    empezarVerificacion()
                }
            }

            EstadoOperativo.VERIFICANDO_PUNTO -> {
                val punto = mision?.puntoActual ?: return
                minutosEnEtapa += minutos
                if (minutosEnEtapa >= punto.minutosVerificacion) {
                    minutosEnEtapa = 0.0
                    terminarVerificacion(punto)
                }
            }

            EstadoOperativo.RETORNANDO -> {
                minutosEnEtapa += minutos
                if (minutosEnEtapa >= Parametros.REGRESO_MIN) {
                    minutosEnEtapa = 0.0
                    llegarAPuntoSeguro()
                }
            }

            else -> Unit
        }
    }

    private fun empezarVerificacion() {
        val punto = mision?.puntoActual ?: return
        estado = EstadoOperativo.VERIFICANDO_PUNTO
        velocidad = 0.0
        sala = punto.sala
        temperaturaMaxC = punto.temperaturaNormalC
    }

    private fun terminarVerificacion(punto: PuntoVerificacion) {
        val resultado = ResultadoVerificacion(
            puntoId = punto.id,
            resultado = ResultadoPunto.OK,
            temperaturaMaxC = temperaturaMaxC,
            ausenciaTensionConfirmada = true
        )
        registrarResultado(resultado)
    }

    /** Agrega el resultado y decide si sigue la ronda o empieza el regreso. */
    private fun registrarResultado(resultado: ResultadoVerificacion) {
        val m = mision ?: return
        mision = m.copy(resultados = m.resultados + resultado)
        registrarEvento(
            ClaseEvento.PUNTO_COMPLETADO,
            "${resultado.puntoId}: ${resultado.resultado} a ${resultado.temperaturaMaxC} °C"
        )
        if (mision!!.completa) empezarRegreso() else empezarTraslado()
    }

    private fun empezarTraslado() {
        estado = EstadoOperativo.EN_TRASLADO
        velocidad = Parametros.VELOCIDAD_MARCHA_MS
        minutosEnEtapa = 0.0
    }

    private fun empezarRegreso() {
        estado = EstadoOperativo.RETORNANDO
        velocidad = Parametros.VELOCIDAD_MARCHA_MS
        minutosEnEtapa = 0.0
    }

    private fun llegarAPuntoSeguro() {
        estado = EstadoOperativo.EN_ESPERA
        velocidad = 0.0
        sala = Rondas.SALA_PUNTO_SEGURO
        temperaturaMaxC = 24.0
        val m = mision
        if (m != null && !m.cerrada) {
            mision = m.copy(cerrada = true)
            registrarEvento(
                ClaseEvento.RONDA_CERRADA,
                if (m.completa) "Ronda completa: ${m.completados} de ${m.total} puntos"
                else "Ronda incompleta: faltaron ${m.puntosSinVerificar.size} puntos"
            )
        }
    }

    // =====================================================================
    // Inicio de ronda
    // =====================================================================

    /**
     * Arranca una ronda sin pasar por la evaluación de habilitación.
     * Lo usa el simulador internamente; la app siempre entra por enviar().
     */
    fun iniciarRonda() {
        contadorIds++
        mision = Mision(
            id = "RND-%03d".format(contadorIds),
            nombre = Rondas.NOMBRE_RONDA,
            puntos = puntos
        )
        empezarTraslado()
        registrarEvento(ClaseEvento.RONDA_INICIADA, Rondas.NOMBRE_RONDA)
    }

    // =====================================================================
    // Alertas y eventos
    // =====================================================================

    fun emitirAlerta(severidad: Severidad, titulo: String, detalle: String) {
        contadorIds++
        alertas.add(
            Alerta(
                id = "ALT-%04d".format(contadorIds),
                severidad = severidad,
                titulo = titulo,
                detalle = detalle,
                tsMs = relojMs
            )
        )
        registrarEvento(ClaseEvento.ALERTA_EMITIDA, "$severidad: $titulo")
    }

    private fun registrarEvento(clase: ClaseEvento, detalle: String) {
        contadorIds++
        eventos.add(
            Evento(
                id = "EVT-%04d".format(contadorIds),
                misionId = mision?.id,
                clase = clase,
                tsMs = relojMs,
                // -1 marca "generado durante la desconexión, todavía no recibido"
                recibidoTsMs = if (enlace == EstadoEnlace.PERDIDO) -1L else relojMs,
                detalle = detalle
            )
        )
    }
}
```

- [ ] **Step 4: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*AvanceMisionTest*"`
Expected: PASS, 10 tests.

Si falla el test de batería en espera, revisar que el factor de consumo en reposo sea `0.2`: 30 minutos a un quinto de consumo son 4 puntos de batería, de 100 a 96.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "agrega simulador con avance de ronda, telemetria y consumo de bateria"
```

---

## Task 3: Simulador — habilitación y respuestas de comando

**Files:**
- Modify: `sim/RobotSimulator.kt`
- Test: `test/sim/HabilitacionTest.kt`

**Interfaces:**
- Consumes: todo lo de la Task 2.
- Produces:
  - `fun habilitacion(orden: Orden, codigoSupervisor: String? = null): Habilitacion`
  - `fun enviar(orden: Orden, codigoSupervisor: String? = null): OrdenEnviada`
  - `fun forzarBateria(pct: Double)`
  - `fun forzarPresencia(distanciaM: Double?)`
  - `fun forzarPerdidaLocalizacion(perdida: Boolean)`

- [ ] **Step 1: Escribir el test de habilitación y rechazos**

Create `app/src/test/java/ar/edu/uade/salastecnicas/sim/HabilitacionTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.Severidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabilitacionTest {

    private fun RobotSimulator.avanzarMinutos(minutos: Int) {
        val ticks = (minutos * 60_000L / Parametros.FACTOR_DEMO) / Parametros.TICK_MS
        repeat(ticks.toInt()) { avanzar(Parametros.TICK_MS) }
    }

    // ---- Iniciar misión ----

    @Test
    fun `iniciar mision esta habilitado con el robot en espera y bateria llena`() {
        val sim = RobotSimulator()
        assertTrue(sim.habilitacion(Orden.INICIAR_MISION).habilitada)
    }

    @Test
    fun `iniciar mision se rechaza si la bateria no alcanza para la ronda y el regreso`() {
        val sim = RobotSimulator()
        // 66 minutos son los necesarios: (48 + 9) * 1,15
        assertEquals(66, sim.minutosNecesarios())
        sim.forzarBateria(43.0) // 64 minutos de autonomía
        val habilitacion = sim.habilitacion(Orden.INICIAR_MISION)
        assertFalse(habilitacion.habilitada)
        assertEquals("BATERIA_INSUFICIENTE", habilitacion.motivo)
        assertTrue(habilitacion.detalle!!.contains("48 min"))
        assertTrue(habilitacion.detalle!!.contains("64 min"))
    }

    @Test
    fun `iniciar mision se rechaza si hay una alerta critica sin reconocer`() {
        val sim = RobotSimulator()
        sim.emitirAlerta(Severidad.CRITICA, "Temperatura fuera de rango", "78,4 °C")
        val habilitacion = sim.habilitacion(Orden.INICIAR_MISION)
        assertFalse(habilitacion.habilitada)
        assertEquals("ALERTA_CRITICA_PENDIENTE", habilitacion.motivo)
    }

    @Test
    fun `reconocer la alerta critica vuelve a habilitar el inicio`() {
        val sim = RobotSimulator()
        sim.emitirAlerta(Severidad.CRITICA, "Temperatura fuera de rango", "78,4 °C")
        sim.enviar(Orden.RECONOCER_ALERTAS)
        assertTrue(sim.habilitacion(Orden.INICIAR_MISION).habilitada)
    }

    @Test
    fun `iniciar mision se rechaza si el robot ya esta en ronda`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        assertEquals("ESTADO_INVALIDO", sim.habilitacion(Orden.INICIAR_MISION).motivo)
    }

    @Test
    fun `enviar iniciar mision devuelve confirmada y arranca la ronda`() {
        val sim = RobotSimulator()
        val orden = sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.telemetria().estadoOperativo)
    }

    @Test
    fun `enviar una orden no habilitada devuelve rechazada con su motivo`() {
        val sim = RobotSimulator()
        sim.forzarBateria(43.0)
        val orden = sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.RECHAZADA, orden.estado)
        assertEquals("BATERIA_INSUFICIENTE", orden.motivoRechazo)
        assertEquals(EstadoOperativo.EN_ESPERA, sim.telemetria().estadoOperativo)
    }

    // ---- Pausar y reanudar ----

    @Test
    fun `pausar detiene al robot y reanudar lo vuelve a poner en marcha`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.PAUSAR).estado)
        assertEquals(EstadoOperativo.PAUSADO, sim.telemetria().estadoOperativo)
        assertEquals(0.0, sim.telemetria().velocidadMs, 0.001)

        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.REANUDAR).estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.telemetria().estadoOperativo)
    }

    @Test
    fun `pausar se rechaza si el robot esta en espera`() {
        val sim = RobotSimulator()
        assertEquals("ESTADO_INVALIDO", sim.habilitacion(Orden.PAUSAR).motivo)
    }

    @Test
    fun `reanudar se rechaza si hay una presencia cerca en zona ciega`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.forzarPresencia(1.8)
        val habilitacion = sim.habilitacion(Orden.REANUDAR)
        assertFalse(habilitacion.habilitada)
        assertEquals("PRESENCIA_DETECTADA", habilitacion.motivo)
        assertTrue(habilitacion.detalle!!.contains("1,8 m"))
    }

    @Test
    fun `al alejarse la presencia reanudar vuelve a habilitarse`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.forzarPresencia(1.8)
        sim.forzarPresencia(null)
        assertTrue(sim.habilitacion(Orden.REANUDAR).habilitada)
    }

    @Test
    fun `reanudar se rechaza si no alcanza la bateria para terminar y regresar`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.forzarBateria(20.0) // 30 minutos, contra 48 + 9 que faltan
        val habilitacion = sim.habilitacion(Orden.REANUDAR)
        assertFalse(habilitacion.habilitada)
        assertEquals("BATERIA_INSUFICIENTE", habilitacion.motivo)
    }

    // ---- Cancelar ----

    @Test
    fun `cancelar cierra la mision como incompleta y deja los puntos sin verificar`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion)
        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.CANCELAR).estado)
        val mision = sim.mision()!!
        assertTrue(mision.cerrada)
        assertFalse(mision.completa)
        assertEquals(4, mision.puntosSinVerificar.size)
    }

    @Test
    fun `cancelar se rechaza si no hay mision activa`() {
        val sim = RobotSimulator()
        assertEquals("SIN_MISION_ACTIVA", sim.habilitacion(Orden.CANCELAR).motivo)
    }

    // ---- Regreso a punto seguro ----

    @Test
    fun `regreso a punto seguro pone al robot en retorno`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.REGRESO_PUNTO_SEGURO).estado)
        assertEquals(EstadoOperativo.RETORNANDO, sim.telemetria().estadoOperativo)
    }

    @Test
    fun `regreso a punto seguro se rechaza sin localizacion valida`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarPerdidaLocalizacion(true)
        val habilitacion = sim.habilitacion(Orden.REGRESO_PUNTO_SEGURO)
        assertFalse(habilitacion.habilitada)
        assertEquals("SIN_LOCALIZACION", habilitacion.motivo)
    }

    // ---- Detención operativa ----

    @Test
    fun `la detencion operativa esta habilitada siempre, sin condiciones`() {
        val sim = RobotSimulator()
        assertTrue(sim.habilitacion(Orden.DETENCION_OPERATIVA).habilitada)

        sim.forzarBateria(1.0)
        sim.emitirAlerta(Severidad.CRITICA, "Falla", "Cualquiera")
        sim.forzarPresencia(0.4)
        sim.forzarPerdidaLocalizacion(true)
        assertTrue(sim.habilitacion(Orden.DETENCION_OPERATIVA).habilitada)
    }

    @Test
    fun `la detencion operativa deja al robot detenido`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOperativo.DETENIDO, sim.telemetria().estadoOperativo)
        assertEquals(0.0, sim.telemetria().velocidadMs, 0.001)
    }

    // ---- Reconocer alertas ----

    @Test
    fun `reconocer alertas se rechaza si no hay ninguna sin reconocer`() {
        val sim = RobotSimulator()
        assertEquals("SIN_ALERTAS", sim.habilitacion(Orden.RECONOCER_ALERTAS).motivo)
    }

    @Test
    fun `reconocer una alerta no la borra, solo la marca`() {
        val sim = RobotSimulator()
        sim.emitirAlerta(Severidad.ADVERTENCIA, "Batería baja", "Autonomía 41 min")
        sim.enviar(Orden.RECONOCER_ALERTAS)
        assertEquals(1, sim.alertas().size)
        assertTrue(sim.alertas().first().reconocida)
        assertEquals(0, sim.telemetria().alertasActivas)
    }

    // ---- Registro ----

    @Test
    fun `cada orden enviada queda en la lista de ordenes`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        assertEquals(2, sim.ordenes().size)
        assertEquals(Orden.PAUSAR, sim.ordenes().last().orden)
    }

    @Test
    fun `una orden confirmada trae el criterio con el que se verifica su ejecucion`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertTrue(orden.criterioEjecucion!!.contains("velocidad"))
    }

    @Test
    fun `el enlace degradado no impide pausar pero si accionar el seccionador`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.DEGRADADO)
        assertTrue(sim.habilitacion(Orden.PAUSAR).habilitada)
        assertEquals("ENLACE_DEGRADADO", sim.habilitacion(Orden.ACCIONAR_SECCIONADOR).motivo)
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "*HabilitacionTest*"`
Expected: FAIL por compilación — `Unresolved reference: habilitacion`, `enviar`, `forzarBateria`, `forzarPresencia`, `forzarPerdidaLocalizacion`, `forzarEnlace`.

`forzarEnlace` se implementa en esta tarea aunque su comportamiento completo llega en la Task 5.

- [ ] **Step 3: Agregar el estado que necesitan los comandos**

Modify `sim/RobotSimulator.kt`. Agregar estos campos junto a los demás, debajo de `private var margenEstabilidadPct = 88`:

```kotlin
    // ---- Estado vinculado a los comandos ----
    private var localizacionValida = true
    private var msAccionamientoRestante = 0L
    private var autorizacionVigenteHastaMs: Long? = null
```

- [ ] **Step 4: Agregar los forzadores de estado**

Modify `sim/RobotSimulator.kt`. Agregar al final de la clase, antes del cierre:

```kotlin
    // =====================================================================
    // Forzadores de estado, usados por el Modo demo y por los tests
    // =====================================================================

    fun forzarBateria(pct: Double) {
        bateria = pct.coerceIn(0.0, 100.0)
        ultimaTelemetria = construirTelemetria()
    }

    fun forzarPresencia(distanciaM: Double?) {
        distanciaPresenciaM = distanciaM
        presencias = if (distanciaM == null) 0 else 1
        coberturaPct = if (distanciaM == null) 72 else 54
        ultimaTelemetria = construirTelemetria()
    }

    fun forzarPerdidaLocalizacion(perdida: Boolean) {
        localizacionValida = !perdida
    }

    fun forzarEnlace(nuevo: EstadoEnlace) {
        val anterior = enlace
        enlace = nuevo
        if (anterior != EstadoEnlace.PERDIDO && nuevo == EstadoEnlace.PERDIDO) {
            registrarEvento(ClaseEvento.ENLACE_PERDIDO, "Enlace perdido")
        }
        if (anterior == EstadoEnlace.PERDIDO && nuevo != EstadoEnlace.PERDIDO) {
            liberarEventosPendientes()
            registrarEvento(ClaseEvento.ENLACE_RECUPERADO, "Enlace recuperado")
            ultimaTelemetria = construirTelemetria()
        }
    }

    /** Al volver el enlace, los eventos generados a ciegas pasan a recibidos. */
    private fun liberarEventosPendientes() {
        for (i in eventos.indices) {
            if (eventos[i].recibidoTsMs < 0) {
                eventos[i] = eventos[i].copy(recibidoTsMs = relojMs)
            }
        }
    }
```

- [ ] **Step 5: Escribir la función de habilitación**

Modify `sim/RobotSimulator.kt`. Agregar antes de los forzadores:

```kotlin
    // =====================================================================
    // Habilitación de comandos
    // =====================================================================

    private fun no(motivo: String, detalle: String) = Habilitacion(false, motivo, detalle)

    /**
     * Única fuente de verdad sobre si una orden se puede enviar.
     * La UI la usa para deshabilitar botones y el simulador para rechazar.
     */
    fun habilitacion(orden: Orden, codigoSupervisor: String? = null): Habilitacion =
        when (orden) {

            // La parada no tiene condiciones: sería inaceptable rechazarla.
            Orden.DETENCION_OPERATIVA -> Habilitacion(true)

            Orden.INICIAR_MISION -> when {
                estado != EstadoOperativo.EN_ESPERA ->
                    no("ESTADO_INVALIDO", "El robot no está en espera")
                enlace != EstadoEnlace.OK ->
                    no("ENLACE_NO_OK", "El enlace no está en OK")
                alertas.any { it.severidad == Severidad.CRITICA && !it.reconocida } ->
                    no("ALERTA_CRITICA_PENDIENTE", "Hay una alerta crítica sin reconocer")
                autonomiaMin() < minutosNecesarios() -> no(
                    "BATERIA_INSUFICIENTE",
                    "Ronda ${minutosRonda()} min + regreso ${Parametros.REGRESO_MIN} min; " +
                        "autonomía ${autonomiaMin()} min"
                )
                else -> Habilitacion(true)
            }

            Orden.PAUSAR -> when {
                estado !in listOf(
                    EstadoOperativo.EN_TRASLADO,
                    EstadoOperativo.VERIFICANDO_PUNTO,
                    EstadoOperativo.RETORNANDO
                ) -> no("ESTADO_INVALIDO", "No hay un avance en curso que pausar")
                msAccionamientoRestante > 0 -> no(
                    "ACCIONAMIENTO_EN_CURSO",
                    "No se puede pausar durante el accionamiento del seccionador"
                )
                else -> Habilitacion(true)
            }

            Orden.REANUDAR -> when {
                estado != EstadoOperativo.PAUSADO ->
                    no("ESTADO_INVALIDO", "El robot no está pausado")
                enlace != EstadoEnlace.OK ->
                    no("ENLACE_NO_OK", "El enlace no está en OK")
                presenciaBloquea() -> no(
                    "PRESENCIA_DETECTADA",
                    "Presencia detectada a ${formatearMetros(distanciaPresenciaM)} " +
                        "en sector no cubierto"
                )
                autonomiaMin() < minutosRestantes() + Parametros.REGRESO_MIN -> no(
                    "BATERIA_INSUFICIENTE",
                    "Faltan ${minutosRestantes()} min + regreso ${Parametros.REGRESO_MIN} min; " +
                        "autonomía ${autonomiaMin()} min"
                )
                else -> Habilitacion(true)
            }

            Orden.CANCELAR -> {
                val m = mision
                if (m == null || m.cerrada) no("SIN_MISION_ACTIVA", "No hay una ronda en curso")
                else Habilitacion(true)
            }

            Orden.REGRESO_PUNTO_SEGURO -> when {
                estado == EstadoOperativo.EN_FALLA ->
                    no("ROBOT_EN_FALLA", "El robot no está en condiciones de caminar")
                !localizacionValida ->
                    no("SIN_LOCALIZACION", "Sin localización válida desde la posición actual")
                estado == EstadoOperativo.EN_ESPERA ->
                    no("YA_EN_PUNTO_SEGURO", "El robot ya está en el punto seguro")
                else -> Habilitacion(true)
            }

            Orden.RECONOCER_ALERTAS ->
                if (alertas.none { !it.reconocida }) no("SIN_ALERTAS", "No hay alertas sin reconocer")
                else Habilitacion(true)

            Orden.ACCIONAR_SECCIONADOR -> when {
                estado != EstadoOperativo.ESPERANDO_DECISION -> no(
                    "FUERA_DE_SUBPROCESO",
                    "Sólo disponible durante el subproceso de anomalía"
                )
                enlace != EstadoEnlace.OK -> no("ENLACE_DEGRADADO", "Enlace degradado")
                codigoSupervisor != Parametros.CODIGO_SUPERVISOR ->
                    no("CODIGO_INVALIDO", "Código de supervisor inválido")
                autorizacionVencida() -> no("AUTORIZACION_VENCIDA", "Autorización vencida")
                else -> Habilitacion(true)
            }
        }

    private fun autorizacionVencida(): Boolean {
        val hasta = autorizacionVigenteHastaMs ?: return false
        return relojMs > hasta
    }

    /** 1.8 se muestra como "1,8 m", con coma decimal. */
    private fun formatearMetros(valor: Double?): String =
        if (valor == null) "—" else "%.1f m".format(valor).replace('.', ',')
```

- [ ] **Step 6: Escribir el envío de órdenes**

Modify `sim/RobotSimulator.kt`. Agregar inmediatamente después de `habilitacion`:

```kotlin
    // =====================================================================
    // Envío de órdenes
    // =====================================================================

    /**
     * Evalúa la habilitación, responde y aplica el efecto.
     * CONFIRMADA significa que el robot recibió la orden, no que la ejecutó.
     */
    fun enviar(orden: Orden, codigoSupervisor: String? = null): OrdenEnviada {
        contadorIds++
        val comandoId = "CMD-%04d".format(contadorIds)
        val habilitacion = habilitacion(orden, codigoSupervisor)

        val enviada = if (!habilitacion.habilitada) {
            OrdenEnviada(
                comandoId = comandoId,
                orden = orden,
                enviadaTsMs = relojMs,
                estado = EstadoOrden.RECHAZADA,
                motivoRechazo = habilitacion.motivo,
                detalleRechazo = habilitacion.detalle
            )
        } else {
            aplicarEfecto(orden)
            OrdenEnviada(
                comandoId = comandoId,
                orden = orden,
                enviadaTsMs = relojMs,
                estado = EstadoOrden.CONFIRMADA,
                criterioEjecucion = criterioDe(orden)
            )
        }

        ordenes.add(enviada)
        registrarEvento(ClaseEvento.ORDEN_ENVIADA, "$orden: ${enviada.estado}")
        return enviada
    }

    private fun aplicarEfecto(orden: Orden) {
        when (orden) {
            Orden.INICIAR_MISION -> iniciarRonda()

            Orden.PAUSAR -> {
                estado = EstadoOperativo.PAUSADO
                velocidad = 0.0
            }

            Orden.REANUDAR -> {
                // Vuelve a la etapa que corresponda según si ya llegó al punto
                estado = if (mision?.puntoActual != null && sala == mision?.puntoActual?.sala)
                    EstadoOperativo.VERIFICANDO_PUNTO else EstadoOperativo.EN_TRASLADO
                velocidad = if (estado == EstadoOperativo.EN_TRASLADO)
                    Parametros.VELOCIDAD_MARCHA_MS else 0.0
            }

            Orden.CANCELAR -> {
                val m = mision ?: return
                mision = m.copy(cerrada = true)
                estado = EstadoOperativo.RETORNANDO
                velocidad = Parametros.VELOCIDAD_MARCHA_MS
                minutosEnEtapa = 0.0
                registrarEvento(
                    ClaseEvento.RONDA_CERRADA,
                    "Ronda cancelada: faltaron ${m.puntosSinVerificar.size} puntos. " +
                        "La cuadrilla no puede ingresar"
                )
            }

            Orden.REGRESO_PUNTO_SEGURO -> {
                estado = EstadoOperativo.RETORNANDO
                velocidad = Parametros.VELOCIDAD_MARCHA_MS
                minutosEnEtapa = 0.0
            }

            Orden.DETENCION_OPERATIVA -> {
                estado = EstadoOperativo.DETENIDO
                velocidad = 0.0
            }

            Orden.RECONOCER_ALERTAS -> {
                for (i in alertas.indices) {
                    if (!alertas[i].reconocida) {
                        alertas[i] = alertas[i].copy(reconocida = true)
                    }
                }
            }

            Orden.ACCIONAR_SECCIONADOR -> {
                msAccionamientoRestante = Parametros.ACCIONAMIENTO_MS
            }
        }
    }

    private fun criterioDe(orden: Orden): String = when (orden) {
        Orden.DETENCION_OPERATIVA ->
            "velocidad igual a 0 sostenida ${Parametros.VERIFICACION_DETENCION_MS / 1000} s"
        Orden.PAUSAR -> "estado operativo igual a PAUSADO"
        Orden.REGRESO_PUNTO_SEGURO -> "estado operativo igual a RETORNANDO"
        Orden.ACCIONAR_SECCIONADOR -> "lectura de ausencia de tensión en el punto"
        Orden.INICIAR_MISION -> "estado operativo igual a EN_TRASLADO"
        Orden.REANUDAR -> "estado operativo distinto de PAUSADO"
        Orden.CANCELAR -> "ronda marcada como cerrada"
        Orden.RECONOCER_ALERTAS -> "ninguna alerta sin reconocer"
    }
```

- [ ] **Step 7: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*HabilitacionTest*"`
Expected: PASS, 21 tests.

Si falla el test del detalle de batería, revisar que `minutosRonda()` dé 48: `8+8+7+8+7 = 38` de verificación más `5 * 2 = 10` de traslado.

- [ ] **Step 8: Correr toda la suite para verificar que nada se rompió**

Run: `./gradlew test`
Expected: PASS, 36 tests.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "agrega habilitacion de comandos y envio de ordenes al simulador"
```

---

## Task 4: Simulador — ejecución verificada y escenario 5

**Files:**
- Modify: `sim/RobotSimulator.kt`
- Test: `test/sim/EjecucionVerificadaTest.kt`

**Interfaces:**
- Consumes: todo lo de la Task 3.
- Produces:
  - `fun forzarDetencionIgnorada(ignorar: Boolean)`
  - Actualización de `ordenes()`: cada `OrdenEnviada` confirmada se actualiza en el lugar con `ejecucionVerificada` o `ejecucionFallida` a medida que avanza el reloj.

Esta es la tarea que materializa la advertencia del enunciado: una orden enviada no equivale a una orden ejecutada.

- [ ] **Step 1: Escribir el test de ejecución verificada**

Create `app/src/test/java/ar/edu/uade/salastecnicas/sim/EjecucionVerificadaTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EjecucionVerificadaTest {

    private fun RobotSimulator.avanzarMs(ms: Long) {
        repeat((ms / Parametros.TICK_MS).toInt()) { avanzar(Parametros.TICK_MS) }
    }

    @Test
    fun `una orden recien confirmada todavia no esta verificada`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
        assertFalse(orden.ejecucionVerificada)
        assertFalse(orden.ejecucionFallida)
    }

    @Test
    fun `la detencion se verifica cuando la velocidad se mantiene en cero`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.VERIFICACION_DETENCION_MS)
        val orden = sim.ordenes().last()
        assertTrue(orden.ejecucionVerificada)
        assertFalse(orden.ejecucionFallida)
    }

    @Test
    fun `con la detencion ignorada el robot confirma pero sigue caminando`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)

        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.telemetria().estadoOperativo)
        assertTrue(sim.telemetria().caminando)
    }

    @Test
    fun `vencido el plazo sin efecto la orden queda marcada como ejecucion fallida`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)

        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)
        val orden = sim.ordenes().last()
        assertFalse(orden.ejecucionVerificada)
        assertTrue(orden.ejecucionFallida)
    }

    @Test
    fun `una detencion no ejecutada emite una alerta critica`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)

        val alerta = sim.alertas().last()
        assertEquals("CRITICA", alerta.severidad.name)
        assertTrue(alerta.titulo.contains("no se ejecutó"))
    }

    @Test
    fun `una detencion no ejecutada deja un evento en el historial`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)

        val eventos = sim.drenarEventos()
        assertTrue(eventos.any { it.clase == ClaseEvento.DETENCION_NO_EJECUTADA })
    }

    @Test
    fun `la alerta critica por detencion no ejecutada se emite una sola vez`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS * 3)

        assertEquals(1, sim.alertas().count { it.severidad.name == "CRITICA" })
    }

    @Test
    fun `al dejar de ignorar la detencion una orden nueva si se verifica`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)

        sim.forzarDetencionIgnorada(false)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.VERIFICACION_DETENCION_MS)
        assertTrue(sim.ordenes().last().ejecucionVerificada)
    }

    @Test
    fun `pausar se verifica en cuanto el estado pasa a pausado`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.avanzarMs(Parametros.TICK_MS)
        assertTrue(sim.ordenes().last().ejecucionVerificada)
    }

    @Test
    fun `una orden rechazada nunca se marca como verificada ni como fallida`() {
        val sim = RobotSimulator()
        sim.forzarBateria(43.0)
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS * 2)
        val orden = sim.ordenes().last()
        assertEquals(EstadoOrden.RECHAZADA, orden.estado)
        assertFalse(orden.ejecucionVerificada)
        assertFalse(orden.ejecucionFallida)
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "*EjecucionVerificadaTest*"`
Expected: FAIL por compilación — `Unresolved reference: forzarDetencionIgnorada`.

- [ ] **Step 3: Agregar el estado de la falla forzada**

Modify `sim/RobotSimulator.kt`. Agregar junto a los otros campos de comandos:

```kotlin
    private var detencionIgnorada = false
    private var msVelocidadCero = 0L
```

Y en el bloque de forzadores:

```kotlin
    /** Escenario 5: el robot confirma la detención pero no la ejecuta. */
    fun forzarDetencionIgnorada(ignorar: Boolean) {
        detencionIgnorada = ignorar
    }
```

- [ ] **Step 4: Hacer que la detención ignorada no aplique el efecto**

Modify `sim/RobotSimulator.kt`. En `aplicarEfecto`, reemplazar la rama de `DETENCION_OPERATIVA` por:

```kotlin
            Orden.DETENCION_OPERATIVA -> {
                // Si la falla está forzada, el robot responde pero no se detiene.
                if (!detencionIgnorada) {
                    estado = EstadoOperativo.DETENIDO
                    velocidad = 0.0
                }
            }
```

- [ ] **Step 5: Escribir la verificación de ejecución**

Modify `sim/RobotSimulator.kt`. Agregar la llamada dentro de `avanzar`, inmediatamente después de `avanzarEtapa(minutos)`:

```kotlin
        avanzarAccionamiento(dtMs)
        verificarEjecuciones(dtMs)
```

Y agregar estos métodos en la sección de avance del tiempo:

```kotlin
    private fun avanzarAccionamiento(dtMs: Long) {
        if (msAccionamientoRestante > 0) {
            msAccionamientoRestante = (msAccionamientoRestante - dtMs).coerceAtLeast(0L)
        }
    }

    /**
     * Revisa, orden por orden, si su criterio de ejecución se cumplió.
     * Una orden confirmada que no produce su efecto en el plazo queda
     * marcada como ejecución fallida. Esta es la diferencia entre que el
     * robot acuse recibo y que el robot obedezca.
     */
    private fun verificarEjecuciones(dtMs: Long) {
        msVelocidadCero = if (velocidad == 0.0) msVelocidadCero + dtMs else 0L

        for (i in ordenes.indices) {
            val orden = ordenes[i]
            if (orden.estado != EstadoOrden.CONFIRMADA) continue
            if (orden.ejecucionVerificada || orden.ejecucionFallida) continue

            if (criterioCumplido(orden.orden)) {
                ordenes[i] = orden.copy(ejecucionVerificada = true)
                continue
            }

            val transcurrido = relojMs - orden.enviadaTsMs
            if (transcurrido > Parametros.PLAZO_VERIFICACION_MS) {
                ordenes[i] = orden.copy(ejecucionFallida = true)
                alDetectarEjecucionFallida(orden)
            }
        }
    }

    private fun criterioCumplido(orden: Orden): Boolean = when (orden) {
        Orden.DETENCION_OPERATIVA ->
            velocidad == 0.0 && msVelocidadCero >= Parametros.VERIFICACION_DETENCION_MS
        Orden.PAUSAR -> estado == EstadoOperativo.PAUSADO
        Orden.REGRESO_PUNTO_SEGURO ->
            estado == EstadoOperativo.RETORNANDO || estado == EstadoOperativo.EN_ESPERA
        Orden.ACCIONAR_SECCIONADOR -> msAccionamientoRestante == 0L
        Orden.INICIAR_MISION -> mision != null
        Orden.REANUDAR -> estado != EstadoOperativo.PAUSADO
        Orden.CANCELAR -> mision?.cerrada == true
        Orden.RECONOCER_ALERTAS -> alertas.none { !it.reconocida }
    }

    private fun alDetectarEjecucionFallida(orden: OrdenEnviada) {
        if (orden.orden != Orden.DETENCION_OPERATIVA) return
        registrarEvento(
            ClaseEvento.DETENCION_NO_EJECUTADA,
            "La orden ${orden.comandoId} fue confirmada pero el robot sigue en movimiento"
        )
        emitirAlerta(
            Severidad.CRITICA,
            "La detención no se ejecutó",
            "El robot confirmó la orden y sigue en movimiento. " +
                "La detención por software no reemplaza una parada física: " +
                "recurrir al acompañante presencial."
        )
    }
```

- [ ] **Step 6: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*EjecucionVerificadaTest*"`
Expected: PASS, 10 tests.

Si el test de "una sola vez" falla con 2 o más alertas, es que `ejecucionFallida` no se está persistiendo en la lista; revisar que el `for` escriba con `ordenes[i] = ...` y no sobre una copia local.

- [ ] **Step 7: Correr toda la suite**

Run: `./gradlew test`
Expected: PASS, 46 tests.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "distingue orden confirmada de ejecucion verificada y agrega el escenario 5"
```

---

## Task 5: Simulador — desconexión, vencimiento y reconciliación

**Files:**
- Modify: `sim/RobotSimulator.kt`
- Test: `test/sim/DesconexionTest.kt`

**Interfaces:**
- Consumes: todo lo de la Task 4.
- Produces:
  - `fun antiguedadMs(): Long` — cuánto hace que la telemetría no se actualiza.
  - `fun datoAntiguo(): Boolean`
  - Órdenes enviadas sin enlace quedan en `PENDIENTE` y pasan a `SIN_RESPUESTA` vencido el plazo.
  - Regreso autónomo al punto seguro tras `REGRESO_AUTONOMO_MS` sin enlace.

- [ ] **Step 1: Escribir el test de desconexión**

Create `app/src/test/java/ar/edu/uade/salastecnicas/sim/DesconexionTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesconexionTest {

    private fun RobotSimulator.avanzarMs(ms: Long) {
        repeat((ms / Parametros.TICK_MS).toInt()) { avanzar(Parametros.TICK_MS) }
    }

    @Test
    fun `con enlace OK el dato no es antiguo`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMs(5_000)
        assertFalse(sim.datoAntiguo())
        assertEquals(0L, sim.antiguedadMs())
    }

    @Test
    fun `perdido el enlace la telemetria deja de actualizarse`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMs(2_000)
        val tsAntes = sim.telemetria().tsMs

        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(6_000)

        assertEquals(tsAntes, sim.telemetria().tsMs)
        assertTrue(sim.antiguedadMs() >= 6_000)
        assertTrue(sim.datoAntiguo())
    }

    @Test
    fun `el robot sigue trabajando aunque la app no lo vea`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        // Traslado más verificación del primer punto, a ciegas
        sim.avanzarMs((Parametros.TRASLADO_MIN + 8) * 1_000L)
        assertEquals(1, sim.mision()!!.completados)
    }

    @Test
    fun `una orden enviada sin enlace queda pendiente y no aplica efecto`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)

        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOrden.PENDIENTE, orden.estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.telemetria().estadoOperativo)
    }

    @Test
    fun `una orden pendiente pasa a sin respuesta al vencer el plazo`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.enviar(Orden.DETENCION_OPERATIVA)

        sim.avanzarMs(Parametros.VENCIMIENTO_ORDEN_MS + Parametros.TICK_MS)
        assertEquals(EstadoOrden.SIN_RESPUESTA, sim.ordenes().last().estado)
    }

    @Test
    fun `una orden sin respuesta no se reintenta sola al volver el enlace`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.VENCIMIENTO_ORDEN_MS + Parametros.TICK_MS)

        sim.forzarEnlace(EstadoEnlace.OK)
        sim.avanzarMs(2_000)

        assertEquals(EstadoOrden.SIN_RESPUESTA, sim.ordenes().last().estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.telemetria().estadoOperativo)
    }

    @Test
    fun `los eventos generados a ciegas no se entregan hasta que vuelve el enlace`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.drenarEventos()

        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs((Parametros.TRASLADO_MIN + 8) * 1_000L)
        assertTrue(sim.drenarEventos().isEmpty())

        sim.forzarEnlace(EstadoEnlace.OK)
        val eventos = sim.drenarEventos()
        assertTrue(eventos.any { it.clase == ClaseEvento.PUNTO_COMPLETADO })
    }

    @Test
    fun `un evento reconciliado tiene el momento de origen antes del de recepcion`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.drenarEventos()

        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs((Parametros.TRASLADO_MIN + 8) * 1_000L)
        sim.forzarEnlace(EstadoEnlace.OK)

        val evento = sim.drenarEventos().first { it.clase == ClaseEvento.PUNTO_COMPLETADO }
        assertTrue(evento.tsMs < evento.recibidoTsMs)
    }

    @Test
    fun `sin enlace por mas de un minuto el robot regresa solo al punto seguro`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)

        sim.avanzarMs(Parametros.REGRESO_AUTONOMO_MS - 1_000)
        assertFalse(sim.estadoReal() == EstadoOperativo.RETORNANDO)

        sim.avanzarMs(2_000)
        assertEquals(EstadoOperativo.RETORNANDO, sim.estadoReal())
    }

    @Test
    fun `el regreso autonomo se decide una sola vez`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(Parametros.REGRESO_AUTONOMO_MS + 5_000)
        sim.forzarEnlace(EstadoEnlace.OK)

        val eventos = sim.drenarEventos()
        assertEquals(1, eventos.count { it.detalle.contains("Regreso autónomo") })
    }

    @Test
    fun `al recuperar el enlace la telemetria vuelve a actualizarse`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(6_000)
        sim.forzarEnlace(EstadoEnlace.OK)
        sim.avanzarMs(Parametros.TICK_MS)

        assertFalse(sim.datoAntiguo())
        assertEquals(EstadoEnlace.OK, sim.telemetria().enlace)
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "*DesconexionTest*"`
Expected: FAIL por compilación — `Unresolved reference: datoAntiguo`, `antiguedadMs`, `estadoReal`.

- [ ] **Step 3: Exponer la antigüedad del dato y el estado real**

Modify `sim/RobotSimulator.kt`. Agregar en la sección de lectura, después de `fun telemetria()`:

```kotlin
    /**
     * Cuánto hace que la telemetría no se actualiza. Con el enlace en OK es 0.
     * Es el número que la app muestra como "hace X s".
     */
    fun antiguedadMs(): Long = relojMs - telemetria().tsMs

    fun datoAntiguo(): Boolean = antiguedadMs() > Parametros.DATO_ANTIGUO_MS

    /**
     * Estado verdadero del robot, incluso si la app no lo está viendo.
     * La UI nunca lo usa: es sólo para los tests y para el Modo demo.
     */
    fun estadoReal(): EstadoOperativo = estado
```

- [ ] **Step 4: Agregar el estado del enlace perdido**

Modify `sim/RobotSimulator.kt`. Agregar junto a los demás campos:

```kotlin
    private var msSinEnlace = 0L
    private var regresoAutonomoDecidido = false
```

Y en `forzarEnlace`, dentro del bloque de recuperación, reiniciar el contador agregando estas dos líneas después de `liberarEventosPendientes()`:

```kotlin
            msSinEnlace = 0L
            regresoAutonomoDecidido = false
```

- [ ] **Step 5: Hacer que las órdenes sin enlace queden pendientes**

Modify `sim/RobotSimulator.kt`. En `enviar`, reemplazar el cálculo de `enviada` por:

```kotlin
        val enviada = when {
            // Sin enlace la orden sale pero nadie la contesta.
            enlace == EstadoEnlace.PERDIDO -> OrdenEnviada(
                comandoId = comandoId,
                orden = orden,
                enviadaTsMs = relojMs,
                estado = EstadoOrden.PENDIENTE,
                criterioEjecucion = criterioDe(orden)
            )

            !habilitacion.habilitada -> OrdenEnviada(
                comandoId = comandoId,
                orden = orden,
                enviadaTsMs = relojMs,
                estado = EstadoOrden.RECHAZADA,
                motivoRechazo = habilitacion.motivo,
                detalleRechazo = habilitacion.detalle
            )

            else -> {
                aplicarEfecto(orden)
                OrdenEnviada(
                    comandoId = comandoId,
                    orden = orden,
                    enviadaTsMs = relojMs,
                    estado = EstadoOrden.CONFIRMADA,
                    criterioEjecucion = criterioDe(orden)
                )
            }
        }
```

- [ ] **Step 6: Escribir el vencimiento de órdenes y el regreso autónomo**

Modify `sim/RobotSimulator.kt`. Agregar la llamada en `avanzar`, después de `verificarEjecuciones(dtMs)`:

```kotlin
        vencerOrdenes()
        controlarEnlacePerdido(dtMs)
```

Y agregar los métodos:

```kotlin
    /** Una orden sin respuesta no se reintenta: queda registrada como tal. */
    private fun vencerOrdenes() {
        for (i in ordenes.indices) {
            val orden = ordenes[i]
            if (orden.estado != EstadoOrden.PENDIENTE) continue
            if (relojMs - orden.enviadaTsMs > Parametros.VENCIMIENTO_ORDEN_MS) {
                ordenes[i] = orden.copy(estado = EstadoOrden.SIN_RESPUESTA)
                registrarEvento(
                    ClaseEvento.ORDEN_RESUELTA,
                    "${orden.orden}: sin respuesta tras " +
                        "${Parametros.VENCIMIENTO_ORDEN_MS / 1000} s"
                )
            }
        }
    }

    /**
     * Medida de procedimiento del riesgo 4: pasado un minuto sin enlace, el
     * robot no se queda esperando órdenes que no llegan, vuelve al punto seguro.
     */
    private fun controlarEnlacePerdido(dtMs: Long) {
        if (enlace != EstadoEnlace.PERDIDO) {
            msSinEnlace = 0L
            return
        }
        msSinEnlace += dtMs
        if (msSinEnlace < Parametros.REGRESO_AUTONOMO_MS) return
        if (regresoAutonomoDecidido) return

        regresoAutonomoDecidido = true
        val puedeVolver = estado !in listOf(
            EstadoOperativo.EN_ESPERA,
            EstadoOperativo.RETORNANDO,
            EstadoOperativo.EN_FALLA
        )
        if (puedeVolver) {
            estado = EstadoOperativo.RETORNANDO
            velocidad = Parametros.VELOCIDAD_MARCHA_MS
            minutosEnEtapa = 0.0
        }
        registrarEvento(
            ClaseEvento.ORDEN_RESUELTA,
            "Regreso autónomo al punto seguro por pérdida de enlace de " +
                "${Parametros.REGRESO_AUTONOMO_MS / 1000} s"
        )
    }
```

- [ ] **Step 7: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*DesconexionTest*"`
Expected: PASS, 11 tests.

Si falla "el robot sigue trabajando aunque la app no lo vea", revisar que `avanzarEtapa` se llame siempre en `avanzar`, sin condicionarla al estado del enlace. El punto del escenario es justamente que el robot no se detiene porque la app deje de verlo.

- [ ] **Step 8: Correr toda la suite**

Run: `./gradlew test`
Expected: PASS, 57 tests.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "agrega desconexion, vencimiento de ordenes, reconciliacion y regreso autonomo"
```

---

## Task 6: Simulador — anomalía, autorización y batería baja

**Files:**
- Modify: `sim/RobotSimulator.kt`
- Test: `test/sim/AnomaliaTest.kt`

**Interfaces:**
- Consumes: todo lo de la Task 5.
- Produces:
  - `fun forzarTemperaturaAlta(puntoId: String, temperaturaC: Double)`
  - `fun ingresarAutorizacion(codigo: String): Boolean`
  - `fun decidir(decision: DecisionAnomalia)`
  - `fun esperandoDecision(): Boolean`
  - La alerta de batería baja pausa la ronda y se emite una sola vez.

- [ ] **Step 1: Escribir el test de anomalía, autorización y batería baja**

Create `app/src/test/java/ar/edu/uade/salastecnicas/sim/AnomaliaTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import ar.edu.uade.salastecnicas.model.Severidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomaliaTest {

    private fun RobotSimulator.avanzarMinutos(minutos: Int) {
        val ticks = (minutos * 60_000L / Parametros.FACTOR_DEMO) / Parametros.TICK_MS
        repeat(ticks.toInt()) { avanzar(Parametros.TICK_MS) }
    }

    /** Arranca una ronda con el primer punto en falla por temperatura. */
    private fun simConAnomaliaEnP1(): RobotSimulator {
        val sim = RobotSimulator()
        sim.forzarTemperaturaAlta("P1_TABLERO_GENERAL", 78.4)
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion)
        return sim
    }

    // ---- Subproceso de anomalía ----

    @Test
    fun `un punto por encima del umbral queda en falla y detiene la ronda`() {
        val sim = simConAnomaliaEnP1()
        assertEquals(EstadoOperativo.ESPERANDO_DECISION, sim.estadoReal())
        assertTrue(sim.esperandoDecision())
        val resultado = sim.mision()!!.resultados.first()
        assertEquals(ResultadoPunto.FALLA, resultado.resultado)
        assertEquals(78.4, resultado.temperaturaMaxC, 0.001)
        assertFalse(resultado.ausenciaTensionConfirmada)
    }

    @Test
    fun `la falla emite una alerta critica`() {
        val sim = simConAnomaliaEnP1()
        val alerta = sim.alertas().last()
        assertEquals(Severidad.CRITICA, alerta.severidad)
        assertTrue(alerta.detalle.contains("78,4"))
    }

    @Test
    fun `sin decision del operador la ronda no avanza`() {
        val sim = simConAnomaliaEnP1()
        sim.avanzarMinutos(30)
        assertEquals(1, sim.mision()!!.completados)
        assertEquals(EstadoOperativo.ESPERANDO_DECISION, sim.estadoReal())
    }

    @Test
    fun `marcar y continuar deja la falla registrada y sigue con el punto siguiente`() {
        val sim = simConAnomaliaEnP1()
        sim.decidir(DecisionAnomalia.MARCAR_Y_CONTINUAR)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[1].minutosVerificacion)
        assertEquals(2, sim.mision()!!.completados)
        assertEquals(ResultadoPunto.OK, sim.mision()!!.resultados[1].resultado)
    }

    @Test
    fun `abortar cierra la ronda como incompleta y manda al robot de regreso`() {
        val sim = simConAnomaliaEnP1()
        sim.decidir(DecisionAnomalia.ABORTAR)
        assertEquals(EstadoOperativo.RETORNANDO, sim.estadoReal())
        val mision = sim.mision()!!
        assertTrue(mision.cerrada)
        assertFalse(mision.completa)
        assertEquals(4, mision.puntosSinVerificar.size)
    }

    // ---- Autorización de supervisor ----

    @Test
    fun `accionar el seccionador se rechaza sin autorizacion`() {
        val sim = simConAnomaliaEnP1()
        val orden = sim.enviar(Orden.ACCIONAR_SECCIONADOR)
        assertEquals(EstadoOrden.RECHAZADA, orden.estado)
        assertEquals("CODIGO_INVALIDO", orden.motivoRechazo)
    }

    @Test
    fun `un codigo equivocado no habilita la autorizacion`() {
        val sim = simConAnomaliaEnP1()
        assertFalse(sim.ingresarAutorizacion("0000"))
        assertEquals(
            "CODIGO_INVALIDO",
            sim.habilitacion(Orden.ACCIONAR_SECCIONADOR, "0000").motivo
        )
    }

    @Test
    fun `con el codigo correcto se puede accionar el seccionador`() {
        val sim = simConAnomaliaEnP1()
        assertTrue(sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR))
        val orden = sim.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
    }

    @Test
    fun `no se puede pausar durante el accionamiento`() {
        val sim = simConAnomaliaEnP1()
        sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        sim.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        assertEquals("ACCIONAMIENTO_EN_CURSO", sim.habilitacion(Orden.PAUSAR).motivo)
    }

    @Test
    fun `la autorizacion vence pasados dos minutos`() {
        val sim = simConAnomaliaEnP1()
        sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        repeat(((Parametros.VIGENCIA_AUTORIZACION_MS + 1_000) / Parametros.TICK_MS).toInt()) {
            sim.avanzar(Parametros.TICK_MS)
        }
        assertEquals(
            "AUTORIZACION_VENCIDA",
            sim.habilitacion(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR).motivo
        )
    }

    @Test
    fun `aislar confirma la ausencia de tension en el punto`() {
        val sim = simConAnomaliaEnP1()
        sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        sim.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        repeat(((Parametros.ACCIONAMIENTO_MS + 1_000) / Parametros.TICK_MS).toInt()) {
            sim.avanzar(Parametros.TICK_MS)
        }
        sim.decidir(DecisionAnomalia.AISLAR)
        assertTrue(sim.mision()!!.resultados.first().ausenciaTensionConfirmada)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
    }

    // ---- Batería baja ----

    @Test
    fun `al bajar la bateria del umbral la ronda se pausa con una advertencia`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(40.0) // 60 minutos, contra 48 + 9 + margen
        sim.avanzar(Parametros.TICK_MS)

        assertEquals(EstadoOperativo.PAUSADO, sim.estadoReal())
        val alerta = sim.alertas().last()
        assertEquals(Severidad.ADVERTENCIA, alerta.severidad)
        assertTrue(alerta.titulo.contains("Batería"))
    }

    @Test
    fun `la advertencia de bateria baja se emite una sola vez`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(40.0)
        repeat(20) { sim.avanzar(Parametros.TICK_MS) }
        assertEquals(1, sim.alertas().count { it.severidad == Severidad.ADVERTENCIA })
    }

    @Test
    fun `con bateria baja se puede continuar si todavia alcanza para el regreso`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(40.0) // 60 min, y faltan 48 + 9 = 57
        sim.avanzar(Parametros.TICK_MS)
        assertTrue(sim.habilitacion(Orden.REANUDAR).habilitada)
    }

    @Test
    fun `cuando ya no alcanza para el regreso continuar se deshabilita solo`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(30.0) // 45 min, y faltan 48 + 9 = 57
        sim.avanzar(Parametros.TICK_MS)
        assertFalse(sim.habilitacion(Orden.REANUDAR).habilitada)
        assertTrue(sim.habilitacion(Orden.REGRESO_PUNTO_SEGURO).habilitada)
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "*AnomaliaTest*"`
Expected: FAIL por compilación — `Unresolved reference: forzarTemperaturaAlta`, `ingresarAutorizacion`, `decidir`, `esperandoDecision`.

- [ ] **Step 3: Agregar el estado de la anomalía**

Modify `sim/RobotSimulator.kt`. Agregar junto a los demás campos:

```kotlin
    private var puntoConTemperaturaAlta: String? = null
    private var temperaturaForzadaC = 0.0
    private var advertenciaBateriaEmitida = false
```

- [ ] **Step 4: Agregar los métodos de anomalía y autorización**

Modify `sim/RobotSimulator.kt`. Agregar en el bloque de forzadores:

```kotlin
    /** Escenario 4: el punto indicado va a medir una temperatura de falla. */
    fun forzarTemperaturaAlta(puntoId: String, temperaturaC: Double) {
        puntoConTemperaturaAlta = puntoId
        temperaturaForzadaC = temperaturaC
    }
```

Y agregar una sección nueva antes de los forzadores:

```kotlin
    // =====================================================================
    // Subproceso de anomalía
    // =====================================================================

    fun esperandoDecision(): Boolean = estado == EstadoOperativo.ESPERANDO_DECISION

    /** Verdadero si hay una autorización de supervisor vigente en este momento. */
    fun autorizacionVigente(): Boolean = autorizacionVigenteHastaMs?.let { relojMs <= it } ?: false

    /** Habilita la ventana de autorización del supervisor si el código es correcto. */
    fun ingresarAutorizacion(codigo: String): Boolean {
        if (codigo != Parametros.CODIGO_SUPERVISOR) return false
        autorizacionVigenteHastaMs = relojMs + Parametros.VIGENCIA_AUTORIZACION_MS
        return true
    }

    /**
     * Resuelve el subproceso con la decisión del operador.
     * Sin esta llamada la ronda queda detenida: el robot no decide solo.
     */
    fun decidir(decision: DecisionAnomalia) {
        if (estado != EstadoOperativo.ESPERANDO_DECISION) return
        when (decision) {
            DecisionAnomalia.MARCAR_Y_CONTINUAR -> {
                registrarEvento(ClaseEvento.ORDEN_RESUELTA, "Decisión: marcar y continuar")
                continuarDespuesDeLaFalla()
            }

            DecisionAnomalia.AISLAR -> {
                confirmarAusenciaDeTension()
                registrarEvento(ClaseEvento.ORDEN_RESUELTA, "Decisión: aislar y continuar")
                continuarDespuesDeLaFalla()
            }

            DecisionAnomalia.ABORTAR -> {
                val m = mision ?: return
                mision = m.copy(cerrada = true)
                registrarEvento(
                    ClaseEvento.RONDA_CERRADA,
                    "Ronda abortada por anomalía: faltaron ${m.puntosSinVerificar.size} " +
                        "puntos. La cuadrilla no puede ingresar"
                )
                empezarRegreso()
            }
        }
    }

    private fun continuarDespuesDeLaFalla() {
        if (mision?.completa == true) empezarRegreso() else empezarTraslado()
    }

    /** Tras accionar el seccionador, el punto pasa a tener ausencia de tensión. */
    private fun confirmarAusenciaDeTension() {
        val m = mision ?: return
        val ultimo = m.resultados.lastOrNull() ?: return
        mision = m.copy(
            resultados = m.resultados.dropLast(1) +
                ultimo.copy(ausenciaTensionConfirmada = true)
        )
    }
```

- [ ] **Step 5: Hacer que la verificación pueda dar falla**

Modify `sim/RobotSimulator.kt`. Reemplazar `empezarVerificacion` y `terminarVerificacion` por:

```kotlin
    private fun empezarVerificacion() {
        val punto = mision?.puntoActual ?: return
        estado = EstadoOperativo.VERIFICANDO_PUNTO
        velocidad = 0.0
        sala = punto.sala
        temperaturaMaxC = if (punto.id == puntoConTemperaturaAlta) temperaturaForzadaC
        else punto.temperaturaNormalC
    }

    private fun terminarVerificacion(punto: PuntoVerificacion) {
        val enFalla = temperaturaMaxC >= Parametros.TEMPERATURA_FALLA_C
        val resultado = ResultadoVerificacion(
            puntoId = punto.id,
            resultado = if (enFalla) ResultadoPunto.FALLA else ResultadoPunto.OK,
            temperaturaMaxC = temperaturaMaxC,
            ausenciaTensionConfirmada = !enFalla
        )
        val m = mision ?: return
        mision = m.copy(resultados = m.resultados + resultado)
        registrarEvento(
            ClaseEvento.PUNTO_COMPLETADO,
            "${resultado.puntoId}: ${resultado.resultado} a " +
                "${formatearGrados(resultado.temperaturaMaxC)}"
        )

        if (enFalla) {
            // El robot se detiene y espera: no decide solo.
            estado = EstadoOperativo.ESPERANDO_DECISION
            velocidad = 0.0
            emitirAlerta(
                Severidad.CRITICA,
                "Temperatura fuera de rango en ${punto.sala}",
                "Lectura de ${formatearGrados(temperaturaMaxC)}, " +
                    "umbral ${formatearGrados(Parametros.TEMPERATURA_FALLA_C)}. " +
                    "Requiere decisión del operador."
            )
            return
        }

        if (mision!!.completa) empezarRegreso() else empezarTraslado()
    }

    /** 78.4 se muestra como "78,4 °C", con coma decimal. */
    private fun formatearGrados(valor: Double): String =
        "%.1f °C".format(valor).replace('.', ',')
```

Borrar el método `registrarResultado`, que queda sin uso.

- [ ] **Step 6: Agregar el control de batería baja**

Modify `sim/RobotSimulator.kt`. Agregar la llamada en `avanzar`, después de `controlarEnlacePerdido(dtMs)`:

```kotlin
        controlarBateriaBaja()
```

Y agregar el método:

```kotlin
    /**
     * Escenario 2: si la autonomía ya no cubre lo que falta más el regreso con
     * margen, el robot se pausa y avisa. La decisión de continuar o volver es
     * del operador, y "continuar" se deshabilita solo cuando ya no alcanza.
     */
    private fun controlarBateriaBaja() {
        if (advertenciaBateriaEmitida) return
        val enRonda = estado in listOf(
            EstadoOperativo.EN_TRASLADO,
            EstadoOperativo.VERIFICANDO_PUNTO
        )
        if (!enRonda) return

        val necesarios =
            ((minutosRestantes() + Parametros.REGRESO_MIN) * (1 + Parametros.MARGEN_BATERIA)).toInt()
        if (autonomiaMin() >= necesarios) return

        advertenciaBateriaEmitida = true
        estado = EstadoOperativo.PAUSADO
        velocidad = 0.0
        emitirAlerta(
            Severidad.ADVERTENCIA,
            "Batería baja: ronda pausada",
            "Autonomía ${autonomiaMin()} min. Faltan ${minutosRestantes()} min de ronda " +
                "más ${Parametros.REGRESO_MIN} min de regreso."
        )
    }
```

- [ ] **Step 7: Permitir reiniciar la advertencia al empezar una ronda nueva**

Modify `sim/RobotSimulator.kt`. En `iniciarRonda`, agregar como primera línea del cuerpo:

```kotlin
        advertenciaBateriaEmitida = false
```

- [ ] **Step 8: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*AnomaliaTest*"`
Expected: PASS, 16 tests.

Si falla "al bajar la batería del umbral la ronda se pausa", revisar la cuenta: con 40 % la autonomía es 60 min, y lo necesario es `(48 + 9) * 1,15 = 65`. Como 60 es menor que 65, se pausa.

- [ ] **Step 9: Correr toda la suite**

Run: `./gradlew test`
Expected: PASS, 73 tests.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "agrega subproceso de anomalia, autorizacion de supervisor y bateria baja"
```

---

## Task 7: Repositorio, Room y derivación del historial

**Files:**
- Create: `data/RobotRepository.kt`, `data/SimulatedRobotRepository.kt`, `data/HistorialMapper.kt`, `data/local/EventoEntity.kt`, `data/local/EventoDao.kt`, `data/local/AppDatabase.kt`, `SalasApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `test/data/HistorialMapperTest.kt`

**Interfaces:**
- Consumes: todo `model/` y `sim/RobotSimulator.kt`.
- Produces:
  - `interface RobotRepository` con `val estado: StateFlow<EstadoRobot>`, `val historial: Flow<List<ResumenRonda>>`, y los métodos `seleccionarRobot`, `enviar`, `ingresarAutorizacion`, `decidir`, `aplicarEscenario`, `forzarEnlace`, `forzarPresencia`, `forzarBateria`, `reiniciar`, `arrancar`.
  - `data class EstadoRobot(telemetria, mision, alertas, ordenes, antiguedadMs, datoAntiguo, esperandoDecision, robotSeleccionado)`
  - `data class ResumenRonda(misionId, nombre, iniciadaTsMs, cerrada, completa, puntosCompletados, puntosTotales, eventos)`
  - `object HistorialMapper` con `fun aResumenes(eventos: List<Evento>): List<ResumenRonda>`

- [ ] **Step 1: Escribir el test del mapper de historial**

Create `app/src/test/java/ar/edu/uade/salastecnicas/data/HistorialMapperTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.Evento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorialMapperTest {

    private fun evento(
        id: String,
        misionId: String?,
        clase: ClaseEvento,
        tsMs: Long,
        detalle: String = ""
    ) = Evento(id, misionId, clase, tsMs, tsMs, detalle)

    @Test
    fun `sin eventos el historial esta vacio`() {
        assertTrue(HistorialMapper.aResumenes(emptyList()).isEmpty())
    }

    @Test
    fun `los eventos sin mision no generan rondas`() {
        val eventos = listOf(
            evento("E1", null, ClaseEvento.ENLACE_PERDIDO, 100)
        )
        assertTrue(HistorialMapper.aResumenes(eventos).isEmpty())
    }

    @Test
    fun `una ronda iniciada aparece como abierta`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100, "Verificación previa")
        )
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertEquals("RND-001", resumen.misionId)
        assertEquals("Verificación previa", resumen.nombre)
        assertEquals(100L, resumen.iniciadaTsMs)
        assertFalse(resumen.cerrada)
        assertEquals(0, resumen.puntosCompletados)
    }

    @Test
    fun `cuenta los puntos completados de la ronda`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E2", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200),
            evento("E3", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 300),
            evento("E4", "RND-001", ClaseEvento.ALERTA_EMITIDA, 350)
        )
        assertEquals(2, HistorialMapper.aResumenes(eventos).single().puntosCompletados)
    }

    @Test
    fun `una ronda cerrada con los cinco puntos queda completa`() {
        val eventos = buildList {
            add(evento("E0", "RND-001", ClaseEvento.RONDA_INICIADA, 100))
            repeat(5) { add(evento("E$it", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200L + it)) }
            add(evento("EF", "RND-001", ClaseEvento.RONDA_CERRADA, 900, "Ronda completa"))
        }
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertTrue(resumen.cerrada)
        assertTrue(resumen.completa)
        assertEquals(5, resumen.puntosCompletados)
    }

    @Test
    fun `una ronda cerrada con menos puntos queda incompleta`() {
        val eventos = listOf(
            evento("E0", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E1", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200),
            evento("EF", "RND-001", ClaseEvento.RONDA_CERRADA, 900, "Ronda cancelada")
        )
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertTrue(resumen.cerrada)
        assertFalse(resumen.completa)
    }

    @Test
    fun `las rondas salen de la mas reciente a la mas vieja`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E2", "RND-002", ClaseEvento.RONDA_INICIADA, 500),
            evento("E3", "RND-003", ClaseEvento.RONDA_INICIADA, 300)
        )
        val ids = HistorialMapper.aResumenes(eventos).map { it.misionId }
        assertEquals(listOf("RND-002", "RND-003", "RND-001"), ids)
    }

    @Test
    fun `cada ronda conserva sus eventos del mas reciente al mas viejo`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E2", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 300),
            evento("E3", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200)
        )
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertEquals(listOf("E2", "E3", "E1"), resumen.eventos.map { it.id })
    }

    @Test
    fun `una ronda sin evento de inicio igual aparece con el nombre por defecto`() {
        val eventos = listOf(
            evento("E1", "RND-009", ClaseEvento.PUNTO_COMPLETADO, 400)
        )
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertEquals("RND-009", resumen.misionId)
        assertEquals("Ronda sin registro de inicio", resumen.nombre)
        assertEquals(400L, resumen.iniciadaTsMs)
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "*HistorialMapperTest*"`
Expected: FAIL por compilación — `Unresolved reference: HistorialMapper`.

- [ ] **Step 3: Escribir el mapper**

Create `data/HistorialMapper.kt`:

```kotlin
package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.Evento
import ar.edu.uade.salastecnicas.sim.Rondas

/** Una ronda del historial, derivada de los eventos y no de una tabla propia. */
data class ResumenRonda(
    val misionId: String,
    val nombre: String,
    val iniciadaTsMs: Long,
    val cerrada: Boolean,
    val completa: Boolean,
    val puntosCompletados: Int,
    val puntosTotales: Int,
    val eventos: List<Evento>
)

/**
 * Función pura que agrupa los eventos por ronda.
 * Es la razón por la que alcanza una sola tabla en Room: el historial de
 * misiones no se guarda, se deduce.
 */
object HistorialMapper {

    fun aResumenes(eventos: List<Evento>): List<ResumenRonda> =
        eventos
            .filter { it.misionId != null }
            .groupBy { it.misionId!! }
            .map { (misionId, delaRonda) -> resumir(misionId, delaRonda) }
            .sortedByDescending { it.iniciadaTsMs }

    private fun resumir(misionId: String, eventos: List<Evento>): ResumenRonda {
        val inicio = eventos.firstOrNull { it.clase == ClaseEvento.RONDA_INICIADA }
        val cierre = eventos.firstOrNull { it.clase == ClaseEvento.RONDA_CERRADA }
        val completados = eventos.count { it.clase == ClaseEvento.PUNTO_COMPLETADO }
        val totales = Rondas.PUNTOS.size

        return ResumenRonda(
            misionId = misionId,
            nombre = inicio?.detalle?.takeIf { it.isNotBlank() }
                ?: "Ronda sin registro de inicio",
            iniciadaTsMs = inicio?.tsMs ?: eventos.minOf { it.tsMs },
            cerrada = cierre != null,
            completa = cierre != null && completados >= totales,
            puntosCompletados = completados,
            puntosTotales = totales,
            eventos = eventos.sortedByDescending { it.tsMs }
        )
    }
}
```

- [ ] **Step 4: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*HistorialMapperTest*"`
Expected: PASS, 9 tests.

- [ ] **Step 5: Escribir la capa de Room**

Create `data/local/EventoEntity.kt`:

```kotlin
package ar.edu.uade.salastecnicas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.Evento

/**
 * Única tabla persistida. El historial de rondas se deriva por consulta
 * sobre esta tabla, agrupando por misionId.
 */
@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey val id: String,
    val misionId: String?,
    val clase: String,
    /** Momento en que ocurrió, según el reloj del robot. */
    val tsMs: Long,
    /** Momento en que la app lo recibió. Distinto si hubo desconexión. */
    val recibidoTsMs: Long,
    val detalle: String
)

fun Evento.aEntity() = EventoEntity(
    id = id,
    misionId = misionId,
    clase = clase.name,
    tsMs = tsMs,
    recibidoTsMs = recibidoTsMs,
    detalle = detalle
)

fun EventoEntity.aModelo() = Evento(
    id = id,
    misionId = misionId,
    clase = ClaseEvento.valueOf(clase),
    tsMs = tsMs,
    recibidoTsMs = recibidoTsMs,
    detalle = detalle
)
```

Create `data/local/EventoDao.kt`:

```kotlin
package ar.edu.uade.salastecnicas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(eventos: List<EventoEntity>)

    @Query("SELECT * FROM eventos ORDER BY tsMs DESC")
    fun observarTodos(): Flow<List<EventoEntity>>

    @Query("DELETE FROM eventos")
    suspend fun borrarTodo()
}
```

Create `data/local/AppDatabase.kt`:

```kotlin
package ar.edu.uade.salastecnicas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventoEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventoDao(): EventoDao

    companion object {
        fun crear(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "salas-tecnicas.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
```

- [ ] **Step 6: Escribir la interfaz del repositorio**

Create `data/RobotRepository.kt`:

```kotlin
package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.model.Alerta
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Mision
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.model.Telemetria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Todo lo que la app sabe del robot en un instante dado. */
data class EstadoRobot(
    val robotSeleccionado: Robot?,
    val telemetria: Telemetria?,
    val mision: Mision?,
    val alertas: List<Alerta>,
    val ordenes: List<OrdenEnviada>,
    val antiguedadMs: Long,
    val datoAntiguo: Boolean,
    val esperandoDecision: Boolean,
    val autorizacionVigente: Boolean
)

/**
 * Única frontera entre la UI y el robot. La UI no conoce el simulador.
 * Cambiar esta implementación por una real no obligaría a tocar la UI.
 */
interface RobotRepository {

    val estado: StateFlow<EstadoRobot>

    val historial: Flow<List<ResumenRonda>>

    /** Arranca el bucle de simulación. Se llama una sola vez. */
    fun arrancar()

    fun seleccionarRobot(robot: Robot)

    suspend fun enviar(orden: Orden, codigoSupervisor: String? = null): OrdenEnviada

    fun ingresarAutorizacion(codigo: String): Boolean

    fun decidir(decision: DecisionAnomalia)

    /** Modo demo: deja el simulador listo para reproducir un escenario. */
    suspend fun aplicarEscenario(escenario: Escenario)

    fun forzarEnlace(enlace: EstadoEnlace)

    fun forzarPresencia(distanciaM: Double?)

    fun forzarBateria(pct: Double)

    /** Modo demo: vuelve todo al estado inicial, incluido el historial. */
    suspend fun reiniciar()
}
```

- [ ] **Step 7: Escribir la implementación simulada**

Create `data/SimulatedRobotRepository.kt`:

```kotlin
package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.data.local.EventoDao
import ar.edu.uade.salastecnicas.data.local.aEntity
import ar.edu.uade.salastecnicas.data.local.aModelo
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.sim.RobotSimulator
import ar.edu.uade.salastecnicas.sim.Rondas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Envuelve el simulador y lo publica como estado observable.
 * El bucle es deliberadamente simple: un while con delay, sin operadores
 * de flows encadenados, para que se pueda leer de arriba a abajo.
 */
class SimulatedRobotRepository(
    private val scope: CoroutineScope,
    private val eventoDao: EventoDao
) : RobotRepository {

    private var simulador = RobotSimulator()
    private var robotSeleccionado: Robot? = null
    private var arrancado = false

    private val _estado = MutableStateFlow(estadoActual())
    override val estado: StateFlow<EstadoRobot> = _estado

    override val historial: Flow<List<ResumenRonda>> =
        eventoDao.observarTodos().map { filas ->
            HistorialMapper.aResumenes(filas.map { it.aModelo() })
        }

    override fun arrancar() {
        if (arrancado) return
        arrancado = true
        scope.launch {
            while (true) {
                delay(Parametros.TICK_MS)
                simulador.avanzar(Parametros.TICK_MS)
                guardarEventosNuevos()
                _estado.value = estadoActual()
            }
        }
    }

    private suspend fun guardarEventosNuevos() {
        val nuevos = simulador.drenarEventos()
        if (nuevos.isNotEmpty()) {
            eventoDao.insertar(nuevos.map { it.aEntity() })
        }
    }

    private fun estadoActual() = EstadoRobot(
        robotSeleccionado = robotSeleccionado,
        telemetria = if (robotSeleccionado == null) null else simulador.telemetria(),
        mision = simulador.mision(),
        alertas = simulador.alertas(),
        ordenes = simulador.ordenes(),
        antiguedadMs = simulador.antiguedadMs(),
        datoAntiguo = simulador.datoAntiguo(),
        esperandoDecision = simulador.esperandoDecision(),
        autorizacionVigente = simulador.autorizacionVigente()
    )

    override fun seleccionarRobot(robot: Robot) {
        robotSeleccionado = robot
        _estado.value = estadoActual()
    }

    override suspend fun enviar(orden: Orden, codigoSupervisor: String?): OrdenEnviada {
        val resultado = simulador.enviar(orden, codigoSupervisor)
        guardarEventosNuevos()
        _estado.value = estadoActual()
        return resultado
    }

    override fun ingresarAutorizacion(codigo: String): Boolean {
        val ok = simulador.ingresarAutorizacion(codigo)
        _estado.value = estadoActual()
        return ok
    }

    override fun decidir(decision: DecisionAnomalia) {
        simulador.decidir(decision)
        _estado.value = estadoActual()
    }

    override fun forzarEnlace(enlace: EstadoEnlace) {
        simulador.forzarEnlace(enlace)
        _estado.value = estadoActual()
    }

    override fun forzarPresencia(distanciaM: Double?) {
        simulador.forzarPresencia(distanciaM)
        _estado.value = estadoActual()
    }

    override fun forzarBateria(pct: Double) {
        simulador.forzarBateria(pct)
        _estado.value = estadoActual()
    }

    override suspend fun reiniciar() {
        simulador = RobotSimulator()
        eventoDao.borrarTodo()
        _estado.value = estadoActual()
    }

    /**
     * Deja el simulador en la condición de partida de cada escenario.
     * El operador después opera normalmente y el escenario se desarrolla.
     */
    override suspend fun aplicarEscenario(escenario: Escenario) {
        reiniciar()
        when (escenario) {
            Escenario.MISION_NORMAL -> Unit

            Escenario.BATERIA_BAJA -> {
                simulador.enviar(Orden.INICIAR_MISION)
                simulador.forzarBateria(40.0)
            }

            Escenario.DESCONEXION -> {
                simulador.enviar(Orden.INICIAR_MISION)
                simulador.forzarEnlace(EstadoEnlace.PERDIDO)
            }

            Escenario.INCIDENTE -> {
                simulador.forzarTemperaturaAlta(Rondas.PUNTOS[0].id, 78.4)
                simulador.enviar(Orden.INICIAR_MISION)
            }

            Escenario.ORDEN_NO_EJECUTADA -> {
                simulador.enviar(Orden.INICIAR_MISION)
                simulador.forzarDetencionIgnorada(true)
            }
        }
        guardarEventosNuevos()
        _estado.value = estadoActual()
    }
}
```

- [ ] **Step 8: Evitar que los forzadores refresquen un dato antiguo**

Modify `sim/RobotSimulator.kt`. En `forzarBateria` y en `forzarPresencia`, la línea `ultimaTelemetria = construirTelemetria()` tiene que respetar la desconexión: si el enlace está perdido, la app debe seguir viendo el dato viejo. Reemplazar esa línea, en ambos métodos, por:

```kotlin
        if (enlace != EstadoEnlace.PERDIDO) ultimaTelemetria = construirTelemetria()
```

Sin esta guarda, forzar la batería desde el Modo demo durante una desconexión haría reaparecer datos frescos y rompería el escenario 3.

- [ ] **Step 9: Escribir la clase Application**

Create `SalasApp.kt`:

```kotlin
package ar.edu.uade.salastecnicas

import android.app.Application
import ar.edu.uade.salastecnicas.data.RobotRepository
import ar.edu.uade.salastecnicas.data.SimulatedRobotRepository
import ar.edu.uade.salastecnicas.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Acá se arma el objeto grande de la app, una sola vez.
 * Esto reemplaza a un framework de inyección de dependencias: son pocas
 * líneas y se explican sin anotaciones de por medio.
 */
class SalasApp : Application() {

    private val scope = CoroutineScope(SupervisorJob())

    lateinit var repositorio: RobotRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val base = AppDatabase.crear(this)
        repositorio = SimulatedRobotRepository(scope, base.eventoDao())
        repositorio.arrancar()
    }
}
```

- [ ] **Step 10: Declarar la Application en el manifiesto**

Modify `app/src/main/AndroidManifest.xml`. Dentro de la etiqueta `<application>`, agregar el atributo:

```xml
android:name=".SalasApp"
```

El manifiesto **no** declara ningún permiso. El APK no usa red.

- [ ] **Step 11: Verificar que compila y que la suite pasa**

Run: `./gradlew assembleDebug test`
Expected: `BUILD SUCCESSFUL` y PASS, 82 tests.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "agrega repositorio simulado, persistencia en room y derivacion del historial"
```

---

## Task 8: Estado de la UI y ViewModel

**Files:**
- Create: `viewmodel/UiState.kt`, `viewmodel/RobotViewModel.kt`
- Test: `test/viewmodel/RobotViewModelTest.kt`

**Interfaces:**
- Consumes: `RobotRepository`, `EstadoRobot`, `ResumenRonda`.
- Produces:
  - `data class UiState(pantalla, estadoRobot, historial, robots, habilitaciones, dialogo, ultimaRespuesta, errorAutorizacion)`
  - `enum class Dialogo { NINGUNO, DECISION_ANOMALIA, AUTORIZACION, CONFIRMAR_CANCELAR, CONFIRMAR_SECCIONADOR }`
  - `class RobotViewModel(repo: RobotRepository) : ViewModel()` con `val uiState: StateFlow<UiState>` y los métodos `irA`, `seleccionarRobot`, `pedirOrden`, `confirmarOrden`, `cerrarDialogo`, `enviarAutorizacion`, `decidir`, `aplicarEscenario`, `forzarEnlace`, `forzarPresencia`, `forzarBateria`, `reiniciar`.
  - `class RobotViewModelFactory(repo: RobotRepository) : ViewModelProvider.Factory`

- [ ] **Step 1: Escribir el test del ViewModel**

Create `app/src/test/java/ar/edu/uade/salastecnicas/viewmodel/RobotViewModelTest.kt`:

```kotlin
package ar.edu.uade.salastecnicas.viewmodel

import ar.edu.uade.salastecnicas.data.EstadoRobot
import ar.edu.uade.salastecnicas.data.ResumenRonda
import ar.edu.uade.salastecnicas.data.RobotRepository
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.sim.Rondas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RobotViewModelTest {

    /** Repositorio de prueba: registra lo que se le pide, sin simulador. */
    private class RepoFalso : RobotRepository {
        val ordenesEnviadas = mutableListOf<Orden>()
        var escenarioAplicado: Escenario? = null
        var reiniciado = false
        var enlaceForzado: EstadoEnlace? = null
        var codigoRecibido: String? = null
        var decisionRecibida: DecisionAnomalia? = null
        var autorizacionValida = true

        private val _estado = MutableStateFlow(
            EstadoRobot(null, null, null, emptyList(), emptyList(), 0, false, false, true)
        )
        override val estado: StateFlow<EstadoRobot> = _estado
        override val historial: Flow<List<ResumenRonda>> = flowOf(emptyList())

        override fun arrancar() = Unit

        override fun seleccionarRobot(robot: ar.edu.uade.salastecnicas.model.Robot) {
            _estado.value = _estado.value.copy(robotSeleccionado = robot)
        }

        override suspend fun enviar(orden: Orden, codigoSupervisor: String?): OrdenEnviada {
            ordenesEnviadas.add(orden)
            codigoRecibido = codigoSupervisor
            return OrdenEnviada("CMD-0001", orden, 0, EstadoOrden.CONFIRMADA)
        }

        override fun ingresarAutorizacion(codigo: String): Boolean {
            codigoRecibido = codigo
            return autorizacionValida
        }

        override fun decidir(decision: DecisionAnomalia) { decisionRecibida = decision }

        override suspend fun aplicarEscenario(escenario: Escenario) {
            escenarioAplicado = escenario
        }

        override fun forzarEnlace(enlace: EstadoEnlace) { enlaceForzado = enlace }
        override fun forzarPresencia(distanciaM: Double?) = Unit
        override fun forzarBateria(pct: Double) = Unit
        override suspend fun reiniciar() { reiniciado = true }

        fun ponerEsperandoDecision() {
            _estado.value = _estado.value.copy(esperandoDecision = true)
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `arranca en la pantalla de seleccion de robot`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertEquals(Pantalla.SELECCION_ROBOT, vm.uiState.value.pantalla)
    }

    @Test
    fun `ofrece los tres robots del listado`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertEquals(3, vm.uiState.value.robots.size)
        assertEquals(1, vm.uiState.value.robots.count { it.disponible })
    }

    @Test
    fun `seleccionar un robot lleva a supervision`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        vm.seleccionarRobot(Rondas.ROBOTS.first())
        assertEquals(Pantalla.SUPERVISION, vm.uiState.value.pantalla)
        assertEquals("G1-01", vm.uiState.value.estadoRobot.robotSeleccionado?.id)
    }

    @Test
    fun `navegar cambia la pantalla activa`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        vm.irA(Pantalla.CONTROL)
        assertEquals(Pantalla.CONTROL, vm.uiState.value.pantalla)
    }

    @Test
    fun `una orden comun se envia directo al repositorio`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.PAUSAR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(Orden.PAUSAR), repo.ordenesEnviadas)
        assertEquals(Dialogo.NINGUNO, vm.uiState.value.dialogo)
    }

    @Test
    fun `cancelar abre primero la confirmacion de dos pasos`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.CANCELAR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Dialogo.CONFIRMAR_CANCELAR, vm.uiState.value.dialogo)
        assertTrue(repo.ordenesEnviadas.isEmpty())
    }

    @Test
    fun `confirmar la cancelacion la envia y cierra el dialogo`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.CANCELAR)
        vm.confirmarOrden()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(Orden.CANCELAR), repo.ordenesEnviadas)
        assertEquals(Dialogo.NINGUNO, vm.uiState.value.dialogo)
    }

    @Test
    fun `cerrar el dialogo descarta la orden pendiente`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.CANCELAR)
        vm.cerrarDialogo()
        vm.confirmarOrden()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.ordenesEnviadas.isEmpty())
    }

    @Test
    fun `accionar el seccionador pide primero la autorizacion`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.ACCIONAR_SECCIONADOR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Dialogo.AUTORIZACION, vm.uiState.value.dialogo)
        assertTrue(repo.ordenesEnviadas.isEmpty())
    }

    @Test
    fun `un codigo valido envia la orden con el codigo`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.ACCIONAR_SECCIONADOR)
        vm.enviarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(Orden.ACCIONAR_SECCIONADOR), repo.ordenesEnviadas)
        assertEquals(Parametros.CODIGO_SUPERVISOR, repo.codigoRecibido)
        assertNull(vm.uiState.value.errorAutorizacion)
    }

    @Test
    fun `un codigo invalido muestra el error y no envia la orden`() = runTest(dispatcher) {
        val repo = RepoFalso()
        repo.autorizacionValida = false
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.ACCIONAR_SECCIONADOR)
        vm.enviarAutorizacion("0000")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.ordenesEnviadas.isEmpty())
        assertEquals("Código de supervisor inválido", vm.uiState.value.errorAutorizacion)
        assertEquals(Dialogo.AUTORIZACION, vm.uiState.value.dialogo)
    }

    @Test
    fun `cuando el robot espera decision se abre el dialogo de anomalia`() =
        runTest(dispatcher) {
            val repo = RepoFalso()
            val vm = RobotViewModel(repo)
            dispatcher.scheduler.advanceUntilIdle()
            repo.ponerEsperandoDecision()
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(Dialogo.DECISION_ANOMALIA, vm.uiState.value.dialogo)
        }

    @Test
    fun `decidir reenvia la decision y cierra el dialogo`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.decidir(DecisionAnomalia.ABORTAR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(DecisionAnomalia.ABORTAR, repo.decisionRecibida)
        assertEquals(Dialogo.NINGUNO, vm.uiState.value.dialogo)
    }

    @Test
    fun `aplicar un escenario lo delega y lleva a supervision`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.aplicarEscenario(Escenario.DESCONEXION)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Escenario.DESCONEXION, repo.escenarioAplicado)
        assertEquals(Pantalla.SUPERVISION, vm.uiState.value.pantalla)
    }

    @Test
    fun `reiniciar vuelve a la seleccion de robot`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.irA(Pantalla.CONTROL)
        vm.reiniciar()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.reiniciado)
        assertEquals(Pantalla.SELECCION_ROBOT, vm.uiState.value.pantalla)
    }

    @Test
    fun `forzar el enlace lo delega al repositorio`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.forzarEnlace(EstadoEnlace.PERDIDO)
        assertEquals(EstadoEnlace.PERDIDO, repo.enlaceForzado)
    }

    @Test
    fun `el estado expone una habilitacion por cada orden`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertEquals(Orden.entries.size, vm.uiState.value.habilitaciones.size)
    }

    @Test
    fun `sin robot seleccionado ninguna orden esta habilitada`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertTrue(vm.uiState.value.habilitaciones.values.none { it.habilitada })
        assertFalse(vm.uiState.value.habilitaciones[Orden.PAUSAR]!!.habilitada)
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "*RobotViewModelTest*"`
Expected: FAIL por compilación — `Unresolved reference: RobotViewModel`, `UiState`, `Dialogo`.

- [ ] **Step 3: Agregar la habilitación al estado del repositorio**

El ViewModel necesita las habilitaciones, y sólo el simulador sabe calcularlas. Se agregan al `EstadoRobot`.

Modify `data/RobotRepository.kt`. Agregar el campo al final de `EstadoRobot`:

```kotlin
    val habilitaciones: Map<Orden, Habilitacion>
```

Agregar el import:

```kotlin
import ar.edu.uade.salastecnicas.model.Habilitacion
```

Modify `data/SimulatedRobotRepository.kt`. Agregar en `estadoActual()`, como último campo:

```kotlin
        habilitaciones = Orden.entries.associateWith { orden ->
            if (robotSeleccionado == null) Habilitacion(false, "SIN_ROBOT", "No hay robot seleccionado")
            else simulador.habilitacion(orden, Parametros.CODIGO_SUPERVISOR)
        }
```

Agregar el import de `Habilitacion`.

Modify el `RepoFalso` del test: agregar `emptyMap()` como último argumento del `EstadoRobot` inicial, y en el test `el estado expone una habilitacion por cada orden` el ViewModel completa el mapa cuando el repositorio lo deja vacío (ver Step 4).

- [ ] **Step 4: Escribir el estado de la UI**

Create `viewmodel/UiState.kt`:

```kotlin
package ar.edu.uade.salastecnicas.viewmodel

import ar.edu.uade.salastecnicas.data.EstadoRobot
import ar.edu.uade.salastecnicas.data.ResumenRonda
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Robot

enum class Dialogo {
    NINGUNO,
    DECISION_ANOMALIA,
    AUTORIZACION,
    CONFIRMAR_CANCELAR,
    CONFIRMAR_SECCIONADOR
}

/** Todo lo que las seis pantallas necesitan, en un solo objeto. */
data class UiState(
    val pantalla: Pantalla = Pantalla.SELECCION_ROBOT,
    val estadoRobot: EstadoRobot,
    val historial: List<ResumenRonda> = emptyList(),
    val robots: List<Robot> = emptyList(),
    val habilitaciones: Map<Orden, Habilitacion> = emptyMap(),
    val dialogo: Dialogo = Dialogo.NINGUNO,
    /** Última respuesta recibida, para mostrarla como aviso breve. */
    val ultimaRespuesta: String? = null,
    val errorAutorizacion: String? = null
)
```

- [ ] **Step 5: Escribir el ViewModel**

Create `viewmodel/RobotViewModel.kt`:

```kotlin
package ar.edu.uade.salastecnicas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ar.edu.uade.salastecnicas.data.RobotRepository
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.sim.Rondas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Único ViewModel de la app. La app tiene un solo dominio, el estado del
 * robot: partirlo en varios obligaría a sincronizar copias del mismo estado.
 */
class RobotViewModel(
    private val repositorio: RobotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UiState(
            estadoRobot = repositorio.estado.value,
            robots = Rondas.ROBOTS,
            habilitaciones = habilitacionesDe(repositorio.estado.value.habilitaciones)
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    /** Orden que espera confirmación o autorización. */
    private var ordenPendiente: Orden? = null

    init {
        viewModelScope.launch {
            repositorio.estado.collect { estado ->
                val abrirAnomalia = estado.esperandoDecision &&
                    _uiState.value.dialogo == Dialogo.NINGUNO
                _uiState.value = _uiState.value.copy(
                    estadoRobot = estado,
                    habilitaciones = habilitacionesDe(estado.habilitaciones),
                    dialogo = if (abrirAnomalia) Dialogo.DECISION_ANOMALIA
                    else _uiState.value.dialogo
                )
            }
        }
        viewModelScope.launch {
            repositorio.historial.collect { rondas ->
                _uiState.value = _uiState.value.copy(historial = rondas)
            }
        }
    }

    /** Si el repositorio no informó habilitaciones, ninguna orden se habilita. */
    private fun habilitacionesDe(delRepo: Map<Orden, Habilitacion>): Map<Orden, Habilitacion> =
        Orden.entries.associateWith { orden ->
            delRepo[orden] ?: Habilitacion(false, "SIN_DATOS", "Sin datos del robot")
        }

    // =====================================================================
    // Navegación
    // =====================================================================

    fun irA(pantalla: Pantalla) {
        _uiState.value = _uiState.value.copy(pantalla = pantalla)
    }

    fun seleccionarRobot(robot: Robot) {
        if (!robot.disponible) return
        repositorio.seleccionarRobot(robot)
        _uiState.value = _uiState.value.copy(pantalla = Pantalla.SUPERVISION)
    }

    // =====================================================================
    // Órdenes
    // =====================================================================

    /**
     * Punto de entrada de todos los botones de comando.
     * Las órdenes críticas no se envían acá: abren un diálogo primero.
     */
    fun pedirOrden(orden: Orden) {
        when (orden) {
            Orden.CANCELAR -> {
                ordenPendiente = orden
                _uiState.value = _uiState.value.copy(dialogo = Dialogo.CONFIRMAR_CANCELAR)
            }
            Orden.ACCIONAR_SECCIONADOR -> {
                ordenPendiente = orden
                _uiState.value = _uiState.value.copy(
                    dialogo = Dialogo.AUTORIZACION,
                    errorAutorizacion = null
                )
            }
            else -> enviar(orden, null)
        }
    }

    /** Segundo paso de la confirmación de dos pasos. */
    fun confirmarOrden() {
        val orden = ordenPendiente ?: return
        ordenPendiente = null
        _uiState.value = _uiState.value.copy(dialogo = Dialogo.NINGUNO)
        enviar(orden, null)
    }

    fun cerrarDialogo() {
        ordenPendiente = null
        _uiState.value = _uiState.value.copy(
            dialogo = Dialogo.NINGUNO,
            errorAutorizacion = null
        )
    }

    fun enviarAutorizacion(codigo: String) {
        if (!repositorio.ingresarAutorizacion(codigo)) {
            _uiState.value = _uiState.value.copy(
                errorAutorizacion = "Código de supervisor inválido"
            )
            return
        }
        val orden = ordenPendiente ?: Orden.ACCIONAR_SECCIONADOR
        ordenPendiente = null
        _uiState.value = _uiState.value.copy(
            dialogo = Dialogo.NINGUNO,
            errorAutorizacion = null
        )
        enviar(orden, codigo)
    }

    private fun enviar(orden: Orden, codigo: String?) {
        viewModelScope.launch {
            val resultado = repositorio.enviar(orden, codigo)
            val aviso = when (resultado.estado) {
                EstadoOrden.CONFIRMADA -> "${textoDe(orden)}: confirmada"
                EstadoOrden.RECHAZADA ->
                    "${textoDe(orden)}: rechazada — ${resultado.detalleRechazo}"
                EstadoOrden.PENDIENTE -> "${textoDe(orden)}: enviada, sin confirmar"
                EstadoOrden.SIN_RESPUESTA -> "${textoDe(orden)}: sin respuesta"
            }
            _uiState.value = _uiState.value.copy(ultimaRespuesta = aviso)
        }
    }

    fun limpiarAviso() {
        _uiState.value = _uiState.value.copy(ultimaRespuesta = null)
    }

    fun decidir(decision: DecisionAnomalia) {
        repositorio.decidir(decision)
        _uiState.value = _uiState.value.copy(dialogo = Dialogo.NINGUNO)
    }

    // =====================================================================
    // Modo demo
    // =====================================================================

    fun aplicarEscenario(escenario: Escenario) {
        viewModelScope.launch {
            repositorio.aplicarEscenario(escenario)
            repositorio.seleccionarRobot(Rondas.ROBOTS.first())
            _uiState.value = _uiState.value.copy(
                pantalla = Pantalla.SUPERVISION,
                dialogo = Dialogo.NINGUNO,
                ultimaRespuesta = "Escenario preparado: ${textoDe(escenario)}"
            )
        }
    }

    fun forzarEnlace(enlace: EstadoEnlace) = repositorio.forzarEnlace(enlace)

    fun forzarPresencia(distanciaM: Double?) = repositorio.forzarPresencia(distanciaM)

    fun forzarBateria(pct: Double) = repositorio.forzarBateria(pct)

    fun reiniciar() {
        viewModelScope.launch {
            repositorio.reiniciar()
            _uiState.value = _uiState.value.copy(
                pantalla = Pantalla.SELECCION_ROBOT,
                dialogo = Dialogo.NINGUNO,
                ultimaRespuesta = null
            )
        }
    }

    // =====================================================================
    // Textos
    // =====================================================================

    private fun textoDe(orden: Orden): String = when (orden) {
        Orden.INICIAR_MISION -> "Iniciar ronda"
        Orden.PAUSAR -> "Pausar"
        Orden.REANUDAR -> "Reanudar"
        Orden.CANCELAR -> "Cancelar ronda"
        Orden.REGRESO_PUNTO_SEGURO -> "Regresar al punto seguro"
        Orden.DETENCION_OPERATIVA -> "Solicitar detención"
        Orden.RECONOCER_ALERTAS -> "Reconocer alertas"
        Orden.ACCIONAR_SECCIONADOR -> "Accionar seccionador"
    }

    private fun textoDe(escenario: Escenario): String = when (escenario) {
        Escenario.MISION_NORMAL -> "misión normal"
        Escenario.BATERIA_BAJA -> "batería baja"
        Escenario.DESCONEXION -> "desconexión"
        Escenario.INCIDENTE -> "incidente por temperatura"
        Escenario.ORDEN_NO_EJECUTADA -> "orden confirmada que no se ejecuta"
    }
}

class RobotViewModelFactory(
    private val repositorio: RobotRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RobotViewModel(repositorio) as T
}
```

- [ ] **Step 6: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "*RobotViewModelTest*"`
Expected: PASS, 18 tests.

- [ ] **Step 7: Correr toda la suite y compilar**

Run: `./gradlew assembleDebug test`
Expected: `BUILD SUCCESSFUL` y PASS, 100 tests.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "agrega estado unico de la ui y el viewmodel con sus dialogos"
```

---

## Task 9: Componentes Compose con sus cuatro estados visuales

**Files:**
- Create: `ui/components/EstadoVisual.kt`, `ui/components/TarjetaTelemetria.kt`, `ui/components/BotonComando.kt`, `ui/components/ChipEstadoOrden.kt`, `ui/components/BannerAlerta.kt`, `ui/components/IndicadorAntiguedad.kt`, `ui/components/BarraProgresoRonda.kt`

**Interfaces:**
- Consumes: `model/` y `Habilitacion`.
- Produces:
  - `enum class EstadoVisual { NORMAL, DESHABILITADO, ADVERTENCIA, ERROR }`
  - `@Composable fun TarjetaTelemetria(titulo: String, valor: String, detalle: String?, estado: EstadoVisual, modifier: Modifier)`
  - `@Composable fun BotonComando(texto: String, habilitacion: Habilitacion, critico: Boolean, onClick: () -> Unit, modifier: Modifier)`
  - `@Composable fun ChipEstadoOrden(orden: OrdenEnviada)`
  - `@Composable fun BannerAlerta(alerta: Alerta, onReconocer: (() -> Unit)?)`
  - `@Composable fun IndicadorAntiguedad(antiguedadMs: Long, datoAntiguo: Boolean, enlace: EstadoEnlace)`
  - `@Composable fun BarraProgresoRonda(completados: Int, total: Int, etiqueta: String)`

Estos son los componentes reutilizables que el Figma tiene que entregar en los cuatro estados. Son la contraparte en código de esa exigencia del enunciado.

- [ ] **Step 1: Escribir el enum de estado visual**

Create `ui/components/EstadoVisual.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Los cuatro estados visuales que el enunciado exige para cada componente. */
enum class EstadoVisual { NORMAL, DESHABILITADO, ADVERTENCIA, ERROR }

@Composable
fun EstadoVisual.colorDeFondo(): Color = when (this) {
    EstadoVisual.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
    EstadoVisual.DESHABILITADO -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    EstadoVisual.ADVERTENCIA -> Color(0xFFFFF4D6)
    EstadoVisual.ERROR -> MaterialTheme.colorScheme.errorContainer
}

@Composable
fun EstadoVisual.colorDeTexto(): Color = when (this) {
    EstadoVisual.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    EstadoVisual.DESHABILITADO -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    EstadoVisual.ADVERTENCIA -> Color(0xFF6B4E00)
    EstadoVisual.ERROR -> MaterialTheme.colorScheme.onErrorContainer
}
```

- [ ] **Step 2: Escribir la tarjeta de telemetría**

Create `ui/components/TarjetaTelemetria.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TarjetaTelemetria(
    titulo: String,
    valor: String,
    detalle: String? = null,
    estado: EstadoVisual = EstadoVisual.NORMAL,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = estado.colorDeFondo())
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = estado.colorDeTexto()
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                color = estado.colorDeTexto()
            )
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = estado.colorDeTexto()
                )
            }
        }
    }
}
```

- [ ] **Step 3: Escribir el botón de comando**

Create `ui/components/BotonComando.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Habilitacion

/**
 * El botón se deshabilita solo y muestra POR QUÉ está deshabilitado.
 * Un botón gris sin explicación deja al operador sin saber qué hacer.
 */
@Composable
fun BotonComando(
    texto: String,
    habilitacion: Habilitacion,
    critico: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Button(
            onClick = onClick,
            enabled = habilitacion.habilitada,
            modifier = Modifier.fillMaxWidth(),
            colors = if (critico) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) else ButtonDefaults.buttonColors()
        ) {
            Text(texto)
        }
        if (!habilitacion.habilitada && habilitacion.detalle != null) {
            Text(
                text = habilitacion.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
```

- [ ] **Step 4: Escribir el chip de estado de orden**

Create `ui/components/ChipEstadoOrden.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.OrdenEnviada

/**
 * Muestra DOS cosas distintas: el estado de la orden y si su efecto se
 * verificó. Una orden confirmada no es una orden ejecutada.
 */
@Composable
fun ChipEstadoOrden(orden: OrdenEnviada, modifier: Modifier = Modifier) {
    val estadoVisual = when {
        orden.ejecucionFallida -> EstadoVisual.ERROR
        orden.estado == EstadoOrden.RECHAZADA -> EstadoVisual.ERROR
        orden.estado == EstadoOrden.SIN_RESPUESTA -> EstadoVisual.ADVERTENCIA
        orden.estado == EstadoOrden.PENDIENTE -> EstadoVisual.ADVERTENCIA
        else -> EstadoVisual.NORMAL
    }

    val lineaEjecucion = when {
        orden.estado != EstadoOrden.CONFIRMADA -> null
        orden.ejecucionFallida ->
            "Efecto NO verificado. La detención por software no reemplaza una parada física"
        orden.ejecucionVerificada -> "Efecto verificado: ${orden.criterioEjecucion}"
        else -> "Verificando efecto: ${orden.criterioEjecucion}"
    }

    Surface(
        color = estadoVisual.colorDeFondo(),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = "${orden.comandoId} · ${orden.orden.name}",
                style = MaterialTheme.typography.labelMedium,
                color = estadoVisual.colorDeTexto()
            )
            Text(
                text = orden.estado.name,
                style = MaterialTheme.typography.titleSmall,
                color = estadoVisual.colorDeTexto()
            )
            if (orden.detalleRechazo != null) {
                Text(
                    text = "Motivo: ${orden.motivoRechazo} — ${orden.detalleRechazo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = estadoVisual.colorDeTexto()
                )
            }
            if (lineaEjecucion != null) {
                Text(
                    text = lineaEjecucion,
                    style = MaterialTheme.typography.bodySmall,
                    color = estadoVisual.colorDeTexto()
                )
            }
        }
    }
}
```

- [ ] **Step 5: Escribir el banner de alerta**

Create `ui/components/BannerAlerta.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Alerta
import ar.edu.uade.salastecnicas.model.Severidad

@Composable
fun BannerAlerta(
    alerta: Alerta,
    onReconocer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val estado = when {
        alerta.reconocida -> EstadoVisual.DESHABILITADO
        alerta.severidad == Severidad.CRITICA -> EstadoVisual.ERROR
        alerta.severidad == Severidad.ADVERTENCIA -> EstadoVisual.ADVERTENCIA
        else -> EstadoVisual.NORMAL
    }

    Surface(
        color = estado.colorDeFondo(),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = "${alerta.severidad.name}${if (alerta.reconocida) " · RECONOCIDA" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = estado.colorDeTexto()
            )
            Text(
                text = alerta.titulo,
                style = MaterialTheme.typography.titleSmall,
                color = estado.colorDeTexto()
            )
            Text(
                text = alerta.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = estado.colorDeTexto()
            )
            if (onReconocer != null && !alerta.reconocida) {
                TextButton(onClick = onReconocer) {
                    Text("Reconocer")
                }
                Text(
                    text = "Reconocer registra que la viste. No resuelve la alerta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = estado.colorDeTexto()
                )
            }
        }
    }
}
```

- [ ] **Step 6: Escribir el indicador de antigüedad**

Create `ui/components/IndicadorAntiguedad.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.EstadoEnlace

/**
 * Riesgo 4: el operador no debe poder confundir el último valor conocido
 * con el valor actual. Por eso la antigüedad se muestra siempre, en segundos.
 */
@Composable
fun IndicadorAntiguedad(
    antiguedadMs: Long,
    datoAntiguo: Boolean,
    enlace: EstadoEnlace,
    modifier: Modifier = Modifier
) {
    val estado = when {
        enlace == EstadoEnlace.PERDIDO -> EstadoVisual.ERROR
        datoAntiguo || enlace == EstadoEnlace.DEGRADADO -> EstadoVisual.ADVERTENCIA
        else -> EstadoVisual.NORMAL
    }
    val segundos = antiguedadMs / 1000

    Surface(
        color = estado.colorDeFondo(),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = "Enlace ${enlace.name} · actualizado hace $segundos s",
                style = MaterialTheme.typography.titleSmall,
                color = estado.colorDeTexto()
            )
            if (enlace == EstadoEnlace.PERDIDO) {
                Text(
                    text = "DATOS ANTIGUOS. El robot puede estar en otra posición " +
                        "y en otro estado que el que muestra esta pantalla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = estado.colorDeTexto()
                )
            }
        }
    }
}
```

- [ ] **Step 7: Escribir la barra de progreso de la ronda**

Create `ui/components/BarraProgresoRonda.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BarraProgresoRonda(
    completados: Int,
    total: Int,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    val fraccion = if (total == 0) 0f else completados.toFloat() / total
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = "$etiqueta · $completados de $total puntos",
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
            progress = { fraccion },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
```

- [ ] **Step 8: Verificar que compila**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

Si `LinearProgressIndicator` se queja de la firma, la versión de Material 3 del proyecto es anterior a la que toma `progress` como lambda. En ese caso usar `LinearProgressIndicator(progress = fraccion, ...)`.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "agrega componentes compose reutilizables con sus cuatro estados visuales"
```

---

## Task 10: Navegación, selección de robot y panel de supervisión

**Files:**
- Create: `ui/AppNavigation.kt`, `ui/screens/SeleccionRobotScreen.kt`, `ui/screens/SupervisionScreen.kt`
- Modify: `MainActivity.kt`

**Interfaces:**
- Consumes: `RobotViewModel`, `UiState`, todos los componentes de la Task 9.
- Produces:
  - `@Composable fun AppNavigation(vm: RobotViewModel)`
  - `@Composable fun SeleccionRobotScreen(estado: UiState, onSeleccionar: (Robot) -> Unit)`
  - `@Composable fun SupervisionScreen(estado: UiState, onIrA: (Pantalla) -> Unit)`
  - `fun formatearGrados(valor: Double): String` y `fun formatearMetros(valor: Double?): String` en `ui/Formato.kt`

- [ ] **Step 1: Escribir los formateadores compartidos**

Create `ui/Formato.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui

/** Coma decimal, como se escribe en español. */
fun formatearGrados(valor: Double): String = "%.1f °C".format(valor).replace('.', ',')

fun formatearMetros(valor: Double?): String =
    if (valor == null) "—" else "%.1f m".format(valor).replace('.', ',')

fun formatearVelocidad(valor: Double): String = "%.1f m/s".format(valor).replace('.', ',')

/** El reloj del simulador son milisegundos desde el arranque. */
fun formatearReloj(ms: Long): String {
    val totalSeg = ms / 1000
    return "%02d:%02d".format(totalSeg / 60, totalSeg % 60)
}
```

- [ ] **Step 2: Escribir la pantalla de selección de robot**

Create `ui/screens/SeleccionRobotScreen.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.colorDeFondo
import ar.edu.uade.salastecnicas.ui.components.colorDeTexto
import ar.edu.uade.salastecnicas.viewmodel.UiState

@Composable
fun SeleccionRobotScreen(
    estado: UiState,
    onSeleccionar: (Robot) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Selección de robot", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Verificación previa de salas técnicas · UADE",
            style = MaterialTheme.typography.bodyMedium
        )

        estado.robots.forEach { robot ->
            val visual = if (robot.disponible) EstadoVisual.NORMAL else EstadoVisual.DESHABILITADO
            Card(
                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = visual.colorDeFondo())
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        robot.id,
                        style = MaterialTheme.typography.titleMedium,
                        color = visual.colorDeTexto()
                    )
                    Text(
                        robot.modelo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = visual.colorDeTexto()
                    )
                    if (robot.motivoNoDisponible != null) {
                        Text(
                            robot.motivoNoDisponible,
                            style = MaterialTheme.typography.bodySmall,
                            color = visual.colorDeTexto()
                        )
                    }
                    Button(
                        onClick = { onSeleccionar(robot) },
                        enabled = robot.disponible,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(if (robot.disponible) "Supervisar" else "No disponible")
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Escribir la pantalla de supervisión**

Create `ui/screens/SupervisionScreen.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.ui.components.BannerAlerta
import ar.edu.uade.salastecnicas.ui.components.BarraProgresoRonda
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.IndicadorAntiguedad
import ar.edu.uade.salastecnicas.ui.components.TarjetaTelemetria
import ar.edu.uade.salastecnicas.ui.formatearGrados
import ar.edu.uade.salastecnicas.ui.formatearMetros
import ar.edu.uade.salastecnicas.ui.formatearVelocidad
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * Resumen arriba y telemetría completa abajo, en una sola pantalla scrolleable.
 * El enunciado permite combinar vistas si la navegación sigue siendo clara.
 */
@Composable
fun SupervisionScreen(
    estado: UiState,
    onIrA: (Pantalla) -> Unit,
    modifier: Modifier = Modifier
) {
    val robot = estado.estadoRobot
    val tele = robot.telemetria

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Panel de supervisión", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${robot.robotSeleccionado?.id ?: "—"} · operador sin línea de vista",
            style = MaterialTheme.typography.bodySmall
        )

        if (tele == null) {
            Text(
                "Sin robot seleccionado.",
                modifier = Modifier.padding(top = 16.dp)
            )
            return@Column
        }

        IndicadorAntiguedad(
            antiguedadMs = robot.antiguedadMs,
            datoAntiguo = robot.datoAntiguo,
            enlace = tele.enlace,
            modifier = Modifier.padding(top = 12.dp)
        )

        // ---- Resumen ----
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            TarjetaTelemetria(
                titulo = "Estado",
                valor = tele.estadoOperativo.name,
                detalle = tele.sala,
                estado = if (robot.esperandoDecision) EstadoVisual.ERROR else EstadoVisual.NORMAL,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )
            TarjetaTelemetria(
                titulo = "Batería",
                valor = "${tele.bateriaPct} %",
                detalle = "Autonomía ${tele.autonomiaMin} min",
                estado = estadoDeBateria(tele.autonomiaMin),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        BarraProgresoRonda(
            completados = tele.puntosCompletados,
            total = tele.puntosTotales,
            etiqueta = robot.mision?.nombre ?: "Sin ronda activa"
        )

        robot.alertas.filter { !it.reconocida }.forEach { alerta ->
            BannerAlerta(alerta = alerta)
        }

        // ---- Accesos ----
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(
                onClick = { onIrA(Pantalla.MISION) },
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) { Text("Misión") }
            OutlinedButton(
                onClick = { onIrA(Pantalla.CONTROL) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) { Text("Control") }
            OutlinedButton(
                onClick = { onIrA(Pantalla.ALERTAS_HISTORIAL) },
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) { Text("Alertas") }
        }

        Divider(Modifier.padding(vertical = 16.dp))

        // ---- Telemetría completa ----
        Text("Telemetría", style = MaterialTheme.typography.titleMedium)

        TarjetaTelemetria(
            "Conectividad",
            tele.enlace.name,
            "Latencia ${tele.latenciaMs} ms",
            if (tele.enlace == EstadoEnlace.OK) EstadoVisual.NORMAL else EstadoVisual.ADVERTENCIA
        )
        TarjetaTelemetria("Estado operativo", tele.estadoOperativo.name)
        TarjetaTelemetria(
            "Misión y progreso",
            robot.mision?.id ?: "—",
            "${tele.puntosCompletados} de ${tele.puntosTotales} puntos"
        )
        TarjetaTelemetria("Ubicación", tele.sala, tele.puntoActualId ?: "—")
        TarjetaTelemetria(
            "Movimiento",
            formatearVelocidad(tele.velocidadMs),
            if (tele.caminando) "Caminando" else "Detenido"
        )
        TarjetaTelemetria(
            "Alertas activas",
            tele.alertasActivas.toString(),
            "Sin reconocer"
        )
        TarjetaTelemetria(
            "Última actualización",
            "hace ${robot.antiguedadMs / 1000} s",
            if (robot.datoAntiguo) "Dato antiguo" else "Al día",
            if (robot.datoAntiguo) EstadoVisual.ADVERTENCIA else EstadoVisual.NORMAL
        )

        Text(
            "Variables del servicio",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )

        TarjetaTelemetria(
            "Temperatura máxima del punto",
            formatearGrados(tele.temperaturaMaxC),
            "Umbral de falla ${formatearGrados(Parametros.TEMPERATURA_FALLA_C)} · " +
                "requiere cámara térmica adicional",
            if (tele.temperaturaMaxC >= Parametros.TEMPERATURA_FALLA_C) EstadoVisual.ERROR
            else EstadoVisual.NORMAL
        )
        TarjetaTelemetria(
            "Cobertura del entorno",
            "${tele.coberturaPct} %",
            "${tele.presencias} presencia(s) a ${formatearMetros(tele.distanciaPresenciaM)}. " +
                "El sector no cubierto es zona ciega: no detectar no es que no haya nadie",
            if (tele.presencias > 0) EstadoVisual.ADVERTENCIA else EstadoVisual.NORMAL
        )
        TarjetaTelemetria(
            "Margen de estabilidad",
            "${tele.margenEstabilidadPct} %",
            "Punto débil del G1: pisa fuerte y la estabilidad es su límite",
            if (tele.margenEstabilidadPct < 60) EstadoVisual.ADVERTENCIA else EstadoVisual.NORMAL
        )
    }
}

private fun estadoDeBateria(autonomiaMin: Int): EstadoVisual = when {
    autonomiaMin < Parametros.REGRESO_MIN -> EstadoVisual.ERROR
    autonomiaMin < 70 -> EstadoVisual.ADVERTENCIA
    else -> EstadoVisual.NORMAL
}
```

- [ ] **Step 4: Escribir la navegación**

Create `ui/AppNavigation.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.ui.screens.SeleccionRobotScreen
import ar.edu.uade.salastecnicas.ui.screens.SupervisionScreen
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModel

/**
 * Navegación con un when sobre un enum del estado.
 * Con seis pantallas no hace falta Navigation Compose, y así queda una
 * dependencia menos que explicar.
 */
@Composable
fun AppNavigation(vm: RobotViewModel) {
    val estado by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (estado.pantalla != Pantalla.SELECCION_ROBOT) {
                NavigationBar {
                    PestañaNav(estado.pantalla, Pantalla.SUPERVISION, "Panel", vm)
                    PestañaNav(estado.pantalla, Pantalla.MISION, "Misión", vm)
                    PestañaNav(estado.pantalla, Pantalla.CONTROL, "Control", vm)
                    PestañaNav(estado.pantalla, Pantalla.ALERTAS_HISTORIAL, "Alertas", vm)
                    PestañaNav(estado.pantalla, Pantalla.MODO_DEMO, "Demo", vm)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (estado.pantalla) {
                Pantalla.SELECCION_ROBOT ->
                    SeleccionRobotScreen(estado, vm::seleccionarRobot)

                Pantalla.SUPERVISION ->
                    SupervisionScreen(estado, vm::irA)

                // Las cuatro pantallas restantes llegan en las Task 11 y 12.
                else -> Column(Modifier.padding(16.dp)) {
                    Text(
                        "Pantalla ${estado.pantalla.name} en construcción",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (estado.ultimaRespuesta != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Snackbar(
                        modifier = Modifier.padding(12.dp),
                        action = {
                            Text(
                                "OK",
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickableSinRipple { vm.limpiarAviso() }
                            )
                        }
                    ) { Text(estado.ultimaRespuesta) }
                }
            }
        }
    }
}

@Composable
private fun PestañaNav(
    actual: Pantalla,
    destino: Pantalla,
    texto: String,
    vm: RobotViewModel
) {
    NavigationBarItem(
        selected = actual == destino,
        onClick = { vm.irA(destino) },
        icon = { Text(texto.take(1)) },
        label = { Text(texto) }
    )
}
```

Y agregar al final de `ui/Formato.kt` el modificador que usa el Snackbar:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

/** Clic simple, sin ripple, para el texto de acción del aviso. */
fun Modifier.clickableSinRipple(onClick: () -> Unit): Modifier = this.clickable { onClick() }
```

- [ ] **Step 5: Conectar MainActivity**

Modify `MainActivity.kt`. Reemplazar su contenido por:

```kotlin
package ar.edu.uade.salastecnicas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.uade.salastecnicas.ui.AppNavigation
import ar.edu.uade.salastecnicas.ui.theme.SalasTecnicasTheme
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModel
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repositorio = (application as SalasApp).repositorio
        setContent {
            SalasTecnicasTheme {
                val vm: RobotViewModel = viewModel(
                    factory = RobotViewModelFactory(repositorio)
                )
                AppNavigation(vm)
            }
        }
    }
}
```

El nombre del tema lo generó la plantilla a partir del nombre del proyecto. Si no se llama `SalasTecnicasTheme`, usar el que esté en `ui/theme/Theme.kt`.

- [ ] **Step 6: Verificar que compila y corre**

Run: `./gradlew assembleDebug test`
Expected: `BUILD SUCCESSFUL` y PASS, 100 tests.

Instalar y abrir la app en el emulador. Se debe ver la lista de tres robots, con G1-02 y GO2-01 deshabilitados, y al elegir G1-01 el panel de supervisión con la telemetría viva: la batería baja de a poco y la última actualización se mantiene en 0 s.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "agrega navegacion, seleccion de robot y panel de supervision"
```

---

## Task 11: Pantallas de misión y control, con sus tres modales

**Files:**
- Create: `ui/screens/MisionScreen.kt`, `ui/screens/ControlScreen.kt`, `ui/dialogs/DecisionAnomaliaDialog.kt`, `ui/dialogs/AutorizacionSupervisorDialog.kt`, `ui/dialogs/ConfirmacionDialog.kt`
- Modify: `ui/AppNavigation.kt`

**Interfaces:**
- Consumes: `UiState`, `Dialogo`, componentes de la Task 9.
- Produces:
  - `@Composable fun MisionScreen(estado: UiState)`
  - `@Composable fun ControlScreen(estado: UiState, onPedirOrden: (Orden) -> Unit)`
  - `@Composable fun DecisionAnomaliaDialog(estado: UiState, onDecidir: (DecisionAnomalia) -> Unit, onPedirOrden: (Orden) -> Unit)`
  - `@Composable fun AutorizacionSupervisorDialog(error: String?, onEnviar: (String) -> Unit, onCerrar: () -> Unit)`
  - `@Composable fun ConfirmacionDialog(titulo: String, cuerpo: String, textoConfirmar: String, onConfirmar: () -> Unit, onCerrar: () -> Unit)`

- [ ] **Step 1: Escribir la pantalla de misión**

Create `ui/screens/MisionScreen.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import ar.edu.uade.salastecnicas.sim.Rondas
import ar.edu.uade.salastecnicas.ui.components.BarraProgresoRonda
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.colorDeFondo
import ar.edu.uade.salastecnicas.ui.components.colorDeTexto
import ar.edu.uade.salastecnicas.ui.formatearGrados
import ar.edu.uade.salastecnicas.viewmodel.UiState

@Composable
fun MisionScreen(estado: UiState, modifier: Modifier = Modifier) {
    val mision = estado.estadoRobot.mision

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Ronda de verificación", style = MaterialTheme.typography.headlineSmall)

        if (mision == null) {
            Text(
                "No hay ronda activa. Los cinco puntos del recorrido son:",
                modifier = Modifier.padding(top = 8.dp)
            )
            Rondas.PUNTOS.forEach { punto ->
                FilaPunto(
                    titulo = punto.sala,
                    subtitulo = punto.queVerifica,
                    detalle = "${punto.minutosVerificacion} min estimados",
                    visual = EstadoVisual.DESHABILITADO
                )
            }
            return@Column
        }

        Text(mision.nombre, style = MaterialTheme.typography.bodyMedium)
        BarraProgresoRonda(mision.completados, mision.total, mision.id)

        if (mision.cerrada && !mision.completa) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EstadoVisual.ERROR.colorDeFondo()
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Ronda incompleta",
                        style = MaterialTheme.typography.titleSmall,
                        color = EstadoVisual.ERROR.colorDeTexto()
                    )
                    Text(
                        "Quedaron ${mision.puntosSinVerificar.size} puntos sin verificar. " +
                            "La cuadrilla de mantenimiento no puede ingresar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EstadoVisual.ERROR.colorDeTexto()
                    )
                }
            }
        }

        mision.puntos.forEachIndexed { indice, punto ->
            val resultado = mision.resultados.getOrNull(indice)
            val visual = when {
                resultado == null && indice == mision.completados -> EstadoVisual.ADVERTENCIA
                resultado == null -> EstadoVisual.DESHABILITADO
                resultado.resultado == ResultadoPunto.FALLA -> EstadoVisual.ERROR
                resultado.resultado == ResultadoPunto.OBSERVACION -> EstadoVisual.ADVERTENCIA
                else -> EstadoVisual.NORMAL
            }
            val detalle = when {
                resultado != null -> "${resultado.resultado.name} · " +
                    formatearGrados(resultado.temperaturaMaxC) + " · " +
                    if (resultado.ausenciaTensionConfirmada) "ausencia de tensión confirmada"
                    else "ausencia de tensión NO confirmada"
                indice == mision.completados -> "En curso"
                else -> "Pendiente"
            }
            FilaPunto(punto.sala, punto.queVerifica, detalle, visual)
        }
    }
}

@Composable
private fun FilaPunto(
    titulo: String,
    subtitulo: String,
    detalle: String,
    visual: EstadoVisual
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = visual.colorDeFondo())
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                color = visual.colorDeTexto()
            )
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = visual.colorDeTexto()
            )
            Text(
                detalle,
                style = MaterialTheme.typography.labelMedium,
                color = visual.colorDeTexto()
            )
        }
    }
}
```

- [ ] **Step 2: Escribir la pantalla de control**

Create `ui/screens/ControlScreen.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.ui.components.BotonComando
import ar.edu.uade.salastecnicas.ui.components.ChipEstadoOrden
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * Las órdenes comunes arriba y las críticas al final, separadas y en rojo,
 * fuera de la zona donde cae el pulgar. Es la medida de interfaz del riesgo 3.
 */
@Composable
fun ControlScreen(
    estado: UiState,
    onPedirOrden: (Orden) -> Unit,
    modifier: Modifier = Modifier
) {
    fun hab(orden: Orden): Habilitacion =
        estado.habilitaciones[orden] ?: Habilitacion(false, "SIN_DATOS", "Sin datos")

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Control", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Órdenes discretas. La app no ofrece control manual continuo: " +
                "en modo manual el G1 no consulta sus sensores de entorno.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "Misión",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        BotonComando("Iniciar ronda", hab(Orden.INICIAR_MISION)) {
            onPedirOrden(Orden.INICIAR_MISION)
        }
        BotonComando("Pausar", hab(Orden.PAUSAR)) { onPedirOrden(Orden.PAUSAR) }
        BotonComando("Reanudar", hab(Orden.REANUDAR)) { onPedirOrden(Orden.REANUDAR) }
        BotonComando("Cancelar ronda", hab(Orden.CANCELAR)) { onPedirOrden(Orden.CANCELAR) }

        Text(
            "Contingencias",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        BotonComando("Regresar al punto seguro", hab(Orden.REGRESO_PUNTO_SEGURO)) {
            onPedirOrden(Orden.REGRESO_PUNTO_SEGURO)
        }
        BotonComando("Reconocer alertas", hab(Orden.RECONOCER_ALERTAS)) {
            onPedirOrden(Orden.RECONOCER_ALERTAS)
        }

        Divider(Modifier.padding(vertical = 20.dp))

        Text("Órdenes críticas", style = MaterialTheme.typography.titleMedium)
        Text(
            "La detención es una SOLICITUD por software. No reemplaza una parada " +
                "de emergencia física: el G1 no tiene corte físico. Ante una " +
                "detención no ejecutada, recurrir al acompañante presencial.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        BotonComando(
            texto = "Solicitar detención operativa",
            habilitacion = hab(Orden.DETENCION_OPERATIVA),
            critico = true
        ) { onPedirOrden(Orden.DETENCION_OPERATIVA) }

        BotonComando(
            texto = "Accionar seccionador",
            habilitacion = hab(Orden.ACCIONAR_SECCIONADOR),
            critico = true
        ) { onPedirOrden(Orden.ACCIONAR_SECCIONADOR) }

        Divider(Modifier.padding(vertical = 20.dp))

        Text("Órdenes enviadas", style = MaterialTheme.typography.titleMedium)
        if (estado.estadoRobot.ordenes.isEmpty()) {
            Text("Todavía no se envió ninguna orden.")
        }
        estado.estadoRobot.ordenes.reversed().forEach { orden ->
            ChipEstadoOrden(orden)
        }
    }
}
```

- [ ] **Step 3: Escribir el modal de decisión de anomalía**

Create `ui/dialogs/DecisionAnomaliaDialog.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.ui.formatearGrados
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * El robot se detuvo y espera. No se puede descartar este modal:
 * el subproceso exige una decisión del operador.
 */
@Composable
fun DecisionAnomaliaDialog(
    estado: UiState,
    onDecidir: (DecisionAnomalia) -> Unit,
    onPedirOrden: (Orden) -> Unit
) {
    val resultado = estado.estadoRobot.mision?.resultados?.lastOrNull()
    val sala = estado.estadoRobot.telemetria?.sala ?: "—"
    val aislado = resultado?.ausenciaTensionConfirmada == true

    AlertDialog(
        onDismissRequest = { /* no se puede descartar */ },
        title = { Text("Anomalía en $sala") },
        text = {
            Column {
                Text(
                    "Lectura de ${formatearGrados(resultado?.temperaturaMaxC ?: 0.0)}. " +
                        "El punto quedó marcado como FALLA y el robot está detenido " +
                        "esperando tu decisión."
                )
                Text(
                    if (aislado) "Ausencia de tensión confirmada."
                    else "Ausencia de tensión NO confirmada.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedButton(
                    onClick = { onPedirOrden(Orden.ACCIONAR_SECCIONADOR) },
                    enabled = estado.habilitaciones[Orden.ACCIONAR_SECCIONADOR]
                        ?.habilitada == true ||
                        estado.habilitaciones[Orden.ACCIONAR_SECCIONADOR]
                            ?.motivo == "CODIGO_INVALIDO",
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Accionar seccionador (requiere supervisor)") }
            }
        },
        confirmButton = {
            Button(onClick = { onDecidir(DecisionAnomalia.AISLAR) }, enabled = aislado) {
                Text("Aislar y continuar")
            }
        },
        dismissButton = {
            Column {
                OutlinedButton(onClick = { onDecidir(DecisionAnomalia.MARCAR_Y_CONTINUAR) }) {
                    Text("Marcar y continuar")
                }
                OutlinedButton(onClick = { onDecidir(DecisionAnomalia.ABORTAR) }) {
                    Text("Abortar y regresar")
                }
            }
        }
    )
}
```

- [ ] **Step 4: Escribir el modal de autorización**

Create `ui/dialogs/AutorizacionSupervisorDialog.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import ar.edu.uade.salastecnicas.model.Parametros

@Composable
fun AutorizacionSupervisorDialog(
    error: String?,
    onEnviar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Autorización de supervisor") },
        text = {
            Column {
                Text(
                    "Accionar un seccionador requiere autorización nominal. " +
                        "La autorización vence a los " +
                        "${Parametros.VIGENCIA_AUTORIZACION_MS / 1000} s y queda registrada."
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Código") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (error != null) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onEnviar(codigo) }, enabled = codigo.isNotBlank()) {
                Text("Autorizar y accionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}
```

- [ ] **Step 5: Escribir el modal de confirmación de dos pasos**

Create `ui/dialogs/ConfirmacionDialog.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmacionDialog(
    titulo: String,
    cuerpo: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = { Text(cuerpo) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(textoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Volver") }
        }
    )
}
```

- [ ] **Step 6: Conectar las dos pantallas y los tres modales**

Modify `ui/AppNavigation.kt`. En el `when (estado.pantalla)`, reemplazar las ramas por:

```kotlin
                Pantalla.SELECCION_ROBOT ->
                    SeleccionRobotScreen(estado, vm::seleccionarRobot)

                Pantalla.SUPERVISION ->
                    SupervisionScreen(estado, vm::irA)

                Pantalla.MISION ->
                    MisionScreen(estado)

                Pantalla.CONTROL ->
                    ControlScreen(estado, vm::pedirOrden)

                // Las dos pantallas restantes llegan en la Task 12.
                else -> Column(Modifier.padding(16.dp)) {
                    Text(
                        "Pantalla ${estado.pantalla.name} en construcción",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
```

Y agregar, inmediatamente después del bloque del Snackbar y dentro del mismo `Box`:

```kotlin
            when (estado.dialogo) {
                Dialogo.NINGUNO -> Unit

                Dialogo.DECISION_ANOMALIA ->
                    DecisionAnomaliaDialog(estado, vm::decidir, vm::pedirOrden)

                Dialogo.AUTORIZACION -> AutorizacionSupervisorDialog(
                    error = estado.errorAutorizacion,
                    onEnviar = vm::enviarAutorizacion,
                    onCerrar = vm::cerrarDialogo
                )

                Dialogo.CONFIRMAR_CANCELAR -> ConfirmacionDialog(
                    titulo = "Cancelar la ronda",
                    cuerpo = "La ronda queda cerrada como incompleta y los puntos " +
                        "sin verificar se registran. La cuadrilla de mantenimiento " +
                        "no podrá ingresar hasta completar una ronda.",
                    textoConfirmar = "Cancelar la ronda",
                    onConfirmar = vm::confirmarOrden,
                    onCerrar = vm::cerrarDialogo
                )

                Dialogo.CONFIRMAR_SECCIONADOR -> ConfirmacionDialog(
                    titulo = "Accionar el seccionador",
                    cuerpo = "Se va a accionar un seccionador de potencia. " +
                        "La acción queda registrada a nombre del supervisor que autorizó.",
                    textoConfirmar = "Accionar",
                    onConfirmar = vm::confirmarOrden,
                    onCerrar = vm::cerrarDialogo
                )
            }
```

Agregar los imports necesarios:

```kotlin
import ar.edu.uade.salastecnicas.ui.dialogs.AutorizacionSupervisorDialog
import ar.edu.uade.salastecnicas.ui.dialogs.ConfirmacionDialog
import ar.edu.uade.salastecnicas.ui.dialogs.DecisionAnomaliaDialog
import ar.edu.uade.salastecnicas.ui.screens.ControlScreen
import ar.edu.uade.salastecnicas.ui.screens.MisionScreen
import ar.edu.uade.salastecnicas.viewmodel.Dialogo
```

- [ ] **Step 7: Verificar que compila y probar a mano**

Run: `./gradlew assembleDebug test`
Expected: `BUILD SUCCESSFUL` y PASS, 100 tests.

En el emulador: elegir G1-01, ir a Control, iniciar la ronda, ver el progreso en Misión, pausar y reanudar. Probar cancelar y comprobar que aparece la confirmación de dos pasos y que, al confirmar, Misión muestra el cartel de ronda incompleta.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "agrega pantallas de mision y control con sus tres modales"
```

---

## Task 12: Pantallas de alertas e historial, y modo demo

**Files:**
- Create: `ui/screens/AlertasHistorialScreen.kt`, `ui/screens/ModoDemoScreen.kt`
- Modify: `ui/AppNavigation.kt`

**Interfaces:**
- Consumes: `UiState`, `ResumenRonda`, componentes de la Task 9.
- Produces:
  - `@Composable fun AlertasHistorialScreen(estado: UiState, onReconocer: () -> Unit)`
  - `@Composable fun ModoDemoScreen(estado: UiState, vm: RobotViewModel)`

- [ ] **Step 1: Escribir la pantalla de alertas e historial**

Create `ui/screens/AlertasHistorialScreen.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.data.ResumenRonda
import ar.edu.uade.salastecnicas.ui.components.BannerAlerta
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.colorDeFondo
import ar.edu.uade.salastecnicas.ui.components.colorDeTexto
import ar.edu.uade.salastecnicas.ui.formatearReloj
import ar.edu.uade.salastecnicas.viewmodel.UiState

@Composable
fun AlertasHistorialScreen(
    estado: UiState,
    onReconocer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pestaña by remember { mutableStateOf(0) }

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pestaña) {
            Tab(
                selected = pestaña == 0,
                onClick = { pestaña = 0 },
                text = { Text("Alertas (${estado.estadoRobot.alertas.count { !it.reconocida }})") }
            )
            Tab(
                selected = pestaña == 1,
                onClick = { pestaña = 1 },
                text = { Text("Historial (${estado.historial.size})") }
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (pestaña == 0) {
                if (estado.estadoRobot.alertas.isEmpty()) {
                    Text("No hay alertas registradas.")
                }
                estado.estadoRobot.alertas.reversed().forEach { alerta ->
                    BannerAlerta(alerta = alerta, onReconocer = onReconocer)
                }
            } else {
                if (estado.historial.isEmpty()) {
                    Text("Todavía no se registró ninguna ronda.")
                }
                estado.historial.forEach { ronda -> TarjetaRonda(ronda) }
            }
        }
    }
}

@Composable
private fun TarjetaRonda(ronda: ResumenRonda) {
    val visual = when {
        !ronda.cerrada -> EstadoVisual.ADVERTENCIA
        ronda.completa -> EstadoVisual.NORMAL
        else -> EstadoVisual.ERROR
    }
    val cierre = when {
        !ronda.cerrada -> "En curso"
        ronda.completa -> "Completa"
        else -> "Incompleta: la cuadrilla no puede ingresar"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = visual.colorDeFondo())
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${ronda.misionId} · ${formatearReloj(ronda.iniciadaTsMs)}",
                style = MaterialTheme.typography.titleSmall,
                color = visual.colorDeTexto()
            )
            Text(
                "${ronda.puntosCompletados} de ${ronda.puntosTotales} puntos · $cierre",
                style = MaterialTheme.typography.bodySmall,
                color = visual.colorDeTexto()
            )
            ronda.eventos.forEach { evento ->
                // Si llegó con retraso, se muestran los dos momentos: es la
                // reconciliación posterior a una desconexión.
                val retrasado = evento.recibidoTsMs > evento.tsMs
                val sello = if (retrasado)
                    "${formatearReloj(evento.tsMs)} (recibido ${formatearReloj(evento.recibidoTsMs)})"
                else formatearReloj(evento.tsMs)
                Text(
                    "· $sello  ${evento.clase.name}  ${evento.detalle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = visual.colorDeTexto(),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Escribir la pantalla de modo demo**

Create `ui/screens/ModoDemoScreen.kt`:

```kotlin
package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModel
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * Herramienta de prueba, no parte de la app que se entregaría a un cliente.
 * Existe para que cada escenario se pueda reproducir a voluntad, tanto para
 * grabar las evidencias como para la defensa.
 */
@Composable
fun ModoDemoScreen(
    estado: UiState,
    vm: RobotViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Modo demo", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pantalla de prueba. No forma parte de la app de operación: " +
                "sirve para reproducir cada escenario de forma repetible.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "Escenarios",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        BotonEscenario(
            "1 · Misión normal",
            "Reinicia todo. Iniciar la ronda desde Control y verla completarse."
        ) { vm.aplicarEscenario(Escenario.MISION_NORMAL) }

        BotonEscenario(
            "2 · Batería baja",
            "Ronda en curso con 40 % de batería: se pausa sola y avisa. " +
                "Continuar se deshabilita cuando ya no alcanza para el regreso."
        ) { vm.aplicarEscenario(Escenario.BATERIA_BAJA) }

        BotonEscenario(
            "3 · Desconexión",
            "Ronda en curso sin enlace: datos antiguos, órdenes sin respuesta y, " +
                "al restablecer, reconciliación de eventos en el historial."
        ) { vm.aplicarEscenario(Escenario.DESCONEXION) }

        BotonEscenario(
            "4 · Incidente",
            "El primer punto va a medir 78,4 °C: falla, alerta crítica y " +
                "decisión obligatoria del operador."
        ) { vm.aplicarEscenario(Escenario.INCIDENTE) }

        BotonEscenario(
            "5 · Orden confirmada que no se ejecuta",
            "La detención operativa responde confirmada y el robot sigue caminando. " +
                "Enviarla desde Control y mirar el estado de la orden."
        ) { vm.aplicarEscenario(Escenario.ORDEN_NO_EJECUTADA) }

        Divider(Modifier.padding(vertical = 20.dp))

        Text("Fallas sueltas", style = MaterialTheme.typography.titleMedium)

        OutlinedButton(
            onClick = { vm.forzarEnlace(EstadoEnlace.PERDIDO) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Cortar el enlace") }

        OutlinedButton(
            onClick = { vm.forzarEnlace(EstadoEnlace.DEGRADADO) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Degradar el enlace") }

        OutlinedButton(
            onClick = { vm.forzarEnlace(EstadoEnlace.OK) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Restablecer el enlace") }

        OutlinedButton(
            onClick = { vm.forzarPresencia(1.8) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Presencia a 1,8 m en zona ciega") }

        OutlinedButton(
            onClick = { vm.forzarPresencia(null) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Despejar la presencia") }

        OutlinedButton(
            onClick = { vm.forzarBateria(43.0) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Bajar la batería a 43 % (rechaza el inicio)") }

        Divider(Modifier.padding(vertical = 20.dp))

        Button(
            onClick = { vm.reiniciar() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reiniciar todo y borrar el historial") }

        Text(
            "Reloj del simulador: ${estado.estadoRobot.telemetria?.tsMs ?: 0} ms. " +
                "1 segundo real equivale a 1 minuto simulado.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun BotonEscenario(titulo: String, descripcion: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(titulo) }
        Text(
            descripcion,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
```

- [ ] **Step 3: Conectar las dos pantallas**

Modify `ui/AppNavigation.kt`. Reemplazar la rama `else` del `when (estado.pantalla)` por:

```kotlin
                Pantalla.ALERTAS_HISTORIAL -> AlertasHistorialScreen(
                    estado = estado,
                    onReconocer = { vm.pedirOrden(Orden.RECONOCER_ALERTAS) }
                )

                Pantalla.MODO_DEMO ->
                    ModoDemoScreen(estado, vm)
```

Agregar los imports:

```kotlin
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.ui.screens.AlertasHistorialScreen
import ar.edu.uade.salastecnicas.ui.screens.ModoDemoScreen
```

Ahora el `when` cubre los seis valores de `Pantalla` y ya no necesita rama `else`. Si el compilador avisa que falta, es que quedó alguna pantalla sin conectar.

- [ ] **Step 4: Verificar que compila**

Run: `./gradlew assembleDebug test`
Expected: `BUILD SUCCESSFUL` y PASS, 100 tests.

- [ ] **Step 5: Recorrer los cinco escenarios a mano**

En el emulador, desde Modo demo, disparar cada escenario y verificar:

1. **Misión normal**: la ronda avanza punto por punto y termina con la ronda completa en el historial.
2. **Batería baja**: aparece la advertencia, el robot se pausa y Reanudar muestra el motivo si no alcanza.
3. **Desconexión**: la antigüedad del dato sube, la detención queda sin respuesta, y al restablecer el enlace el historial muestra eventos con los dos momentos.
4. **Incidente**: se abre el modal de anomalía y no se puede descartar. Accionar el seccionador pide el código `2048`.
5. **Orden no ejecutada**: la detención dice CONFIRMADA y el chip avisa que el efecto no se verificó.

Si alguno no se comporta así, el problema está en el simulador y no en la UI: los tests de las Tasks 2 a 6 lo cubren, así que revisar cuál falla.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "agrega pantallas de alertas, historial y modo demo"
```

---

## Task 13: Contrato, README, APK y evidencias

**Files:**
- Create: `README.md`, `docs/evidencias/README.md`
- Modify: `docs/superpowers/specs/2026-11-17-hmi-g1-verificacion-salas-tecnicas-design.md` (sección 12, si hace falta)

**Interfaces:**
- Consumes: la app terminada de las Tasks 1 a 12.
- Produces: APK instalable, README y la carpeta de evidencias con los cinco escenarios.

- [ ] **Step 1: Verificar que el contrato JSON documentado coincide con lo implementado**

El enunciado pide documentar con ejemplos JSON el contrato de telemetría, comandos y respuestas. Esos ejemplos están en la sección 12 del documento de diseño, y se escribieron antes del código: hay que confirmar que siguen siendo verdaderos.

Abrir `model/Telemetria.kt` y comparar campo por campo contra el ejemplo de telemetría del diseño:

| JSON del diseño | Campo en `Telemetria` |
|---|---|
| `enlace.estado` | `enlace` |
| `enlace.latenciaMs` | `latenciaMs` |
| `bateria.porcentaje` | `bateriaPct` |
| `bateria.autonomiaMin` | `autonomiaMin` |
| `estadoOperativo` | `estadoOperativo` |
| `mision.id` | `misionId` |
| `mision.puntoActual` | `puntoActualId` |
| `mision.completados` | `puntosCompletados` |
| `mision.total` | `puntosTotales` |
| `ubicacion.sala` | `sala` |
| `movimiento.velocidadMs` | `velocidadMs` |
| `movimiento.caminando` | `caminando` |
| `servicio.temperaturaMaxC` | `temperaturaMaxC` |
| `servicio.entorno.coberturaPct` | `coberturaPct` |
| `servicio.entorno.presencias` | `presencias` |
| `servicio.margenEstabilidadPct` | `margenEstabilidadPct` |
| `alertasActivas` | `alertasActivas` |

El modelo tiene además `distanciaPresenciaM`, que el JSON del diseño no muestra. Agregarlo al ejemplo de la sección 12, dentro de `servicio.entorno`:

```json
      "distanciaPresenciaM": null
```

Lo mismo con las respuestas: el ejemplo del diseño trae `ejecucion.verificada` y `ejecucion.criterio`, que corresponden a `OrdenEnviada.ejecucionVerificada` y `OrdenEnviada.criterioEjecucion`. Agregar al ejemplo de respuesta confirmada el tercer campo que el código tiene:

```json
    "fallida": false
```

No hay que implementar serialización JSON: el contrato es documentación de lo que viajaría si el robot fuera real. El APK no usa red.

- [ ] **Step 2: Escribir el README**

Create `README.md`:

```markdown
# HMI/HRI para verificación previa de salas técnicas — Unitree G1

TP grupal de Desarrollo de Aplicaciones I — UADE, Facultad de Ingeniería y Cs. Exactas.

## Qué es

App Android de supervisión y operación de un Unitree G1 que recorre las salas
técnicas del edificio verificando condiciones antes de que la cuadrilla de
mantenimiento intervenga un tablero.

El robot es simulado. **La app funciona sin robot, sin servidor y sin internet**,
y no declara ningún permiso.

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
2. **Batería baja** — ronda en curso al 40 %: se pausa sola y avisa.
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

- La UI observa un único `StateFlow<UiState>` y nunca accede al simulador.
- `RobotRepository` es la única frontera: cambiar la implementación simulada por
  una real no obligaría a tocar la UI.
- El simulador es **determinista**: no usa `Random` ni el reloj del sistema, por
  lo que los tests son confiables y la demo se comporta siempre igual.
- Room guarda **una sola tabla de eventos**; el historial de rondas se deriva por
  consulta, agrupando por misión.

## Decisiones que conviene saber

- **No hay joystick.** En modo manual el G1 no consulta sus sensores de entorno,
  así que un control remoto continuo operado sin línea de vista sería la
  principal fuente de riesgo. Todo el control es por órdenes discretas.
- **La detención operativa es el único comando sin condiciones de habilitación.**
  Rechazar una parada por batería baja sería inaceptable.
- **Orden confirmada no es orden ejecutada.** Cada orden lleva, además de su
  estado, un indicador de si su efecto se verificó en la telemetría. El G1 real
  no tiene corte físico de emergencia.
- **Reconocer una alerta no la resuelve.** Sólo registra que el operador la vio.

## Documentos

- Diseño: `docs/superpowers/specs/2026-11-17-hmi-g1-verificacion-salas-tecnicas-design.md`
- Plan de implementación: `docs/superpowers/plans/2026-11-17-apk-hmi-salas-tecnicas.md`
- Evidencias: `docs/evidencias/`

## Limitaciones declaradas

- La medición de temperatura requiere una cámara térmica que el G1 no trae de
  fábrica. Lo mismo la detección de gases y la lectura de instrumentos analógicos.
- El accionamiento de seccionadores requeriría validación de torque y
  certificación eléctrica, fuera del alcance del trabajo.
- El contrato de mensajes es una propuesta propia, **no** el protocolo oficial de
  Unitree.
- No hay validación con hardware físico.
```

- [ ] **Step 3: Generar el APK y verificar que se instala**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`, con el APK en `app/build/outputs/apk/debug/app-debug.apk`.

Instalarlo en un emulador limpio y abrirlo. Verificar que arranca en la selección de robot y que no pide ningún permiso.

- [ ] **Step 4: Capturar las evidencias de los cinco escenarios**

Create `docs/evidencias/README.md`:

```markdown
# Evidencias de los cinco escenarios

Cada escenario se dispara desde la pantalla **Modo demo** y se documenta con
capturas en orden. El archivo se nombra `N-escenario-M-descripcion.png`.

## 1. Misión normal

- `1-mision-normal-1-inicio.png` — Control con "Iniciar ronda" habilitado.
- `1-mision-normal-2-progreso.png` — Misión con 3 de 5 puntos en OK.
- `1-mision-normal-3-completa.png` — Historial con la ronda completa.

## 2. Batería baja

- `2-bateria-1-advertencia.png` — Panel con la alerta de advertencia y el robot
  pausado.
- `2-bateria-2-continuar-habilitado.png` — Control con "Reanudar" habilitado.
- `2-bateria-3-continuar-deshabilitado.png` — "Reanudar" deshabilitado con el
  motivo visible: la autonomía ya no cubre lo que falta más el regreso.

## 3. Desconexión

- `3-desconexion-1-dato-antiguo.png` — Panel con el enlace PERDIDO y la
  antigüedad del dato en segundos.
- `3-desconexion-2-sin-respuesta.png` — Control con una orden en SIN_RESPUESTA.
- `3-desconexion-3-reconciliacion.png` — Historial con eventos que muestran el
  momento de origen y el de recepción.

## 4. Incidente

- `4-incidente-1-modal.png` — Modal de anomalía con la lectura de 78,4 °C y los
  tres caminos.
- `4-incidente-2-autorizacion.png` — Modal de autorización de supervisor.
- `4-incidente-3-resultado.png` — Misión con el punto en FALLA y la ausencia de
  tensión confirmada tras aislar.

## 5. Orden confirmada que no se ejecuta

- `5-no-ejecutada-1-confirmada.png` — Orden en CONFIRMADA con el efecto todavía
  sin verificar.
- `5-no-ejecutada-2-no-verificada.png` — La misma orden marcada como efecto NO
  verificado, con el aviso de recurrir al acompañante presencial.
- `5-no-ejecutada-3-alerta.png` — Alerta crítica "La detención no se ejecutó".

## Cómo capturar

En el emulador, `Ctrl + S` guarda una captura. Desde la línea de comandos:

```bash
adb exec-out screencap -p > docs/evidencias/1-mision-normal-1-inicio.png
```
```

Capturar las quince imágenes siguiendo esa lista.

- [ ] **Step 5: Grabar el video de demostración**

Recorrer los cinco escenarios de corrido, narrando en cada uno qué se está
viendo y por qué importa. Puntos que conviene decir en voz alta, porque son
los que distinguen el trabajo:

- En el escenario 3, que el robot **sigue trabajando** aunque la app no lo vea.
- En el escenario 5, que la orden dice confirmada y el robot no se detuvo, y que
  por eso existe el acompañante presencial: el G1 no tiene corte físico.
- Que ningún botón se deshabilita sin decir por qué.

Grabar con el emulador:

```bash
adb shell screenrecord /sdcard/demo.mp4
```

- [ ] **Step 6: Correr la suite completa una última vez**

Run: `./gradlew assembleDebug test`
Expected: `BUILD SUCCESSFUL` y PASS, 100 tests.

No afirmar que algo funciona sin haber visto esta salida.

- [ ] **Step 7: Commit y subir**

```bash
git add -A
git commit -m "agrega readme, evidencias de los cinco escenarios y verifica el contrato"
git push -u origin main
```

---

## Cobertura del enunciado

Repaso de cada exigencia del TP contra la tarea que la cumple.

| Exigencia del enunciado | Dónde se cumple |
|---|---|
| Caso de uso en lugar real, cómo se resuelve hoy, actores | Documento de diseño, secciones 1 y 4 |
| Esquema del entorno y recorrido | `sim/Rondas.kt` (Task 1) + relevamiento en sitio |
| Justificar G1 frente a Go2 y frente a no usar robot | Diseño, sección 1 |
| Investigar sensores, movilidad, autonomía, restricciones | Diseño, sección 2 |
| Distinguir capacidades disponibles de las que requieren desarrollo | Diseño, sección 2 + Task 13 Step 1 |
| HMI del operador y HRI con las personas presentes | Tasks 9 a 12 + diseño, sección 4 |
| Roles y situaciones de intervención humana | Diseño, sección 4 + Tasks 6 y 11 |
| Telemetría: las ocho obligatorias | `model/Telemetria.kt` (Task 1), `SupervisionScreen` (Task 10) |
| Telemetría: tres variables del servicio | Las mismas, bloque "Variables del servicio" |
| Comandos: iniciar, pausar, reanudar, cancelar | Task 3 |
| Comandos: regreso, detención, reconocer alertas | Task 3 |
| Condiciones de habilitación, respuesta y estado visible | Task 3 + `BotonComando` y `ChipEstadoOrden` (Task 9) |
| Una orden enviada no es una orden ejecutada | Task 4 |
| Figma: selección, panel, telemetría, misiones, control, alertas, historial, desconexión | Tasks 10 a 12, seis pantallas y tres modales |
| Componentes reutilizables en cuatro estados visuales | Task 9 |
| Flujo normal y flujo con falla | Tasks 2 y 6; escenarios 1 y 4 |
| Kotlin, Compose, MVVM, ViewModel, StateFlow | Tasks 8 a 12 |
| Repository que desacopla telemetría, comandos y eventos | Task 7 |
| Separar modelos, Repository, ViewModel y componentes | Estructura de archivos |
| Persistir el historial localmente | Task 7, una sola tabla en Room |
| Funciona sin robot, servidor ni internet | Sin permisos en el manifiesto (Task 7 Step 10) |
| Simulador que actualiza batería y telemetría | Task 2 |
| Simulador que ejecuta misiones coherentes con sus estados | Tasks 2 y 6 |
| Simulador que emite alertas | Tasks 2, 4 y 6 |
| Simulador que permite forzar desconexiones y rechazos | Tasks 3, 5 y 12 |
| Contrato JSON documentado, no oficial de Unitree | Diseño, sección 12 + Task 13 Step 1 |
| Los cinco escenarios demostrables | Tasks 2 a 6, disparables desde Task 12 |
| Cinco riesgos con medidas de interfaz, lógica y procedimiento | Diseño, sección 9; medidas en Tasks 3 a 12 |
| Diagrama de arquitectura | README (Task 13) + diseño, sección 11 |
| Evidencias de los cinco escenarios | Task 13 Step 4 |
| Registro de decisiones y limitaciones | Diseño, sección 13 + README |
| Video de demostración | Task 13 Step 5 |

**Queda fuera del plan, porque no es código:** el prototipo navegable en Figma, el
relevamiento en sitio de las salas y el documento de preentrega. Las Tasks 9 a 12
definen las pantallas, los componentes y los cuatro estados visuales que el Figma
tiene que reproducir, así que el diseño gráfico puede hacerse en paralelo contra
esa misma lista.
