package ar.edu.uade.salastecnicas.model

data class Robot(
    val id: String,
    val modelo: String,
    val disponible: Boolean,
    val motivoNoDisponible: String? = null
)
