package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.ui.components.BotonComando
import ar.edu.uade.salastecnicas.ui.components.ChipEstadoOrden
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * Las órdenes comunes arriba y las críticas al final, separadas y en rojo,
 * fuera de la zona donde cae el pulgar. Es la medida de interfaz del riesgo 3.
 */
@Composable
fun ControlScreen(
    estado: UiState,
    onPedirOrden: (Orden) -> Unit,
    modifier: Modifier = Modifier
) {
    fun hab(orden: Orden): Habilitacion =
        estado.habilitaciones[orden] ?: Habilitacion(false, "SIN_DATOS", "Sin datos")

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Control", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Órdenes discretas. La app no ofrece control manual continuo: en modo " +
                "manual el G1 no consulta sus sensores de entorno.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "Misión",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        BotonComando("Iniciar ronda", hab(Orden.INICIAR_MISION)) {
            onPedirOrden(Orden.INICIAR_MISION)
        }
        BotonComando("Pausar", hab(Orden.PAUSAR)) { onPedirOrden(Orden.PAUSAR) }
        BotonComando("Reanudar", hab(Orden.REANUDAR)) { onPedirOrden(Orden.REANUDAR) }
        BotonComando("Cancelar ronda", hab(Orden.CANCELAR)) { onPedirOrden(Orden.CANCELAR) }

        Text(
            "Contingencias",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        BotonComando("Regresar al punto seguro", hab(Orden.REGRESO_PUNTO_SEGURO)) {
            onPedirOrden(Orden.REGRESO_PUNTO_SEGURO)
        }
        BotonComando("Reconocer alertas", hab(Orden.RECONOCER_ALERTAS)) {
            onPedirOrden(Orden.RECONOCER_ALERTAS)
        }

        HorizontalDivider(Modifier.padding(vertical = 20.dp))

        Text("Órdenes críticas", style = MaterialTheme.typography.titleMedium)
        Text(
            "La detención es una SOLICITUD por software. No reemplaza una parada de " +
                "emergencia física: el G1 no tiene corte físico. Ante una detención no " +
                "ejecutada, recurrir al acompañante presencial.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        BotonComando(
            texto = "Solicitar detención operativa",
            habilitacion = hab(Orden.DETENCION_OPERATIVA),
            critico = true
        ) { onPedirOrden(Orden.DETENCION_OPERATIVA) }

        BotonComando(
            texto = "Accionar seccionador",
            habilitacion = hab(Orden.ACCIONAR_SECCIONADOR),
            critico = true
        ) { onPedirOrden(Orden.ACCIONAR_SECCIONADOR) }

        HorizontalDivider(Modifier.padding(vertical = 20.dp))

        Text("Órdenes enviadas", style = MaterialTheme.typography.titleMedium)
        if (estado.estadoRobot.ordenes.isEmpty()) {
            Text("Todavía no se envió ninguna orden.")
        }
        estado.estadoRobot.ordenes.reversed().forEach { orden ->
            ChipEstadoOrden(orden)
        }
    }
}
