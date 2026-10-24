package com.example.atlantvpn

import java.net.URI

internal fun parsePingEndpoint(endpoint: String): Pair<String, Int> {
    val uri = URI("tcp://$endpoint")
    val host = requireNotNull(uri.host).removeSurrounding("[", "]")
    require(host.isNotBlank() && uri.rawUserInfo == null && uri.rawPath.isNullOrEmpty() && uri.rawQuery == null && uri.rawFragment == null)
    val port = if (uri.port == -1) 443 else uri.port
    require(port in 1..65535)
    return host to port
}
