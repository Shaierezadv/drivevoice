package com.drivevoice.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    onPrimary = Color.White,
    primaryContainer = BlueDark,
    onPrimaryContainer = Color.White,
    secondary = BluePrimary,
    background = SurfaceDark,
    onBackground = OnSurface,
    surface = Color(0xFF102A43),
    onSurface = OnSurface,
    error = CancelRed
)

@Composable
fun DriveVoiceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Driving Mode: always dark for glare reduction
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
