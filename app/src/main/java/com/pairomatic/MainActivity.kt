package com.pairomatic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairomatic.data.settings.AppSettings
import com.pairomatic.data.settings.ThemeMode
import com.pairomatic.ui.navigation.AppNavHost
import com.pairomatic.ui.onboarding.OnboardingScreen
import com.pairomatic.ui.theme.PairomaticTheme
import com.pairomatic.ui.theme.appBackgroundGradient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = (application as PairOMaticApp).container.settingsRepository

        setContent {
            val scope = rememberCoroutineScope()
            val settings: AppSettings? by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = null as AppSettings?)

            val dark = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()   // SYSTEM lub jeszcze niewczytane
            }

            PairomaticTheme(darkTheme = dark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBackgroundGradient(dark))
                ) {
                    val current = settings
                    when {
                        current == null -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )  // krótki splash zanim wczytają się ustawienia
                        !current.onboardingDone -> OnboardingScreen(
                            onDone = { scope.launch { settingsRepository.setOnboardingDone(true) } }
                        )
                        else -> AppNavHost()
                    }
                }
            }
        }
    }
}
