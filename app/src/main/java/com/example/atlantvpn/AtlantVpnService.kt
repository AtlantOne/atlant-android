package com.example.atlantvpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import atlant.Atlant
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class AtlantVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.atlantvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.atlantvpn.DISCONNECT"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "atlant_vpn_channel"
        private const val TAG = "AtlantVpnService"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val tunnelExecutor = Executors.newSingleThreadExecutor()
    private val tunnelGeneration = AtomicLong(0)
    private val durationHandler = Handler(Looper.getMainLooper())
    private val durationRunnable = object : Runnable {
        override fun run() {
            if (connectedSince == 0L) return
            VpnManager.updateDuration((System.currentTimeMillis() - connectedSince) / 1000)
            durationHandler.postDelayed(this, 1000L)
        }
    }
    private var connectedSince = 0L
    private lateinit var connectivityManager: ConnectivityManager
    private val defaultNetworkState = DefaultNetworkState()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) ||
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return
            try {
                if (defaultNetworkState.available(network.networkHandle) && VpnManager.status.value == VpnStatus.CONNECTED) {
                    VpnManager.log("Сеть изменилась, транспорт переподключается", LogLevel.INFO)
                    Atlant.notifyNetworkChanged()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Cannot update underlying network: ${e.message}")
            }
        }

        override fun onLost(network: Network) {
            if (!defaultNetworkState.lost(network.networkHandle)) return
            try {
                setUnderlyingNetworks(null)
                if (VpnManager.status.value == VpnStatus.CONNECTED) {
                    Atlant.notifyNetworkChanged()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Cannot handle lost network: ${e.message}")
            }
        }
    }
    @Volatile
    private var isStopping = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        // Our UID is excluded from the VPN in every supported routing mode, so
        // its default is the physical network. A broad NOT_VPN request also
        // reports standby cellular and caused unnecessary live tunnel resets.
        // Capabilities callbacks avoid querying capabilities during onAvailable.
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onDestroy() {
        durationHandler.removeCallbacks(durationRunnable)
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot unregister network callback: ${e.message}")
        }
        stopTunnel()
        tunnelExecutor.shutdown()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            stopTunnel()
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Throwable) {
                Log.w(TAG, "stopForeground error: ${e.message}")
            }
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_CONNECT) {
            if (VpnManager.status.value != VpnStatus.DISCONNECTED) return START_STICKY
            val server = intent.getStringExtra("server")?.trim().orEmpty()
            if (server.isEmpty()) {
                VpnManager.log("Выберите сервер перед подключением", LogLevel.ERROR)
                stopSelf()
                return START_NOT_STICKY
            }
            val sni = intent.getStringExtra("sni") ?: "www.microsoft.com"
            val pbk = intent.getStringExtra("pbk") ?: ""
            val user = intent.getStringExtra("user") ?: "test"
            val serverPubKey = intent.getStringExtra("server_pub_key") ?: ""
            val wireVersion = intent.getIntExtra("wire_version", 4)
            val tcpProto = intent.getStringExtra("tcp_proto") ?: "tcp"
            val udpMode = intent.getStringExtra("udp_mode") ?: "quic"
            val fingerprint = intent.getStringExtra("fingerprint") ?: "firefox"
            val xhttpPath = intent.getStringExtra("xhttp_path") ?: "/api/v1/sync"

            // Immediately satisfy the foreground-service deadline. A VPN is a
            // documented special-use service; systemExempted is not applicable.
            val initialNotif = buildNotification("Подключение к $server...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        initialNotif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Typed startForeground failed, using compatible call: ${e.message}")
                    startForeground(NOTIFICATION_ID, initialNotif)
                }
            } else {
                startForeground(NOTIFICATION_ID, initialNotif)
            }

            startTunnel(server, sni, pbk, user, serverPubKey, udpMode, tcpProto, fingerprint, wireVersion, xhttpPath)
            return START_STICKY
        }

        return START_NOT_STICKY
    }

    private fun startTunnel(
        server: String,
        sni: String,
        pbk: String,
        user: String,
        serverPubKey: String,
        udpMode: String,
        tcpProto: String,
        fingerprint: String,
        wireVersion: Int,
        xhttpPath: String
    ) {
        val effectiveFp = if (fingerprint.isNotBlank()) fingerprint else VpnManager.appSettings.value.fingerprint
        val generation = tunnelGeneration.incrementAndGet()
        VpnManager.updateStatus(VpnStatus.CONNECTING)
        VpnManager.log("Инициализация подключения к $server...", LogLevel.INFO)
        VpnManager.log("Параметры: SNI=$sni, FP=$effectiveFp, TCP=$tcpProto (REALITY), UDP=$udpMode", LogLevel.INFO)

        tunnelExecutor.execute {
            if (generation != tunnelGeneration.get()) return@execute
            try {
                val settings = VpnManager.appSettings.value
                val routing = VpnManager.routingSettings.value
                require(pbk.isNotBlank()) { "В профиле отсутствует ключ пользователя" }
                require(user.matches(Regex("[A-Za-z0-9_-]{1,64}"))) { "Некорректное имя пользователя в профиле" }
                val addressPolicy = tunAddressPolicy(settings.blockIPv6, settings.dns)
                val effectiveMtu = settings.mtu.coerceIn(addressPolicy.minimumMtu, 1500)

                val builder = Builder()
                    .setSession("AtlantVPN")
                    .setMtu(effectiveMtu)
                    .addAddress("172.19.0.2", 24)
                    .addDnsServer(addressPolicy.dns)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("198.18.0.0", 15) // FakeDNS synthetic pool

                if (addressPolicy.captureIPv6) {
                    builder.addAddress("fd00::2", 120)
                    builder.addRoute("::", 0)
                    VpnManager.log("IPv6 направлен через VPN; серверу требуется IPv6-доступ в интернет", LogLevel.INFO)
                } else {
                    // Do not add an IPv6 address/route/DNS or call allowFamily:
                    // Android then blocks IPv6 instead of advertising a dead path.
                    VpnManager.log("IPv6 заблокирован средствами Android; VPN использует IPv4", LogLevel.INFO)
                }

                // Android's per-app VPN rules are applied before establishing the TUN.
                if (!routing.enabled || routing.mode == RouteMode.ALL) {
                    VpnManager.log("Маршрутизация: весь трафик устройства через VPN (ALL)", LogLevel.INFO)
                    try {
                        builder.addDisallowedApplication(packageName)
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot exclude own package: ${e.message}")
                    }
                } else {
                    when (routing.mode) {
                        RouteMode.RULES -> {
                            if (routing.proxyApps.isNotEmpty() && routing.proxyDomains.isBlank()) {
                                VpnManager.log("Маршрутизация: только выбранные приложения (${routing.proxyApps.size} шт.)", LogLevel.INFO)
                                for (pkg in routing.proxyApps) {
                                    if (pkg != packageName) {
                                        try {
                                            builder.addAllowedApplication(pkg)
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Cannot add allowed app $pkg: ${e.message}")
                                        }
                                    }
                                }
                            } else {
                                VpnManager.log("Маршрутизация: правила по доменам и приложениям через ядро Atlant (FakeDNS)", LogLevel.INFO)
                                try {
                                    builder.addDisallowedApplication(packageName)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Cannot exclude own package: ${e.message}")
                                }
                            }
                        }
                        RouteMode.BYPASS -> {
                            VpnManager.log("Маршрутизация: в обход VPN (${routing.bypassApps.size} шт.)", LogLevel.INFO)
                            try {
                                builder.addDisallowedApplication(packageName)
                            } catch (e: Exception) {
                                Log.w(TAG, "Cannot exclude own package: ${e.message}")
                            }
                            for (pkg in routing.bypassApps) {
                                if (pkg != packageName) {
                                    try {
                                        builder.addDisallowedApplication(pkg)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Cannot add disallowed app $pkg: ${e.message}")
                                    }
                                }
                            }
                        }
                        RouteMode.ALL -> { /* already handled above */ }
                    }
                }

                VpnManager.log("Создание виртуального TUN адаптера (MTU $effectiveMtu)...", LogLevel.INFO)
                val pfd = builder.establish()
                if (pfd == null) {
                    val err = "Не удалось создать виртуальный TUN интерфейс"
                    Log.e(TAG, err)
                    VpnManager.log(err, LogLevel.ERROR)
                    VpnManager.updateStatus(VpnStatus.DISCONNECTED)
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } catch (e: Throwable) {
                        Log.w(TAG, "stopForeground error: ${e.message}")
                    }
                    stopSelf()
                    return@execute
                }
                // The native TUN layer owns the descriptor after detachFd().
                val fd = pfd.detachFd()
                vpnInterface = null

                VpnManager.log("TUN интерфейс поднят (FD: $fd, IP: 172.19.0.2)", LogLevel.SUCCESS)
                VpnManager.log("Запуск сетевого ядра Atlant Go (Reality / QUIC)...", LogLevel.INFO)
                // Hook Go engine log output directly to the Android app UI
                try {
                    Atlant.setLogListener { msg, level ->
                        val lvl = when (level) {
                            1L -> LogLevel.SUCCESS
                            2L -> LogLevel.WARN
                            3L -> LogLevel.ERROR
                            else -> LogLevel.INFO
                        }
                        VpnManager.log(msg, lvl)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot register Go log listener: ${e.message}")
                }

                try {
                    Atlant.setTunnelStateListener { state, message ->
                        if (state == "failed") {
                            durationHandler.post {
                                Log.e(TAG, "Native tunnel failed: $message")
                                VpnManager.log("Критическая ошибка TUN: $message", LogLevel.ERROR)
                                stopTunnel()
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Cannot register tunnel state listener: ${e.message}")
                }

                // Register socket protector so direct bypass sockets route through physical Wi-Fi/LTE
                try {
                    Atlant.setSocketProtector { sFd ->
                        this@AtlantVpnService.protect(sFd.toInt())
                    }
                    val mixedAppRules = routing.enabled && routing.mode == RouteMode.RULES &&
                        routing.proxyApps.isNotEmpty() && routing.proxyDomains.isNotBlank()
                    if (mixedAppRules) {
                        val proxyUids = routing.proxyApps.filter { it != packageName }.map {
                            packageManager.getApplicationInfo(it, 0).uid
                        }.toSet()
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            VpnManager.log("Смешанные правила: на Android ниже 10 неизвестные приложения защищаются VPN", LogLevel.INFO)
                        }
                        Atlant.setAppRouter { protocol, sourceIp, sourcePort, destinationIp, destinationPort ->
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) -1L else try {
                                val uid = connectivityManager.getConnectionOwnerUid(protocol.toInt(),
                                    InetSocketAddress(sourceIp, sourcePort.toInt()),
                                    InetSocketAddress(destinationIp, destinationPort.toInt()))
                                if (uid < 0) -1L else if (uid in proxyUids) 1L else 0L
                            } catch (_: Exception) { -1L }
                        }
                    } else {
                        Atlant.setAppRouter(null)
                    }
                } catch (e: Exception) {
                    throw IllegalStateException("Не удалось настроить защиту сокетов и правила приложений", e)
                }

                // Configure multiplexing mode (default: false = direct 1-to-1)
                Atlant.setTunMTU(effectiveMtu.toLong())
                Atlant.setWireVersion(wireVersion.toLong())
                Atlant.setMuxEnabled(settings.mux)

                // Select direct REALITY or HTTP/2 over REALITY for TCP traffic.
                try {
                    Atlant.setTCPProto(tcpProto)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot set TCP proto: ${e.message}")
                }
                Atlant.setXHTTPPath(subscriptionXhttpPath(xhttpPath))

                // Configure core logging level (debug, info, warn, error, none)
                try {
                    Atlant.setLogLevel(settings.logLevel)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot set Go log level: ${e.message}")
                }

                // Configure domain and mode routing
                try {
                    val appOnlyRules = routing.mode == RouteMode.RULES && routing.proxyApps.isNotEmpty() && routing.proxyDomains.isBlank()
                    val modeStr = if (!routing.enabled || appOnlyRules) "all" else when (routing.mode) {
                        RouteMode.RULES -> "rules"
                        RouteMode.BYPASS -> "bypass"
                        RouteMode.ALL -> "all"
                    }
                    Atlant.setRouteMode(modeStr)
                    Atlant.setProxyDomains(routing.proxyDomains)
                    Atlant.setDirectDomains(routing.directDomains)
                    VpnManager.log("Маршрутизация (режим: $modeStr): домены применены", LogLevel.INFO)
                } catch (e: Exception) {
                    throw IllegalStateException("Не удалось применить правила маршрутизации; подключение отменено", e)
                }

                // Configure Obfuscation & Protocol Parameters
                try {
                    Atlant.setFakeDnsEnabled(settings.fakeDns)
                    Atlant.setTrafficPadding(settings.trafficPadding)
                    Atlant.setPaddingRange(settings.minPadding.toLong(), settings.maxPadding.toLong())
                    Atlant.setFingerprint(effectiveFp)
                    Atlant.setChaffing(settings.chaffingEnabled, settings.chaffingInterval.toLong())
                    Atlant.setAutoFallback(settings.autoFallback)
                    Atlant.setPortHopping(settings.portHopping)
                    Atlant.setRemoteDns(settings.remoteDns)
                    Atlant.setDecoyTraffic(settings.decoyTraffic)
                    Atlant.setZeroRtt(settings.zeroRtt)
                    VpnManager.log("Параметры обфускации применены (FakeDNS=${settings.fakeDns}, FP=$effectiveFp, Padding=${settings.trafficPadding}[${settings.minPadding}..${settings.maxPadding}], Chaffing=${settings.chaffingEnabled})", LogLevel.INFO)
                } catch (e: Throwable) {
                    Log.w(TAG, "Cannot configure protocol options: ${e.message}")
                }

                // Start native Go VPN tunnel via Atlant gomobile binding
                Atlant.startVPNWithUser(
                    server,
                    sni,
                    pbk,
                    user,
                    serverPubKey,
                    udpMode,
                    "172.19.0.2",
                    fd.toLong(),
                    24L
                )

                if (generation != tunnelGeneration.get()) {
                    Atlant.stopVPN()
                    Atlant.setAppRouter(null)
                    return@execute
                }

                // The Go binding now returns only after the TUN device, gVisor
                // stack, NIC and addresses have all initialized successfully.
                startDurationTimer()
                VpnManager.updateStatus(VpnStatus.CONNECTED)
                updateNotification("Подключено к $server")
                VpnManager.log("Туннель активен. Трафик защищён.", LogLevel.SUCCESS)
            } catch (e: Throwable) {
                val err = "Ошибка туннеля Atlant: ${e.message}"
                Log.e(TAG, err, e)
                VpnManager.log(err, LogLevel.ERROR)
                stopTunnel()
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (t: Throwable) {
                    Log.w(TAG, "stopForeground error: ${t.message}")
                }
                stopSelf()
            }
        }
    }

    private fun startDurationTimer() {
        durationHandler.removeCallbacks(durationRunnable)
        connectedSince = System.currentTimeMillis()
        durationHandler.postDelayed(durationRunnable, 1000L)
    }

    private fun stopTunnel() {
        tunnelGeneration.incrementAndGet()
        if (tunnelExecutor.isShutdown) return
        if (isStopping) return
        isStopping = true
        tunnelExecutor.execute {
            try {
                VpnManager.log("Остановка VPN туннеля...", LogLevel.WARN)
                durationHandler.removeCallbacks(durationRunnable)
                connectedSince = 0L
                try {
                    Atlant.stopVPN()
                } catch (e: Throwable) {
                    Log.w(TAG, "Error in Atlant.stopVPN: ${e.message}")
                }
                vpnInterface = null
                VpnManager.log("Туннель отключен.", LogLevel.INFO)
            } catch (e: Throwable) {
                Log.e(TAG, "Error stopping tunnel: ${e.message}", e)
                VpnManager.log("Ошибка при отключении: ${e.message}", LogLevel.ERROR)
            } finally {
                VpnManager.updateStatus(VpnStatus.DISCONNECTED)
                isStopping = false
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Atlant VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Статус подключения Atlant VPN"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, AtlantVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Atlant VPN")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", disconnectPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }
}
