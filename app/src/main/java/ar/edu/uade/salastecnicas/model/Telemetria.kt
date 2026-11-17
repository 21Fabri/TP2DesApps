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
