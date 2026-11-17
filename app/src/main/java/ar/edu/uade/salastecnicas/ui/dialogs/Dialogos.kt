package ar.edu.uade.salastecnicas.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.ui.formatearGrados
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * El robot se detuvo y espera. No se puede descartar este modal:
 * el subproceso exige una decisión del operador.
 */
@Composable
fun DecisionAnomaliaDialog(
    estado: UiState,
    onDecidir: (DecisionAnomalia) -> Unit,
    onPedirOrden: (Orden) -> Unit
) {
    val resultado = estado.estadoRobot.mision?.resultados?.lastOrNull()
    val sala = estado.estadoRobot.telemetria?.sala ?: "—"
    val aislado = resultado?.ausenciaTensionConfirmada == true

    AlertDialog(
        onDismissRequest = { /* el subproceso exige decidir */ },
        title = { Text("Anomalía en $sala") },
        text = {
            Column {
                Text(
                    "Lectura de ${formatearGrados(resultado?.temperaturaMaxC ?: 0.0)}. " +
                        "El punto quedó marcado como FALLA y el robot está detenido " +
                        "esperando tu decisión."
                )
                Text(
                    if (aislado) "Ausencia de tensión confirmada."
                    else "Ausencia de tensión NO confirmada.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (!aislado) {
                    OutlinedButton(
                        onClick = { onPedirOrden(Orden.ACCIONAR_SECCIONADOR) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) { Text("Accionar seccionador (requiere supervisor)") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDecidir(DecisionAnomalia.AISLAR) }, enabled = aislado) {
                Text("Aislar y continuar")
            }
        },
        dismissButton = {
            Column {
                OutlinedButton(onClick = { onDecidir(DecisionAnomalia.MARCAR_Y_CONTINUAR) }) {
                    Text("Marcar y continuar")
                }
                OutlinedButton(onClick = { onDecidir(DecisionAnomalia.ABORTAR) }) {
                    Text("Abortar y regresar")
                }
            }
        }
    )
}

@Composable
fun AutorizacionSupervisorDialog(
    error: String?,
    onEnviar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Autorización de supervisor") },
        text = {
            Column {
                Text(
                    "Accionar un seccionador requiere autorización nominal. " +
                        "La autorización vence a los " +
                        "${Parametros.VIGENCIA_AUTORIZACION_MS / 1000} s y queda registrada."
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Código") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (error != null) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onEnviar(codigo) }, enabled = codigo.isNotBlank()) {
                Text("Autorizar y accionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}

@Composable
fun ConfirmacionDialog(
    titulo: String,
    cuerpo: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = { Text(cuerpo) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(textoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Volver") }
        }
    )
}
