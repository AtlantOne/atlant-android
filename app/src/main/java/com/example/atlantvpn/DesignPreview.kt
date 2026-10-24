package com.example.atlantvpn

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.atlantvpn.ui.theme.AtlantVPNTheme

@Preview(name = "Settings · Light", widthDp = 360, heightDp = 740)
@Preview(name = "Settings · Large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SettingsDesignPreview() {
    AtlantVPNTheme(darkTheme = false) {
        Surface { SettingsContent(AppSettings(), RoutingSettings(), SettingsPage.HOME, {}, {}, {}, {}) }
    }
}

@Preview(name = "Routing · Dark", widthDp = 360, heightDp = 740)
@Preview(name = "Routing · Large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun RoutingDesignPreview() {
    AtlantVPNTheme(darkTheme = true) {
        Surface { RoutingSubScreen(RoutingSettings(mode = RouteMode.BYPASS), emptyList(), false, {}, {}) }
    }
}

// Offline Compose previews: no native initialization, real profiles or VPN start.
@Preview(name = "Atlant · Light", widthDp = 393, heightDp = 780, showBackground = true)
@Composable
private fun LightDesignPreview() = DesignPreview(false)

@Preview(name = "Atlant · Dark", widthDp = 393, heightDp = 780, showBackground = true)
@Composable
private fun DarkDesignPreview() = DesignPreview(true)

@Composable
private fun DesignPreview(dark: Boolean) {
    val profile = ServerProfile("preview", "Личный сервер", "198.51.100.20:443", "example.com", "", "", "", "QUIC", pingMs = 24)
    AtlantVPNTheme(darkTheme = dark) {
        Surface {
            ConnectionScreen(
                status = VpnStatus.DISCONNECTED, durationSec = 0,
                servers = listOf(profile), selectedServer = profile, subscription = null,
                onConnectToggle = {}, onSelectServer = {}, onDeleteServer = {}, onEditServer = {},
                onPingServer = {}, onPingAll = {}, onAddServerClick = {}, onPasteClipboard = {},
                onQrScanClick = {}, onUpdateSubscription = {}, onEditSubscription = {},
                onClearSubscription = {}, onCopyLink = {}
            )
        }
    }
}
