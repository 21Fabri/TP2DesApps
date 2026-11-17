package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import ar.edu.uade.salastecnicas.model.Severidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomaliaTest {

    /** Arranca una ronda con el primer punto en falla por temperatura. */
    private fun simConAnomaliaEnP1(): RobotSimulator {
        val sim = RobotSimulator()
        sim.forzarTemperaturaAlta("P1_TABLERO_GENERAL", 78.4)
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion)
        return sim
    }

    // ---- Subproceso de anomalía ----

    @Test
    fun `un punto por encima del umbral queda en falla y detiene la ronda`() {
        val sim = simConAnomaliaEnP1()
        assertEquals(EstadoOperativo.ESPERANDO_DECISION, sim.estadoReal())
        assertTrue(sim.esperandoDecision())
        val resultado = sim.mision()!!.resultados.first()
        assertEquals(ResultadoPunto.FALLA, resultado.resultado)
        assertEquals(78.4, resultado.temperaturaMaxC, 0.001)
        assertFalse(resultado.ausenciaTensionConfirmada)
    }

    @Test
    fun `la falla emite una alerta critica`() {
        val sim = simConAnomaliaEnP1()
        val alerta = sim.alertas().last()
        assertEquals(Severidad.CRITICA, alerta.severidad)
        assertTrue(alerta.detalle.contains("78,4"))
    }

    @Test
    fun `sin decision del operador la ronda no avanza`() {
        val sim = simConAnomaliaEnP1()
        sim.avanzarMinutos(30)
        assertEquals(1, sim.mision()!!.completados)
        assertEquals(EstadoOperativo.ESPERANDO_DECISION, sim.estadoReal())
    }

    @Test
    fun `marcar y continuar deja la falla registrada y sigue con el punto siguiente`() {
        val sim = simConAnomaliaEnP1()
        sim.decidir(DecisionAnomalia.MARCAR_Y_CONTINUAR)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[1].minutosVerificacion)
        assertEquals(2, sim.mision()!!.completados)
        assertEquals(ResultadoPunto.OK, sim.mision()!!.resultados[1].resultado)
    }

    @Test
    fun `abortar cierra la ronda como incompleta y manda al robot de regreso`() {
        val sim = simConAnomaliaEnP1()
        sim.decidir(DecisionAnomalia.ABORTAR)
        assertEquals(EstadoOperativo.RETORNANDO, sim.estadoReal())
        val mision = sim.mision()!!
        assertTrue(mision.cerrada)
        assertFalse(mision.completa)
        assertEquals(4, mision.puntosSinVerificar.size)
    }

    // ---- Autorización de supervisor ----

    @Test
    fun `accionar el seccionador se rechaza sin autorizacion`() {
        val sim = simConAnomaliaEnP1()
        val orden = sim.enviar(Orden.ACCIONAR_SECCIONADOR)
        assertEquals(EstadoOrden.RECHAZADA, orden.estado)
        assertEquals("CODIGO_INVALIDO", orden.motivoRechazo)
    }

    @Test
    fun `un codigo equivocado no habilita la autorizacion`() {
        val sim = simConAnomaliaEnP1()
        assertFalse(sim.ingresarAutorizacion("0000"))
        assertEquals(
            "CODIGO_INVALIDO",
            sim.habilitacion(Orden.ACCIONAR_SECCIONADOR, "0000").motivo
        )
    }

    @Test
    fun `con el codigo correcto se puede accionar el seccionador`() {
        val sim = simConAnomaliaEnP1()
        assertTrue(sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR))
        val orden = sim.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
    }

    @Test
    fun `no se puede pausar durante el accionamiento`() {
        val sim = simConAnomaliaEnP1()
        sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        sim.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        assertEquals("ACCIONAMIENTO_EN_CURSO", sim.habilitacion(Orden.PAUSAR).motivo)
    }

    @Test
    fun `la autorizacion vence pasado su plazo`() {
        val sim = simConAnomaliaEnP1()
        sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        assertTrue(sim.autorizacionVigente())

        sim.avanzarMs(Parametros.VIGENCIA_AUTORIZACION_MS + 1_000)

        assertFalse(sim.autorizacionVigente())
        assertEquals(
            "AUTORIZACION_VENCIDA",
            sim.habilitacion(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR).motivo
        )
    }

    @Test
    fun `aislar confirma la ausencia de tension en el punto`() {
        val sim = simConAnomaliaEnP1()
        sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        sim.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        sim.avanzarMs(Parametros.ACCIONAMIENTO_MS + 1_000)
        sim.decidir(DecisionAnomalia.AISLAR)
        assertTrue(sim.mision()!!.resultados.first().ausenciaTensionConfirmada)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
    }

    @Test
    fun `el accionamiento se verifica cuando termina`() {
        val sim = simConAnomaliaEnP1()
        sim.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        sim.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        sim.avanzarMs(Parametros.ACCIONAMIENTO_MS + 1_000)
        assertTrue(sim.ordenes().last().ejecucionVerificada)
    }

    // ---- Batería baja ----

    @Test
    fun `al bajar la bateria del umbral la ronda se pausa con una advertencia`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(40.0) // 60 minutos, contra 48 + 9 más margen
        sim.avanzar(Parametros.TICK_MS)

        assertEquals(EstadoOperativo.PAUSADO, sim.estadoReal())
        val alerta = sim.alertas().last()
        assertEquals(Severidad.ADVERTENCIA, alerta.severidad)
        assertTrue(alerta.titulo.contains("Batería"))
    }

    @Test
    fun `la advertencia de bateria baja se emite una sola vez`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(40.0)
        repeat(20) { sim.avanzar(Parametros.TICK_MS) }
        assertEquals(1, sim.alertas().count { it.severidad == Severidad.ADVERTENCIA })
    }

    @Test
    fun `con bateria baja se puede continuar si todavia alcanza para el regreso`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(40.0) // 60 min, y faltan 48 + 9 = 57
        sim.avanzar(Parametros.TICK_MS)
        assertTrue(sim.habilitacion(Orden.REANUDAR).habilitada)
    }

    @Test
    fun `cuando ya no alcanza para el regreso continuar se deshabilita solo`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarBateria(30.0) // 45 min, y faltan 48 + 9 = 57
        sim.avanzar(Parametros.TICK_MS)
        assertFalse(sim.habilitacion(Orden.REANUDAR).habilitada)
        assertEquals("BATERIA_INSUFICIENTE", sim.habilitacion(Orden.REANUDAR).motivo)
        assertTrue(sim.habilitacion(Orden.REGRESO_PUNTO_SEGURO).habilitada)
    }
}
