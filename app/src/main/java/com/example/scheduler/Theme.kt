package com.example.scheduler

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF5C6BC0),
    surface = Color(0xFFF7F7FA),
    onSurface = Color(0xFF1A1C1E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFFC5CAE9),
    surface = Color(0xFF101113),
    onSurface = Color(0xFFE2E2E6)
)

val LocalDimens = staticCompositionLocalOf { Dimens() }

class Dimens(
    val tiny: Int = 4,
    val small: Int = 8,
    val medium: Int = 16,
    val large: Int = 24
)

@Composable
fun ScheduleTheme(content: @Composable () -> Unit) {
    val colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        DarkColors
    } else {
        LightColors
    }

    CompositionLocalProvider(LocalDimens provides Dimens()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}
