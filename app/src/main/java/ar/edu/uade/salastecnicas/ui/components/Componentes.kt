package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.salastecnicas.model.Alerta
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Severidad

/** Tarjeta de un dato de telemetría, en cualquiera de los cuatro estados. */
@Composable
fun TarjetaTelemetria(
    titulo: String,
    valor: String,
    detalle: String? = null,
    estado: EstadoVisual = EstadoVisual.NORMAL,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = estado.colorDeFondo())
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = estado.colorDeTexto()
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                color = estado.colorDeTexto()
            )
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = estado.colorDeTexto()
                )
            }
        }
    }
}

/**
 * El botón se deshabilita solo y muestra POR QUÉ está deshabilitado.
 * Un botón gris sin explicación deja al operador sin saber qué hacer.
 */
@Composable
fun BotonComando(
    texto: String,
    habilitacion: Habilitacion,
    critico: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Button(
            onClick = onClick,
            enabled = habilitacion.habilitada,
            modifier = Modifier.fillMaxWidth(),
            colors = if (critico) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) else ButtonDefaults.buttonColors()
        ) {
            Text(texto)
        }
        if (!habilitacion.habilitada && habilitacion.detalle != null) {
            Text(
                text = habilitacion.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Muestra DOS cosas distintas: el estado de la orden y si su efecto se
 * verificó. Una orden confirmada no es una orden ejecutada.
 */
@Composable
fun ChipEstadoOrden(orden: OrdenEnviada, modifier: Modifier = Modifier) {
    val estadoVisual = when {
        orden.ejecucionFallida -> EstadoVisual.ERROR
        orden.estado == EstadoOrden.RECHAZADA -> EstadoVisual.ERROR
        orden.estado == EstadoOrden.SIN_RESPUESTA -> EstadoVisual.ADVERTENCIA
        orden.estado == EstadoOrden.PENDIENTE -> EstadoVisual.ADVERTENCIA
        else -> EstadoVisual.NORMAL
    }

    val lineaEjecucion = when {
        orden.estado != EstadoOrden.CONFIRMADA -> null
        orden.ejecucionFallida ->
            "Efecto NO verificado. La detención por software no reemplaza una parada física"
        orden.ejecucionVerificada -> "Efecto verificado: ${orden.criterioEjecucion}"
        else -> "Verificando efecto: ${orden.criterioEjecucion}"
    }

    Surface(
        color = estadoVisual.colorDeFondo(),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = "${orden.comandoId} · ${orden.orden.name}",
                style = MaterialTheme.typography.labelMedium,
                color = estadoVisual.colorDeTexto()
            )
            Text(
                text = orden.estado.name,
                style = MaterialTheme.typography.titleSmall,
                color = estadoVisual.colorDeTexto()
            )
            if (orden.detalleRechazo != null) {
                Text(
                    text = "Motivo: ${orden.motivoRechazo} — ${orden.detalleRechazo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = estadoVisual.colorDeTexto()
                )
            }
            if (lineaEjecucion != null) {
                Text(
                    text = lineaEjecucion,
                    style = MaterialTheme.typography.bodySmall,
                    color = estadoVisual.colorDeTexto()
                )
            }
        }
    }
}

@Composable
fun BannerAlerta(
    alerta: Alerta,
    onReconocer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val estado = when {
        alerta.reconocida -> EstadoVisual.DESHABILITADO
        alerta.severidad == Severidad.CRITICA -> EstadoVisual.ERROR
        alerta.severidad == Severidad.ADVERTENCIA -> EstadoVisual.ADVERTENCIA
        else -> EstadoVisual.NORMAL
    }

    Surface(
        color = estado.colorDeFondo(),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = alerta.severidad.name +
                    if (alerta.reconocida) " · RECONOCIDA" else "",
                style = MaterialTheme.typography.labelSmall,
                color = estado.colorDeTexto()
            )
            Text(
                text = alerta.titulo,
                style = MaterialTheme.typography.titleSmall,
                color = estado.colorDeTexto()
            )
            Text(
                text = alerta.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = estado.colorDeTexto()
            )
            if (onReconocer != null && !alerta.reconocida) {
                TextButton(onClick = onReconocer) { Text("Reconocer") }
                Text(
                    text = "Reconocer registra que la viste. No resuelve la alerta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = estado.colorDeTexto()
                )
            }
        }
    }
}

/**
 * Riesgo 4: el operador no debe poder confundir el último valor conocido con
 * el valor actual. Por eso la antigüedad se muestra siempre, en segundos.
 */
@Composable
fun IndicadorAntiguedad(
    antiguedadMs: Long,
    datoAntiguo: Boolean,
    enlace: EstadoEnlace,
    modifier: Modifier = Modifier
) {
    val estado = when {
        enlace == EstadoEnlace.PERDIDO -> EstadoVisual.ERROR
        datoAntiguo || enlace == EstadoEnlace.DEGRADADO -> EstadoVisual.ADVERTENCIA
        else -> EstadoVisual.NORMAL
    }

    Surface(
        color = estado.colorDeFondo(),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = "Enlace ${enlace.name} · actualizado hace ${antiguedadMs / 1000} s",
                style = MaterialTheme.typography.titleSmall,
                color = estado.colorDeTexto()
            )
            if (enlace == EstadoEnlace.PERDIDO) {
                Text(
                    text = "DATOS ANTIGUOS. El robot puede estar en otra posición y en " +
                        "otro estado que el que muestra esta pantalla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = estado.colorDeTexto()
                )
            }
        }
    }
}

@Composable
fun BarraProgresoRonda(
    completados: Int,
    total: Int,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    val fraccion = if (total == 0) 0f else completados.toFloat() / total
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = "$etiqueta · $completados de $total puntos",
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
            progress = { fraccion },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
