package com.digitalchopda.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore AES-256-GCM Vault
 */
class KeystoreVault(context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "DigitalChopdaMasterKey"

    init {
        createMasterKeyIfNeeded()
    }

    private fun createMasterKeyIfNeeded() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine IV (12 bytes) + CipherBytes
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = ByteArray(12)
            val cipherBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, cipherBytes, 0, cipherBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * Token and Session Vault backed by Keystore AES-256-GCM
 */
class TokenStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("chopda_vault_prefs", Context.MODE_PRIVATE)
    private val vault = KeystoreVault(context)

    companion object {
        private const val KEY_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_USER_ID = "enc_user_id"
        private const val KEY_MOBILE = "enc_mobile"
        private const val KEY_ACTIVE_COMPANY = "active_company_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SERVER_URL = "server_base_url"
        const val DEFAULT_BASE_URL = "http://100.99.250.0:3000/api/"
        const val URL_CLOUD_PROD = "https://app.digitalchopda.in/api/"
        const val URL_OFFICE_WIFI = "http://10.74.4.212:3000/api/"
        const val URL_TAILSCALE_DEV = "http://100.99.250.0:3000/api/"
        const val URL_EMULATOR = "http://10.0.2.2:3000/api/"
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_SERVER_URL, sanitized).apply()
        com.digitalchopda.app.core.network.NetworkClient.reset()
        com.digitalchopda.app.core.network.AuthHeaderInterceptor.activeHostOverride = null
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, vault.encrypt(accessToken))
            .putString(KEY_REFRESH_TOKEN, vault.encrypt(refreshToken))
            .apply()
    }

    fun getAccessToken(): String {
        val enc = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return ""
        return vault.decrypt(enc)
    }

    fun getRefreshToken(): String {
        val enc = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return ""
        return vault.decrypt(enc)
    }

    fun saveUserSession(userId: String, mobile: String, activeCompanyId: Long) {
        prefs.edit()
            .putString(KEY_USER_ID, vault.encrypt(userId))
            .putString(KEY_MOBILE, vault.encrypt(mobile))
            .putLong(KEY_ACTIVE_COMPANY, activeCompanyId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getActiveCompanyId(): Long {
        return prefs.getLong(KEY_ACTIVE_COMPANY, 1L)
    }

    fun setActiveCompanyId(companyId: Long) {
        prefs.edit().putLong(KEY_ACTIVE_COMPANY, companyId).apply()
    }

    fun isTokenExpired(): Boolean {
        val token = getAccessToken()
        if (token.isBlank()) return true
        val parts = token.split(":")
        val exp = when (parts.size) {
            5 -> parts[3].toLongOrNull()
            4 -> parts[2].toLongOrNull()
            else -> null
        } ?: return false
        return System.currentTimeMillis() > exp
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken().isNotEmpty() && !isTokenExpired()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
