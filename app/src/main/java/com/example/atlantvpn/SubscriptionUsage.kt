package com.example.atlantvpn

internal data class SubscriptionUsage(val used: Long, val remaining: Long, val fraction: Float)

internal fun subscriptionUsage(upload: Long, download: Long, total: Long): SubscriptionUsage {
    val up = upload.coerceAtLeast(0)
    val down = download.coerceAtLeast(0)
    val used = if (up > Long.MAX_VALUE - down) Long.MAX_VALUE else up + down
    val limit = total.coerceAtLeast(0)
    return SubscriptionUsage(used, (limit - used).coerceAtLeast(0),
        if (limit == 0L) 0f else (used.toDouble() / limit).coerceIn(0.0, 1.0).toFloat())
}
