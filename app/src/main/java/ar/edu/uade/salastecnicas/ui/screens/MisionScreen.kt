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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import ar.edu.uade.salastecnicas.sim.Rondas
import ar.edu.uade.salastecnicas.ui.components.BarraProgresoRonda
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.colorDeFondo
import ar.edu.uade.salastecnicas.ui.components.colorDeTexto
import ar.edu.uade.salastecnicas.ui.formatearGrados
import ar.edu.uade.salastecnicas.viewmodel.UiState

@Composable
fun MisionScreen(estado: UiState, modifier: Modifier = Modifier) {
    val mision = estado.estadoRobot.mision

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Ronda de verificación", style = MaterialTheme.typography.headlineSmall)

        if (mision == null) {
            Text(
                "No hay ronda activa. Los cinco puntos del recorrido son:",
                modifier = Modifier.padding(top = 8.dp)
            )
            Rondas.PUNTOS.forEach { punto ->
                FilaPunto(
                    titulo = punto.sala,
                    subtitulo = punto.queVerifica,
                    detalle = "${punto.minutosVerificacion} min estimados",
                    visual = EstadoVisual.DESHABILITADO
                )
            }
            return@Column
        }

        Text(mision.nombre, style = MaterialTheme.typography.bodyMedium)
        BarraProgresoRonda(mision.completados, mision.total, mision.id)

        if (mision.cerrada && !mision.completa) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EstadoVisual.ERROR.colorDeFondo()
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Ronda incompleta",
                        style = MaterialTheme.typography.titleSmall,
                        color = EstadoVisual.ERROR.colorDeTexto()
                    )
                    Text(
                        "Quedaron ${mision.puntosSinVerificar.size} puntos sin verificar. " +
                            "La cuadrilla de mantenimiento no puede ingresar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EstadoVisual.ERROR.colorDeTexto()
                    )
                }
            }
        }

        mision.puntos.forEachIndexed { indice, punto ->
            val resultado = mision.resultados.getOrNull(indice)
            val visual = when {
                resultado == null && indice == mision.completados -> EstadoVisual.ADVERTENCIA
                resultado == null -> EstadoVisual.DESHABILITADO
                resultado.resultado == ResultadoPunto.FALLA -> EstadoVisual.ERROR
                resultado.resultado == ResultadoPunto.OBSERVACION -> EstadoVisual.ADVERTENCIA
                else -> EstadoVisual.NORMAL
            }
            val detalle = when {
                resultado != null ->
                    "${resultado.resultado.name} · " +
                        formatearGrados(resultado.temperaturaMaxC) + " · " +
                        if (resultado.ausenciaTensionConfirmada)
                            "ausencia de tensión confirmada"
                        else "ausencia de tensión NO confirmada"
                indice == mision.completados -> "En curso"
                else -> "Pendiente"
            }
            FilaPunto(punto.sala, punto.queVerifica, detalle, visual)
        }
    }
}

@Composable
private fun FilaPunto(
    titulo: String,
    subtitulo: String,
    detalle: String,
    visual: EstadoVisual
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = visual.colorDeFondo())
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                color = visual.colorDeTexto()
            )
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = visual.colorDeTexto()
            )
            Text(
                detalle,
                style = MaterialTheme.typography.labelMedium,
                color = visual.colorDeTexto()
            )
        }
    }
}
