package ar.edu.uade.salastecnicas.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.ui.dialogs.AutorizacionSupervisorDialog
import ar.edu.uade.salastecnicas.ui.dialogs.ConfirmacionDialog
import ar.edu.uade.salastecnicas.ui.dialogs.DecisionAnomaliaDialog
import ar.edu.uade.salastecnicas.ui.screens.AlertasHistorialScreen
import ar.edu.uade.salastecnicas.ui.screens.ControlScreen
import ar.edu.uade.salastecnicas.ui.screens.MisionScreen
import ar.edu.uade.salastecnicas.ui.screens.ModoDemoScreen
import ar.edu.uade.salastecnicas.ui.screens.SeleccionRobotScreen
import ar.edu.uade.salastecnicas.ui.screens.SupervisionScreen
import ar.edu.uade.salastecnicas.viewmodel.Dialogo
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModel

/**
 * Navegación con un when sobre un enum del estado.
 *
 * Con seis pantallas no hace falta Navigation Compose, y así queda una
 * dependencia menos que explicar.
 */
@Composable
fun AppNavigation(vm: RobotViewModel) {
    val estado by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (estado.pantalla != Pantalla.SELECCION_ROBOT) {
                NavigationBar {
                    PestaniaNav(estado.pantalla, Pantalla.SUPERVISION, "Panel", vm)
                    PestaniaNav(estado.pantalla, Pantalla.MISION, "Misión", vm)
                    PestaniaNav(estado.pantalla, Pantalla.CONTROL, "Control", vm)
                    PestaniaNav(estado.pantalla, Pantalla.ALERTAS_HISTORIAL, "Alertas", vm)
                    PestaniaNav(estado.pantalla, Pantalla.MODO_DEMO, "Demo", vm)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            when (estado.pantalla) {
                Pantalla.SELECCION_ROBOT ->
                    SeleccionRobotScreen(estado, vm::seleccionarRobot)

                Pantalla.SUPERVISION ->
                    SupervisionScreen(estado, vm::irA)

                Pantalla.MISION ->
                    MisionScreen(estado)

                Pantalla.CONTROL ->
                    ControlScreen(estado, vm::pedirOrden)

                Pantalla.ALERTAS_HISTORIAL -> AlertasHistorialScreen(
                    estado = estado,
                    onReconocer = { vm.pedirOrden(Orden.RECONOCER_ALERTAS) }
                )

                Pantalla.MODO_DEMO ->
                    ModoDemoScreen(estado, vm)
            }

            if (estado.ultimaRespuesta != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Snackbar(
                        modifier = Modifier.padding(12.dp),
                        action = {
                            TextButton(onClick = { vm.limpiarAviso() }) { Text("OK") }
                        }
                    ) { Text(estado.ultimaRespuesta!!) }
                }
            }

            when (estado.dialogo) {
                Dialogo.NINGUNO -> Unit

                Dialogo.DECISION_ANOMALIA ->
                    DecisionAnomaliaDialog(estado, vm::decidir, vm::pedirOrden)

                Dialogo.AUTORIZACION -> AutorizacionSupervisorDialog(
                    error = estado.errorAutorizacion,
                    onEnviar = vm::enviarAutorizacion,
                    onCerrar = vm::cerrarDialogo
                )

                Dialogo.CONFIRMAR_CANCELAR -> ConfirmacionDialog(
                    titulo = "Cancelar la ronda",
                    cuerpo = "La ronda queda cerrada como incompleta y los puntos sin " +
                        "verificar se registran. La cuadrilla de mantenimiento no podrá " +
                        "ingresar hasta completar una ronda.",
                    textoConfirmar = "Cancelar la ronda",
                    onConfirmar = vm::confirmarOrden,
                    onCerrar = vm::cerrarDialogo
                )
            }
        }
    }
}

/** NavigationBarItem es una extensión de RowScope, así que esto también. */
@Composable
private fun RowScope.PestaniaNav(
    actual: Pantalla,
    destino: Pantalla,
    texto: String,
    vm: RobotViewModel
) {
    NavigationBarItem(
        selected = actual == destino,
        onClick = { vm.irA(destino) },
        icon = { Text(texto.take(1)) },
        label = { Text(texto) }
    )
}
