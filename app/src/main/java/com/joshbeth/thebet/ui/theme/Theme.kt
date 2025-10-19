package com.joshbeth.thebet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Updated Dark Color Scheme for a Sensual & Playful Feel
private val DarkColorScheme = darkColorScheme(
    primary = SensualPink,
    secondary = LightPink,
    tertiary = DarkPurple,
    background = DeepPurple,
    surface = DarkPurple,
    onPrimary = DeepPurple,
    onSecondary = DeepPurple,
    onTertiary = OffWhite,
    onBackground = OffWhite,
    onSurface = OffWhite,
    onSurfaceVariant = LightPink
)


@Composable
fun TheBetTheme(
    darkTheme: Boolean = true, // Forcing dark theme for this aesthetic
    dynamicColor: Boolean = false, // Disabling dynamic color to maintain our theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Always use our custom dark scheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
