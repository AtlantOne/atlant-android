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
    primary = Color(0xFF63D7C1), onPrimary = Color(0xFF073D34),
    primaryContainer = Color(0xFF173E38), onPrimaryContainer = Color(0xFFB3F3E4),
    secondary = Color(0xFFA5BEB7), background = Color(0xFF101918),
    surface = Color(0xFF182321), surfaceContainerHigh = Color(0xFF22302D),
    onBackground = Color(0xFFEDF4F0), onSurface = Color(0xFFEDF4F0),
    onSurfaceVariant = Color(0xFFAEBCB6), outline = Color(0xFF7D9087),
    outlineVariant = Color(0xFF33433D), error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF4D2827)
)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF087B68), onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EFE5), onPrimaryContainer = Color(0xFF063D32),
    secondary = Color(0xFF506960), background = Color(0xFFF4F6F2),
    surface = Color(0xFFFEFFFC), surfaceContainerHigh = Color(0xFFEAF0E9),
    onBackground = Color(0xFF182C25), onSurface = Color(0xFF182C25),
    onSurfaceVariant = Color(0xFF53675D), outline = Color(0xFF70877B),
    outlineVariant = Color(0xFFD6E0D6), error = Color(0xFFB33D39),
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
