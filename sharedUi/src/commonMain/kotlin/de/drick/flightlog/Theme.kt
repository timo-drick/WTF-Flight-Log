package de.drick.flightlog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// IntelliJ Darcula Color Palette
val DarculaBackground = Color(0xFF2B2B2B)
val DarculaCurrentLine = Color(0xFF323232)
val DarculaForeground = Color(0xFFA9B7C6)
val DarculaSelection = Color(0xFF214283)
val DarculaBlue = Color(0xFF6897BB)
val DarculaGreen = Color(0xFF6A8759)
val DarculaOrange = Color(0xFFCC7832)
val DarculaPurple = Color(0xFF9876AA)
val DarculaYellow = Color(0xFFBBB529)
val DarculaGray = Color(0xFF808080)
val DarculaError = Color(0xFFBC3F3C)

private val DarculaColorScheme = darkColorScheme(
    primary = DarculaOrange,
    onPrimary = Color.Black,
    primaryContainer = DarculaSelection,
    onPrimaryContainer = Color.White,
    secondary = DarculaBlue,
    onSecondary = Color.Black,
    secondaryContainer = DarculaCurrentLine,
    onSecondaryContainer = DarculaForeground,
    tertiary = DarculaPurple,
    onTertiary = Color.Black,
    background = DarculaBackground,
    onBackground = DarculaForeground,
    surface = DarculaBackground,
    onSurface = DarculaForeground,
    surfaceVariant = DarculaCurrentLine,
    onSurfaceVariant = DarculaForeground,
    error = DarculaError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun FlightLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarculaColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
