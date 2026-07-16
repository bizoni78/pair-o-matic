package com.pairomatic.ui.settings

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pairomatic.data.settings.LearningMode
import com.pairomatic.data.settings.NotificationImportance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Eksport: przechowujemy wybór „ze statystykami" do momentu wskazania pliku.
    var includeStats by remember { mutableStateOf(true) }
    var replaceAll by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) viewModel.export(uri, includeStats) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.import(uri, replaceAll) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ustawienia") }) },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("Tryb nauki")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.mode == LearningMode.TEST,
                    onClick = { viewModel.setMode(LearningMode.TEST) },
                    label = { Text("Test") }
                )
                FilterChip(
                    selected = settings.mode == LearningMode.IMMERSION,
                    onClick = { viewModel.setMode(LearningMode.IMMERSION) },
                    label = { Text("Immersja") }
                )
            }

            Divider()
            SwitchRow(
                label = "Powiadomienia włączone",
                checked = settings.notificationsEnabled,
                onCheckedChange = viewModel::setNotificationsEnabled
            )
            Button(onClick = viewModel::startNow, modifier = Modifier.fillMaxWidth()) {
                Text("Pokaż teraz pierwsze powiadomienie")
            }

            Divider()
            SectionTitle("Godziny ciszy")
            SwitchRow(
                label = "Włącz godziny ciszy",
                checked = settings.quietHoursEnabled,
                onCheckedChange = viewModel::setQuietHoursEnabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pickTime(context, settings.quietStartMinute) { minute ->
                            viewModel.setQuietHours(minute, settings.quietEndMinute)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Od: ${formatMinute(settings.quietStartMinute)}") }
                OutlinedButton(
                    onClick = {
                        pickTime(context, settings.quietEndMinute) { minute ->
                            viewModel.setQuietHours(settings.quietStartMinute, minute)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Do: ${formatMinute(settings.quietEndMinute)}") }
            }

            Divider()
            SectionTitle("Kadencja immersji")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60).forEach { minutes ->
                    FilterChip(
                        selected = settings.immersionIntervalMinutes == minutes,
                        onClick = { viewModel.setImmersionInterval(minutes) },
                        label = { Text("$minutes min") }
                    )
                }
            }

            Divider()
            SectionTitle("Poziom ważności powiadomień")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.importance == NotificationImportance.SILENT,
                    onClick = { viewModel.setImportance(NotificationImportance.SILENT) },
                    label = { Text("Cichy") }
                )
                FilterChip(
                    selected = settings.importance == NotificationImportance.HEADS_UP,
                    onClick = { viewModel.setImportance(NotificationImportance.HEADS_UP) },
                    label = { Text("Heads-up") }
                )
            }

            Divider()
            SectionTitle("Import / eksport")
            SwitchRow(
                label = "Eksportuj ze statystykami",
                checked = includeStats,
                onCheckedChange = { includeStats = it }
            )
            OutlinedButton(
                onClick = { exportLauncher.launch("pairs-export.zip") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Eksportuj do .zip") }

            SwitchRow(
                label = "Import zastępuje całą bazę",
                checked = replaceAll,
                onCheckedChange = { replaceAll = it }
            )
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importuj z .zip") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun pickTime(
    context: android.content.Context,
    initialMinuteOfDay: Int,
    onPicked: (Int) -> Unit
) {
    val hour = initialMinuteOfDay / 60
    val minute = initialMinuteOfDay % 60
    TimePickerDialog(
        context,
        { _, h, m -> onPicked(h * 60 + m) },
        hour, minute, true
    ).show()
}

private fun formatMinute(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    return "%02d:%02d".format(h, m)
}
