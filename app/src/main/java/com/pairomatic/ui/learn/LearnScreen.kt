package com.pairomatic.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.pairomatic.ui.components.AppTopBar
import com.pairomatic.ui.components.BrandFilterChip
import com.pairomatic.ui.components.boldPairLetters
import com.pairomatic.ui.rememberAppContainer
import com.pairomatic.ui.theme.BrandAmber
import com.pairomatic.ui.theme.BrandGreen
import com.pairomatic.ui.theme.BrandRed
import kotlinx.coroutines.delay

@Composable
fun LearnScreen() {
    val container = rememberAppContainer()
    val viewModel: LearnViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LearnViewModel(container.pairRepository, container.settingsRepository) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val celebrate by viewModel.celebrate.collectAsStateWithLifecycle()

    var showCelebration by remember { mutableStateOf(false) }
    LaunchedEffect(celebrate) { if (celebrate > 0) showCelebration = true }
    LaunchedEffect(showCelebration) {
        if (showCelebration) { delay(2800); showCelebration = false }
    }

    Scaffold(
        topBar = { AppTopBar("Nauka") },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!state.loading && !state.empty) {
                    LearnControls(
                        direction = state.direction,
                        group = state.group,
                        inputMode = state.inputMode,
                        onDirection = viewModel::setDirection,
                        onGroup = viewModel::setGroup,
                        onInputMode = viewModel::setInputMode
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

                                    if (state.inputMode) {
                                        InputSection(
                                            pairId = pair.id,
                                            revealed = state.revealed,
                                            result = state.answerResult,
                                            correctAnswer = if (state.direction == LearnDirection.LETTERS_TO_WORD)
                                                pair.word else pair.letters,
                                            onSubmit = viewModel::submitAnswer,
                                            onNext = viewModel::skip
                                        )
                                    } else if (!state.revealed) {
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

            if (showCelebration) {
                CelebrationOverlay(onDismiss = { showCelebration = false })
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

@Composable
private fun InputSection(
    pairId: Long,
    revealed: Boolean,
    result: AnswerResult?,
    correctAnswer: String,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit
) {
    if (!revealed) {
        var answer by remember(pairId) { mutableStateOf("") }
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            singleLine = true,
            label = { Text("Wpisz odpowiedź") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (answer.isNotBlank()) onSubmit(answer) }),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { if (answer.isNotBlank()) onSubmit(answer) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) { Text("Sprawdź", style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = onNext) { Text("Dalej (pomiń)") }
    } else {
        val correct = result == AnswerResult.CORRECT
        Text(
            if (correct) "✅ Dobrze!" else "❌ Niestety — poprawnie: „$correctAnswer”",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (correct) BrandGreen else BrandRed,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) { Text("Następna", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun LearnControls(
    direction: LearnDirection,
    group: LearnGroup,
    inputMode: Boolean,
    onDirection: (LearnDirection) -> Unit,
    onGroup: (LearnGroup) -> Unit,
    onInputMode: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrandFilterChip(
                selected = direction == LearnDirection.LETTERS_TO_WORD,
                onClick = { onDirection(LearnDirection.LETTERS_TO_WORD) },
                label = "Litery → słowo"
            )
            BrandFilterChip(
                selected = direction == LearnDirection.WORD_TO_LETTERS,
                onClick = { onDirection(LearnDirection.WORD_TO_LETTERS) },
                label = "Słowo → litery"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrandFilterChip(
                selected = !inputMode,
                onClick = { onInputMode(false) },
                label = "Samoocena"
            )
            BrandFilterChip(
                selected = inputMode,
                onClick = { onInputMode(true) },
                label = "Wpisywanie"
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GroupChip(group, LearnGroup.ALL, "Wszystkie", onGroup)
            GroupChip(group, LearnGroup.MISTAKES, "🔁 Błędy", onGroup)
            GroupChip(group, LearnGroup.NEW, "Nowe", onGroup)
            GroupChip(group, LearnGroup.DONT_KNOW, "Nie znam", onGroup)
            GroupChip(group, LearnGroup.SOSO, "W miarę", onGroup)
            GroupChip(group, LearnGroup.WELL, "Dobrze", onGroup)
            GroupChip(group, LearnGroup.HARD, "Trudne", onGroup)
            GroupChip(group, LearnGroup.REVIEW, "Do zmiany", onGroup)
        }
    }
}

@Composable
private fun GroupChip(
    current: LearnGroup,
    value: LearnGroup,
    label: String,
    onGroup: (LearnGroup) -> Unit
) {
    BrandFilterChip(
        selected = current == value,
        onClick = { onGroup(value) },
        label = label
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

@Composable
private fun CelebrationOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎉🎊✨", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Cel dnia osiągnięty!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Świetna robota — seria trwa! 🔥",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
