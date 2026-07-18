package com.pairomatic.ui.settings

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pairomatic.data.settings.LearningMode
import com.pairomatic.data.settings.NotificationImportance
import com.pairomatic.ui.components.AppTopBar

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

    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importCsv(uri, replaceAll) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { AppTopBar("Ustawienia") },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Color.Transparent
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
            Button(
                onClick = viewModel::startNow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
            ) {
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
                FilledTonalButton(
                shape = RoundedCornerShape(18.dp),
                    onClick = {
                        pickTime(context, settings.quietStartMinute) { minute ->
                            viewModel.setQuietHours(minute, settings.quietEndMinute)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Od: ${formatMinute(settings.quietStartMinute)}") }
                FilledTonalButton(
                shape = RoundedCornerShape(18.dp),
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
            FilledTonalButton(
                shape = RoundedCornerShape(18.dp),
                onClick = { exportLauncher.launch("pairs-export.zip") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Eksportuj do .zip") }

            SwitchRow(
                label = "Import zastępuje całą bazę",
                checked = replaceAll,
                onCheckedChange = { replaceAll = it }
            )
            ImportHelp()
            FilledTonalButton(
                shape = RoundedCornerShape(18.dp),
                onClick = {
                    importCsvLauncher.launch(
                        arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "text/plain",
                            "application/vnd.ms-excel",
                            "application/octet-stream"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importuj z CSV") }
            FilledTonalButton(
                shape = RoundedCornerShape(18.dp),
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importuj z .zip (z obrazkami)") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

/**
 * Rozwijana pomoc opisująca format pliku CSV do importu par.
 */
@Composable
private fun ImportHelp() {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Text(
                text = "ℹ️ Jak przygotować plik CSV?" + if (expanded) "  ▲" else "  ▼",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "CSV to zwykły plik tekstowy z arkusza (Excel / Google Sheets) — trzy kolumny:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Przykład:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = """
                        litery,slowo,obrazek
                        CT,Cytryna,ct.png
                        SK,Skorpion,sk.png
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "• 1. kolumna — litery pary (wymagane, np. CT)\n" +
                        "• 2. kolumna — słowo-obraz (np. Cytryna)\n" +
                        "• 3. kolumna — nazwa pliku obrazka (opcjonalna)\n" +
                        "• separator: przecinek lub średnik\n" +
                        "• pierwszy wiersz nagłówka jest pomijany\n" +
                        "• zapisz w kodowaniu UTF-8 (polskie znaki)",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "CSV wgrywa tylko tekst. Chcesz od razu z obrazkami? Spakuj plik .csv razem " +
                        "z folderem images/ (z obrazkami) do jednego .zip i użyj „Importuj z .zip\". " +
                        "Nazwy plików w images/ muszą pasować do 3. kolumny CSV. „Import zastępuje " +
                        "całą bazę\" działa dla obu formatów.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
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
