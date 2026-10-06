package com.andrecoura.homemonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF0F4C5C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBEE6F0),
        onPrimaryContainer = Color(0xFF001F27),
        secondary = Color(0xFF4A6368),
        tertiary = Color(0xFF8A4F00),
        tertiaryContainer = Color(0xFFFFDDB8),
        onTertiaryContainer = Color(0xFF2C1600),
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF6FAFB),
        surface = Color(0xFFF6FAFB),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8CD0E0),
        onPrimary = Color(0xFF00363F),
        primaryContainer = Color(0xFF004E5C),
        onPrimaryContainer = Color(0xFFBEE6F0),
        secondary = Color(0xFFB1CBD0),
        tertiary = Color(0xFFFFB86B),
        tertiaryContainer = Color(0xFF693C00),
        onTertiaryContainer = Color(0xFFFFDDB8),
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E1415),
        surface = Color(0xFF0E1415),
    )

/** Fixed palette (no dynamic color) so the app looks the same on every phone and in the screenshots. */
@Composable
fun HomeMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
