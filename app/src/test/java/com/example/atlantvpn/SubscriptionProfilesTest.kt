package com.example.atlantvpn

import org.junit.Assert.*
import org.junit.Test

class SubscriptionProfilesTest {
    private val server = ServerProfile("old", "Atlant", "192.0.2.1:1026", "example.com", "key", "alice", "public", isSubscription = true)

    @Test fun transportCopiesBecomeOneEndpoint() {
        val result = mergeSubscriptionEndpoints(listOf(server, server.copy(id = "two", transport = "XHTTP"), server.copy(id = "three", transport = "QUIC")))
        assertEquals(1, result.size)
        assertEquals("REALITY", result.single().transport)
        assertEquals(result, mergeSubscriptionEndpoints(result))
    }

    @Test fun distinctServersKeysAndWireVersionsArePreserved() {
        val result = mergeSubscriptionEndpoints(listOf(server, server.copy(server = "192.0.2.2:1026"), server.copy(pbk = "another"), server.copy(wireVersion = 3)))
        assertEquals(4, result.size)
        assertEquals(4, result.map { it.id }.toSet().size)
    }

    @Test fun transportChoiceComesOnlyFromClientSettings() {
        val settings = AppSettings(trafficMode = TrafficMode.SPLIT, tcpProto = "xhttp", udpProto = "quic")
        for (legacy in listOf("REALITY", "XHTTP", "QUIC")) {
            val endpoint = mergeSubscriptionEndpoints(listOf(server.copy(transport = legacy))).single()
            assertEquals("REALITY", endpoint.transport)
            assertEquals("xhttp" to "quic", settings.transportModes())
        }
    }

    @Test fun legacySelectionStillMatchesTheMergedEndpoint() {
        val previous = server.copy(transport = "QUIC")
        val merged = mergeSubscriptionEndpoints(listOf(server, previous)).single()
        assertEquals(profileIdentity(previous), profileIdentity(merged))
    }

    @Test fun nativeIdentitySurvivesAddressAndCredentialChanges() {
        val old = server.copy(subscriptionId = "group", serverId = "vds", inboundId = "inbound", xhttpPath = "/old")
        val updated = old.copy(server = "192.0.2.2:8443", sni = "new.example", pbk = "rotated", xhttpPath = "/new")
        assertEquals(profileIdentity(old), profileIdentity(updated))
        assertEquals(mergeSubscriptionEndpoints(listOf(old)).single().id, mergeSubscriptionEndpoints(listOf(updated)).single().id)
        assertNotEquals(profileIdentity(old), profileIdentity(updated.copy(inboundId = "another")))
        assertNotEquals(mergeSubscriptionEndpoints(listOf(old)).single().id,
            mergeSubscriptionEndpoints(listOf(updated.copy(subscriptionId = "other-group"))).single().id)
    }

    @Test fun endpointPathsAndCapabilitiesArePreserved() {
        val profile = server.copy(xhttpPath = "/custom/upload", availableTransports = listOf("tcp", "xhttp", "quic"))
        assertEquals(profile.xhttpPath, mergeSubscriptionEndpoints(listOf(profile)).single().xhttpPath)
        assertEquals(profile.availableTransports, mergeSubscriptionEndpoints(listOf(profile)).single().availableTransports)
        assertEquals(2, mergeSubscriptionEndpoints(listOf(profile, profile.copy(xhttpPath = "/other"))).size)
        assertEquals(listOf("tcp", "xhttp", "quic"), parseAvailableTransports("tcp,xhttp,quic,tcp"))
        assertEquals("/api/v1/sync", subscriptionXhttpPath(null))
        for (bad in listOf("//external.example", "/bad?query", "/bad#fragment", "/bad\n", "relative")) {
            try { subscriptionXhttpPath(bad); fail("Invalid path accepted") } catch (_: IllegalArgumentException) { }
        }
        try { parseAvailableTransports("tcp,unknown"); fail("Invalid transport accepted") } catch (_: IllegalArgumentException) { }
    }

    @Test fun migrationMatchesTheExistingEndpointRatherThanTheFirstServer() {
        val previous = server.copy(wireVersion = 7, transport = "QUIC")
        val upgraded = previous.copy(serverId = "vds", inboundId = "local", pbk = "rotated", transport = "REALITY")
        val remote = upgraded.copy(server = "192.0.2.2:443", inboundId = "remote")
        assertFalse(migratedSubscriptionProfile(previous, remote))
        assertTrue(migratedSubscriptionProfile(previous, upgraded))
        assertFalse(migratedSubscriptionProfile(previous, upgraded.copy(serverPubKey = "another pin")))
        assertFalse(migratedSubscriptionProfile(upgraded, upgraded.copy(inboundId = "other")))
    }
}
