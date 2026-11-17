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
