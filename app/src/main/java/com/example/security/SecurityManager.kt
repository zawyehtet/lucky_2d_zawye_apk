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
 * Cryptographic & Keystore helper for 100% offline license validation.
 * Uses HMAC-SHA256 with the fixed shared secret "Lucky3D_Super_Secret_Key_2026!#".
 */
object SecurityManager {

    private const val PREFS_FILE_NAME = "lucky2d_secure_license_prefs"
    private const val FALLBACK_PREFS_FILE_NAME = "lucky2d_fallback_license_prefs"
    const val SHARED_SECRET = "Lucky3D_Super_Secret_Key_2026!#"

    /**
     * Returns the fixed shared secret shared between the Android client and Admin HTML/JS generator.
     */
    fun getSharedSecret(): String = SHARED_SECRET

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
