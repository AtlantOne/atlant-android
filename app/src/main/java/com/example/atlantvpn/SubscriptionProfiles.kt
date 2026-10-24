package com.example.atlantvpn

import java.security.MessageDigest
import java.util.Locale

// Older subscriptions repeated the same endpoint for each transport. Keep
// distinct endpoints/credentials, not redundant transport-specific copies.
internal fun mergeSubscriptionEndpoints(profiles: List<ServerProfile>): List<ServerProfile> {
    return profiles.distinctBy { profile ->
        subscriptionEndpointIdentity(profile)
    }.map { profile ->
        val identity = subscriptionEndpointIdentity(profile).joinToString("\u0000")
        val id = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        profile.copy(id = "sub-$id", transport = "REALITY")
    }
}

private fun subscriptionEndpointIdentity(p: ServerProfile): List<String> =
    if (p.serverId.isNotBlank() && p.inboundId.isNotBlank()) listOf(p.subscriptionId, "native", p.serverId, p.inboundId)
    else listOf(p.subscriptionId, p.server.lowercase(Locale.ROOT), p.sni.lowercase(Locale.ROOT),
        p.user, p.pbk, p.serverPubKey, p.wireVersion.toString(), p.fingerprint, p.xhttpPath)

internal fun migratedSubscriptionProfile(previous: ServerProfile, next: ServerProfile): Boolean =
    (previous.serverId.isBlank() || previous.inboundId.isBlank()) && next.serverId.isNotBlank() && next.inboundId.isNotBlank() &&
        previous.server.equals(next.server, ignoreCase = true) && previous.sni.equals(next.sni, ignoreCase = true) &&
        previous.user == next.user && previous.wireVersion == next.wireVersion &&
        (previous.serverPubKey.isBlank() || previous.serverPubKey.equals(next.serverPubKey, ignoreCase = true))

internal fun parseAvailableTransports(value: String): List<String> {
    if (value.isBlank()) return emptyList()
    val items = value.split(',').map { it.trim().lowercase(Locale.ROOT) }.distinct()
    require(items.all { it in listOf("tcp", "xhttp", "quic") }) { "Invalid subscription transports" }
    return items
}

internal fun subscriptionXhttpPath(value: String?): String {
    val path = value?.takeIf { it.isNotEmpty() } ?: "/api/v1/sync"
    require(path.startsWith('/') && !path.startsWith("//") && path.length <= 2048 &&
        path.none { it.isISOControl() || it == '?' || it == '#' || it == '\\' }) { "Invalid XHTTP path" }
    return path
}

internal fun subscriptionId(url: String): String = "subscription-" + MessageDigest.getInstance("SHA-256")
    .digest(url.trim().toByteArray(Charsets.UTF_8)).take(12).joinToString("") { "%02x".format(it.toInt() and 0xff) }

internal fun replaceSubscriptionProfiles(existing: List<ServerProfile>, groupId: String, incoming: List<ServerProfile>): List<ServerProfile> {
    val first = existing.indexOfFirst { it.subscriptionId == groupId }
    if (first < 0) return existing + incoming
    return existing.take(first) + incoming + existing.drop(first).filter { it.subscriptionId != groupId }
}

internal fun refreshSubscriptionInfo(existing: List<SubscriptionInfo>, incoming: SubscriptionInfo): List<SubscriptionInfo> {
    val current = existing.firstOrNull { it.id == incoming.id } ?: return existing + incoming
    val updated = incoming.copy(name = current.name.ifBlank { incoming.name })
    return existing.map { if (it.id == incoming.id) updated else it }
}
