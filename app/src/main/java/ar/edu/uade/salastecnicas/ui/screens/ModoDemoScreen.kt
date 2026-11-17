package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Escenario
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModel
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * Herramienta de prueba, no parte de la app que se entregaría a un cliente.
 *
 * Existe para que cada escenario se pueda reproducir a voluntad, tanto para
 * grabar las evidencias como para la defensa.
 */
@Composable
fun ModoDemoScreen(
    estado: UiState,
    vm: RobotViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Modo demo", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pantalla de prueba. No forma parte de la app de operación: sirve para " +
                "reproducir cada escenario de forma repetible.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "Escenarios",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        BotonEscenario(
            "1 · Misión normal",
            "Reinicia todo. Iniciar la ronda desde Control y verla completarse."
        ) { vm.aplicarEscenario(Escenario.MISION_NORMAL) }

        BotonEscenario(
            "2 · Batería baja",
            "Ronda en curso con 40 % de batería: se pausa sola y avisa. Continuar " +
                "se deshabilita cuando ya no alcanza para el regreso."
        ) { vm.aplicarEscenario(Escenario.BATERIA_BAJA) }

        BotonEscenario(
            "3 · Desconexión",
            "Ronda en curso sin enlace: datos antiguos, órdenes sin respuesta y, al " +
                "restablecer, reconciliación de eventos en el historial."
        ) { vm.aplicarEscenario(Escenario.DESCONEXION) }

        BotonEscenario(
            "4 · Incidente",
            "El primer punto va a medir 78,4 °C: falla, alerta crítica y decisión " +
                "obligatoria del operador."
        ) { vm.aplicarEscenario(Escenario.INCIDENTE) }

        BotonEscenario(
            "5 · Orden confirmada que no se ejecuta",
            "La detención operativa responde confirmada y el robot sigue caminando. " +
                "Enviarla desde Control y mirar el estado de la orden."
        ) { vm.aplicarEscenario(Escenario.ORDEN_NO_EJECUTADA) }

        HorizontalDivider(Modifier.padding(vertical = 20.dp))

        Text("Fallas sueltas", style = MaterialTheme.typography.titleMedium)

        OutlinedButton(
            onClick = { vm.forzarEnlace(EstadoEnlace.PERDIDO) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Cortar el enlace") }

        OutlinedButton(
            onClick = { vm.forzarEnlace(EstadoEnlace.DEGRADADO) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Degradar el enlace") }

        OutlinedButton(
            onClick = { vm.forzarEnlace(EstadoEnlace.OK) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Restablecer el enlace") }

        OutlinedButton(
            onClick = { vm.forzarPresencia(1.8) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Presencia a 1,8 m en zona ciega") }

        OutlinedButton(
            onClick = { vm.forzarPresencia(null) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Despejar la presencia") }

        OutlinedButton(
            onClick = { vm.forzarBateria(43.0) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) { Text("Bajar la batería a 43 % (rechaza el inicio)") }

        HorizontalDivider(Modifier.padding(vertical = 20.dp))

        Button(
            onClick = { vm.reiniciar() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reiniciar todo y borrar el historial") }

        Text(
            "Código de supervisor: ${Parametros.CODIGO_SUPERVISOR}. " +
                "El simulador corre a 1 segundo real por minuto simulado, así que " +
                "una ronda completa de 48 minutos tarda unos 48 segundos.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun BotonEscenario(titulo: String, descripcion: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(titulo) }
        Text(
            descripcion,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
