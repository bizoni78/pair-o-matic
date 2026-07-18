package com.pairomatic.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.loading -> CircularProgressIndicator()
                state.empty -> Text(
                    "Brak par do nauki.\nDodaj pary w zakładce Pary.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> {
                    val pair = state.pair
                    if (pair != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                    Text(
                                        pair.letters,
                                        style = MaterialTheme.typography.displayLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (state.revealed) {
                                        val file = viewModel.imageFile(pair.imagePath)
                                        if (file != null) {
                                            AsyncImage(
                                                model = file,
                                                contentDescription = "Obrazek pary",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxWidth().height(220.dp)
                                            )
                                        }
                                        Text(
                                            pair.word.ifBlank { "—" },
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            "Przypomnij sobie skojarzenie…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

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
