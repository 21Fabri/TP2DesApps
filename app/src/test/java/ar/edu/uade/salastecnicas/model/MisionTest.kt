package ar.edu.uade.salastecnicas.model

import ar.edu.uade.salastecnicas.sim.Rondas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MisionTest {

    private fun misionConResultados(cantidad: Int): Mision {
        val resultados = Rondas.PUNTOS.take(cantidad).map {
            ResultadoVerificacion(it.id, ResultadoPunto.OK, it.temperaturaNormalC, true)
        }
        return Mision("RND-001", Rondas.NOMBRE_RONDA, Rondas.PUNTOS, resultados)
    }

    @Test
    fun `una mision sin resultados esta en cero por ciento`() {
        val mision = misionConResultados(0)
        assertEquals(0, mision.completados)
        assertEquals(5, mision.total)
        assertEquals(0, mision.progresoPct)
        assertFalse(mision.completa)
    }

    @Test
    fun `el progreso se calcula sobre el total de puntos`() {
        assertEquals(40, misionConResultados(2).progresoPct)
    }

    @Test
    fun `una mision con todos los resultados esta completa`() {
        val mision = misionConResultados(5)
        assertTrue(mision.completa)
        assertEquals(100, mision.progresoPct)
        assertTrue(mision.puntosSinVerificar.isEmpty())
    }

    @Test
    fun `los puntos sin verificar son los que siguen al ultimo resultado`() {
        val mision = misionConResultados(3)
        assertEquals(2, mision.puntosSinVerificar.size)
        assertEquals("P4_ASCENSORES", mision.puntosSinVerificar.first().id)
    }

    @Test
    fun `el punto actual es el primero sin resultado`() {
        assertEquals("P3_UPS", misionConResultados(2).puntoActual?.id)
    }
}
