package ar.edu.uade.salastecnicas.viewmodel

import ar.edu.uade.salastecnicas.data.EstadoRobot
import ar.edu.uade.salastecnicas.data.ResumenRonda
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Robot

enum class Dialogo {
    NINGUNO,
    DECISION_ANOMALIA,
    AUTORIZACION,
    CONFIRMAR_CANCELAR
}

/** Todo lo que las seis pantallas necesitan, en un solo objeto. */
data class UiState(
    val pantalla: Pantalla = Pantalla.SELECCION_ROBOT,
    val estadoRobot: EstadoRobot,
    val historial: List<ResumenRonda> = emptyList(),
    val robots: List<Robot> = emptyList(),
    val habilitaciones: Map<Orden, Habilitacion> = emptyMap(),
    val dialogo: Dialogo = Dialogo.NINGUNO,
    /** Última respuesta recibida, para mostrarla como aviso breve. */
    val ultimaRespuesta: String? = null,
    val errorAutorizacion: String? = null
)
