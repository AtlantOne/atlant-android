package com.example.atlantvpn

/** Only receives the UID's default physical network, never standby availability. */
internal class DefaultNetworkState {
    private var current: Long? = null
    private var seen = false

    @Synchronized fun available(id: Long): Boolean {
        if (current == id) return false
        val reconnect = seen
        current = id
        seen = true
        return reconnect
    }

    @Synchronized fun lost(id: Long): Boolean {
        if (current != id) return false
        current = null
        return true
    }
}
