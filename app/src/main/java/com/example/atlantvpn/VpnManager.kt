package com.example.atlantvpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import atlant.Atlant

object VpnManager {
    private const val TAG = "VpnManager"
    private const val PREFS_NAME = "atlant_vpn_prefs"
    private const val KEY_SERVERS = "saved_servers"
    private const val KEY_SELECTED_ID = "selected_server_id"
    private const val KEY_SETTINGS = "app_settings"
    private const val KEY_ROUTING = "routing_settings"
    private const val KEY_SUBSCRIPTION = "subscription_info"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val pingSlots = Semaphore(8)
    private val pingsInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _connectionDuration = MutableStateFlow(0L)
    val connectionDuration: StateFlow<Long> = _connectionDuration.asStateFlow()

    private val _servers = MutableStateFlow<List<ServerProfile>>(emptyList())
    val servers: StateFlow<List<ServerProfile>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<ServerProfile?>(null)
    val selectedServer: StateFlow<ServerProfile?> = _selectedServer.asStateFlow()

    private val _subscription = MutableStateFlow<SubscriptionInfo?>(null)
    val subscription: StateFlow<SubscriptionInfo?> = _subscription.asStateFlow()
    private val _subscriptions = MutableStateFlow<List<SubscriptionInfo>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionInfo>> = _subscriptions.asStateFlow()

