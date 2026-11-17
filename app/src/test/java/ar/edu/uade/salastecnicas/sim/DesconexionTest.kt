package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesconexionTest {

    /** Minutos de reloj que tarda el primer punto: traslado más verificación. */
    private val msPrimerPunto =
        (Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion) * 1_000L

    @Test
    fun `con enlace disponible el dato no es antiguo`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMs(5_000)
        assertFalse(sim.datoAntiguo())
        assertEquals(0L, sim.antiguedadMs())
    }

    @Test
    fun `perdido el enlace la telemetria deja de actualizarse`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMs(2_000)
        val tsAntes = sim.telemetria().tsMs

        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(6_000)

        assertEquals(tsAntes, sim.telemetria().tsMs)
        assertTrue(sim.antiguedadMs() >= 6_000)
        assertTrue(sim.datoAntiguo())
    }

    @Test
    fun `el robot sigue trabajando aunque la app no lo vea`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(msPrimerPunto)
        assertEquals(1, sim.mision()!!.completados)
    }

    @Test
    fun `una orden enviada sin enlace queda pendiente y no aplica efecto`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)

        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOrden.PENDIENTE, orden.estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
    }

    @Test
    fun `una orden pendiente pasa a sin respuesta al vencer el plazo`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.enviar(Orden.DETENCION_OPERATIVA)

        sim.avanzarMs(Parametros.VENCIMIENTO_ORDEN_MS + Parametros.TICK_MS)
        assertEquals(EstadoOrden.SIN_RESPUESTA, sim.ordenes().last().estado)
    }

    @Test
    fun `una orden sin respuesta no se reintenta sola al volver el enlace`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        sim.avanzarMs(Parametros.VENCIMIENTO_ORDEN_MS + Parametros.TICK_MS)

        sim.forzarEnlace(EstadoEnlace.OK)
        sim.avanzarMs(2_000)

        assertEquals(EstadoOrden.SIN_RESPUESTA, sim.ordenes().last().estado)
        assertNotEquals(EstadoOperativo.DETENIDO, sim.estadoReal())
    }

    @Test
    fun `los eventos generados a ciegas no se entregan hasta que vuelve el enlace`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.drenarEventos()

        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(msPrimerPunto)
        assertTrue(sim.drenarEventos().isEmpty())

        sim.forzarEnlace(EstadoEnlace.OK)
        val eventos = sim.drenarEventos()
        assertTrue(eventos.any { it.clase == ClaseEvento.PUNTO_COMPLETADO })
    }

    @Test
    fun `un evento reconciliado tiene el momento de origen antes del de recepcion`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.drenarEventos()

        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        // Dos segundos extra para que la recepción sea posterior al origen
        sim.avanzarMs(msPrimerPunto + 2_000)
        sim.forzarEnlace(EstadoEnlace.OK)

        val evento = sim.drenarEventos().first { it.clase == ClaseEvento.PUNTO_COMPLETADO }
        assertTrue(evento.tsMs < evento.recibidoTsMs)
    }

    @Test
    fun `sin enlace por mas del plazo el robot regresa solo al punto seguro`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)

        sim.avanzarMs(Parametros.REGRESO_AUTONOMO_MS - 1_000)
        assertNotEquals(EstadoOperativo.RETORNANDO, sim.estadoReal())

        sim.avanzarMs(2_000)
        assertEquals(EstadoOperativo.RETORNANDO, sim.estadoReal())
    }

    @Test
    fun `el regreso autonomo se decide una sola vez`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(Parametros.REGRESO_AUTONOMO_MS + 5_000)
        sim.forzarEnlace(EstadoEnlace.OK)

        val eventos = sim.drenarEventos()
        assertEquals(1, eventos.count { it.detalle.contains("Regreso autónomo") })
    }

    @Test
    fun `al recuperar el enlace la telemetria vuelve a actualizarse`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(6_000)
        sim.forzarEnlace(EstadoEnlace.OK)
        sim.avanzarMs(Parametros.TICK_MS)

        assertFalse(sim.datoAntiguo())
        assertEquals(EstadoEnlace.OK, sim.telemetria().enlace)
    }

    @Test
    fun `forzar la bateria durante la desconexion no refresca el dato antiguo`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMs(2_000)
        sim.forzarEnlace(EstadoEnlace.PERDIDO)
        sim.avanzarMs(6_000)
        val tsAntes = sim.telemetria().tsMs

        sim.forzarBateria(55.0)

        assertEquals(tsAntes, sim.telemetria().tsMs)
        assertTrue(sim.datoAntiguo())
    }
}
