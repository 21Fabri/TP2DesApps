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
