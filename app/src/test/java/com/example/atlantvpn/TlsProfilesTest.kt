package com.example.atlantvpn

import org.junit.Assert.assertEquals
import org.junit.Test

class TlsProfilesTest {
 @Test fun defaultsStayOnFirefox() { assertEquals("firefox", AppSettings().fingerprint) }
 @Test fun aliasesSelectDocumentedVersions() {
  assertEquals("chrome_144", normalizeTlsProfile("chrome"))
  assertEquals("chrome_144", normalizeTlsProfile(" CHROME_AUTO "))
  assertEquals("firefox_120", normalizeTlsProfile("firefox"))
  assertEquals("safari_16", normalizeTlsProfile("safari"))
 }
 @Test fun explicitVersionsAndRandomModesArePreserved() {
  for (value in listOf("chrome_133","chrome_144","chrome_120","firefox_105","random_chrome","random_firefox","random_browser","unknown","")) {
   assertEquals(value, normalizeTlsProfile(value))
  }
 }
 @Test fun legacyRandomAliasUsesBrowserPool() {
  assertEquals("random_browser", normalizeTlsProfile("randomized"))
 }
}
