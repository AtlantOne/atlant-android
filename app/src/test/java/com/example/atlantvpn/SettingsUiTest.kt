package com.example.atlantvpn

import org.junit.Assert.*
import org.junit.Test

class SettingsUiTest {
    @Test fun mtuDoesNotSavePartialOrInvalidInput() {
        listOf("", "1", "136", "oops", "1501", "-1").forEach { assertNull(validMtu(it, false)) }
        assertEquals(1360, validMtu("1360", true))
        assertEquals(1200, validMtu("1200", false))
        assertNull(validMtu("1279", true))
        assertEquals(1280, validMtu("1280", true))
        assertEquals(1500, validMtu("1500", true))
    }
    @Test fun allAndDisabledRoutingAreNotShownAsSelectedApps() {
        assertEquals("Всё через VPN", routingSummary(RoutingSettings()))
        assertEquals("Правила выключены · всё через VPN", routingSummary(RoutingSettings(enabled = false, mode = RouteMode.RULES)))
    }
    @Test fun routingSummaryMatchesStoredMode() {
        assertEquals("Только по правилам", routingSummary(RoutingSettings(mode = RouteMode.RULES)))
        assertEquals("Через VPN, кроме исключений", routingSummary(RoutingSettings(mode = RouteMode.BYPASS)))
    }
}
