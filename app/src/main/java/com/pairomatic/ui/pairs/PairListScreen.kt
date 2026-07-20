package com.pairomatic.ui.pairs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.pairomatic.data.db.PairEntity
import com.pairomatic.ui.components.AppTopBar
import com.pairomatic.ui.components.BrandFilterChip
import com.pairomatic.ui.components.boldPairLetters
import com.pairomatic.ui.rememberAppContainer
import com.pairomatic.ui.theme.BrandAmber
import com.pairomatic.ui.theme.BrandBlue
import com.pairomatic.ui.theme.BrandGreen
import com.pairomatic.ui.theme.BrandRed
import com.pairomatic.ui.theme.letterColor

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
    val undo by viewModel.undo.collectAsStateWithLifecycle()
    val selectionActive = state.selection.active

    // Para, dla której otwarto szybką edycję słowa (null = dialog zamknięty).
    var editWordFor by remember { mutableStateOf<PairEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(undo.id) {
        if (undo.id > 0L) {
            val result = snackbarHostState.showSnackbar(
                message = "Usunięto ${undo.count} ${pairCountWord(undo.count)}",
                actionLabel = "Cofnij"
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectionActive) {
                SelectionTopBar(
                    count = state.selection.ids.size,
                    onClose = viewModel::clearSelection,
                    onReview = { viewModel.bulkReview(true) },
                    onUnreview = { viewModel.bulkReview(false) },
                    onHard = { viewModel.bulkHard(true) },
                    onUnhard = { viewModel.bulkHard(false) },
                    onDelete = viewModel::bulkDelete
                )
            } else {
                AppTopBar("Pary (${state.pairs.size})")
            }
        },
        floatingActionButton = {
            if (!selectionActive) {
                FloatingActionButton(
                    onClick = onAddPair,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj parę")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!selectionActive) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Szukaj po literach lub słowie") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
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
                    LevelFilterChip(state.filter, LevelFilter.NO_WORD, "Bez słowa", viewModel)
                    LevelFilterChip(state.filter, LevelFilter.NO_IMAGE, "Bez obrazka", viewModel)
                    LevelFilterChip(state.filter, LevelFilter.REVIEW, "Do zmiany", viewModel)
                }

                SortRow(current = state.sort, onSortChange = viewModel::onSortChange)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.pairs, key = { it.id }) { pair ->
                    PairRow(
                        pair = pair,
                        selectionActive = selectionActive,
                        selected = pair.id in state.selection.ids,
                        imageFile = viewModel.imageFile(pair.imagePath),
                        onOpen = {
                            if (selectionActive) viewModel.toggleSelection(pair.id)
                            else onEditPair(pair.id)
                        },
                        onLongPress = {
                            if (selectionActive) viewModel.toggleSelection(pair.id)
                            else viewModel.enterSelection(pair.id)
                        },
                        onToggleReview = { viewModel.onToggleReview(pair) },
                        onToggleHard = { viewModel.onToggleHard(pair) },
                        onEditWord = { editWordFor = pair },
                        onDelete = { viewModel.onDeleteOne(pair) }
                    )
                }
            }
        }
    }

    editWordFor?.let { pair ->
        QuickWordDialog(
            pair = pair,
            onDismiss = { editWordFor = null },
            onSave = { newWord ->
                viewModel.onUpdateWord(pair.id, newWord)
                editWordFor = null
            }
        )
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onReview: () -> Unit,
    onUnreview: () -> Unit,
    onHard: () -> Unit,
    onUnhard: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppTopBar("Zaznaczono $count") {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Zakończ zaznaczanie")
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Akcje dla zaznaczonych")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Oznacz: do zmiany") },
                    onClick = { menuOpen = false; onReview() }
                )
                DropdownMenuItem(
                    text = { Text("Zdejmij: do zmiany") },
                    onClick = { menuOpen = false; onUnreview() }
                )
                DropdownMenuItem(
                    text = { Text("Oznacz: trudne") },
                    onClick = { menuOpen = false; onHard() }
                )
                DropdownMenuItem(
                    text = { Text("Zdejmij: trudne") },
                    onClick = { menuOpen = false; onUnhard() }
                )
                DropdownMenuItem(
                    text = { Text("Usuń zaznaczone") },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun SortRow(current: SortOrder, onSortChange: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            TextButton(onClick = { open = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("  Sortuj: ${sortLabel(current)}")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(sortLabel(order)) },
                        onClick = { open = false; onSortChange(order) }
                    )
                }
            }
        }
    }
}

