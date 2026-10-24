package com.example.atlantvpn

/** Keep stored Chrome aliases consistent with the preset used by the core. */
fun normalizeTlsProfile(profile: String): String = when (profile.trim().lowercase(java.util.Locale.ROOT)) {
    "chrome", "chrome_auto", "chrome_144", "chrome_133" -> "chrome"
    else -> profile
}
