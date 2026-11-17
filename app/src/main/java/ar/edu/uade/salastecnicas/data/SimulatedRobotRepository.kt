package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.data.local.EventoDao
import ar.edu.uade.salastecnicas.data.local.aEntity
import ar.edu.uade.salastecnicas.data.local.aModelo
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.sim.RobotSimulator
import ar.edu.uade.salastecnicas.sim.Rondas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Envuelve el simulador y lo publica como estado observable.
 *
 * El bucle es deliberadamente simple: un while con delay, sin operadores de
 * flows encadenados, para que se pueda leer de arriba a abajo.
 */
class SimulatedRobotRepository(
    private val scope: CoroutineScope,
    private val eventoDao: EventoDao
) : RobotRepository {

    private var simulador = RobotSimulator()
    private var robotSeleccionado: Robot? = null
    private var arrancado = false

    private val _estado = MutableStateFlow(estadoActual())
    override val estado: StateFlow<EstadoRobot> = _estado

    override val historial: Flow<List<ResumenRonda>> =
        eventoDao.observarTodos().map { filas ->
            HistorialMapper.aResumenes(filas.map { it.aModelo() })
        }

    override fun arrancar() {
        if (arrancado) return
        arrancado = true
        scope.launch {
            while (true) {
                delay(Parametros.TICK_MS)
                simulador.avanzar(Parametros.TICK_MS)
                guardarEventosNuevos()
                _estado.value = estadoActual()
            }
        }
    }

    private suspend fun guardarEventosNuevos() {
        val nuevos = simulador.drenarEventos()
        if (nuevos.isNotEmpty()) {
            eventoDao.insertar(nuevos.map { it.aEntity() })
        }
    }

    private fun estadoActual() = EstadoRobot(
        robotSeleccionado = robotSeleccionado,
        telemetria = if (robotSeleccionado == null) null else simulador.telemetria(),
        mision = simulador.mision(),
        alertas = simulador.alertas(),
        ordenes = simulador.ordenes(),
        antiguedadMs = simulador.antiguedadMs(),
        datoAntiguo = simulador.datoAntiguo(),
        esperandoDecision = simulador.esperandoDecision(),
        autorizacionVigente = simulador.autorizacionVigente(),
        habilitaciones = Orden.entries.associateWith { orden ->
            if (robotSeleccionado == null)
                Habilitacion(false, "SIN_ROBOT", "No hay robot seleccionado")
            else simulador.habilitacion(orden, Parametros.CODIGO_SUPERVISOR)
        }
    )

    override fun seleccionarRobot(robot: Robot) {
        robotSeleccionado = robot
        _estado.value = estadoActual()
    }

    override suspend fun enviar(orden: Orden, codigoSupervisor: String?): OrdenEnviada {
        val resultado = simulador.enviar(orden, codigoSupervisor)
        guardarEventosNuevos()
        _estado.value = estadoActual()
        return resultado
    }

    override fun ingresarAutorizacion(codigo: String): Boolean {
        val ok = simulador.ingresarAutorizacion(codigo)
        _estado.value = estadoActual()
        return ok
    }

    override fun decidir(decision: DecisionAnomalia) {
        simulador.decidir(decision)
        _estado.value = estadoActual()
    }

    override fun forzarEnlace(enlace: EstadoEnlace) {
        simulador.forzarEnlace(enlace)
        _estado.value = estadoActual()
    }

    override fun forzarPresencia(distanciaM: Double?) {
        simulador.forzarPresencia(distanciaM)
        _estado.value = estadoActual()
    }

    override fun forzarBateria(pct: Double) {
        simulador.forzarBateria(pct)
        _estado.value = estadoActual()
    }

    override suspend fun reiniciar() {
        simulador = RobotSimulator()
        eventoDao.borrarTodo()
        _estado.value = estadoActual()
    }

    /**
     * Deja el simulador en la condición de partida de cada escenario.
     * El operador después opera normalmente y el escenario se desarrolla.
     */
    override suspend fun aplicarEscenario(escenario: Escenario) {
        reiniciar()
        when (escenario) {
            Escenario.MISION_NORMAL -> Unit

            Escenario.BATERIA_BAJA -> {
                simulador.enviar(Orden.INICIAR_MISION)
                simulador.forzarBateria(40.0)
            }

            Escenario.DESCONEXION -> {
                simulador.enviar(Orden.INICIAR_MISION)
                simulador.forzarEnlace(EstadoEnlace.PERDIDO)
            }

            Escenario.INCIDENTE -> {
                simulador.forzarTemperaturaAlta(Rondas.PUNTOS[0].id, 78.4)
                simulador.enviar(Orden.INICIAR_MISION)
            }

            Escenario.ORDEN_NO_EJECUTADA -> {
                simulador.enviar(Orden.INICIAR_MISION)
                simulador.forzarDetencionIgnorada(true)
            }
        }
        guardarEventosNuevos()
        _estado.value = estadoActual()
    }
}
