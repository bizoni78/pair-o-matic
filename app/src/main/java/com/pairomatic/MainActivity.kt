package com.pairomatic

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.pairomatic.ui.navigation.AppNavHost
import com.pairomatic.ui.theme.PairomaticTheme
import com.pairomatic.ui.theme.appBackgroundGradient

class MainActivity : ComponentActivity() {

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* wynik nieistotny */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationsPermission()

        setContent {
            PairomaticTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBackgroundGradient(isSystemInDarkTheme()))
                ) {
                    AppNavHost()
                }
            }
        }
    }

    private fun maybeRequestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
