package com.pairomatic.ui.learn

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.pairomatic.ui.components.AppTopBar
import com.pairomatic.ui.components.boldPairLetters
import com.pairomatic.ui.rememberAppContainer
import com.pairomatic.ui.theme.BrandAmber
import com.pairomatic.ui.theme.BrandGreen
import com.pairomatic.ui.theme.BrandRed

@Composable
fun LearnScreen() {
    val container = rememberAppContainer()
    val viewModel: LearnViewModel = viewModel(
        factory = viewModelFactory { initializer { LearnViewModel(container.pairRepository) } }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar("Nauka") },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!state.loading && !state.empty) {
                LearnControls(
                    direction = state.direction,
                    group = state.group,
                    onDirection = viewModel::setDirection,
                    onGroup = viewModel::setGroup
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.loading -> CircularProgressIndicator()
                    state.empty -> CenterMessage("Brak par do nauki.\nDodaj pary w zakładce Pary.")
                    state.groupEmpty -> CenterMessage("Brak par w tej grupie.\nZmień filtr powyżej.")
                    else -> {
                        val pair = state.pair
                        if (pair != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                LearnCard(
                                    letters = pair.letters,
                                    word = pair.word,
                                    imageFile = viewModel.imageFile(pair.imagePath),
                                    direction = state.direction,
                                    revealed = state.revealed
                                )

                                if (!state.revealed) {
                                    Button(
                                        onClick = viewModel::reveal,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(18.dp)
                                    ) { Text("Pokaż odpowiedź", style = MaterialTheme.typography.titleMedium) }
                                    TextButton(onClick = viewModel::skip) { Text("Dalej (pomiń)") }
                                } else {
                                    GradeButton("Nie znam", BrandRed) { viewModel.grade(0) }
                                    GradeButton("Znam w miarę", BrandAmber) { viewModel.grade(1) }
                                    GradeButton("Znam bardzo dobrze", BrandGreen) { viewModel.grade(2) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnControls(
    direction: LearnDirection,
    group: LearnGroup,
    onDirection: (LearnDirection) -> Unit,
    onGroup: (LearnGroup) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // Kierunek testu
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = direction == LearnDirection.LETTERS_TO_WORD,
                onClick = { onDirection(LearnDirection.LETTERS_TO_WORD) },
                label = { Text("Litery → słowo") }
            )
            FilterChip(
                selected = direction == LearnDirection.WORD_TO_LETTERS,
                onClick = { onDirection(LearnDirection.WORD_TO_LETTERS) },
                label = { Text("Słowo → litery") }
            )
        }

        // Grupa
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GroupChip(group, LearnGroup.ALL, "Wszystkie", onGroup)
            GroupChip(group, LearnGroup.NEW, "Nowe", onGroup)
            GroupChip(group, LearnGroup.DONT_KNOW, "Nie znam", onGroup)
            GroupChip(group, LearnGroup.SOSO, "W miarę", onGroup)
            GroupChip(group, LearnGroup.WELL, "Dobrze", onGroup)
            GroupChip(group, LearnGroup.HARD, "Trudne", onGroup)
            GroupChip(group, LearnGroup.REVIEW, "Do zmiany", onGroup)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupChip(
    current: LearnGroup,
    value: LearnGroup,
    label: String,
    onGroup: (LearnGroup) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onGroup(value) },
        label = { Text(label) }
    )
}

@Composable
private fun LearnCard(
    letters: String,
    word: String,
    imageFile: java.io.File?,
    direction: LearnDirection,
    revealed: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Zagadka (zawsze widoczna): litery albo słowo, zależnie od kierunku.
            if (direction == LearnDirection.LETTERS_TO_WORD) {
                Text(
                    letters,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    word.ifBlank { "—" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (revealed) {
                // Odpowiedź przeciwna do zagadki.
                if (direction == LearnDirection.WORD_TO_LETTERS) {
                    Text(
                        letters,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val file = imageFile?.takeIf { it.exists() }
                if (file != null) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Obrazek pary",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
                if (direction == LearnDirection.LETTERS_TO_WORD && word.isNotBlank()) {
                    Text(
                        text = boldPairLetters(word, letters),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            } else {
                Text(
                    if (direction == LearnDirection.LETTERS_TO_WORD) "Przypomnij sobie skojarzenie…"
                    else "Jakie to litery?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GradeButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
