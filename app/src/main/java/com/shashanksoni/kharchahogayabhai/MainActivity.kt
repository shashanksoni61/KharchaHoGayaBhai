package com.shashanksoni.kharchahogayabhai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.shashanksoni.kharchahogayabhai.navigation.KharchaApp
import com.shashanksoni.kharchahogayabhai.ui.theme.KharchaTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = appContainer
        requestNotificationPermissionIfNeeded()
        setContent {
            KharchaTheme {
                KharchaApp(container = container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        lifecycleScope.launch {
            runCatching { appContainer.importSmsInbox.syncOnAppOpen() }
        }
    }

    override fun onStop() {
        super.onStop()
        val session = appContainer.securitySession
        if (!isChangingConfigurations && !session.isAuthenticating) {
            session.lockApp()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (!appContainer.smsScanPreferences.listenInBackgroundEnabled()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST,
        )
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1101
    }
}
