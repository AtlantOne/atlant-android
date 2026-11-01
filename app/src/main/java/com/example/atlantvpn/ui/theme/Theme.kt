package com.example.atlantvpn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4F4F5), onPrimary = Color(0xFF111113),
    primaryContainer = Color(0xFF27272C), onPrimaryContainer = Color(0xFFF4F4F5),
    secondary = Color(0xFFADADB5), secondaryContainer = Color(0xFF27272C), onSecondaryContainer = Color(0xFFF4F4F5),
    background = Color(0xFF070708), surface = Color(0xFF141416),
    surfaceDim = Color(0xFF101012), surfaceBright = Color(0xFF303035),
    surfaceContainerLowest = Color(0xFF09090A), surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = Color(0xFF1A1A1D), surfaceContainerHigh = Color(0xFF202023), surfaceContainerHighest = Color(0xFF2A2A2F),
    onBackground = Color(0xFFF4F4F5), onSurface = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFFADADB5), outline = Color(0xFF72727C),
    outlineVariant = Color(0xFF333338), error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF4D2827)
)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF202024), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8EC), onPrimaryContainer = Color(0xFF171719),
    secondary = Color(0xFF626268), background = Color(0xFFF4F4F5),
    surface = Color.White, surfaceContainerHigh = Color(0xFFEEEEEF),
    onBackground = Color(0xFF171719), onSurface = Color(0xFF171719),
    onSurfaceVariant = Color(0xFF626268), outline = Color(0xFF71717A),
    outlineVariant = Color(0xFFDADADD), error = Color(0xFFB33D39),
    errorContainer = Color(0xFFFFDAD6)
)
@Composable
fun AtlantVPNTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content
    )
}
