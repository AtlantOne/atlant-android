package com.example.atlantvpn

import java.net.URL
import org.junit.Assert.*
import org.junit.Test

class SubscriptionConnectionTest {
    @Test fun httpsAndExplicitLoopbackAccepted() {
        listOf("https://example.com/sub/token", "https://198.51.100.20:8443/sub/token", "http://127.0.0.1/sub/test").forEach { validateSubscriptionUrl(URL(it)) }
        listOf("http://198.51.100.20/sub/token", "https://user:password@example.com/sub/token", "ftp://example.com/a").forEach {
            try { validateSubscriptionUrl(URL(it)); fail("Unsafe URL accepted") } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun profileIdStableButSeparatesPortsTransportsAndFormats() {
        val base = stableProfileId("198.51.100.20:443", "example.com", "alice", "quic", 3)
        assertEquals(base, stableProfileId("198.51.100.20:443", "example.com", "alice", "QUIC", 3))
        assertNotEquals(base, stableProfileId("198.51.100.20:8443", "example.com", "alice", "quic", 3))
        assertNotEquals(base, stableProfileId("198.51.100.20:443", "example.com", "alice", "reality", 3))
        assertNotEquals(base, stableProfileId("198.51.100.20:443", "example.com", "alice", "quic", 2))
    }
}
