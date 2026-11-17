package ar.edu.uade.salastecnicas.model

data class Evento(
    val id: String,
    val misionId: String?,
    val clase: ClaseEvento,
    /** Momento en que ocurrió, según el reloj del robot. */
    val tsMs: Long,
    /** Momento en que la app lo recibió. Distinto de tsMs si hubo desconexión. */
    val recibidoTsMs: Long,
    val detalle: String
)
