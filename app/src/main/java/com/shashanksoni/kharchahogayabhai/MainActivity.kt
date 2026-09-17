package com.shashanksoni.kharchahogayabhai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shashanksoni.kharchahogayabhai.navigation.KharchaApp
import com.shashanksoni.kharchahogayabhai.ui.theme.KharchaTheme

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
}
