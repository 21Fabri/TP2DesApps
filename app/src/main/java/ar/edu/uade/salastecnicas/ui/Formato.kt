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
