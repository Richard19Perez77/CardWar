package com.rick.cardwar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MikuDarkColorScheme = darkColorScheme(
    primary = MikuTeal,
    onPrimary = MikuInk,
    primaryContainer = MikuTealDark,
    onPrimaryContainer = MikuMint,
    secondary = MikuGrey,
    onSecondary = MikuInk,
    secondaryContainer = Color(0xFF1E3A38),
    onSecondaryContainer = MikuMint,
    tertiary = MikuTealLight,
    onTertiary = MikuInk,
    background = MikuInk,
    onBackground = MikuMint,
    surface = MikuNight,
    onSurface = MikuWhite,
    surfaceVariant = Color(0xFF1A3331),
    onSurfaceVariant = MikuGrey,
    outline = MikuTealDark,
)

private val MikuLightColorScheme = lightColorScheme(
    primary = MikuTealDark,
    onPrimary = MikuWhite,
    primaryContainer = MikuTealLight,
    onPrimaryContainer = MikuInk,
    secondary = Color(0xFF4A5C5B),
    onSecondary = MikuWhite,
    secondaryContainer = Color(0xFFD5E6E4),
    onSecondaryContainer = MikuInk,
    tertiary = MikuTeal,
    onTertiary = MikuInk,
    background = MikuMint,
    onBackground = MikuInk,
    surface = MikuWhite,
    onSurface = MikuInk,
    surfaceVariant = Color(0xFFE3F4F2),
    onSurfaceVariant = Color(0xFF3D4F4D),
    outline = MikuTealDark,
)

@Composable
fun CardWarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MikuDarkColorScheme else MikuLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
