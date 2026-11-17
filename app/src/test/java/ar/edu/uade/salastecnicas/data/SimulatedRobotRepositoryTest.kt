package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.data.local.EventoDao
import ar.edu.uade.salastecnicas.data.local.EventoEntity
import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.sim.Rondas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que cada escenario del Modo demo deje el simulador en la condición
 * de partida correcta. Si uno de estos falla, la demostración no se puede dar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedRobotRepositoryTest {

    /** DAO en memoria: alcanza porque el DAO real es una consulta trivial. */
    private class DaoFalso : EventoDao {
        private val filas = MutableStateFlow<List<EventoEntity>>(emptyList())

        override suspend fun insertar(eventos: List<EventoEntity>) {
            filas.value = filas.value + eventos
        }

        override fun observarTodos(): Flow<List<EventoEntity>> = filas

        override suspend fun borrarTodo() {
            filas.value = emptyList()
        }

        fun cantidad() = filas.value.size

        fun clases() = filas.value.map { it.clase }
    }

    private fun TestScope.repo(dao: EventoDao = DaoFalso()) =
        SimulatedRobotRepository(backgroundScope, dao).also {
            it.seleccionarRobot(Rondas.ROBOTS.first())
        }

    // ---- Escenario 1 ----

    @Test
    fun `mision normal deja el robot en espera y sin ronda`() = runTest {
        val repo = repo()
        repo.aplicarEscenario(Escenario.MISION_NORMAL)
        val estado = repo.estado.value
        assertNull(estado.mision)
        assertEquals(EstadoOperativo.EN_ESPERA, estado.telemetria!!.estadoOperativo)
        assertTrue(estado.habilitaciones[Orden.INICIAR_MISION]!!.habilitada)
    }

    // ---- Escenario 2 ----

    @Test
    fun `bateria baja arranca la ronda con autonomia insuficiente`() = runTest {
        val repo = repo()
        repo.aplicarEscenario(Escenario.BATERIA_BAJA)
        val estado = repo.estado.value
        assertNotNull(estado.mision)
        assertEquals(40, estado.telemetria!!.bateriaPct)
        assertEquals(60, estado.telemetria.autonomiaMin)
    }

    @Test
    fun `bateria baja pausa la ronda y avisa en cuanto corre el reloj`() = runTest {
        val repo = repo()
        repo.aplicarEscenario(Escenario.BATERIA_BAJA)
        repo.arrancar()
        advanceTimeBy(Parametros.TICK_MS * 3)

        val estado = repo.estado.value
        assertEquals(EstadoOperativo.PAUSADO, estado.telemetria!!.estadoOperativo)
        assertTrue(estado.alertas.any { it.titulo.contains("Batería") })
    }

    // ---- Escenario 3 ----

    @Test
    fun `desconexion deja el enlace perdido con la ronda en curso`() = runTest {
        val repo = repo()
        repo.aplicarEscenario(Escenario.DESCONEXION)
        val estado = repo.estado.value
        assertNotNull(estado.mision)
        assertEquals(EstadoEnlace.PERDIDO, estado.telemetria!!.enlace)
    }

    @Test
    fun `sin enlace el dato se marca antiguo y las ordenes quedan sin respuesta`() =
        runTest {
            val repo = repo()
            repo.aplicarEscenario(Escenario.DESCONEXION)
            repo.arrancar()
            advanceTimeBy(5_000)

            assertTrue(repo.estado.value.datoAntiguo)

            val orden = repo.enviar(Orden.DETENCION_OPERATIVA)
            assertEquals(EstadoOrden.PENDIENTE, orden.estado)

            advanceTimeBy(Parametros.VENCIMIENTO_ORDEN_MS + Parametros.TICK_MS)
            assertEquals(
                EstadoOrden.SIN_RESPUESTA,
                repo.estado.value.ordenes.last().estado
            )
        }

    @Test
    fun `al restablecer el enlace los eventos retrasados llegan al historial`() = runTest {
        val dao = DaoFalso()
        val repo = repo(dao)
        repo.aplicarEscenario(Escenario.DESCONEXION)
        repo.arrancar()

        // El primer punto se completa a ciegas: traslado más verificación
        advanceTimeBy(
            (Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion) * 1_000L +
                Parametros.TICK_MS
        )
        advanceTimeBy(2_000)
        assertFalse(dao.clases().contains(ClaseEvento.PUNTO_COMPLETADO.name))

        repo.forzarEnlace(EstadoEnlace.OK)
        advanceTimeBy(Parametros.TICK_MS * 2)

        assertTrue(dao.clases().contains(ClaseEvento.PUNTO_COMPLETADO.name))
    }

    // ---- Escenario 4 ----

    @Test
    fun `incidente llega al subproceso de anomalia y pide decision`() = runTest {
        val repo = repo()
        repo.aplicarEscenario(Escenario.INCIDENTE)
        repo.arrancar()
        advanceTimeBy(
            (Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion) * 1_000L +
                Parametros.TICK_MS
        )

        val estado = repo.estado.value
        assertTrue(estado.esperandoDecision)
        assertEquals(78.4, estado.mision!!.resultados.first().temperaturaMaxC, 0.001)
        assertFalse(estado.mision.resultados.first().ausenciaTensionConfirmada)
    }

    @Test
    fun `durante la anomalia el seccionador necesita el codigo del supervisor`() = runTest {
        val repo = repo()
        repo.aplicarEscenario(Escenario.INCIDENTE)
        repo.arrancar()
        advanceTimeBy(
            (Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion) * 1_000L +
                Parametros.TICK_MS
        )

        assertFalse(repo.ingresarAutorizacion("0000"))
        assertTrue(repo.ingresarAutorizacion(Parametros.CODIGO_SUPERVISOR))

        val orden = repo.enviar(Orden.ACCIONAR_SECCIONADOR, Parametros.CODIGO_SUPERVISOR)
        assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
    }

    // ---- Escenario 5 ----

    @Test
    fun `orden no ejecutada confirma la detencion pero el robot sigue caminando`() =
        runTest {
            val repo = repo()
            repo.aplicarEscenario(Escenario.ORDEN_NO_EJECUTADA)
            repo.arrancar()

            val orden = repo.enviar(Orden.DETENCION_OPERATIVA)
            assertEquals(EstadoOrden.CONFIRMADA, orden.estado)
            assertTrue(repo.estado.value.telemetria!!.caminando)

            advanceTimeBy(Parametros.PLAZO_VERIFICACION_MS + Parametros.TICK_MS * 2)

            val registrada = repo.estado.value.ordenes.last { it.orden == Orden.DETENCION_OPERATIVA }
            assertFalse(registrada.ejecucionVerificada)
            assertTrue(registrada.ejecucionFallida)
            assertTrue(
                repo.estado.value.alertas.any { it.titulo.contains("no se ejecutó") }
            )
        }

    // ---- Persistencia y reinicio ----

    @Test
    fun `los eventos del simulador se guardan en el dao`() = runTest {
        val dao = DaoFalso()
        val repo = repo(dao)
        repo.aplicarEscenario(Escenario.MISION_NORMAL)
        repo.enviar(Orden.INICIAR_MISION)
        repo.arrancar()
        advanceTimeBy(Parametros.TICK_MS * 2)

        assertTrue(dao.cantidad() > 0)
        assertTrue(dao.clases().contains(ClaseEvento.RONDA_INICIADA.name))
    }

    @Test
    fun `reiniciar borra el historial y vuelve a cero`() = runTest {
        val dao = DaoFalso()
        val repo = repo(dao)
        repo.enviar(Orden.INICIAR_MISION)
        repo.arrancar()
        advanceTimeBy(Parametros.TICK_MS * 2)
        assertTrue(dao.cantidad() > 0)

        repo.reiniciar()

        assertEquals(0, dao.cantidad())
        assertNull(repo.estado.value.mision)
    }

    @Test
    fun `sin robot seleccionado no hay telemetria ni ordenes habilitadas`() = runTest {
        val repo = SimulatedRobotRepository(backgroundScope, DaoFalso())
        val estado = repo.estado.value
        assertNull(estado.telemetria)
        assertTrue(estado.habilitaciones.values.none { it.habilitada })
    }
}
