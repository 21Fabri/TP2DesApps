package ar.edu.uade.salastecnicas.data

import ar.edu.uade.salastecnicas.model.Alerta
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Mision
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.model.Telemetria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Todo lo que la app sabe del robot en un instante dado. */
data class EstadoRobot(
    val robotSeleccionado: Robot? = null,
    val telemetria: Telemetria? = null,
    val mision: Mision? = null,
    val alertas: List<Alerta> = emptyList(),
    val ordenes: List<OrdenEnviada> = emptyList(),
    val antiguedadMs: Long = 0L,
    val datoAntiguo: Boolean = false,
    val esperandoDecision: Boolean = false,
    val autorizacionVigente: Boolean = false,
    val habilitaciones: Map<Orden, Habilitacion> = emptyMap()
)

/**
 * Única frontera entre la UI y el robot. La UI no conoce el simulador.
 *
 * Cambiar esta implementación por una real no obligaría a tocar la UI ni el
 * ViewModel, que es lo que pide el enunciado.
 */
interface RobotRepository {

    val estado: StateFlow<EstadoRobot>

    val historial: Flow<List<ResumenRonda>>

    /** Arranca el bucle de simulación. Se llama una sola vez. */
    fun arrancar()

    fun seleccionarRobot(robot: Robot)

    suspend fun enviar(orden: Orden, codigoSupervisor: String? = null): OrdenEnviada

    fun ingresarAutorizacion(codigo: String): Boolean

    fun decidir(decision: DecisionAnomalia)

    /** Modo demo: deja el simulador listo para reproducir un escenario. */
    suspend fun aplicarEscenario(escenario: Escenario)

    fun forzarEnlace(enlace: EstadoEnlace)

    fun forzarPresencia(distanciaM: Double?)

    fun forzarBateria(pct: Double)

    /** Modo demo: vuelve todo al estado inicial, incluido el historial. */
    suspend fun reiniciar()
}
