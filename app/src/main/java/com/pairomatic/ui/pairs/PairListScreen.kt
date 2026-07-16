package com.pairomatic.ui.pairs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pairomatic.data.db.PairEntity
import com.pairomatic.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairListScreen(
    onAddPair: () -> Unit,
    onEditPair: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: PairListViewModel = viewModel(
        factory = viewModelFactory { initializer { PairListViewModel(container.pairRepository) } }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pary (${state.pairs.size})") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPair) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj parę")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Szukaj po literach lub słowie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LevelFilterChip(state.filter, LevelFilter.ALL, "Wszystkie", viewModel)
                LevelFilterChip(state.filter, LevelFilter.NEW, "Nowe", viewModel)
                LevelFilterChip(state.filter, LevelFilter.DONT_KNOW, "Nie znam", viewModel)
                LevelFilterChip(state.filter, LevelFilter.SOSO, "W miarę", viewModel)
                LevelFilterChip(state.filter, LevelFilter.WELL, "Dobrze", viewModel)
                LevelFilterChip(state.filter, LevelFilter.HARD, "Trudne", viewModel)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.pairs, key = { it.id }) { pair ->
                    PairRow(pair, onClick = { onEditPair(pair.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelFilterChip(
    current: LevelFilter,
    value: LevelFilter,
    label: String,
    viewModel: PairListViewModel
) {
    FilterChip(
        selected = current == value,
        onClick = { viewModel.onFilterChange(value) },
        label = { Text(label) }
    )
}

@Composable
private fun PairRow(pair: PairEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = pair.letters,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(pair.word, style = MaterialTheme.typography.bodyLarge)
                Text(levelLabel(pair), style = MaterialTheme.typography.bodySmall)
            }
            if (pair.hardFlag) Text("🚩")
        }
    }
}

private fun levelLabel(pair: PairEntity): String {
    val level = when (pair.level) {
        null -> "nieoceniona"
        0 -> "nie znam"
        1 -> "w miarę"
        2 -> "bardzo dobrze"
        else -> "?"
    }
    return level
}
