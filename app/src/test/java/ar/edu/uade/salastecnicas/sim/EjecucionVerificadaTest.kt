package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.Severidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El corazón del trabajo: que el robot acuse recibo de una orden no significa
 * que la haya ejecutado.
 */
class EjecucionVerificadaTest {

    @Test
    fun `una orden recien confirmada todavia no esta verificada`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
        assertFalse(orden.ejecucionVerificada)
        assertFalse(orden.ejecucionFallida)
    }

    @Test
    fun `la detencion se verifica cuando la velocidad se mantiene en cero`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.VERIFICACION_DETENCION_MS)
        val orden = sim.ordenes().last()
        assertTrue(orden.ejecucionVerificada)
        assertFalse(orden.ejecucionFallida)
    }

    @Test
    fun `con la detencion ignorada el robot confirma pero sigue caminando`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)

        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
        assertTrue(sim.telemetria().caminando)
    }

    @Test
    fun `vencido el plazo sin efecto la orden queda marcada como ejecucion fallida`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)

        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)
        val orden = sim.ordenes().last()
        assertFalse(orden.ejecucionVerificada)
        assertTrue(orden.ejecucionFallida)
    }

    @Test
    fun `estar quieto verificando un punto no cuenta como haber obedecido`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)

        // Durante el tramo el robot llega al punto y se queda quieto para
        // verificarlo. Eso lo deja con velocidad 0 sin haberse detenido.
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)
        assertEquals(EstadoOperativo.VERIFICANDO_PUNTO, sim.estadoReal())
        assertEquals(0.0, sim.telemetria().velocidadMs, 0.001)
        assertTrue(sim.ordenes().last().ejecucionFallida)
    }

    @Test
    fun `una detencion no ejecutada emite una alerta critica`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)

        val alerta = sim.alertas().last()
        assertEquals(Severidad.CRITICA, alerta.severidad)
        assertTrue(alerta.titulo.contains("no se ejecutó"))
        assertTrue(alerta.detalle.contains("acompañante presencial"))
    }

    @Test
    fun `una detencion no ejecutada deja un evento en el historial`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)

        val eventos = sim.drenarEventos()
        assertTrue(eventos.any { it.clase == ClaseEvento.DETENCION_NO_EJECUTADA })
    }

    @Test
    fun `la alerta critica por detencion no ejecutada se emite una sola vez`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS * 3)

        assertEquals(1, sim.alertas().count { it.severidad == Severidad.CRITICA })
    }

    @Test
    fun `al dejar de ignorar la detencion una orden nueva si se verifica`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarDetencionIgnorada(true)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS)

        sim.forzarDetencionIgnorada(false)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.VERIFICACION_DETENCION_MS)
        assertTrue(sim.ordenes().last().ejecucionVerificada)
    }

    @Test
    fun `pausar se verifica en cuanto el estado pasa a pausado`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.avanzarMs(Parametros.TICK_MS)
        assertTrue(sim.ordenes().last().ejecucionVerificada)
    }

    @Test
    fun `una orden rechazada nunca se marca como verificada ni como fallida`() {
        val sim = RobotSimulator()
        sim.forzarBateria(43.0)
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMs(Parametros.PLAZO_VERIFICACION_MS * 2)
        val orden = sim.ordenes().last()
        assertEquals(EstadoOrden.RECHAZADA, orden.estado)
        assertFalse(orden.ejecucionVerificada)
        assertFalse(orden.ejecucionFallida)
    }
}
