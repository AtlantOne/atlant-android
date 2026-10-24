package com.example.atlantvpn

// Android blocks an address family when the VPN has no address, route or DNS
// server in that family. A ::/0 route captures IPv6; it does not block it.
internal data class TunAddressPolicy(val captureIPv6: Boolean, val dns: String) {
    val minimumMtu: Int get() = if (captureIPv6) 1280 else 1200
}

internal fun tunAddressPolicy(blockIPv6: Boolean, configuredDns: String): TunAddressPolicy {
    val dns = configuredDns.trim().ifBlank { "172.19.0.2" }
    require(!blockIPv6 || ':' !in dns) {
        "IPv6 заблокирован: укажите IPv4 DNS-сервер или разрешите IPv6 через VPN"
    }
    return TunAddressPolicy(captureIPv6 = !blockIPv6, dns = dns)
}
