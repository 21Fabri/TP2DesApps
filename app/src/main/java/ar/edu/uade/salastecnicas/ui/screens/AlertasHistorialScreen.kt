package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.data.ResumenRonda
import ar.edu.uade.salastecnicas.ui.components.BannerAlerta
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.colorDeFondo
import ar.edu.uade.salastecnicas.ui.components.colorDeTexto
import ar.edu.uade.salastecnicas.ui.formatearReloj
import ar.edu.uade.salastecnicas.viewmodel.UiState

@Composable
fun AlertasHistorialScreen(
    estado: UiState,
    onReconocer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pestania by remember { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pestania) {
            Tab(
                selected = pestania == 0,
                onClick = { pestania = 0 },
                text = {
                    val sinReconocer = estado.estadoRobot.alertas.count { !it.reconocida }
                    Text("Alertas ($sinReconocer)")
                }
            )
            Tab(
                selected = pestania == 1,
                onClick = { pestania = 1 },
                text = { Text("Historial (${estado.historial.size})") }
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (pestania == 0) {
                if (estado.estadoRobot.alertas.isEmpty()) {
                    Text("No hay alertas registradas.")
                }
                estado.estadoRobot.alertas.reversed().forEach { alerta ->
                    BannerAlerta(alerta = alerta, onReconocer = onReconocer)
                }
            } else {
                if (estado.historial.isEmpty()) {
                    Text("Todavía no se registró ninguna ronda.")
                }
                estado.historial.forEach { ronda -> TarjetaRonda(ronda) }
            }
        }
    }
}

@Composable
private fun TarjetaRonda(ronda: ResumenRonda) {
    val visual = when {
        !ronda.cerrada -> EstadoVisual.ADVERTENCIA
        ronda.completa -> EstadoVisual.NORMAL
        else -> EstadoVisual.ERROR
    }
    val cierre = when {
        !ronda.cerrada -> "En curso"
        ronda.completa -> "Completa"
        else -> "Incompleta: la cuadrilla no puede ingresar"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = visual.colorDeFondo())
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${ronda.misionId} · ${formatearReloj(ronda.iniciadaTsMs)}",
                style = MaterialTheme.typography.titleSmall,
                color = visual.colorDeTexto()
            )
            Text(
                "${ronda.puntosCompletados} de ${ronda.puntosTotales} puntos · $cierre",
                style = MaterialTheme.typography.bodySmall,
                color = visual.colorDeTexto()
            )
            ronda.eventos.forEach { evento ->
                // Si llegó con retraso se muestran los dos momentos: es la
                // reconciliación posterior a una desconexión.
                val retrasado = evento.recibidoTsMs > evento.tsMs
                val sello = if (retrasado)
                    "${formatearReloj(evento.tsMs)} " +
                        "(recibido ${formatearReloj(evento.recibidoTsMs)})"
                else formatearReloj(evento.tsMs)

                Text(
                    "· $sello  ${evento.clase.name}  ${evento.detalle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = visual.colorDeTexto(),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
