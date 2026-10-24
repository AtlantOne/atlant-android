package com.example.atlantvpn

import android.util.Base64

internal fun subscriptionHeaderText(value: String?): String {
    if (value == null || value.length > 4096) return ""
    val text = if (value.startsWith("base64:", ignoreCase = true)) {
        try { String(Base64.decode(value.substring(7), Base64.DEFAULT), Charsets.UTF_8) }
        catch (_: IllegalArgumentException) { return "" }
    } else value
    return text.filter { it >= ' ' && it != '\u007f' }.take(2048)
}
