package com.hmiot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B5ED7),
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    background = Color(0xFFF3F5F8),
    surface = Color.White,
    onSurface = Color(0xFF1D232E)
)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF78A9FF),
    secondary = Color(0xFF7FD089),
    background = Color(0xFF0E1116),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3)
)

@Composable
fun HMIoTAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content
    )
}
