package com.example.atlantvpn

data class ServerProfile(
    val id: String,
    val name: String,
    val server: String,
    val sni: String,
    val pbk: String,
    val user: String,
    val serverPubKey: String,
    val transport: String = "REALITY",
    val pingMs: Int = -1, // -1 = not pinged
    val isSubscription: Boolean = false,
    val group: String = "",
    val fingerprint: String = "", // Optional per-server uTLS fingerprint override
    val wireVersion: Int = 6,
    val subscriptionId: String = "",
    val serverId: String = "",
    val inboundId: String = "",
    val xhttpPath: String = "/api/v1/sync",
    val availableTransports: List<String> = emptyList(),
)

data class SubscriptionInfo(
    val id: String = "",
    val url: String = "",
    val name: String = "",
    val lastUpdated: String = "",
    val count: Int = 0,
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
    val remainingGB: String = "",
    val updateHours: Int = 1,
    val supportUrl: String = "",
    val announcement: String = "",
    val usageScope: String = ""
)

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class RouteMode {
    ALL,     // Global: all traffic via VPN
    RULES,   // Proxy rules: only selected apps via VPN
    BYPASS   // Direct / bypass: all via VPN except selected apps
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String
)

data class RoutingSettings(
    val enabled: Boolean = true,
    val mode: RouteMode = RouteMode.ALL,
    val proxyApps: Set<String> = emptySet(),
    val bypassApps: Set<String> = emptySet(),
    val proxyDomains: String = "youtube.com\ngooglevideo.com\ndiscord.com\ninstagram.com\ntwitter.com\nx.com",
    val directDomains: String = "*.ru\ngosuslugi.ru\nvk.com\nyandex.ru\n192.168.0.0/16\n127.0.0.1"
)

enum class TrafficMode {
    SPLIT, // TCP and UDP split
    TCP,   // TCP only
    UDP    // UDP only
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    fun isDark(systemDark: Boolean): Boolean = when (this) { SYSTEM -> systemDark; LIGHT -> false; DARK -> true }
    companion object {
        fun fromStored(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Сеть и TUN
    val mtu: Int = 1360,
    val dns: String = "1.1.1.1",
    val tunEnabled: Boolean = true,
    val killSwitch: Boolean = false,
    val blockIPv6: Boolean = true,

    // Протокол трафика
    val trafficMode: TrafficMode = TrafficMode.SPLIT,
    val udpProto: String = "tcp",      // "tcp" (default, 100% reliable Reality tunnel) or "quic"
    val tcpProto: String = "reality",  // "reality" or "xhttp"
    val mux: Boolean = false,          // Yamux multiplexing (default off)

    // Логирование (по умолчанию отключено: off, error, info, debug)
    val logLevel: String = "error",

    // Параметры сетевого протокола и обфускации
    val fakeDns: Boolean = true,
    val trafficPadding: Boolean = true,
    val minPadding: Int = 32,
    val maxPadding: Int = 256,
    val autoFallback: Boolean = true,
    val portHopping: Boolean = false,
    val decoyTraffic: Boolean = false,
    val remoteDns: Boolean = true,
    val zeroRtt: Boolean = true,
    val fingerprint: String = "firefox",
    val chaffingEnabled: Boolean = true,
    val chaffingInterval: Int = 10
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String
)

fun AppSettings.transportModes(): Pair<String, String> {
    val streamTransport = if (tcpProto.equals("xhttp", ignoreCase = true)) "xhttp" else "reality"
    return when (trafficMode) {
        TrafficMode.TCP -> streamTransport to "tcp"
        TrafficMode.UDP -> "quic" to "quic"
        TrafficMode.SPLIT -> streamTransport to udpProto
    }
}
