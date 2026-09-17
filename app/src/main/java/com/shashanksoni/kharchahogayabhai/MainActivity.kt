package com.shashanksoni.kharchahogayabhai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.shashanksoni.kharchahogayabhai.navigation.KharchaApp
import com.shashanksoni.kharchahogayabhai.ui.theme.KharchaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = appContainer
        setContent {
            KharchaTheme {
                KharchaApp(container = container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Pull newest bank/UPI SMS since the last scan cursor (no-op without permission).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        lifecycleScope.launch {
            runCatching { appContainer.importSmsInbox.syncOnAppOpen() }
        }
    }
}
