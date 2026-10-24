package com.example.atlantvpn

import org.junit.Assert.assertEquals
import org.junit.Test

class InputSafetyTest {
    @Test fun acceptsExactLimitAndUnicode() {
        val body = "привет"
        val bytes = body.toByteArray(Charsets.UTF_8)
        assertEquals(body, readSubscriptionBody(bytes.inputStream(), bytes.size))
        assertEquals("", readSubscriptionBody(byteArrayOf().inputStream(), 8))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOversizedSubscription() {
        readSubscriptionBody(ByteArray(1025).inputStream(), 1024)
    }
}
