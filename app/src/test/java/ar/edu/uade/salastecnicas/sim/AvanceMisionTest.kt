package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Avanza el simulador la cantidad de minutos simulados indicada. */
fun RobotSimulator.avanzarMinutos(minutos: Int) {
    val ticks = (minutos * 60_000L / Parametros.FACTOR_DEMO) / Parametros.TICK_MS
    repeat(ticks.toInt()) { avanzar(Parametros.TICK_MS) }
}

/** Avanza el simulador la cantidad de milisegundos de reloj indicada. */
fun RobotSimulator.avanzarMs(ms: Long) {
    repeat((ms / Parametros.TICK_MS).toInt()) { avanzar(Parametros.TICK_MS) }
}

class AvanceMisionTest {

    @Test
    fun `el robot arranca en espera y sin mision`() {
        val sim = RobotSimulator()
        assertEquals(EstadoOperativo.EN_ESPERA, sim.telemetria().estadoOperativo)
        assertNull(sim.mision())
        assertEquals(100, sim.telemetria().bateriaPct)
        assertEquals(Rondas.SALA_PUNTO_SEGURO, sim.telemetria().sala)
    }

    @Test
    fun `al iniciar la ronda el robot se traslada al primer punto`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.telemetria().estadoOperativo)
        assertTrue(sim.telemetria().caminando)
        assertEquals(Parametros.VELOCIDAD_MARCHA_MS, sim.telemetria().velocidadMs, 0.001)
        assertNotNull(sim.mision())
        assertEquals(0, sim.mision()!!.completados)
    }

    @Test
    fun `terminado el traslado el robot verifica el punto y deja de caminar`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(Parametros.TRASLADO_MIN)
        assertEquals(EstadoOperativo.VERIFICANDO_PUNTO, sim.telemetria().estadoOperativo)
        assertEquals(0.0, sim.telemetria().velocidadMs, 0.001)
        assertEquals("P1_TABLERO_GENERAL", sim.telemetria().puntoActualId)
        assertEquals("Sala de tablero general", sim.telemetria().sala)
    }

    @Test
    fun `al terminar la verificacion queda un resultado OK con la temperatura del punto`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion)
        val mision = sim.mision()!!
        assertEquals(1, mision.completados)
        val resultado = mision.resultados.first()
        assertEquals("P1_TABLERO_GENERAL", resultado.puntoId)
        assertEquals(ResultadoPunto.OK, resultado.resultado)
        assertEquals(38.5, resultado.temperaturaMaxC, 0.001)
        assertTrue(resultado.ausenciaTensionConfirmada)
    }

    @Test
    fun `la ronda dura cuarenta y ocho minutos entre verificaciones y traslados`() {
        assertEquals(48, RobotSimulator().minutosRonda())
    }

    @Test
    fun `la ronda completa los cinco puntos y despues retorna`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(48)
        assertEquals(5, sim.mision()!!.completados)
        assertEquals(EstadoOperativo.RETORNANDO, sim.estadoReal())
    }

    @Test
    fun `terminado el regreso la mision queda cerrada y completa`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(48 + Parametros.REGRESO_MIN)
        val mision = sim.mision()!!
        assertTrue(mision.cerrada)
        assertTrue(mision.completa)
        assertEquals(EstadoOperativo.EN_ESPERA, sim.estadoReal())
        assertEquals(Rondas.SALA_PUNTO_SEGURO, sim.telemetria().sala)
    }

    @Test
    fun `la bateria baja mientras el robot trabaja`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.avanzarMinutos(30)
        // 30 de 150 minutos de autonomía son 20 puntos de batería
        assertEquals(80, sim.telemetria().bateriaPct)
        assertEquals(120, sim.telemetria().autonomiaMin)
    }

    @Test
    fun `la bateria casi no baja mientras el robot espera`() {
        val sim = RobotSimulator()
        sim.avanzarMinutos(30)
        assertEquals(96, sim.telemetria().bateriaPct)
    }

    @Test
    fun `iniciar la ronda registra un evento`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        val eventos = sim.drenarEventos()
        assertEquals(1, eventos.size)
        assertEquals(ClaseEvento.RONDA_INICIADA, eventos.first().clase)
    }

    @Test
    fun `drenar eventos los saca de la cola`() {
        val sim = RobotSimulator()
        sim.iniciarRonda()
        sim.drenarEventos()
        assertTrue(sim.drenarEventos().isEmpty())
    }
}
