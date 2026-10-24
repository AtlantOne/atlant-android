package com.example.atlantvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PingEndpointTest {
    @Test fun parsesIpv4DomainsAndBracketedIpv6() {
        assertEquals("vpn.example.test" to 443, parsePingEndpoint("vpn.example.test"))
        assertEquals("192.0.2.1" to 1026, parsePingEndpoint("192.0.2.1:1026"))
        assertEquals("2001:db8::1" to 1026, parsePingEndpoint("[2001:db8::1]:1026"))
        assertEquals("::1" to 443, parsePingEndpoint("[::1]"))
    }
    @Test fun rejectsMalformedEndpointsInsteadOfPingingDifferentHost() {
        for (endpoint in listOf("", "host:0", "host:65536", "host:abc", "user@host:443", "host/path", "host?x=1", "2001:db8::1:443")) {
            assertTrue(endpoint, runCatching { parsePingEndpoint(endpoint) }.isFailure)
        }
    }
}
