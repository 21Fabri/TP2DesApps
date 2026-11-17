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
 *
 * Es la razón por la que alcanza una sola tabla en Room: el historial de
 * misiones no se guarda, se deduce.
 */
object HistorialMapper {

    fun aResumenes(eventos: List<Evento>): List<ResumenRonda> =
        eventos
            .filter { it.misionId != null }
            .groupBy { it.misionId!! }
            .map { (misionId, deLaRonda) -> resumir(misionId, deLaRonda) }
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
