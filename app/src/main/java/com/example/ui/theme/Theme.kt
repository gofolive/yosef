package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MtuPrimaryContainer,
    onPrimary = MtuOnPrimaryContainer,
    primaryContainer = MtuSecondary,
    onPrimaryContainer = MtuPrimaryContainer,
    secondary = MtuSecondaryContainer,
    onSecondary = MtuOnSecondaryContainer,
    tertiary = MtuTertiaryContainer,
    background = MtuDarkBackground,
    onBackground = MtuDarkOnSurface,
    surface = MtuDarkSurface,
    onSurface = MtuDarkOnSurface,
    surfaceVariant = Color(0xFF22302D),
    onSurfaceVariant = Color(0xFFC0CCC9)
)

private val LightColorScheme = lightColorScheme(
    primary = MtuPrimary,
    onPrimary = MtuOnPrimary,
    primaryContainer = MtuPrimaryContainer,
    onPrimaryContainer = MtuOnPrimaryContainer,
    secondary = MtuSecondary,
    onSecondary = MtuOnSecondary,
    secondaryContainer = MtuSecondaryContainer,
    onSecondaryContainer = MtuOnSecondaryContainer,
    tertiary = MtuTertiary,
    background = MtuBackground,
    onBackground = MtuOnBackground,
    surface = MtuSurface,
    onSurface = MtuOnSurface,
    surfaceVariant = MtuSurfaceVariant,
    onSurfaceVariant = MtuOnSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
