package com.shashanksoni.kharchahogayabhai.security

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.appContainer

/**
 * Starts a fingerprint / face prompt with the device PIN / pattern / password
 * as fallback. Marks [SecuritySession.isAuthenticating] so going to the system
 * credential screen does not immediately re-lock the app.
 */
fun SecuritySession.authenticate(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFailed: (String?) -> Unit = {},
) {
    if (!BiometricAuthenticator.canAuthenticate(activity)) {
        onFailed(activity.getString(R.string.security_device_lock_required))
        return
    }
    isAuthenticating = true
    BiometricAuthenticator.authenticate(
        activity = activity,
        title = title,
        subtitle = subtitle,
        onSuccess = {
            isAuthenticating = false
            onSuccess()
        },
        onFailed = { message ->
            isAuthenticating = false
            onFailed(message)
        },
    )
}

fun Context.findFragmentActivity(): FragmentActivity =
    generateSequence(this) { context -> (context as? ContextWrapper)?.baseContext }
        .filterIsInstance<FragmentActivity>()
        .firstOrNull()
        ?: error("Biometric auth needs a FragmentActivity host")

@Composable
fun rememberSecurityAuth(): SecurityAuth {
    val context = LocalContext.current
    val session = context.appContainer.securitySession
    val activity = remember(context) { context.findFragmentActivity() }
    return remember(session, activity) {
        SecurityAuth(session = session, activity = activity)
    }
}

class SecurityAuth(
    private val session: SecuritySession,
    private val activity: FragmentActivity,
) {
    val canAuthenticate: Boolean
        get() = BiometricAuthenticator.canAuthenticate(activity)

    fun prompt(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailed: (String?) -> Unit = {},
    ) {
        session.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = onSuccess,
            onFailed = onFailed,
        )
    }
}
