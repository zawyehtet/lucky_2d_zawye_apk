package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.Locale
import java.util.UUID

/**
 * Singleton manager for One-Time Offline Android License & Activation.
 *
 * Workflow:
 * 1. [getDeviceId]: Fetches normalized Settings.Secure.ANDROID_ID.
 * 2. [generateExpectedKey]: Derives 12-char key `XXXX-XXXX-XXXX` via HMAC-SHA256(Device_ID, SHARED_SECRET).
 * 3. [verifyAndSaveLicense]: Validates user key in constant time and persists tamper-proof activation signature.
 * 4. [isAppActivated]: Verifies both `isActivated == true` and `HMAC-SHA256(Device_ID + "_ACTIVATED", SHARED_SECRET)`
 *    on every app launch so cloned preferences from another device are rejected.
 */
object LicenseManager {

    private const val KEY_IS_ACTIVATED = "is_activated"
    private const val KEY_ACTIVATED_DEVICE_ID = "activated_device_id"
    private const val KEY_ACTIVATION_SIGNATURE = "activation_signature"
    private const val KEY_ACTIVATED_PRODUCT_KEY = "activated_product_key"
    private const val KEY_FALLBACK_DEVICE_UUID = "fallback_device_uuid"

    /**
     * Controls whether Robolectric JVM tests enforce the activation gate by default.
     * Dedicated license tests set this to `true` to test unactivated -> activated flows.
     */
    @Volatile
    var enforceActivationInTests: Boolean = false

    /**
     * Fetches the persistent device identifier (`Settings.Secure.ANDROID_ID`),
     * normalized to uppercase alphanumeric characters.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val rawAndroidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        val cleaned = rawAndroidId
            ?.trim()
            ?.uppercase(Locale.US)
            ?.filter { it.isLetterOrDigit() }
            .orEmpty()

        // 9774D56D682E549C is a known buggy ANDROID_ID on very old Android 2.2 emulators
        if (cleaned.length >= 6 && cleaned != "9774D56D682E549C") {
            return cleaned
        }

        val prefs = SecurityManager.getEncryptedPrefs(context)
        val existingFallback = prefs.getString(KEY_FALLBACK_DEVICE_UUID, null)
        if (!existingFallback.isNullOrBlank()) {
            return existingFallback
        }

        val generated = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .uppercase(Locale.US)
            .take(16)
        prefs.edit().putString(KEY_FALLBACK_DEVICE_UUID, generated).apply()
        return generated
    }

    /**
     * Derives the expected 12-character formatted product key (`XXXX-XXXX-XXXX`)
     * from `HMAC-SHA256(Device_ID, SHARED_SECRET)`.
     */
    fun generateExpectedKey(deviceId: String): String {
        val normalizedDeviceId = normalizeDeviceId(deviceId)
        val fullHmacHex = SecurityManager.computeHmacSha256Hex(normalizedDeviceId)
        val raw12 = fullHmacHex.take(12)
        return formatKeyWithDashes(raw12)
    }

    /**
     * Computes the tamper-proof device activation signature:
     * `HMAC-SHA256(Device_ID + "_ACTIVATED", SHARED_SECRET)`
     */
    fun computeActivationSignature(deviceId: String): String {
        val normalizedDeviceId = normalizeDeviceId(deviceId)
        return SecurityManager.computeHmacSha256Hex("${normalizedDeviceId}_ACTIVATED")
    }

    /**
     * Normalizes user-entered key, performs constant-time verification against [generateExpectedKey],
     * and persists the tamper-proof activation signature into EncryptedSharedPreferences.
     */
    fun verifyAndSaveLicense(context: Context, inputKey: String): Boolean {
        val currentDeviceId = getDeviceId(context)
        val normalizedInput = normalizeKeyInput(inputKey)
        if (normalizedInput.length != 12) {
            return false
        }

        val expectedFormatted = generateExpectedKey(currentDeviceId)
        val normalizedExpected = normalizeKeyInput(expectedFormatted)

        val isValid = SecurityManager.constantTimeEquals(normalizedInput, normalizedExpected)
        if (isValid) {
            val signature = computeActivationSignature(currentDeviceId)
            SecurityManager.getEncryptedPrefs(context).edit()
                .putBoolean(KEY_IS_ACTIVATED, true)
                .putString(KEY_ACTIVATED_DEVICE_ID, currentDeviceId)
                .putString(KEY_ACTIVATION_SIGNATURE, signature)
                .putString(KEY_ACTIVATED_PRODUCT_KEY, expectedFormatted)
                .apply()
        }
        return isValid
    }

    /**
     * Checks whether the current device is validly activated.
     * Verifies:
     * 1. `isActivated == true`
     * 2. `activatedDeviceId == currentDeviceId`
     * 3. `storedSignature == HMAC-SHA256(currentDeviceId + "_ACTIVATED", SHARED_SECRET)`
     */
    fun isAppActivated(context: Context): Boolean {
        if (SecurityManager.isRobolectricRuntime() && !enforceActivationInTests) {
            return true
        }

        val prefs = SecurityManager.getEncryptedPrefs(context)
        val isActivatedFlag = prefs.getBoolean(KEY_IS_ACTIVATED, false)
        if (!isActivatedFlag) return false

        val storedDeviceId = prefs.getString(KEY_ACTIVATED_DEVICE_ID, null) ?: return false
        val storedSignature = prefs.getString(KEY_ACTIVATION_SIGNATURE, null) ?: return false

        val currentDeviceId = getDeviceId(context)
        if (!SecurityManager.constantTimeEquals(
                normalizeDeviceId(storedDeviceId),
                normalizeDeviceId(currentDeviceId)
            )
        ) {
            return false
        }

        val expectedSignature = computeActivationSignature(currentDeviceId)
        return SecurityManager.constantTimeEquals(storedSignature, expectedSignature)
    }

    /**
     * Clears stored license state (used for testing or reset).
     */
    fun clearLicenseForTesting(context: Context) {
        SecurityManager.getEncryptedPrefs(context).edit()
            .remove(KEY_IS_ACTIVATED)
            .remove(KEY_ACTIVATED_DEVICE_ID)
            .remove(KEY_ACTIVATION_SIGNATURE)
            .remove(KEY_ACTIVATED_PRODUCT_KEY)
            .apply()
    }

    /**
     * Normalizes raw key input by stripping whitespace and hyphens and uppercasing.
     */
    fun normalizeKeyInput(rawInput: String): String {
        return rawInput
            .trim()
            .uppercase(Locale.US)
            .replace("-", "")
            .replace(" ", "")
            .filter { it.isLetterOrDigit() }
            .take(12)
    }

    /**
     * Formats up to 12 raw uppercase alphanumeric characters as `XXXX-XXXX-XXXX`.
     */
    fun formatKeyWithDashes(rawAlphanumeric: String): String {
        val cleaned = normalizeKeyInput(rawAlphanumeric)
        return cleaned.chunked(4).joinToString("-")
    }

    private fun normalizeDeviceId(deviceId: String): String {
        return deviceId.trim().uppercase(Locale.US)
    }
}
