package com.example.atlantvpn

import org.junit.Assert.assertEquals
import org.junit.Test

class TlsProfilesTest {
    @Test fun newSettingsUseFirefoxByDefault() {
        assertEquals("firefox", AppSettings().fingerprint)
    }
    @Test fun retiredChromeAliasesUseSupportedProfile() {
        for (value in listOf("chrome", "chrome_auto", "chrome_144", "chrome_133", " CHROME_133 ")) {
            assertEquals("chrome", normalizeTlsProfile(value))
        }
    }

    @Test fun unrelatedAndInheritedProfilesStayUnchanged() {
        for (value in listOf("", "firefox", "safari", "unknown", "chrome_120")) {
            assertEquals(value, normalizeTlsProfile(value))
        }
    }
}
