package ar.edu.uade.salastecnicas.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ar.edu.uade.salastecnicas.data.EstadoRobot
import ar.edu.uade.salastecnicas.data.HistorialMapper
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.sim.RobotSimulator
import ar.edu.uade.salastecnicas.sim.Rondas
import ar.edu.uade.salastecnicas.ui.dialogs.AutorizacionSupervisorDialog
import ar.edu.uade.salastecnicas.ui.dialogs.ConfirmacionDialog
import ar.edu.uade.salastecnicas.ui.dialogs.DecisionAnomaliaDialog
import ar.edu.uade.salastecnicas.ui.screens.AlertasHistorialScreen
import ar.edu.uade.salastecnicas.ui.screens.ControlScreen
import ar.edu.uade.salastecnicas.ui.screens.MisionScreen
import ar.edu.uade.salastecnicas.ui.screens.ModoDemoScreen
import ar.edu.uade.salastecnicas.ui.screens.SeleccionRobotScreen
import ar.edu.uade.salastecnicas.ui.screens.SupervisionScreen
import ar.edu.uade.salastecnicas.ui.theme.SalasTecnicasTheme
import ar.edu.uade.salastecnicas.viewmodel.UiState

/*
 * Previews de las seis pantallas y los tres modales.
 *
 * Los estados no son inventados a mano: se generan corriendo el simulador de
 * verdad, así que lo que se ve en el panel de Android Studio es exactamente lo
 * que va a mostrar la app. Es posible porque el simulador es Kotlin puro y
 * determinista, sin Android ni reloj del sistema de por medio.
 */

/** Avanza el simulador la cantidad de minutos simulados indicada. */
private fun RobotSimulator.correrMinutos(minutos: Int) {
    val ticks = (minutos * 60_000L / Parametros.FACTOR_DEMO) / Parametros.TICK_MS
    repeat(ticks.toInt()) { avanzar(Parametros.TICK_MS) }
}

private fun estadoDe(
    sim: RobotSimulator,
    robot: Robot? = Rondas.ROBOTS.first()
) = EstadoRobot(
    robotSeleccionado = robot,
    telemetria = sim.telemetria(),
    mision = sim.mision(),
    alertas = sim.alertas(),
    ordenes = sim.ordenes(),
    antiguedadMs = sim.antiguedadMs(),
    datoAntiguo = sim.datoAntiguo(),
    esperandoDecision = sim.esperandoDecision(),
    autorizacionVigente = sim.autorizacionVigente(),
    habilitaciones = Orden.entries.associateWith {
        sim.habilitacion(it, Parametros.CODIGO_SUPERVISOR)
    }
)

private fun uiDe(
    sim: RobotSimulator,
    pantalla: Pantalla,
    robot: Robot? = Rondas.ROBOTS.first()
): UiState {
    val eventos = sim.drenarEventos()
    return UiState(
        pantalla = pantalla,
        estadoRobot = estadoDe(sim, robot),
        historial = HistorialMapper.aResumenes(eventos),
        robots = Rondas.ROBOTS,
        habilitaciones = estadoDe(sim, robot).habilitaciones
    )
}

/** Ronda normal, a mitad de camino. */
private fun simEnCurso(): RobotSimulator {
    val sim = RobotSimulator()
    sim.enviar(Orden.INICIAR_MISION)
    sim.correrMinutos(22)
    return sim
}

/** Primer punto en falla por temperatura, esperando decisión. */
private fun simConAnomalia(): RobotSimulator {
    val sim = RobotSimulator()
    sim.forzarTemperaturaAlta(Rondas.PUNTOS[0].id, 78.4)
    sim.enviar(Orden.INICIAR_MISION)
    sim.correrMinutos(Parametros.TRASLADO_MIN + Rondas.PUNTOS[0].minutosVerificacion)
    return sim
}

// =====================================================================
// Pantallas
// =====================================================================

@Preview(name = "1 · Selección de robot", showBackground = true, heightDp = 640)
@Composable
private fun PreviewSeleccionRobot() {
    SalasTecnicasTheme {
        SeleccionRobotScreen(
            estado = uiDe(RobotSimulator(), Pantalla.SELECCION_ROBOT, robot = null),
            onSeleccionar = {}
        )
    }
}