private fun sortLabel(order: SortOrder): String = when (order) {
    SortOrder.LETTERS -> "litery A→Z"
    SortOrder.WORD -> "słowo A→Z"
    SortOrder.WEAKEST -> "najsłabsze"
    SortOrder.RECENT -> "ostatnio widziane"
}

@Composable
private fun LevelFilterChip(
    current: LevelFilter,
    value: LevelFilter,
    label: String,
    viewModel: PairListViewModel
) {
    BrandFilterChip(
        selected = current == value,
        onClick = { viewModel.onFilterChange(value) },
        label = label
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PairRow(
    pair: PairEntity,
    selectionActive: Boolean,
    selected: Boolean,
    imageFile: java.io.File?,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleReview: () -> Unit,
    onToggleHard: () -> Unit,
    onEditWord: () -> Unit,
    onDelete: () -> Unit
) {
    val highlighted = pair.reviewFlag
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        highlighted -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (highlighted && !selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectionActive) {
                Checkbox(checked = selected, onCheckedChange = { onOpen() })
            }

            val file = imageFile?.takeIf { it.exists() }
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                )
            } else {
                LetterAvatar(pair.letters)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pair.word.isBlank()) AnnotatedString("—")
                    else boldPairLetters(pair.word, pair.letters),
                    style = MaterialTheme.typography.headlineSmall,
                    color = when {
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                        highlighted -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    levelLabel(pair),
                    style = MaterialTheme.typography.bodyMedium,
                    color = levelColor(pair)
                )
            }

            if (pair.hardFlag) Text("🚩", style = MaterialTheme.typography.headlineSmall)

            if (!selectionActive) {
                Checkbox(
                    checked = pair.reviewFlag,
                    onCheckedChange = { onToggleReview() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                )
                RowMenu(
                    hardFlag = pair.hardFlag,
                    onEditWord = onEditWord,
                    onToggleHard = onToggleHard,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun RowMenu(
    hardFlag: Boolean,
    onEditWord: () -> Unit,
    onToggleHard: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Więcej akcji")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Edytuj słowo") },
                onClick = { open = false; onEditWord() }
            )
            DropdownMenuItem(
                text = { Text(if (hardFlag) "Zdejmij „trudne”" else "Oznacz jako trudne") },
                onClick = { open = false; onToggleHard() }
            )
            DropdownMenuItem(
                text = { Text("Usuń parę") },
                onClick = { open = false; onDelete() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickWordDialog(
    pair: PairEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(pair.word) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Słowo dla „${pair.letters}”") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("np. Cytryna") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun LetterAvatar(letters: String) {
    val base = letterColor(letters)
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                brush = Brush.linearGradient(listOf(base, base.copy(alpha = 0.65f))),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letters,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Polska odmiana: 1 para, 2–4 pary, 5+ par (z wyjątkiem 12–14). */
private fun pairCountWord(n: Int): String {
    val lastTwo = n % 100
    val last = n % 10
    return when {
        n == 1 -> "parę"
        last in 2..4 && lastTwo !in 12..14 -> "pary"
        else -> "par"
    }
}

private fun levelLabel(pair: PairEntity): String = when (pair.level) {
    null -> "nieoceniona"
    0 -> "nie znam"
    1 -> "w miarę"
    2 -> "znam bardzo dobrze"
    else -> "?"
}

private fun levelColor(pair: PairEntity): Color = when (pair.level) {
    0 -> BrandRed
    1 -> BrandAmber
    2 -> BrandGreen
    else -> BrandBlue
}
