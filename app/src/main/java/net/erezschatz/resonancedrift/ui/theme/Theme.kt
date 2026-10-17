package net.erezschatz.resonancedrift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Background,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoDark,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = Surface,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceLight,
    error = Error,
)

/**
 * Light only, by decision (docs/DECISIONS.md D7). No dynamic colour, and the
 * system dark-mode setting is deliberately ignored.
 */
@Composable
fun ResonanceDriftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
