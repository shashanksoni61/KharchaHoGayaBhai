package com.shashanksoni.kharchahogayabhai.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Fingerprint / face unlock, with the phone PIN / pattern / password as fallback.
 *
 * [BIOMETRIC_WEAK] must not be combined with [DEVICE_CREDENTIAL] — that pair is
 * rejected by the platform and the prompt never appears.
 */
object BiometricAuthenticator {

    private val preferredAuthenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK
        }

    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        if (manager.canAuthenticate(preferredAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            return true
        }
        if (manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            manager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            return true
        }
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return keyguard?.isDeviceSecure == true
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailed: (message: String?) -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onFailed(null)
                    } else {
                        onFailed(errString.toString())
                    }
                }
            },
        )

        val info = buildPromptInfo(activity, title, subtitle) ?: run {
            onFailed(activity.getString(com.shashanksoni.kharchahogayabhai.R.string.security_device_lock_required))
            return
        }
        try {
            prompt.authenticate(info)
        } catch (error: IllegalArgumentException) {
            onFailed(error.message ?: activity.getString(com.shashanksoni.kharchahogayabhai.R.string.security_device_lock_required))
        }
    }

    private fun buildPromptInfo(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): BiometricPrompt.PromptInfo? {
        val manager = BiometricManager.from(activity)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                    builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL).build()
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    manager.canAuthenticate(DEVICE_CREDENTIAL) ==
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                    builder.setAllowedAuthenticators(DEVICE_CREDENTIAL).build()
                }

                manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> {
                    builder
                        .setAllowedAuthenticators(BIOMETRIC_WEAK)
                        .setNegativeButtonText(
                            activity.getString(com.shashanksoni.kharchahogayabhai.R.string.security_cancel),
                        )
                        .build()
                }

                Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                    activity.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true -> {
                    @Suppress("DEPRECATION")
                    builder.setDeviceCredentialAllowed(true).build()
                }

                else -> null
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
