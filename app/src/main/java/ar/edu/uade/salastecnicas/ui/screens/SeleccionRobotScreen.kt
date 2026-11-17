package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Robot
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.colorDeFondo
import ar.edu.uade.salastecnicas.ui.components.colorDeTexto
import ar.edu.uade.salastecnicas.viewmodel.UiState

@Composable
fun SeleccionRobotScreen(
    estado: UiState,
    onSeleccionar: (Robot) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Selección de robot", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Verificación previa de salas técnicas · UADE",
            style = MaterialTheme.typography.bodyMedium
        )

        estado.robots.forEach { robot ->
            val visual = if (robot.disponible) EstadoVisual.NORMAL
            else EstadoVisual.DESHABILITADO

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = visual.colorDeFondo())
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        robot.id,
                        style = MaterialTheme.typography.titleMedium,
                        color = visual.colorDeTexto()
                    )
                    Text(
                        robot.modelo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = visual.colorDeTexto()
                    )
                    if (robot.motivoNoDisponible != null) {
                        Text(
                            robot.motivoNoDisponible,
                            style = MaterialTheme.typography.bodySmall,
                            color = visual.colorDeTexto()
                        )
                    }
                    Button(
                        onClick = { onSeleccionar(robot) },
                        enabled = robot.disponible,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(if (robot.disponible) "Supervisar" else "No disponible")
                    }
                }
            }
        }
    }
}
