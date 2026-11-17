package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.Evento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorialMapperTest {

    private fun evento(
        id: String,
        misionId: String?,
        clase: ClaseEvento,
        tsMs: Long,
        detalle: String = ""
    ) = Evento(id, misionId, clase, tsMs, tsMs, detalle)

    @Test
    fun `sin eventos el historial esta vacio`() {
        assertTrue(HistorialMapper.aResumenes(emptyList()).isEmpty())
    }

    @Test
    fun `los eventos sin mision no generan rondas`() {
        val eventos = listOf(evento("E1", null, ClaseEvento.ENLACE_PERDIDO, 100))
        assertTrue(HistorialMapper.aResumenes(eventos).isEmpty())
    }

    @Test
    fun `una ronda iniciada aparece como abierta`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100, "Verificación previa")
        )
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertEquals("RND-001", resumen.misionId)
        assertEquals("Verificación previa", resumen.nombre)
        assertEquals(100L, resumen.iniciadaTsMs)
        assertFalse(resumen.cerrada)
        assertEquals(0, resumen.puntosCompletados)
    }

    @Test
    fun `cuenta los puntos completados de la ronda`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E2", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200),
            evento("E3", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 300),
            evento("E4", "RND-001", ClaseEvento.ALERTA_EMITIDA, 350)
        )
        assertEquals(2, HistorialMapper.aResumenes(eventos).single().puntosCompletados)
    }

    @Test
    fun `una ronda cerrada con los cinco puntos queda completa`() {
        val eventos = buildList {
            add(evento("E0", "RND-001", ClaseEvento.RONDA_INICIADA, 100))
            repeat(5) { add(evento("E$it", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200L + it)) }
            add(evento("EF", "RND-001", ClaseEvento.RONDA_CERRADA, 900, "Ronda completa"))
        }
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertTrue(resumen.cerrada)
        assertTrue(resumen.completa)
        assertEquals(5, resumen.puntosCompletados)
    }

    @Test
    fun `una ronda cerrada con menos puntos queda incompleta`() {
        val eventos = listOf(
            evento("E0", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E1", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200),
            evento("EF", "RND-001", ClaseEvento.RONDA_CERRADA, 900, "Ronda cancelada")
        )
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertTrue(resumen.cerrada)
        assertFalse(resumen.completa)
    }

    @Test
    fun `las rondas salen de la mas reciente a la mas vieja`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E2", "RND-002", ClaseEvento.RONDA_INICIADA, 500),
            evento("E3", "RND-003", ClaseEvento.RONDA_INICIADA, 300)
        )
        val ids = HistorialMapper.aResumenes(eventos).map { it.misionId }
        assertEquals(listOf("RND-002", "RND-003", "RND-001"), ids)
    }

    @Test
    fun `cada ronda conserva sus eventos del mas reciente al mas viejo`() {
        val eventos = listOf(
            evento("E1", "RND-001", ClaseEvento.RONDA_INICIADA, 100),
            evento("E2", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 300),
            evento("E3", "RND-001", ClaseEvento.PUNTO_COMPLETADO, 200)
        )
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertEquals(listOf("E2", "E3", "E1"), resumen.eventos.map { it.id })
    }

    @Test
    fun `una ronda sin evento de inicio igual aparece con el nombre por defecto`() {
        val eventos = listOf(evento("E1", "RND-009", ClaseEvento.PUNTO_COMPLETADO, 400))
        val resumen = HistorialMapper.aResumenes(eventos).single()
        assertEquals("RND-009", resumen.misionId)
        assertEquals("Ronda sin registro de inicio", resumen.nombre)
        assertEquals(400L, resumen.iniciadaTsMs)
    }
}
