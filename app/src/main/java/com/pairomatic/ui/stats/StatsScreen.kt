package com.pairomatic.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pairomatic.ui.components.AppTopBar
import com.pairomatic.ui.rememberAppContainer
import com.pairomatic.util.dayWord
import com.pairomatic.ui.theme.BrandAmber
import com.pairomatic.ui.theme.BrandBlue
import com.pairomatic.ui.theme.BrandGreen
import com.pairomatic.ui.theme.BrandPink
import com.pairomatic.ui.theme.BrandPurple
import com.pairomatic.ui.theme.BrandRed
import com.pairomatic.ui.theme.brandGradient

@Composable
fun StatsScreen() {
    val container = rememberAppContainer()
    val viewModel: StatsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { StatsViewModel(container.pairRepository, container.settingsRepository) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar("Statystyki") },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MotivationCard(
                streak = progress.streak,
                today = progress.today,
                goal = progress.dailyGoal,
                onGoalDown = { viewModel.setDailyGoal(progress.dailyGoal - 5) },
                onGoalUp = { viewModel.setDailyGoal(progress.dailyGoal + 5) }
            )

            if (progress.backupOverdue) {
                BackupReminder(daysSince = progress.daysSinceBackup)
            }

            MasteryHeader(total = state.total, veryWell = state.veryWell)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(Modifier.weight(1f), "🟢", "Znam bardzo dobrze", state.veryWell, BrandGreen)
                StatTile(Modifier.weight(1f), "🟡", "Znam w miarę", state.soso, BrandAmber)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(Modifier.weight(1f), "🔴", "Nie znam", state.dontKnow, BrandRed)
                StatTile(Modifier.weight(1f), "🚩", "Nie wchodzi do głowy", state.hard, BrandPink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(Modifier.weight(1f), "⚪", "Jeszcze nieklinięte", state.neverGraded, BrandBlue)
                StatTile(Modifier.weight(1f), "✏️", "Bez słowa", state.noWord, BrandBlue)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(Modifier.weight(1f), "🛠️", "Do zmiany", state.review, BrandPurple)
                StatTile(Modifier.weight(1f), "🖼️", "Bez obrazka", state.noImage, BrandBlue)
            }

            Text(
                "Kategorie mogą się przecinać (np. „nie znam” i oflagowana), więc nie sumują się do całości.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MotivationCard(
    streak: Int,
    today: Int,
    goal: Int,
    onGoalDown: () -> Unit,
    onGoalUp: () -> Unit
) {
    val fraction = if (goal > 0) (today.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(brandGradient())
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.size(8.dp))
                Column {
                    Text(
                        if (streak > 0) "Seria: $streak ${dayWord(streak)}" else "Zacznij serię dziś!",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Dziś: $today / $goal ocen",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Cel dzienny",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(onClick = onGoalDown) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text(
                    "  $goal  ",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalIconButton(onClick = onGoalUp) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}


@Composable
private fun BackupReminder(daysSince: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandAmber.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💾", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(12.dp))
            Text(
                text = if (daysSince == null)
                    "Nie masz jeszcze kopii zapasowej. Zrób ją w Ustawieniach → Kopia zapasowa, żeby nie stracić talii."
                else
                    "Ostatnia kopia: $daysSince ${dayWord(daysSince)} temu. Warto zrobić nową (Ustawienia → Kopia zapasowa).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MasteryHeader(total: Int, veryWell: Int) {
    val fraction = if (total > 0) veryWell.toFloat() / total else 0f
    val percent = (fraction * 100).toInt()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(brandGradient())
            .padding(20.dp)
    ) {
        Column {
            Text(
                "Opanowanie talii",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$percent%",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$veryWell z $total par opanowanych bardzo dobrze",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    emoji: String,
    label: String,
    value: Int,
    accent: Color
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column {
                Text(
                    "$value",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