    fun focusSubscription(id: String) { _subscription.value = _subscriptions.value.firstOrNull { it.id == id } }

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _routingSettings = MutableStateFlow(RoutingSettings())
    val routingSettings: StateFlow<RoutingSettings> = _routingSettings.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(level = LogLevel.INFO, message = "Система Atlant VPN инициализирована.")
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(message: String, level: LogLevel = LogLevel.INFO) {
        val currentSetting = _appSettings.value.logLevel
        val lower = message.lowercase(Locale.ROOT)
        val isError = level == LogLevel.ERROR || lower.contains("error") || lower.contains("fail") || lower.contains("[err")
        val isWarn = level == LogLevel.WARN || lower.contains("warn") || lower.contains("timeout")
        val isDebug = lower.contains("debug") || lower.contains("[debug")

        val allow = when (currentSetting) {
            "none", "silent", "off" -> isError
            "error" -> isError
            "warn", "warning" -> isError || isWarn
            "info" -> !isDebug
            "debug" -> true
            else -> !isDebug
        }
        if (!allow) return

        val entry = LogEntry(level = level, message = message)
        synchronized(this) {
            _logs.value = (_logs.value + entry).takeLast(400)
        }
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, message)
            LogLevel.WARN -> Log.w(TAG, message)
            else -> Log.i(TAG, message)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun getLogsFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return _logs.value.joinToString("\n") { entry ->
            val time = sdf.format(Date(entry.timestamp))
            val lvl = when (entry.level) {
                LogLevel.INFO -> "[INFO]"
                LogLevel.SUCCESS -> "[OK]"
                LogLevel.WARN -> "[WARN]"
                LogLevel.ERROR -> "[ERR]"
            }
            "$time $lvl ${entry.message}"
        }
    }

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadFromPrefs(context)
        loadInstalledApps(context)
    }

    fun loadInstalledApps(context: Context) {
        if (_installedApps.value.isNotEmpty() || _isLoadingApps.value) return
        _isLoadingApps.value = true
        scope.launch {
            try {
                val pm = context.packageManager
                val apps = pm.getInstalledApplications(0)
                    .filter { it.packageName != context.packageName }
                    .map {
                        InstalledAppInfo(
                            packageName = it.packageName,
                            appName = pm.getApplicationLabel(it).toString()
                        )
                    }.sortedBy { it.appName.lowercase(Locale.ROOT) }
                _installedApps.value = apps
            } catch (e: Exception) {
                Log.e(TAG, "Error loading installed apps: ${e.message}")
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    private fun readSecretPreference(context: Context, name: String): String? = try {
        SecretPreferences.read(context, name)
    } catch (_: Exception) {
        log("Не удалось открыть защищённые профили. Исходные данные сохранены; не очищайте данные приложения.", LogLevel.ERROR)
        null
    }

    private fun writeSecretPreference(context: Context, name: String, value: String?) {
        try {
            SecretPreferences.write(context, name, value)
        } catch (_: Exception) {
            log("Не удалось сохранить защищённые профили. Изменения не сохранены на устройстве.", LogLevel.ERROR)
        }
    }

    private fun loadFromPrefs(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load subscription metadata before restoring its profiles.
        val subJson = readSecretPreference(context, KEY_SUBSCRIPTION)
        if (!subJson.isNullOrBlank()) {
            try {
                val arr = if (subJson.trim().startsWith("[")) JSONArray(subJson) else JSONArray().put(JSONObject(subJson))
                val loaded = (0 until arr.length()).map { index ->
                val obj = arr.getJSONObject(index)
                SubscriptionInfo(
                    id = obj.optString("id", "").ifBlank { subscriptionId(obj.optString("url", "")) },
                    url = obj.optString("url", ""),
                    name = obj.optString("name", "Подписка"),
                    lastUpdated = obj.optString("last_updated", ""),
                    count = obj.optInt("count", 0),
                    upload = obj.optLong("upload", 0),
                    download = obj.optLong("download", 0),
                    total = obj.optLong("total", 0),
                    expire = obj.optLong("expire", 0),
                    remainingGB = obj.optString("remaining_gb", ""),
                    updateHours = obj.optInt("update_hours", 1),
                    supportUrl = obj.optString("support_url", ""),
                    announcement = obj.optString("announcement", ""),
                    usageScope = obj.optString("usage_scope", "")
                )
                }
                _subscriptions.value = loaded
                _subscription.value = loaded.firstOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load subscription: ${e.message}")
            }
        }

        val serversJson = readSecretPreference(context, KEY_SERVERS)
        val loadedList = mutableListOf<ServerProfile>()
        if (!serversJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(serversJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    loadedList.add(
                        ServerProfile(
                            id = obj.optString("id", "srv-$i"),
                            name = obj.optString("name", "Atlant Server"),
                            server = obj.optString("server", ""),
                            sni = obj.optString("sni", "www.microsoft.com"),
                            pbk = obj.optString("pbk", ""),
                            user = obj.optString("user", "user"),
                            serverPubKey = obj.optString("server_pub_key", ""),
                            transport = obj.optString("transport", "REALITY"),
                            pingMs = obj.optInt("ping_ms", -1),
                            isSubscription = obj.optBoolean("is_subscription", false),
                            group = obj.optString("group", ""),
                            fingerprint = obj.optString("fingerprint", ""),
                            serverId = obj.optString("server_id", ""),
                            inboundId = obj.optString("inbound_id", ""),
                            xhttpPath = subscriptionXhttpPath(obj.optString("path", "")),
                            availableTransports = parseAvailableTransports(obj.optString("transports", "")),
                            wireVersion = obj.optInt("wire_version", 4),
                            subscriptionId = obj.optString("subscription_id", "").ifBlank {
                                if (obj.optBoolean("is_subscription", false) && _subscriptions.value.size == 1) _subscriptions.value.first().id else ""
                            },
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load servers: ${e.message}")
            }
        }
        val selectedId = sp.getString(KEY_SELECTED_ID, null)
        val previousSelection = loadedList.firstOrNull { it.id == selectedId }
        val restored = if (_subscription.value != null) {
            loadedList.filterNot { it.isSubscription } + mergeSubscriptionEndpoints(loadedList.filter { it.isSubscription })
        } else loadedList
        _servers.value = restored
        _selectedServer.value = restored.firstOrNull { it.id == selectedId }
            ?: previousSelection?.let { previous -> restored.firstOrNull { profileIdentity(it) == profileIdentity(previous) } }
            ?: restored.firstOrNull()
        _subscriptions.value = _subscriptions.value.map { sub -> sub.copy(count = restored.count { it.subscriptionId == sub.id }) }
        _subscription.value = _subscriptions.value.firstOrNull()

        val settingsJson = sp.getString(KEY_SETTINGS, null)
        if (!settingsJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(settingsJson)
                _appSettings.value = AppSettings(
                    mtu = obj.optInt("mtu", 1360),
                    dns = obj.optString("dns", "1.1.1.1"),
                    tunEnabled = obj.optBoolean("tun_enabled", true),
                    killSwitch = obj.optBoolean("kill_switch", false),
                    blockIPv6 = obj.optBoolean("block_ipv6", true),
                    trafficMode = TrafficMode.valueOf(obj.optString("traffic_mode", TrafficMode.SPLIT.name)),
                    udpProto = obj.optString("udp_proto", "tcp"),
                    tcpProto = obj.optString("tcp_proto", "reality"),
                    mux = obj.optBoolean("mux", false),
                    logLevel = obj.optString("log_level", "error"),
                    fakeDns = obj.optBoolean("fake_dns", true),
                    trafficPadding = obj.optBoolean("traffic_padding", true),
                    minPadding = obj.optInt("min_padding", 32),
                    maxPadding = obj.optInt("max_padding", 256),
                    autoFallback = obj.optBoolean("auto_fallback", true),
                    portHopping = obj.optBoolean("port_hopping", false),
                    decoyTraffic = obj.optBoolean("decoy_traffic", false),
                    remoteDns = obj.optBoolean("remote_dns", true),
                    zeroRtt = obj.optBoolean("zero_rtt", true),
                    fingerprint = obj.optString("fingerprint", "firefox"),
                    chaffingEnabled = obj.optBoolean("chaffing_enabled", true),
                    chaffingInterval = obj.optInt("chaffing_interval", 10),
                    themeMode = ThemeMode.fromStored(obj.optString("theme_mode", "SYSTEM"))
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load settings: ${e.message}")
            }
        }

        val routingJson = sp.getString(KEY_ROUTING, null)
        if (!routingJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(routingJson)
                val proxyArr = obj.optJSONArray("proxy_apps") ?: JSONArray()
                val proxySet = mutableSetOf<String>()
                for (i in 0 until proxyArr.length()) proxySet.add(proxyArr.getString(i))

                val bypassArr = obj.optJSONArray("bypass_apps") ?: JSONArray()
                val bypassSet = mutableSetOf<String>()
                for (i in 0 until bypassArr.length()) bypassSet.add(bypassArr.getString(i))

                _routingSettings.value = RoutingSettings(
                    enabled = obj.optBoolean("enabled", true),
                    mode = RouteMode.valueOf(obj.optString("mode", RouteMode.ALL.name)),
                    proxyApps = proxySet,
                    bypassApps = bypassSet,
                    proxyDomains = obj.optString("proxy_domains", "youtube.com\ngooglevideo.com\ndiscord.com\ninstagram.com\ntwitter.com\nx.com"),
                    directDomains = obj.optString("direct_domains", "*.ru\ngosuslugi.ru\nvk.com\nyandex.ru\n192.168.0.0/16\n127.0.0.1")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load routing: ${e.message}")
            }
        }
    }

    private fun saveServersToPrefs(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (s in _servers.value) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("server", s.server)
                put("sni", s.sni)
                put("pbk", s.pbk)
                put("user", s.user)
                put("server_pub_key", s.serverPubKey)
                put("transport", s.transport)
                put("ping_ms", s.pingMs)
                put("is_subscription", s.isSubscription)
                put("subscription_id", s.subscriptionId)
                put("group", s.group)
                put("fingerprint", s.fingerprint)
                put("wire_version", s.wireVersion)
                put("server_id", s.serverId)
                put("inbound_id", s.inboundId)
                put("path", s.xhttpPath)
                put("transports", s.availableTransports.joinToString(","))
            }
            arr.put(obj)
        }
        writeSecretPreference(context, KEY_SERVERS, arr.toString())
        sp.edit()
            .putString(KEY_SELECTED_ID, _selectedServer.value?.id)
            .apply()
    }

    private fun saveSubscriptionToPrefs(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (_subscriptions.value.isNotEmpty()) {
            val arr = JSONArray()
            _subscriptions.value.forEach { sub ->
            val obj = JSONObject().apply {
                put("id", sub.id)
                put("url", sub.url)
                put("name", sub.name)
                put("last_updated", sub.lastUpdated)
                put("count", sub.count)
                put("upload", sub.upload)
                put("download", sub.download)
                put("total", sub.total)
                put("expire", sub.expire)
                put("remaining_gb", sub.remainingGB)
                put("update_hours", sub.updateHours)
                put("support_url", sub.supportUrl)
                put("announcement", sub.announcement)
                put("usage_scope", sub.usageScope)
            }
            arr.put(obj)
            }
            writeSecretPreference(context, KEY_SUBSCRIPTION, arr.toString())
        } else {
            writeSecretPreference(context, KEY_SUBSCRIPTION, null)
        }
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        _appSettings.value = settings
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject().apply {
            put("mtu", settings.mtu)
            put("theme_mode", settings.themeMode.name)
            put("dns", settings.dns)
            put("tun_enabled", settings.tunEnabled)
            put("kill_switch", settings.killSwitch)
            put("block_ipv6", settings.blockIPv6)
            put("traffic_mode", settings.trafficMode.name)
            put("udp_proto", settings.udpProto)
            put("tcp_proto", settings.tcpProto)
            put("mux", settings.mux)
            put("log_level", settings.logLevel)
            put("fake_dns", settings.fakeDns)
            put("traffic_padding", settings.trafficPadding)
            put("min_padding", settings.minPadding)
            put("max_padding", settings.maxPadding)
            put("auto_fallback", settings.autoFallback)
            put("port_hopping", settings.portHopping)
            put("decoy_traffic", settings.decoyTraffic)
            put("remote_dns", settings.remoteDns)
            put("zero_rtt", settings.zeroRtt)
            put("fingerprint", settings.fingerprint)
            put("chaffing_enabled", settings.chaffingEnabled)
            put("chaffing_interval", settings.chaffingInterval)
        }
        sp.edit().putString(KEY_SETTINGS, obj.toString()).apply()
    }

    fun saveRouting(context: Context, routing: RoutingSettings) {
        _routingSettings.value = routing
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject().apply {
            put("enabled", routing.enabled)
            put("mode", routing.mode.name)
            put("proxy_apps", JSONArray(routing.proxyApps))
            put("bypass_apps", JSONArray(routing.bypassApps))
            put("proxy_domains", routing.proxyDomains)
            put("direct_domains", routing.directDomains)
        }
        sp.edit().putString(KEY_ROUTING, obj.toString()).apply()
        try {
            val modeStr = if (!routing.enabled) "all" else when (routing.mode) {
                RouteMode.RULES -> "rules"
                RouteMode.BYPASS -> "bypass"
                RouteMode.ALL -> "all"
            }
            atlant.Atlant.setRouteMode(modeStr)
            atlant.Atlant.setProxyDomains(routing.proxyDomains)
            atlant.Atlant.setDirectDomains(routing.directDomains)
        } catch (e: Throwable) {
            // ignore if native engine not loaded yet
        }
    }

    fun selectServer(context: Context, server: ServerProfile) {
        _selectedServer.value = server
        saveServersToPrefs(context)
    }

    fun addServer(context: Context, server: ServerProfile) {
        _servers.value = _servers.value + server
        if (_selectedServer.value == null) {
            _selectedServer.value = server
        }
        saveServersToPrefs(context)
    }

    fun removeServer(context: Context, serverId: String) {
        val updated = _servers.value.filter { it.id != serverId }
        _servers.value = updated
        if (_selectedServer.value?.id == serverId) {
            _selectedServer.value = updated.firstOrNull()
        }
        saveServersToPrefs(context)
    }

    fun updateServer(context: Context, updated: ServerProfile) {
        val list = _servers.value.map {
            if (it.id == updated.id) updated else it
        }
        _servers.value = list
        if (_selectedServer.value?.id == updated.id) {
            _selectedServer.value = updated
        }
        saveServersToPrefs(context)
        log("Сервер '${updated.name}' обновлен", LogLevel.INFO)
    }

    fun updateSubscriptionInfo(
        context: Context,
        newName: String,
        newUrl: String,
        refreshNow: Boolean = false,
        targetSubscriptionId: String = _subscription.value?.id ?: "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val current = _subscriptions.value.firstOrNull { it.id == targetSubscriptionId } ?: return
        try { validateSubscriptionUrl(URL(newUrl.trim())) }
        catch (_: Exception) { onComplete?.invoke(false, "Проверьте ссылку подписки: требуется HTTPS"); return }
        if (_subscriptions.value.any { it.id != current.id && it.url == newUrl.trim() }) {
            onComplete?.invoke(false, "Эта подписка уже добавлена"); return
        }
        val updated = current.copy(name = newName.trim(), url = newUrl.trim())
        _subscription.value = updated
        _subscriptions.value = _subscriptions.value.map { if (it.id == current.id) updated else it }
        saveSubscriptionToPrefs(context)
        log("Параметры подписки сохранены: ${updated.name}", LogLevel.INFO)
        if (refreshNow && newUrl.isNotBlank()) {
            fetchSubscription(context, newUrl.trim()) { ok, msg ->
                onComplete?.invoke(ok, msg)
            }
        } else {
            onComplete?.invoke(true, "Параметры подписки сохранены")
        }
    }

    // Parse single link or whole subscription URL / base64
    fun importClipboardOrUrl(context: Context, raw: String, onComplete: (Boolean, String) -> Unit) {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            val urls = trimmed.split(Regex("\\s+")).distinct()
            if (urls.size > 32) { onComplete(false, "За один импорт можно добавить до 32 подписок"); return }
            try { urls.forEach { validateSubscriptionUrl(URL(it)) } }
            catch (_: Exception) { onComplete(false, "Проверьте ссылки подписок: требуется HTTPS"); return }
            fun fetchNext(index: Int) {
                fetchSubscription(context, urls[index]) { ok, msg ->
                    if (!ok || index == urls.lastIndex) onComplete(ok, if (ok && urls.size > 1) "Подписки добавлены: ${urls.size}" else msg)
                    else fetchNext(index + 1)
                }
            }
            fetchNext(0)
            return
        }

        if (trimmed.startsWith("atlant://", ignoreCase = true)) {
            val ok = parseAndAddAtlantLink(context, trimmed)
            if (ok) {
                onComplete(true, "Сервер успешно добавлен!")
            } else {
                onComplete(false, "Некорректная ссылка atlant://")
            }
            return
        }

        // Try base64 decoding (standard VLESS / V2Ray subscription export)
        try {
            val decoded = String(Base64.decode(trimmed, Base64.DEFAULT), Charsets.UTF_8)
            val lines = decoded.lines().map { it.trim() }.filter { it.startsWith("atlant://", ignoreCase = true) }
            if (lines.isNotEmpty()) {
                var added = 0
                for (l in lines) {
                    if (parseAndAddAtlantLink(context, l)) added++
                }
                onComplete(true, "Добавлено серверов из буфера: $added")
                return
            }
        } catch (_: Exception) {}

        onComplete(false, "Неподдерживаемый формат. Вставьте atlant:// или ссылку на подписку https://")
    }

    // Refresh active subscription
    fun updateSubscription(context: Context, onComplete: (Boolean, String) -> Unit) {
        val sub = _subscription.value
        if (sub == null || sub.url.isBlank()) {
            onComplete(false, "Подписка не настроена")
            return
        }
        fetchSubscription(context, sub.url, onComplete)
    }

    // Delete subscription and its servers
    fun clearSubscription(context: Context) {
        val sub = _subscription.value ?: return
        _servers.value = _servers.value.filter { it.subscriptionId != sub.id }
        if (_selectedServer.value?.subscriptionId == sub.id) {
            _selectedServer.value = _servers.value.firstOrNull()
        }
        _subscriptions.value = _subscriptions.value.filter { it.id != sub.id }
        _subscription.value = _subscriptions.value.firstOrNull()
        saveServersToPrefs(context)
        saveSubscriptionToPrefs(context)
        log("Подписка удалена", LogLevel.INFO)
    }

    private fun fetchSubscription(context: Context, subUrl: String, onComplete: (Boolean, String) -> Unit) {
        log("Загрузка подписки…", LogLevel.INFO) // URL is a bearer secret.
        scope.launch {
            var subscriptionConnection: HttpURLConnection? = null
            try {
                val url = URL(subUrl)
                val conn = openSubscriptionConnection(url)
                subscriptionConnection = conn

                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    withContext(Dispatchers.Main) {
                        log("Ошибка сервера при получении подписки: HTTP $responseCode", LogLevel.ERROR)
                        onComplete(false, "HTTP $responseCode: ошибка загрузки подписки")
                    }
                    return@launch
                }

                // Parse Subscription-Userinfo header (upload, download, total, expire)
                var upload = 0L
                var download = 0L
                var total = 0L
                var expire = 0L

                val userInfo = conn.getHeaderField("Subscription-Userinfo") ?: conn.getHeaderField("subscription-userinfo")
                if (!userInfo.isNullOrBlank()) {
                    val parts = userInfo.split(";")
                    for (p in parts) {
                        val kv = p.trim().split("=", limit = 2)
                        if (kv.size == 2) {
                            val k = kv[0].trim().lowercase(Locale.ROOT)
                            val v = kv[1].trim().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                            when (k) {
                                "upload" -> upload = v
                                "download" -> download = v
                                "total" -> total = v
                                "expire" -> expire = v
                            }
                        }
                    }
                }

                // Read body
                val body = conn.inputStream.use { readSubscriptionBody(it).trim() }

                // Parse links (plain or base64)
                var lines = body.lines()
                if (lines.size <= 2 && !body.contains("://")) {
                    try {
                        val decoded = String(Base64.decode(body, Base64.DEFAULT), Charsets.UTF_8)
                        lines = decoded.lines()
                    } catch (_: Exception) {}
                }

                val groupName = subscriptionHeaderText(conn.getHeaderField("Profile-Title"))
                    .ifBlank { url.host ?: "Atlant Subscription" }
                val parsedProfiles = mutableListOf<ServerProfile>()
                var idx = 1

                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.startsWith("atlant://", ignoreCase = true)) {
                        val parsed = parseAtlantUriToProfile(line, idx, groupName)
                        if (parsed != null) {
                            require(parsedProfiles.size < 256) { "В подписке больше 256 серверов" }
                            parsedProfiles.add(parsed)
                            idx++
                        }
                    }
                }

                val groupId = _subscriptions.value.firstOrNull { it.url == subUrl }?.id ?: subscriptionId(subUrl)
                val newProfiles = mergeSubscriptionEndpoints(parsedProfiles.map { it.copy(subscriptionId = groupId) })
                if (newProfiles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        log("Подписка не содержит серверов atlant://", LogLevel.WARN)
                        onComplete(false, "В ответе подписки не найдено конфигураций atlant://")
                    }
                    return@launch
                }

                // Calculate remaining GB
                val remainingStr = if (total > 0) {
                    val remain = if (upload >= total || download >= total - upload) 0L else total - upload - download
                    val rGB = remain.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val tGB = total.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    String.format(Locale.getDefault(), "Осталось %.2f / %.2f ГиБ", rGB, tGB)
                } else {
                    "Безлимит"
                }

                val nowFmt = SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault()).format(Date())
                val subInfo = SubscriptionInfo(
                    id = groupId,
                    url = subUrl,
                    name = groupName,
                    lastUpdated = nowFmt,
                    count = newProfiles.size,
                    upload = upload,
                    download = download,
                    total = total,
                    expire = expire,
                    remainingGB = remainingStr,
                    updateHours = (conn.getHeaderField("Profile-Update-Interval")?.toIntOrNull() ?: 1).coerceIn(1, 168),
                    supportUrl = conn.getHeaderField("Support-Url") ?: "",
                    announcement = subscriptionHeaderText(conn.getHeaderField("Announce")),
                    usageScope = conn.getHeaderField("X-Subscription-Usage-Scope") ?: ""
                )

                withContext(Dispatchers.Main) {
                    val existingGroup = _subscriptions.value.any { it.id == groupId }
                    val focusedId = _subscription.value?.id
                    _subscriptions.value = refreshSubscriptionInfo(_subscriptions.value, subInfo)
                    _subscription.value = _subscriptions.value.firstOrNull {
                        it.id == if (existingGroup) focusedId else groupId
                    } ?: _subscriptions.value.first { it.id == groupId }

                    // Keep standalone custom servers, replace existing subscription ones
                    val savedName = _subscriptions.value.first { it.id == groupId }.name
                    val namedProfiles = newProfiles.map { it.copy(group = savedName) }
                    _servers.value = replaceSubscriptionProfiles(_servers.value, groupId, namedProfiles)

                    val previous = _selectedServer.value
                    if (previous == null || previous.subscriptionId == groupId) {
                        _selectedServer.value = namedProfiles.firstOrNull { profileIdentity(it) == previous?.let(::profileIdentity) || (previous != null && migratedSubscriptionProfile(previous, it)) }
                            ?: namedProfiles.firstOrNull() ?: _servers.value.firstOrNull()
                    }

                    saveServersToPrefs(context)
                    saveSubscriptionToPrefs(context)

                    val msg = "Подписка обновлена: ${newProfiles.size} серверов ($remainingStr)"
                    log(msg, LogLevel.SUCCESS)
                    onComplete(true, msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val err = "Не удалось загрузить подписку. Проверьте соединение, сертификат и срок действия ссылки."
                    log(err, LogLevel.ERROR)
                    onComplete(false, err)
                }
            } finally {
                subscriptionConnection?.disconnect()
            }
        }
    }

    private fun parseAtlantUriToProfile(line: String, idx: Int, group: String): ServerProfile? {
        return try {
            val uri = Uri.parse(line)
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            val serverAddr = if (host.contains(':')) "[${host.trim('[', ']')}]:$port" else "$host:$port"
            val sni = uri.getQueryParameter("sni") ?: "www.microsoft.com"
            val user = uri.getQueryParameter("user") ?: "user"
            val pbk = uri.getQueryParameter("pbk") ?: ""
            val serverPubKey = uri.getQueryParameter("server_pub_key") ?: ""
            val fp = uri.getQueryParameter("fp") ?: uri.getQueryParameter("fingerprint") ?: ""
            val wire = (uri.getQueryParameter("wire_version") ?: "4").toIntOrNull() ?: return null
            if (wire !in listOf(2, 3, 4, 6, 7) || port !in 1..65535) return null
            val transport = (uri.getQueryParameter("transport") ?: "reality").lowercase(Locale.ROOT)
            if (transport !in listOf("reality", "tcp", "xhttp", "quic")) return null
            val rawFragment = uri.fragment
            val name = if (!rawFragment.isNullOrBlank()) rawFragment else "$group #$idx"

            ServerProfile(
                id = "sub-" + stableProfileId(serverAddr, sni, user, transport, wire),
                name = name,
                server = serverAddr,
                sni = sni,
                pbk = pbk,
                user = user,
                serverPubKey = serverPubKey,
                transport = transport.uppercase(Locale.ROOT),
                pingMs = -1,
                isSubscription = true,
                group = group,
                fingerprint = fp,
                wireVersion = wire,
                serverId = uri.getQueryParameter("server_id") ?: "",
                inboundId = uri.getQueryParameter("inbound_id") ?: "",
                xhttpPath = subscriptionXhttpPath(uri.getQueryParameter("path")),
                availableTransports = parseAvailableTransports(uri.getQueryParameter("transports") ?: ""),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseAndAddAtlantLink(context: Context, link: String): Boolean {
        return try {
            val trimmed = link.trim()
            if (!trimmed.startsWith("atlant://", ignoreCase = true)) return false
            val uri = Uri.parse(trimmed)
            val host = uri.host ?: return false
            val port = if (uri.port != -1) uri.port else 443
            val serverAddr = if (host.contains(':')) "[${host.trim('[', ']')}]:$port" else "$host:$port"
            val sni = uri.getQueryParameter("sni") ?: "www.microsoft.com"
            val user = uri.getQueryParameter("user") ?: "user"
            val pbk = uri.getQueryParameter("pbk") ?: ""
            val serverPubKey = uri.getQueryParameter("server_pub_key") ?: ""
            val fp = uri.getQueryParameter("fp") ?: uri.getQueryParameter("fingerprint") ?: ""
            val wire = (uri.getQueryParameter("wire_version") ?: "4").toIntOrNull() ?: return false
            if (wire !in listOf(2, 3, 4, 6, 7) || port !in 1..65535) return false
            val transport = (uri.getQueryParameter("transport") ?: "reality").lowercase(Locale.ROOT)
            if (transport !in listOf("reality", "tcp", "xhttp", "quic")) return false
            val rawFragment = uri.fragment
            val name = if (!rawFragment.isNullOrBlank()) rawFragment else "Atlant ($user)"

            val newProfile = ServerProfile(
                id = "srv-" + System.currentTimeMillis(),
                name = name,
                server = serverAddr,
                sni = sni,
                pbk = pbk,
                user = user,
                serverPubKey = serverPubKey,
                transport = transport.uppercase(Locale.ROOT),
                pingMs = -1,
                isSubscription = false,
                fingerprint = fp,
                wireVersion = wire,
                serverId = uri.getQueryParameter("server_id") ?: "",
                inboundId = uri.getQueryParameter("inbound_id") ?: "",
                xhttpPath = subscriptionXhttpPath(uri.getQueryParameter("path")),
                availableTransports = parseAvailableTransports(uri.getQueryParameter("transports") ?: ""),
            )
            addServer(context, newProfile)
            _selectedServer.value = newProfile
            saveServersToPrefs(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing atlant link: ${e.message}")
            false
        }
    }

    fun generateAtlantLink(s: ServerProfile): String {
        val uri = Uri.Builder()
            .scheme("atlant")
            .encodedAuthority(s.server)
            .appendQueryParameter("sni", s.sni)
            .appendQueryParameter("user", s.user)
            .appendQueryParameter("pbk", s.pbk)
            .appendQueryParameter("transport", s.transport.lowercase(Locale.ROOT))
            .appendQueryParameter("wire_version", s.wireVersion.toString())
            .appendQueryParameter("path", s.xhttpPath)

        if (s.serverId.isNotBlank() && s.inboundId.isNotBlank()) {
            uri.appendQueryParameter("server_id", s.serverId)
            uri.appendQueryParameter("inbound_id", s.inboundId)
        }
        if (s.availableTransports.isNotEmpty()) uri.appendQueryParameter("transports", s.availableTransports.joinToString(","))

        if (s.serverPubKey.isNotBlank()) {
            uri.appendQueryParameter("server_pub_key", s.serverPubKey)
        }
        val effectiveFp = if (s.fingerprint.isNotBlank()) s.fingerprint else _appSettings.value.fingerprint
        if (effectiveFp.isNotBlank()) {
            uri.appendQueryParameter("fp", effectiveFp)
        }
        uri.fragment(s.name)
        return uri.build().toString()
    }

    fun pingServer(server: ServerProfile) {
        if (!pingsInFlight.add(server.id)) return
        scope.launch {
            try { pingSlots.withPermit {
                val start = System.nanoTime()
                val success = runCatching {
                    val (host, port) = parsePingEndpoint(server.server)
                    Socket().use { socket -> socket.connect(InetSocketAddress(host, port), 2500) }
                }.isSuccess
                val latency = if (success) ((System.nanoTime() - start) / 1_000_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else 999
                _servers.update { current -> current.map {
                    if (it.id == server.id && it.server == server.server) it.copy(pingMs = latency) else it
                } }
                _selectedServer.update { current ->
                    if (current?.id == server.id && current.server == server.server) current.copy(pingMs = latency) else current
                }
            } } finally { pingsInFlight.remove(server.id) }
        }
    }

    fun pingAll() {
        for (server in _servers.value) {
            pingServer(server)
        }
    }

    fun updateStatus(newStatus: VpnStatus) {
        _status.value = newStatus
        if (newStatus == VpnStatus.DISCONNECTED) {
            _connectionDuration.value = 0L
        }
    }

    fun updateDuration(duration: Long) {
        _connectionDuration.value = duration
    }

    fun startVpn(context: Context): Boolean {
        val s = _selectedServer.value ?: return false
        val effectiveFp = if (s.fingerprint.isNotBlank()) s.fingerprint else _appSettings.value.fingerprint
        val intent = Intent(context, AtlantVpnService::class.java).apply {
            action = AtlantVpnService.ACTION_CONNECT
            putExtra("server", s.server)
            putExtra("sni", s.sni)
            putExtra("pbk", s.pbk)
            putExtra("user", s.user)
            putExtra("server_pub_key", s.serverPubKey)
            putExtra("wire_version", s.wireVersion)
            putExtra("xhttp_path", s.xhttpPath)
            putExtra("fingerprint", effectiveFp)
            val settings = _appSettings.value
            val (tcpMode, udpMode) = settings.transportModes()
            putExtra("tcp_proto", tcpMode)
            putExtra("udp_mode", udpMode)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service: ${e.message}", e)
            return false
        }
    }

    fun stopVpn(context: Context) {
        updateStatus(VpnStatus.DISCONNECTED)
        try {
            val intent = Intent(context, AtlantVpnService::class.java).apply {
                action = AtlantVpnService.ACTION_DISCONNECT
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startService ACTION_DISCONNECT failed: ${e.message}", e)
            try {
                context.stopService(Intent(context, AtlantVpnService::class.java))
            } catch (e2: Exception) {
                Log.e(TAG, "stopService failed: ${e2.message}", e2)
            }
        }
    }
}

