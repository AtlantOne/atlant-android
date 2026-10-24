package com.example.atlantvpn

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AtlantVpnTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var statusJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        VpnManager.init(this)
        updateTileState(VpnManager.status.value)
        statusJob = scope.launch {
            VpnManager.status.collect { status ->
                updateTileState(status)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        statusJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        val currentStatus = VpnManager.status.value
        if (currentStatus == VpnStatus.CONNECTED || currentStatus == VpnStatus.CONNECTING) {
            VpnManager.stopVpn(this)
        } else {
            if (VpnManager.selectedServer.value == null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                openActivity(intent)
                return
            }
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                openActivity(intent)
            } else {
                VpnManager.startVpn(this)
            }
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openActivity(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState(status: VpnStatus) {
        val tile = qsTile ?: return
        when (status) {
            VpnStatus.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Atlant VPN"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Подключено"
                }
            }
            VpnStatus.CONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Atlant VPN"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Подключение..."
                }
            }
            VpnStatus.DISCONNECTED -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Atlant VPN"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Отключено"
                }
            }
        }
        tile.updateTile()
    }
}
