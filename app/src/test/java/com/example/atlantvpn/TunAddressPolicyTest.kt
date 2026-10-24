package com.example.atlantvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunAddressPolicyTest {
    @Test fun blockingMustNotAdvertiseIPv6Route() {
        val policy = tunAddressPolicy(true, "1.1.1.1")
        assertFalse(policy.captureIPv6)
        assertEquals(1200, policy.minimumMtu)
        assertEquals("1.1.1.1", policy.dns)
    }

    @Test fun enabledIPv6IsCapturedNotBypassed() {
        val policy = tunAddressPolicy(false, "2606:4700:4700::1111")
        assertTrue(policy.captureIPv6)
        assertEquals(1280, policy.minimumMtu)
    }

    @Test(expected = IllegalArgumentException::class)
    fun ipv6DnsMustNotImplicitlyReenableBlockedFamily() {
        tunAddressPolicy(true, "2606:4700:4700::1111")
    }

    @Test fun emptyDnsUsesIPv4TunnelResolver() {
        assertEquals("172.19.0.2", tunAddressPolicy(true, " ").dns)
    }
}
