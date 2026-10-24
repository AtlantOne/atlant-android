package com.example.atlantvpn

import java.io.ByteArrayOutputStream
import java.io.InputStream

// Limit decoded HTTP bytes, not Content-Length (chunked/gzip may omit it).
internal fun readSubscriptionBody(input: InputStream, limit: Int = 4 * 1024 * 1024): String {
    require(limit > 0)
    val output = ByteArrayOutputStream(minOf(limit, 8192))
    val chunk = ByteArray(8192)
    var total = 0
    while (true) {
        val count = input.read(chunk, 0, minOf(chunk.size, limit - total + 1))
        if (count < 0) break
        if (count == 0) throw java.io.IOException("Subscription reader made no progress")
        total += count
        require(total <= limit) { "Подписка превышает лимит $limit байт" }
        output.write(chunk, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}
