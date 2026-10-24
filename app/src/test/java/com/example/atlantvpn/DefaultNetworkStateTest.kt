package com.example.atlantvpn

import org.junit.Assert.*
import org.junit.Test

class DefaultNetworkStateTest {
    @Test fun firstNetworkAndRepeatedCapabilitiesDoNotResetTransport() {
        val state = DefaultNetworkState()
        assertFalse(state.available(10))
        repeat(10) { assertFalse(state.available(10)) }
    }
    @Test fun obsoleteLossDoesNotDisconnectNewDefault() {
        val state = DefaultNetworkState()
        state.available(10)
        assertTrue(state.available(20))
        assertFalse(state.lost(10))
        assertFalse(state.available(20))
        assertTrue(state.lost(20))
    }
    @Test fun recoveryAfterOutageRefreshesPoolsOnce() {
        val state = DefaultNetworkState()
        state.available(10)
        assertTrue(state.lost(10))
        assertFalse(state.lost(10))
        assertTrue(state.available(20))
        assertFalse(state.available(20))
    }
}
