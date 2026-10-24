package com.example.atlantvpn

import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal fun validateSubscriptionUrl(url: URL) {
    val loopback = url.host.lowercase() in setOf("localhost", "127.0.0.1", "::1", "[::1]")
    require(url.userInfo == null && url.host.isNotBlank() &&
        (url.protocol == "https" || (url.protocol == "http" && loopback))) {
        tr("Для подписки требуется HTTPS с проверяемым сертификатом")
    }
}

internal fun openSubscriptionConnection(initial: URL): HttpURLConnection {
    var current = initial
    repeat(6) { attempt ->
        validateSubscriptionUrl(current)
        val conn = current.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "AtlantVPN/3 (Android)")
        val code = try { conn.responseCode } catch (e: Exception) { conn.disconnect(); throw e }
        if (code !in listOf(301, 302, 303, 307, 308)) return conn
        val location = conn.getHeaderField("Location")
        conn.disconnect()
        require(attempt < 5 && !location.isNullOrBlank()) { tr("Недопустимое перенаправление подписки") }
        val next = URL(current, location)
        require(current.protocol != "https" || next.protocol == "https") { tr("HTTPS downgrade запрещён") }
        current = next
    }
    error(tr("Слишком много перенаправлений"))
}

internal fun stableProfileId(server: String, sni: String, user: String, transport: String, wire: Int): String {
    val identity = listOf(server, sni, user, transport.lowercase(), wire.toString()).joinToString("\u0000")
    return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        .take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal fun profileIdentity(p: ServerProfile): String =
    if (p.serverId.isNotBlank() && p.inboundId.isNotBlank()) "native:${p.serverId}:${p.inboundId}"
    else stableProfileId(p.server, p.sni, p.user,
        if (p.isSubscription) "reality" else p.transport, p.wireVersion) + ":" + p.xhttpPath
