package com.pairomatic.ui.pairs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.pairomatic.ui.components.AppTopBar
import com.pairomatic.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairEditScreen(
    pairId: Long,
    onDone: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: PairEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PairEditViewModel(container.pairRepository, pairId) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onImagePicked(uri) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (pairId == 0L) "Nowa para" else "Edycja pary",
                actions = {
                    if (pairId != 0L) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Usuń")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.letters,
                onValueChange = viewModel::onLettersChange,
                label = { Text("Litery (np. CT)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.word,
                onValueChange = viewModel::onWordChange,
                label = { Text("Słowo-obraz (np. Cytryna)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val imageFile = viewModel.imageFile()
            if (imageFile != null) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = "Obrazek pary",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
            FilledTonalButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (imageFile == null) "Wybierz obrazek" else "Zmień obrazek")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nie wchodzi do głowy", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.hardFlag, onCheckedChange = viewModel::onHardFlagChange)
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
            ) {
                Text("Zapisz")
            }
        }
    }
}
