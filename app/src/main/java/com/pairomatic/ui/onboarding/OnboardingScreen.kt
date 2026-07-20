package com.pairomatic.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pairomatic.ui.components.AppTopBar
import com.pairomatic.ui.theme.BrandGreen

/**
 * Onboarding niezawodności powiadomień: prośba o POST_NOTIFICATIONS (Android 13+) oraz
 * przejście do wyłączenia optymalizacji baterii (kluczowe na Xiaomi/Samsung/Huawei itd.).
 * Pokazywany raz przy pierwszym uruchomieniu; dostępny też z Ustawień.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var notificationsGranted by remember {
        mutableStateOf(
            !needsRuntimePermission ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var batteryOpened by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    Scaffold(
        topBar = { AppTopBar("Zanim zaczniesz") },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Nauka odbywa się głównie przez powiadomienia przy odblokowywaniu telefonu. " +
                    "Żeby działała niezawodnie, ustaw dwie rzeczy:",
                style = MaterialTheme.typography.bodyLarge
            )

            StepCard(
                number = "1",
                title = "Zezwól na powiadomienia",
                body = "Bez tego nie zobaczysz par do nauki.",
                done = notificationsGranted
            ) {
                if (!notificationsGranted && needsRuntimePermission) {
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Zezwól na powiadomienia") }
                } else {
                    Text("Powiadomienia włączone ✓", color = BrandGreen, style = MaterialTheme.typography.bodyMedium)
                }
            }

            StepCard(
                number = "2",
                title = "Wyłącz optymalizację baterii",
                body = "Na wielu telefonach (Xiaomi, Samsung, Huawei, OPPO) system usypia aplikacje " +
                    "w tle i powiadomienia przestają przychodzić. Wyłącz oszczędzanie baterii dla tej aplikacji, " +
                    "a jeśli masz — włącz też „autostart”.",
                done = batteryOpened
            ) {
                FilledTonalButton(
                    onClick = {
                        batteryOpened = true
                        openBatterySettings(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Otwórz ustawienia baterii") }
            }

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
            ) { Text("Gotowe — zaczynamy!", style = MaterialTheme.typography.titleMedium) }

            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Pomiń na razie") }
        }
    }
}

@Composable
private fun StepCard(
    number: String,
    title: String,
    body: String,
    done: Boolean,
    action: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(28.dp))
                } else {
                    Text(
                        number,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "  $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action()
        }
    }
}

/** Otwiera systemowe ustawienia baterii dla aplikacji; z fallbackami dla różnych producentów. */
private fun openBatterySettings(context: android.content.Context) {
    val pkg = context.packageName
    val intents = listOf(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg")),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
    )
    for (intent in intents) {
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
}
