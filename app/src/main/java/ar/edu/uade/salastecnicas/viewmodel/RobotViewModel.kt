package ar.edu.uade.salastecnicas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ar.edu.uade.salastecnicas.data.RobotRepository
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.sim.Rondas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Único ViewModel de la app.
 *
 * La app tiene un solo dominio, el estado del robot: partirlo en varios
 * obligaría a sincronizar copias del mismo estado.
 */
class RobotViewModel(
    private val repositorio: RobotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UiState(
            estadoRobot = repositorio.estado.value,
            robots = Rondas.ROBOTS,
            habilitaciones = habilitacionesDe(repositorio.estado.value.habilitaciones)
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    /** Orden que espera confirmación o autorización. */
    private var ordenPendiente: Orden? = null

    init {
        viewModelScope.launch {
            repositorio.estado.collect { estado ->
                val abrirAnomalia = estado.esperandoDecision &&
                    _uiState.value.dialogo == Dialogo.NINGUNO
                _uiState.value = _uiState.value.copy(
                    estadoRobot = estado,
                    habilitaciones = habilitacionesDe(estado.habilitaciones),
                    dialogo = if (abrirAnomalia) Dialogo.DECISION_ANOMALIA
                    else _uiState.value.dialogo
                )
            }
        }
        viewModelScope.launch {
            repositorio.historial.collect { rondas ->
                _uiState.value = _uiState.value.copy(historial = rondas)
            }
        }
    }

    /** Si el repositorio no informó habilitaciones, ninguna orden se habilita. */
    private fun habilitacionesDe(delRepo: Map<Orden, Habilitacion>): Map<Orden, Habilitacion> =
        Orden.entries.associateWith { orden ->
            delRepo[orden] ?: Habilitacion(false, "SIN_DATOS", "Sin datos del robot")
        }

    // =====================================================================
    // Navegación
    // =====================================================================

    fun irA(pantalla: Pantalla) {
        _uiState.value = _uiState.value.copy(pantalla = pantalla)
    }

    fun seleccionarRobot(robot: Robot) {
        if (!robot.disponible) return
        repositorio.seleccionarRobot(robot)
        _uiState.value = _uiState.value.copy(pantalla = Pantalla.SUPERVISION)
    }

    // =====================================================================
    // Órdenes
    // =====================================================================

    /**
     * Punto de entrada de todos los botones de comando.
     * Las órdenes críticas no se envían acá: abren un diálogo primero.
     */
    fun pedirOrden(orden: Orden) {
        when (orden) {
            Orden.CANCELAR -> {
                ordenPendiente = orden
                _uiState.value = _uiState.value.copy(dialogo = Dialogo.CONFIRMAR_CANCELAR)
            }

            Orden.ACCIONAR_SECCIONADOR -> {
                ordenPendiente = orden
                _uiState.value = _uiState.value.copy(
                    dialogo = Dialogo.AUTORIZACION,
                    errorAutorizacion = null
                )
            }

            else -> enviar(orden, null)
        }
    }

    /** Segundo paso de la confirmación de dos pasos. */
    fun confirmarOrden() {
        val orden = ordenPendiente ?: return
        ordenPendiente = null
        _uiState.value = _uiState.value.copy(dialogo = Dialogo.NINGUNO)
        enviar(orden, null)
    }

    fun cerrarDialogo() {
        ordenPendiente = null
        _uiState.value = _uiState.value.copy(
            dialogo = Dialogo.NINGUNO,
            errorAutorizacion = null
        )
    }

    fun enviarAutorizacion(codigo: String) {
        if (!repositorio.ingresarAutorizacion(codigo)) {
            _uiState.value = _uiState.value.copy(
                errorAutorizacion = "Código de supervisor inválido"
            )
            return
        }
        val orden = ordenPendiente ?: Orden.ACCIONAR_SECCIONADOR
        ordenPendiente = null
        _uiState.value = _uiState.value.copy(
            dialogo = Dialogo.NINGUNO,
            errorAutorizacion = null
        )
        enviar(orden, codigo)
    }

    private fun enviar(orden: Orden, codigo: String?) {
        viewModelScope.launch {
            val resultado = repositorio.enviar(orden, codigo)
            val aviso = when (resultado.estado) {
                EstadoOrden.CONFIRMADA -> "${textoDe(orden)}: confirmada"
                EstadoOrden.RECHAZADA ->
                    "${textoDe(orden)}: rechazada — ${resultado.detalleRechazo}"
                EstadoOrden.PENDIENTE -> "${textoDe(orden)}: enviada, sin confirmar"
                EstadoOrden.SIN_RESPUESTA -> "${textoDe(orden)}: sin respuesta"
            }
            _uiState.value = _uiState.value.copy(ultimaRespuesta = aviso)
        }
    }

    fun limpiarAviso() {
        _uiState.value = _uiState.value.copy(ultimaRespuesta = null)
    }

    fun decidir(decision: DecisionAnomalia) {
        repositorio.decidir(decision)
        _uiState.value = _uiState.value.copy(dialogo = Dialogo.NINGUNO)
    }

    // =====================================================================
    // Modo demo
    // =====================================================================

    fun aplicarEscenario(escenario: Escenario) {
        viewModelScope.launch {
            repositorio.aplicarEscenario(escenario)
            repositorio.seleccionarRobot(Rondas.ROBOTS.first())
            _uiState.value = _uiState.value.copy(
                pantalla = Pantalla.SUPERVISION,
                dialogo = Dialogo.NINGUNO,
                ultimaRespuesta = "Escenario preparado: ${textoDe(escenario)}"
            )
        }
    }

    fun forzarEnlace(enlace: EstadoEnlace) = repositorio.forzarEnlace(enlace)

    fun forzarPresencia(distanciaM: Double?) = repositorio.forzarPresencia(distanciaM)

    fun forzarBateria(pct: Double) = repositorio.forzarBateria(pct)

    fun reiniciar() {
        viewModelScope.launch {
            repositorio.reiniciar()
            _uiState.value = _uiState.value.copy(
                pantalla = Pantalla.SELECCION_ROBOT,
                dialogo = Dialogo.NINGUNO,
                ultimaRespuesta = null
            )
        }
    }

    // =====================================================================
    // Textos
    // =====================================================================

    fun textoDe(orden: Orden): String = when (orden) {
        Orden.INICIAR_MISION -> "Iniciar ronda"
        Orden.PAUSAR -> "Pausar"
        Orden.REANUDAR -> "Reanudar"
        Orden.CANCELAR -> "Cancelar ronda"
        Orden.REGRESO_PUNTO_SEGURO -> "Regresar al punto seguro"
        Orden.DETENCION_OPERATIVA -> "Solicitar detención"
        Orden.RECONOCER_ALERTAS -> "Reconocer alertas"
        Orden.ACCIONAR_SECCIONADOR -> "Accionar seccionador"
    }

    private fun textoDe(escenario: Escenario): String = when (escenario) {
        Escenario.MISION_NORMAL -> "misión normal"
        Escenario.BATERIA_BAJA -> "batería baja"
        Escenario.DESCONEXION -> "desconexión"
        Escenario.INCIDENTE -> "incidente por temperatura"
        Escenario.ORDEN_NO_EJECUTADA -> "orden confirmada que no se ejecuta"
    }
}

class RobotViewModelFactory(
    private val repositorio: RobotRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RobotViewModel(repositorio) as T
}
