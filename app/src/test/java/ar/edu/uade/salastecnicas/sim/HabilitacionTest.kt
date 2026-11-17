package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.Severidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabilitacionTest {

    // ---- Iniciar misión ----

    @Test
    fun `iniciar mision esta habilitado con el robot en espera y bateria llena`() {
        val sim = RobotSimulator()
        assertTrue(sim.habilitacion(Orden.INICIAR_MISION).habilitada)
    }

    @Test
    fun `iniciar mision se rechaza si la bateria no alcanza para la ronda y el regreso`() {
        val sim = RobotSimulator()
        // 66 minutos son los necesarios: (48 + 9) * 1,15
        assertEquals(66, sim.minutosNecesarios())
        sim.forzarBateria(43.0) // 64 minutos de autonomía
        val habilitacion = sim.habilitacion(Orden.INICIAR_MISION)
        assertFalse(habilitacion.habilitada)
        assertEquals("BATERIA_INSUFICIENTE", habilitacion.motivo)
        assertTrue(habilitacion.detalle!!.contains("48 min"))
        assertTrue(habilitacion.detalle!!.contains("64 min"))
    }

    @Test
    fun `iniciar mision se rechaza si hay una alerta critica sin reconocer`() {
        val sim = RobotSimulator()
        sim.emitirAlerta(Severidad.CRITICA, "Temperatura fuera de rango", "78,4 °C")
        val habilitacion = sim.habilitacion(Orden.INICIAR_MISION)
        assertFalse(habilitacion.habilitada)
        assertEquals("ALERTA_CRITICA_PENDIENTE", habilitacion.motivo)
    }

    @Test
    fun `reconocer la alerta critica vuelve a habilitar el inicio`() {
        val sim = RobotSimulator()
        sim.emitirAlerta(Severidad.CRITICA, "Temperatura fuera de rango", "78,4 °C")
        sim.enviar(Orden.RECONOCER_ALERTAS)
        assertTrue(sim.habilitacion(Orden.INICIAR_MISION).habilitada)
    }

    @Test
    fun `iniciar mision se rechaza si el robot ya esta en ronda`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        assertEquals("ESTADO_INVALIDO", sim.habilitacion(Orden.INICIAR_MISION).motivo)
    }

    @Test
    fun `enviar iniciar mision devuelve confirmada y arranca la ronda`() {
        val sim = RobotSimulator()
        val orden = sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
    }

    @Test
    fun `enviar una orden no habilitada devuelve rechazada con su motivo`() {
        val sim = RobotSimulator()
        sim.forzarBateria(43.0)
        val orden = sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.RECHAZADA, orden.estado)
        assertEquals("BATERIA_INSUFICIENTE", orden.motivoRechazo)
        assertEquals(EstadoOperativo.EN_ESPERA, sim.estadoReal())
    }

    // ---- Pausar y reanudar ----

    @Test
    fun `pausar detiene al robot y reanudar lo vuelve a poner en marcha`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.PAUSAR).estado)
        assertEquals(EstadoOperativo.PAUSADO, sim.estadoReal())
        assertEquals(0.0, sim.telemetria().velocidadMs, 0.001)

        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.REANUDAR).estado)
        assertEquals(EstadoOperativo.EN_TRASLADO, sim.estadoReal())
    }

    @Test
    fun `pausar se rechaza si el robot esta en espera`() {
        val sim = RobotSimulator()
        assertEquals("ESTADO_INVALIDO", sim.habilitacion(Orden.PAUSAR).motivo)
    }

    @Test
    fun `reanudar se rechaza si hay una presencia cerca en zona ciega`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.forzarPresencia(1.8)
        val habilitacion = sim.habilitacion(Orden.REANUDAR)
        assertFalse(habilitacion.habilitada)
        assertEquals("PRESENCIA_DETECTADA", habilitacion.motivo)
        assertTrue(habilitacion.detalle!!.contains("1,8 m"))
    }

    @Test
    fun `al alejarse la presencia reanudar vuelve a habilitarse`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.forzarPresencia(1.8)
        sim.forzarPresencia(null)
        assertTrue(sim.habilitacion(Orden.REANUDAR).habilitada)
    }

    @Test
    fun `reanudar se rechaza si no alcanza la bateria para terminar y regresar`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        sim.forzarBateria(20.0) // 30 minutos, contra 48 + 9 que faltan
        val habilitacion = sim.habilitacion(Orden.REANUDAR)
        assertFalse(habilitacion.habilitada)
        assertEquals("BATERIA_INSUFICIENTE", habilitacion.motivo)
    }

    // ---- Cancelar ----

    @Test
    fun `cancelar cierra la mision como incompleta y deja los puntos sin verificar`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.avanzarMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion)
        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.CANCELAR).estado)
        val mision = sim.mision()!!
        assertTrue(mision.cerrada)
        assertFalse(mision.completa)
        assertEquals(4, mision.puntosSinVerificar.size)
    }

    @Test
    fun `cancelar se rechaza si no hay mision activa`() {
        val sim = RobotSimulator()
        assertEquals("SIN_MISION_ACTIVA", sim.habilitacion(Orden.CANCELAR).motivo)
    }

    // ---- Regreso a punto seguro ----

    @Test
    fun `regreso a punto seguro pone al robot en retorno`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        assertEquals(EstadoOrden.CONFIRMADA, sim.enviar(Orden.REGRESO_PUNTO_SEGURO).estado)
        assertEquals(EstadoOperativo.RETORNANDO, sim.estadoReal())
    }

    @Test
    fun `regreso a punto seguro se rechaza sin localizacion valida`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarPerdidaLocalizacion(true)
        val habilitacion = sim.habilitacion(Orden.REGRESO_PUNTO_SEGURO)
        assertFalse(habilitacion.habilitada)
        assertEquals("SIN_LOCALIZACION", habilitacion.motivo)
    }

    // ---- Detención operativa ----

    @Test
    fun `la detencion operativa esta habilitada siempre, sin condiciones`() {
        val sim = RobotSimulator()
        assertTrue(sim.habilitacion(Orden.DETENCION_OPERATIVA).habilitada)

        sim.forzarBateria(1.0)
        sim.emitirAlerta(Severidad.CRITICA, "Falla", "Cualquiera")
        sim.forzarPresencia(0.4)
        sim.forzarPerdidaLocalizacion(true)
        assertTrue(sim.habilitacion(Orden.DETENCION_OPERATIVA).habilitada)
    }

    @Test
    fun `la detencion operativa deja al robot detenido`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.DETENCION_OPERATIVA)
        assertEquals(EstadoOperativo.DETENIDO, sim.estadoReal())
        assertEquals(0.0, sim.telemetria().velocidadMs, 0.001)
    }

    // ---- Reconocer alertas ----

    @Test
    fun `reconocer alertas se rechaza si no hay ninguna sin reconocer`() {
        val sim = RobotSimulator()
        assertEquals("SIN_ALERTAS", sim.habilitacion(Orden.RECONOCER_ALERTAS).motivo)
    }

    @Test
    fun `reconocer una alerta no la borra, solo la marca`() {
        val sim = RobotSimulator()
        sim.emitirAlerta(Severidad.ADVERTENCIA, "Batería baja", "Autonomía 41 min")
        sim.enviar(Orden.RECONOCER_ALERTAS)
        assertEquals(1, sim.alertas().size)
        assertTrue(sim.alertas().first().reconocida)
        assertEquals(0, sim.telemetria().alertasActivas)
    }

    // ---- Registro ----

    @Test
    fun `cada orden enviada queda en la lista de ordenes`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.enviar(Orden.PAUSAR)
        assertEquals(2, sim.ordenes().size)
        assertEquals(Orden.PAUSAR, sim.ordenes().last().orden)
    }

    @Test
    fun `una orden confirmada trae el criterio con el que se verifica su ejecucion`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        val orden = sim.enviar(Orden.DETENCION_OPERATIVA)
        assertTrue(orden.criterioEjecucion!!.contains("velocidad"))
    }

    @Test
    fun `el enlace degradado no impide pausar pero si accionar el seccionador`() {
        val sim = RobotSimulator()
        sim.enviar(Orden.INICIAR_MISION)
        sim.forzarEnlace(EstadoEnlace.DEGRADADO)
        assertTrue(sim.habilitacion(Orden.PAUSAR).habilitada)
        assertEquals("ENLACE_DEGRADADO", sim.habilitacion(Orden.ACCIONAR_SECCIONADOR).motivo)
    }
}
