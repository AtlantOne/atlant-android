package com.example.atlantvpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Semantic tokens keep every screen and dialog in the selected theme.
val AtlantPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val AtlantPrimaryDark: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val AtlantSecondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
val AtlantBackground: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val AtlantSurface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val AtlantSurfaceVariant: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHigh
val AtlantCardBorder: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
val AtlantTextPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val AtlantTextSecondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val AtlantTextMuted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val AtlantBadgeBg: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
val AtlantSelectedBlue: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val AtlantSelectedCardBg: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
val AtlantDanger: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
val AtlantDangerSurface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.errorContainer
val AtlantConnectedGreen = Color(0xFF16B89A)
val AtlantConnectedGreenDark = Color(0xFF087C69)
val AtlantConnectingOrange = Color(0xFFD89B32)
val AtlantDisconnectedGray = Color(0xFF81908E)
