package ar.edu.uade.salastecnicas.model

data class Alerta(
    val id: String,
    val severidad: Severidad,
    val titulo: String,
    val detalle: String,
    val tsMs: Long,
    val reconocida: Boolean = false
)
