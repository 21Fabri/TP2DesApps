package ar.edu.uade.salastecnicas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.Pantalla
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.ui.components.BannerAlerta
import ar.edu.uade.salastecnicas.ui.components.BarraProgresoRonda
import ar.edu.uade.salastecnicas.ui.components.EstadoVisual
import ar.edu.uade.salastecnicas.ui.components.IndicadorAntiguedad
import ar.edu.uade.salastecnicas.ui.components.TarjetaTelemetria
import ar.edu.uade.salastecnicas.ui.formatearGrados
import ar.edu.uade.salastecnicas.ui.formatearMetros
import ar.edu.uade.salastecnicas.ui.formatearVelocidad
import ar.edu.uade.salastecnicas.viewmodel.UiState

/**
 * Resumen arriba y telemetría completa abajo, en una sola pantalla scrolleable.
 * El enunciado permite combinar vistas si la navegación sigue siendo clara.
 */
@Composable
fun SupervisionScreen(
    estado: UiState,
    onIrA: (Pantalla) -> Unit,
    modifier: Modifier = Modifier
) {
    val robot = estado.estadoRobot
    val tele = robot.telemetria

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Panel de supervisión", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${robot.robotSeleccionado?.id ?: "—"} · operador sin línea de vista",
            style = MaterialTheme.typography.bodySmall
        )

        if (tele == null) {
            Text("Sin robot seleccionado.", modifier = Modifier.padding(top = 16.dp))
            return@Column
        }

        IndicadorAntiguedad(
            antiguedadMs = robot.antiguedadMs,
            datoAntiguo = robot.datoAntiguo,
            enlace = tele.enlace,
            modifier = Modifier.padding(top = 12.dp)
        )

        // ---- Resumen ----
        Row(Modifier.fillMaxWidth()) {
            TarjetaTelemetria(
                titulo = "Estado",
                valor = tele.estadoOperativo.name,
                detalle = tele.sala,
                estado = if (robot.esperandoDecision) EstadoVisual.ERROR
                else EstadoVisual.NORMAL,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )
            TarjetaTelemetria(
                titulo = "Batería",
                valor = "${tele.bateriaPct} %",
                detalle = "Autonomía ${tele.autonomiaMin} min",
                estado = estadoDeBateria(tele.autonomiaMin),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        BarraProgresoRonda(
            completados = tele.puntosCompletados,
            total = tele.puntosTotales,
            etiqueta = robot.mision?.nombre ?: "Sin ronda activa"
        )

        robot.alertas.filter { !it.reconocida }.forEach { alerta ->
            BannerAlerta(alerta = alerta)
        }

        // ---- Accesos ----
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(
                onClick = { onIrA(Pantalla.MISION) },
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) { Text("Misión") }
            OutlinedButton(
                onClick = { onIrA(Pantalla.CONTROL) },
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) { Text("Control") }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // ---- Telemetría completa ----
        Text("Telemetría", style = MaterialTheme.typography.titleMedium)

        TarjetaTelemetria(
            "Conectividad",
            tele.enlace.name,
            "Latencia ${tele.latenciaMs} ms",
            if (tele.enlace == EstadoEnlace.OK) EstadoVisual.NORMAL
            else EstadoVisual.ADVERTENCIA
        )
        TarjetaTelemetria("Estado operativo", tele.estadoOperativo.name)
        TarjetaTelemetria(
            "Misión y progreso",
            robot.mision?.id ?: "—",
            "${tele.puntosCompletados} de ${tele.puntosTotales} puntos"
        )
        TarjetaTelemetria("Ubicación", tele.sala, tele.puntoActualId ?: "—")
        TarjetaTelemetria(
            "Movimiento",
            formatearVelocidad(tele.velocidadMs),
            if (tele.caminando) "Caminando" else "Detenido"
        )
        TarjetaTelemetria("Alertas activas", tele.alertasActivas.toString(), "Sin reconocer")
        TarjetaTelemetria(
            "Última actualización",
            "hace ${robot.antiguedadMs / 1000} s",
            if (robot.datoAntiguo) "Dato antiguo" else "Al día",
            if (robot.datoAntiguo) EstadoVisual.ADVERTENCIA else EstadoVisual.NORMAL
        )

        Text(
            "Variables del servicio",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )

        TarjetaTelemetria(
            "Temperatura máxima del punto",
            formatearGrados(tele.temperaturaMaxC),
            "Umbral de falla ${formatearGrados(Parametros.TEMPERATURA_FALLA_C)} · " +
                "requiere cámara térmica adicional",
            if (tele.temperaturaMaxC >= Parametros.TEMPERATURA_FALLA_C) EstadoVisual.ERROR
            else EstadoVisual.NORMAL
        )
        TarjetaTelemetria(
            "Cobertura del entorno",
            "${tele.coberturaPct} %",
            "${tele.presencias} presencia(s) a ${formatearMetros(tele.distanciaPresenciaM)}. " +
                "El sector no cubierto es zona ciega: no detectar no es que no haya nadie",
            if (tele.presencias > 0) EstadoVisual.ADVERTENCIA else EstadoVisual.NORMAL
        )
        TarjetaTelemetria(
            "Margen de estabilidad",
            "${tele.margenEstabilidadPct} %",
            "Punto débil del G1: pisa fuerte y la estabilidad es su límite",
            if (tele.margenEstabilidadPct < 60) EstadoVisual.ADVERTENCIA
            else EstadoVisual.NORMAL
        )
    }
}

private fun estadoDeBateria(autonomiaMin: Int): EstadoVisual = when {
    autonomiaMin < Parametros.REGRESO_MIN -> EstadoVisual.ERROR
    autonomiaMin < 70 -> EstadoVisual.ADVERTENCIA
    else -> EstadoVisual.NORMAL
}
