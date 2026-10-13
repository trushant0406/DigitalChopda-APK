package com.digitalchopda.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BiometricAuthManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("digital_chopda_biometric_prefs", Context.MODE_PRIVATE)

    private val _isBiometricEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    )
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    var isSessionAuthenticated: Boolean = false
        private set

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_lock_enabled_flag"
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun markSessionAuthenticated() {
        isSessionAuthenticated = true
    }

    fun invalidateSession() {
        isSessionAuthenticated = false
    }

    fun canAuthenticate(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            }
            biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "|| 卐 DIGITAL CHOPDA SECURITY 卐 ||",
        subtitle: String = "Verify fingerprint, face unlock or device PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canAuthenticate()) {
            // If device has no hardware or credentials, allow access smoothly
            isSessionAuthenticated = true
            onSuccess()
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isSessionAuthenticated = true
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Transient biometric mismatch, user can retry immediately on prompt
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }

            val promptInfo = promptInfoBuilder.build()
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("BiometricAuthManager", "Failed to invoke biometric prompt", e)
            onError(e.message ?: "Biometric prompt unavailable")
        }
    }
}
