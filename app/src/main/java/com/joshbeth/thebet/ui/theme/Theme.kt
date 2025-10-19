package com.joshbeth.thebet.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.joshbeth.thebet.ThemeSelection

private val PunishmentColorScheme = darkColorScheme(
    primary = CrimsonRed,
    secondary = GoldAccent,
    tertiary = RichBlack,
    background = DeepBlack,
    surface = RichBlack,
    onPrimary = SoftWhite,
    onSecondary = DeepBlack,
    onTertiary = SoftWhite,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    onSurfaceVariant = GoldAccent
)

private val RewardColorScheme = darkColorScheme(
    primary = SexyPeach,
    secondary = GoldHighlight,
    tertiary = RichTeal,
    background = DeepTeal,
    surface = RichTeal,
    onPrimary = DeepTeal,
    onSecondary = DeepTeal,
    onTertiary = WarmCream,
    onBackground = WarmCream,
    onSurface = WarmCream,
    onSurfaceVariant = GoldHighlight
)

private val NeutralColorScheme = darkColorScheme(
    primary = NeutralGrayLight,
    secondary = NeutralGrayMedium,
    tertiary = NeutralGrayDark,
    background = NeutralGrayDark,
    surface = NeutralGrayMedium,
    onPrimary = NeutralWhite,
    onSecondary = NeutralWhite,
    onTertiary = NeutralWhite,
    onBackground = NeutralWhite,
    onSurface = NeutralWhite,
    onSurfaceVariant = NeutralGrayLight
)

@Composable
fun TheBetTheme(
    theme: ThemeSelection = ThemeSelection.NEUTRAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        ThemeSelection.PUNISHMENT -> PunishmentColorScheme
        ThemeSelection.REWARD -> RewardColorScheme
        ThemeSelection.NEUTRAL -> NeutralColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
