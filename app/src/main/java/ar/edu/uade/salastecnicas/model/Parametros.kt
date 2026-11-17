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
    const val REGRESO_AUTONOMO_MS = 20_000L

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

    /** Duración del accionamiento de un seccionador, en ms de reloj. */
    const val ACCIONAMIENTO_MS = 4_000L
}
