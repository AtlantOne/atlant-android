package com.example.atlantvpn

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

// Keys never leave AndroidKeyStore. Backups are disabled in the manifest.
// A lost/invalidated key must not cause silent plaintext fallback or replacement.
internal object SecretPreferences {
    private const val ALIAS = "atlant.preferences.aes.v1"
    private const val PREFS = "atlant_vpn_prefs"
    private const val SUFFIX = ".sealed.v1"
    private val unreadable = mutableSetOf<String>()

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(ALIAS, null)
        if (existing != null) return existing as SecretKey
        check(create) { "Android profile key unavailable; ciphertext retained" }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    @Synchronized
    fun read(context: Context, name: String): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        try {
            val sealed = prefs.getString(name + SUFFIX, null)
            if (sealed != null) {
                val plain = ProtectedPreferenceCodec.open(key(false), name, Base64.decode(sealed, Base64.NO_WRAP))
                return try { String(plain, Charsets.UTF_8) } finally { plain.fill(0) }
            }
            val legacy = prefs.getString(name, null) ?: return null
            // Migration commits ciphertext and removes plaintext in the SAME edit.
            write(context, name, legacy)
            return legacy
        } catch (e: Exception) {
            unreadable.add(name)
            throw e
        }
    }

    @Synchronized
    fun write(context: Context, name: String, value: String?) {
        check(name !in unreadable) { "Unreadable profile retained; refusing overwrite" }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Validate existing ciphertext before replacement, even before first read.
        if (prefs.contains(name + SUFFIX)) {
            val sealed = Base64.decode(prefs.getString(name + SUFFIX, null), Base64.NO_WRAP)
            ProtectedPreferenceCodec.open(key(false), name, sealed).fill(0)
        }
        val editor = prefs.edit().remove(name)
        if (value == null) {
            editor.remove(name + SUFFIX)
        } else {
            val plain = value.toByteArray(Charsets.UTF_8)
            val sealed = try { ProtectedPreferenceCodec.seal(key(true), name, plain) } finally { plain.fill(0) }
            editor.putString(name + SUFFIX, Base64.encodeToString(sealed, Base64.NO_WRAP))
        }
        if (!editor.commit()) throw IOException("Protected preferences were not saved")
    }
}
