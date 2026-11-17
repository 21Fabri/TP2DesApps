package ar.edu.uade.salastecnicas.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Los cuatro estados visuales que el enunciado exige para cada componente. */
enum class EstadoVisual { NORMAL, DESHABILITADO, ADVERTENCIA, ERROR }

@Composable
fun EstadoVisual.colorDeFondo(): Color = when (this) {
    EstadoVisual.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
    EstadoVisual.DESHABILITADO -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    EstadoVisual.ADVERTENCIA -> Color(0xFFFFF4D6)
    EstadoVisual.ERROR -> MaterialTheme.colorScheme.errorContainer
}

@Composable
fun EstadoVisual.colorDeTexto(): Color = when (this) {
    EstadoVisual.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    EstadoVisual.DESHABILITADO ->
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    EstadoVisual.ADVERTENCIA -> Color(0xFF6B4E00)
    EstadoVisual.ERROR -> MaterialTheme.colorScheme.onErrorContainer
}
