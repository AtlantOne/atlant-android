package com.example.atlantvpn

import javax.crypto.KeyGenerator
import org.junit.Assert.*
import org.junit.Test

class ProtectedPreferenceCodecTest {
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test fun roundTripRandomIVAndNoClearText() {
        val key = key()
        val plain = "test-profile-secret".toByteArray()
        val a = ProtectedPreferenceCodec.seal(key, "saved_servers", plain)
        val b = ProtectedPreferenceCodec.seal(key, "saved_servers", plain)
        assertFalse(a.contentEquals(b))
        assertFalse(String(a).contains("test-profile-secret"))
        assertArrayEquals(plain, ProtectedPreferenceCodec.open(key, "saved_servers", a))
    }

    @Test fun rejectTamperWrongKeyAndSwappedName() {
        val key = key()
        val a = ProtectedPreferenceCodec.seal(key, "saved_servers", "profile".toByteArray())
        fun fails(block: () -> Unit) {
            try { block(); fail("Invalid encrypted value accepted") } catch (_: java.security.GeneralSecurityException) { }
        }
        fails { ProtectedPreferenceCodec.open(key(), "saved_servers", a) }
        fails { ProtectedPreferenceCodec.open(key, "subscription_info", a) }
        a[a.lastIndex] = (a.last().toInt() xor 1).toByte()
        fails { ProtectedPreferenceCodec.open(key, "saved_servers", a) }
        fails { ProtectedPreferenceCodec.open(key, "saved_servers", byteArrayOf()) }
    }
}
