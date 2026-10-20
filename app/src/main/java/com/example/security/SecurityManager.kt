package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic & Keystore helper for offline license validation.
 * Reconstructs the shared secret from an XOR-masked byte array at runtime
 * to resist plain-string extraction via `strings` or JADX.
 */
object SecurityManager {

    private const val PREFS_FILE_NAME = "lucky2d_secure_license_prefs"
    private const val FALLBACK_PREFS_FILE_NAME = "lucky2d_fallback_license_prefs"
    private const val XOR_MASK: Int = 0x5A

    // XOR-masked bytes of "LUCKY2D-OFFLINE-MASTER-SECRET-2026-KEY" with 0x5A
    private val OBFUSCATED_SECRET_BYTES = intArrayOf(
        0x16, 0x0F, 0x19, 0x11, 0x03, 0x68, 0x1E, 0x77,
        0x15, 0x1C, 0x1C, 0x16, 0x13, 0x14, 0x1F, 0x77,
        0x17, 0x1B, 0x09, 0x0E, 0x1F, 0x08, 0x77, 0x09,
        0x1F, 0x19, 0x08, 0x1F, 0x0E, 0x77, 0x68, 0x6A,
        0x68, 0x6C, 0x77, 0x11, 0x1F, 0x03
    )

    /**
     * Reconstructs the shared secret at runtime.
     * Matches Admin Key Generator secret: "LUCKY2D-OFFLINE-MASTER-SECRET-2026-KEY"
     */
    fun getSharedSecret(): String {
        val chars = CharArray(OBFUSCATED_SECRET_BYTES.size) { index ->
            (OBFUSCATED_SECRET_BYTES[index] xor XOR_MASK).toChar()
        }
        return String(chars)
    }

    /**
     * Computes uppercase hexadecimal HMAC-SHA256(payload, secret).
     */
    fun computeHmacSha256Hex(payload: String, secret: String = getSharedSecret()): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val rawHmac = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        return rawHmac.joinToString("") { "%02X".format(Locale.US, it) }
    }

    /**
     * Constant-time string comparison using MessageDigest.isEqual to prevent timing attacks.
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        val bytesA = a.toByteArray(StandardCharsets.UTF_8)
        val bytesB = b.toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(bytesA, bytesB)
    }

    /**
     * Returns EncryptedSharedPreferences backed by Android Keystore AES256_GCM MasterKey.
     * Includes graceful recovery if Keystore throws a hardware migration/corruption exception.
     */
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        if (isRobolectricRuntime()) {
            return context.applicationContext.getSharedPreferences(
                FALLBACK_PREFS_FILE_NAME,
                Context.MODE_PRIVATE
            )
        }
        val appContext = context.applicationContext
        return try {
            createEncryptedPrefs(appContext)
        } catch (e: Exception) {
            // Recover from Keystore invalidation after OS/lockscreen migration
            try {
                appContext.deleteSharedPreferences(PREFS_FILE_NAME)
                createEncryptedPrefs(appContext)
            } catch (fallbackEx: Exception) {
                appContext.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    internal fun isRobolectricRuntime(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }
}
