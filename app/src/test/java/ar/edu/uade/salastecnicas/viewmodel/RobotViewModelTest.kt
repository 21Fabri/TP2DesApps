package ar.edu.uade.salastecnicas.viewmodel

import ar.edu.uade.salastecnicas.data.EstadoRobot
import ar.edu.uade.salastecnicas.data.ResumenRonda
import ar.edu.uade.salastecnicas.data.RobotRepository
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.sim.Rondas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RobotViewModelTest {

    /** Repositorio de prueba: registra lo que se le pide, sin simulador. */
    private class RepoFalso : RobotRepository {
        val ordenesEnviadas = mutableListOf<Orden>()
        var escenarioAplicado: Escenario? = null
        var reiniciado = false
        var enlaceForzado: EstadoEnlace? = null
        var codigoRecibido: String? = null
        var decisionRecibida: DecisionAnomalia? = null
        var autorizacionValida = true

        private val _estado = MutableStateFlow(EstadoRobot())
        override val estado: StateFlow<EstadoRobot> = _estado
        override val historial: Flow<List<ResumenRonda>> = flowOf(emptyList())

        override fun arrancar() = Unit

        override fun seleccionarRobot(robot: Robot) {
            _estado.value = _estado.value.copy(robotSeleccionado = robot)
        }

        override suspend fun enviar(orden: Orden, codigoSupervisor: String?): OrdenEnviada {
            ordenesEnviadas.add(orden)
            codigoRecibido = codigoSupervisor
            return OrdenEnviada("CMD-0001", orden, 0, EstadoOrden.CONFIRMADA)
        }

        override fun ingresarAutorizacion(codigo: String): Boolean {
            codigoRecibido = codigo
            return autorizacionValida
        }

        override fun decidir(decision: DecisionAnomalia) {
            decisionRecibida = decision
        }

        override suspend fun aplicarEscenario(escenario: Escenario) {
            escenarioAplicado = escenario
        }

        override fun forzarEnlace(enlace: EstadoEnlace) {
            enlaceForzado = enlace
        }

        override fun forzarPresencia(distanciaM: Double?) = Unit

        override fun forzarBateria(pct: Double) = Unit

        override suspend fun reiniciar() {
            reiniciado = true
        }

        fun ponerEsperandoDecision() {
            _estado.value = _estado.value.copy(esperandoDecision = true)
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `arranca en la pantalla de seleccion de robot`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertEquals(Pantalla.SELECCION_ROBOT, vm.uiState.value.pantalla)
    }

    @Test
    fun `ofrece los tres robots del listado`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertEquals(3, vm.uiState.value.robots.size)
        assertEquals(1, vm.uiState.value.robots.count { it.disponible })
    }

    @Test
    fun `seleccionar un robot lleva a supervision`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        vm.seleccionarRobot(Rondas.ROBOTS.first())
        assertEquals(Pantalla.SUPERVISION, vm.uiState.value.pantalla)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("G1-01", vm.uiState.value.estadoRobot.robotSeleccionado?.id)
    }

    @Test
    fun `un robot no disponible no se puede seleccionar`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        vm.seleccionarRobot(Rondas.ROBOTS.first { !it.disponible })
        assertEquals(Pantalla.SELECCION_ROBOT, vm.uiState.value.pantalla)
    }

    @Test
    fun `navegar cambia la pantalla activa`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        vm.irA(Pantalla.CONTROL)
        assertEquals(Pantalla.CONTROL, vm.uiState.value.pantalla)
    }

    @Test
    fun `una orden comun se envia directo al repositorio`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.PAUSAR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(Orden.PAUSAR), repo.ordenesEnviadas)
        assertEquals(Dialogo.NINGUNO, vm.uiState.value.dialogo)
    }

    @Test
    fun `cancelar abre primero la confirmacion de dos pasos`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.CANCELAR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Dialogo.CONFIRMAR_CANCELAR, vm.uiState.value.dialogo)
        assertTrue(repo.ordenesEnviadas.isEmpty())
    }

    @Test
    fun `confirmar la cancelacion la envia y cierra el dialogo`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.CANCELAR)
        vm.confirmarOrden()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(Orden.CANCELAR), repo.ordenesEnviadas)
        assertEquals(Dialogo.NINGUNO, vm.uiState.value.dialogo)
    }

    @Test
    fun `cerrar el dialogo descarta la orden pendiente`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.CANCELAR)
        vm.cerrarDialogo()
        vm.confirmarOrden()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.ordenesEnviadas.isEmpty())
    }

    @Test
    fun `accionar el seccionador pide primero la autorizacion`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.ACCIONAR_SECCIONADOR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Dialogo.AUTORIZACION, vm.uiState.value.dialogo)
        assertTrue(repo.ordenesEnviadas.isEmpty())
    }

    @Test
    fun `un codigo valido envia la orden con el codigo`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.ACCIONAR_SECCIONADOR)
        vm.enviarAutorizacion(Parametros.CODIGO_SUPERVISOR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(Orden.ACCIONAR_SECCIONADOR), repo.ordenesEnviadas)
        assertEquals(Parametros.CODIGO_SUPERVISOR, repo.codigoRecibido)
        assertNull(vm.uiState.value.errorAutorizacion)
    }

    @Test
    fun `un codigo invalido muestra el error y no envia la orden`() = runTest(dispatcher) {
        val repo = RepoFalso()
        repo.autorizacionValida = false
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.ACCIONAR_SECCIONADOR)
        vm.enviarAutorizacion("0000")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.ordenesEnviadas.isEmpty())
        assertEquals("Código de supervisor inválido", vm.uiState.value.errorAutorizacion)
        assertEquals(Dialogo.AUTORIZACION, vm.uiState.value.dialogo)
    }

    @Test
    fun `cuando el robot espera decision se abre el dialogo de anomalia`() =
        runTest(dispatcher) {
            val repo = RepoFalso()
            val vm = RobotViewModel(repo)
            dispatcher.scheduler.advanceUntilIdle()
            repo.ponerEsperandoDecision()
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(Dialogo.DECISION_ANOMALIA, vm.uiState.value.dialogo)
        }

    @Test
    fun `decidir reenvia la decision y cierra el dialogo`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.decidir(DecisionAnomalia.ABORTAR)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(DecisionAnomalia.ABORTAR, repo.decisionRecibida)
        assertEquals(Dialogo.NINGUNO, vm.uiState.value.dialogo)
    }

    @Test
    fun `aplicar un escenario lo delega y lleva a supervision`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.aplicarEscenario(Escenario.DESCONEXION)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Escenario.DESCONEXION, repo.escenarioAplicado)
        assertEquals(Pantalla.SUPERVISION, vm.uiState.value.pantalla)
    }

    @Test
    fun `reiniciar vuelve a la seleccion de robot`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.irA(Pantalla.CONTROL)
        vm.reiniciar()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.reiniciado)
        assertEquals(Pantalla.SELECCION_ROBOT, vm.uiState.value.pantalla)
    }

    @Test
    fun `forzar el enlace lo delega al repositorio`() = runTest(dispatcher) {
        val repo = RepoFalso()
        val vm = RobotViewModel(repo)
        vm.forzarEnlace(EstadoEnlace.PERDIDO)
        assertEquals(EstadoEnlace.PERDIDO, repo.enlaceForzado)
    }

    @Test
    fun `el estado expone una habilitacion por cada orden`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertEquals(Orden.entries.size, vm.uiState.value.habilitaciones.size)
    }

    @Test
    fun `sin datos del repositorio ninguna orden esta habilitada`() = runTest(dispatcher) {
        val vm = RobotViewModel(RepoFalso())
        assertTrue(vm.uiState.value.habilitaciones.values.none { it.habilitada })
        assertFalse(vm.uiState.value.habilitaciones[Orden.PAUSAR]!!.habilitada)
    }

    @Test
    fun `una orden rechazada muestra su motivo en el aviso`() = runTest(dispatcher) {
        val repo = object : RobotRepository by RepoFalso() {
            override suspend fun enviar(orden: Orden, codigoSupervisor: String?) =
                OrdenEnviada(
                    comandoId = "CMD-0002",
                    orden = orden,
                    enviadaTsMs = 0,
                    estado = EstadoOrden.RECHAZADA,
                    motivoRechazo = "BATERIA_INSUFICIENTE",
                    detalleRechazo = "autonomía 41 min"
                )
        }
        val vm = RobotViewModel(repo)
        vm.pedirOrden(Orden.INICIAR_MISION)
        dispatcher.scheduler.advanceUntilIdle()
        val aviso = vm.uiState.value.ultimaRespuesta!!
        assertTrue(aviso.contains("rechazada"))
        assertTrue(aviso.contains("autonomía 41 min"))
    }
}
