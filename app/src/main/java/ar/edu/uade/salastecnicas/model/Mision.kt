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
