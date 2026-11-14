package com.example.atlantvpn

fun normalizeTlsProfile(profile: String): String = when (val p = profile.trim().lowercase(java.util.Locale.ROOT)) {
 "chrome", "chrome_auto" -> "chrome_144"
 "firefox", "firefox_auto" -> "firefox_120"
 "safari", "safari_auto" -> "safari_16"
 "ios", "iphone" -> "ios_14"
 "edge" -> "edge_106"
 "random", "randomized" -> "random_browser"
 else -> p
}

internal fun tlsProfileChoices(): List<Pair<String,String>> = listOf(
 "chrome_144" to "Chrome 144",
 "chrome_133" to "Chrome 133",
 "chrome_131" to "Chrome 131",
 "chrome_120" to "Chrome 120",
 "chrome_106" to "Chrome 106",
 "random_chrome" to tr("Случайный Chrome"),
 "firefox_120" to "Firefox 120",
 "firefox_105" to "Firefox 105",
 "firefox_102" to "Firefox 102",
 "firefox_99" to "Firefox 99",
 "random_firefox" to tr("Случайный Firefox"),
 "safari_16" to "Safari 16",
 "ios_14" to "iOS 14",
 "ios_13" to "iOS 13",
 "edge_106" to "Edge 106",
 "edge_85" to "Edge 85",
 "random_browser" to tr("Случайный браузер")
)
internal fun quicProfileChoices() = listOf("chrome_156" to "Chrome 156", "chrome_146" to "Chrome 146", "random_chrome" to tr("Случайный Chrome"))
