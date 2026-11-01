package com.example.atlantvpn

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.atlantvpn.ui.theme.AtlantVPNTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Preview(name="Server overview · Dark",widthDp=393,heightDp=820,showBackground=true)
@Composable
private fun ServerOverviewDesignPreview() {
    AtlantVPNTheme(darkTheme=true) {Surface {Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)){ServerOverviewPanel(JSONObject().put("total_users",12).put("active_connections",4),JSONObject().put("active","active"),{}, {})}}}
}

@Preview(name="Subscriptions · Dark",widthDp=393,heightDp=820,showBackground=true)
@Composable
private fun SubscriptionDarkPreview() = SubscriptionDesignPreview(true)

@Preview(name="Subscriptions · Light",widthDp=393,heightDp=820,showBackground=true)
@Composable
private fun SubscriptionLightPreview() = SubscriptionDesignPreview(false)

@Composable
private fun SubscriptionDesignPreview(dark:Boolean) {
    val sub=SubscriptionInfo(id="preview",name="Личная подписка",url="https://example.com/sub/preview",total=21474836480L,download=5368709120L)
    val server=ServerProfile("one","Основной сервер","192.0.2.1:1026","example.com","","","",isSubscription=true,subscriptionId="preview",pingMs=24)
    AtlantVPNTheme(darkTheme=dark) {Surface {Box(Modifier.padding(16.dp)){SubscriptionServerList(listOf(server,server.copy(id="two",name="Резервный сервер",pingMs=38)),server,sub,{},{},{},{},{},{},{},{})}}}
}

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
