package com.example.atlantvpn

import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {
    @Test fun storedPreferenceFallsBackSafely() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("unknown"))
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStored(it.name)) }
    }
    @Test fun explicitThemeOverridesSystem() {
        assertFalse(ThemeMode.LIGHT.isDark(true))
        assertTrue(ThemeMode.DARK.isDark(false))
        assertTrue(ThemeMode.SYSTEM.isDark(true))
        assertFalse(ThemeMode.SYSTEM.isDark(false))
    }
}
