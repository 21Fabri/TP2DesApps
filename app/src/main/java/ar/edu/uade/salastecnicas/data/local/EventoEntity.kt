package ar.edu.uade.salastecnicas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.Evento

/**
 * Única tabla persistida.
 *
 * El historial de rondas se deriva por consulta sobre esta tabla, agrupando
 * por misionId, así que no hace falta una segunda entidad.
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