@Preview(name = "2 · Supervisión, ronda en curso", showBackground = true, heightDp = 1400)
@Composable
private fun PreviewSupervision() {
    SalasTecnicasTheme {
        SupervisionScreen(estado = uiDe(simEnCurso(), Pantalla.SUPERVISION), onIrA = {})
    }
}

@Preview(name = "2b · Supervisión, enlace perdido", showBackground = true, heightDp = 1400)
@Composable
private fun PreviewSupervisionDesconectada() {
    val sim = simEnCurso()
    sim.forzarEnlace(EstadoEnlace.PERDIDO)
    sim.correrMinutos(1)
    SalasTecnicasTheme {
        SupervisionScreen(estado = uiDe(sim, Pantalla.SUPERVISION), onIrA = {})
    }
}

@Preview(name = "3 · Misión con un punto en falla", showBackground = true, heightDp = 1100)
@Composable
private fun PreviewMision() {
    SalasTecnicasTheme {
        MisionScreen(estado = uiDe(simConAnomalia(), Pantalla.MISION))
    }
}

@Preview(name = "4 · Control", showBackground = true, heightDp = 1500)
@Composable
private fun PreviewControl() {
    val sim = simEnCurso()
    // Una orden confirmada que no se ejecutó, para ver el chip en error
    sim.forzarDetencionIgnorada(true)
    sim.enviar(Orden.DETENCION_OPERATIVA)
    sim.correrMinutos(1)
    SalasTecnicasTheme {
        ControlScreen(estado = uiDe(sim, Pantalla.CONTROL), onPedirOrden = {})
    }
}

@Preview(name = "5 · Alertas e historial", showBackground = true, heightDp = 900)
@Composable
private fun PreviewAlertasHistorial() {
    SalasTecnicasTheme {
        AlertasHistorialScreen(
            estado = uiDe(simConAnomalia(), Pantalla.ALERTAS_HISTORIAL),
            onReconocer = {}
        )
    }
}

@Preview(name = "6 · Modo demo", showBackground = true, heightDp = 1500)
@Composable
private fun PreviewModoDemo() {
    SalasTecnicasTheme {
        ModoDemoScreen(
            onEscenario = {},
            onForzarEnlace = {},
            onForzarPresencia = {},
            onForzarBateria = {},
            onReiniciar = {}
        )
    }
}

// =====================================================================
// Modales
// =====================================================================

@Preview(name = "Modal · Decisión de anomalía", showBackground = true)
@Composable
private fun PreviewDecisionAnomalia() {
    SalasTecnicasTheme {
        DecisionAnomaliaDialog(
            estado = uiDe(simConAnomalia(), Pantalla.MISION),
            onDecidir = {},
            onPedirOrden = {}
        )
    }
}

@Preview(name = "Modal · Autorización de supervisor", showBackground = true)
@Composable
private fun PreviewAutorizacion() {
    SalasTecnicasTheme {
        AutorizacionSupervisorDialog(error = null, onEnviar = {}, onCerrar = {})
    }
}

@Preview(name = "Modal · Autorización rechazada", showBackground = true)
@Composable
private fun PreviewAutorizacionConError() {
    SalasTecnicasTheme {
        AutorizacionSupervisorDialog(
            error = "Código de supervisor inválido",
            onEnviar = {},
            onCerrar = {}
        )
    }
}

@Preview(name = "Modal · Confirmar cancelación", showBackground = true)
@Composable
private fun PreviewConfirmacion() {
    SalasTecnicasTheme {
        ConfirmacionDialog(
            titulo = "Cancelar la ronda",
            cuerpo = "La ronda queda cerrada como incompleta y los puntos sin " +
                "verificar se registran. La cuadrilla de mantenimiento no podrá " +
                "ingresar hasta completar una ronda.",
            textoConfirmar = "Cancelar la ronda",
            onConfirmar = {},
            onCerrar = {}
        )
    }
}
