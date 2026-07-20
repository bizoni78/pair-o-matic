package com.pairomatic.ui.pairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.data.PairRepository
import com.pairomatic.data.db.PairEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Filtr listy par (null = wszystkie). NO_WORD = pary bez słowa, NO_IMAGE = pary bez obrazka,
 * REVIEW = oznaczone „do zmiany".
 */
enum class LevelFilter { ALL, NEW, DONT_KNOW, SOSO, WELL, HARD, NO_WORD, NO_IMAGE, REVIEW }

/** Sposób sortowania listy par. */
enum class SortOrder { LETTERS, WEAKEST, RECENT, WORD }

/** Stan trybu zaznaczania wielu par (akcje masowe). */
data class SelectionUi(val active: Boolean = false, val ids: Set<Long> = emptySet())

/** Jednorazowe zdarzenie „usunięto — można cofnąć" (do paska Snackbar). */
data class UndoState(val id: Long = 0, val count: Int = 0)

data class PairListState(
    val query: String = "",
    val filter: LevelFilter = LevelFilter.ALL,
    val sort: SortOrder = SortOrder.LETTERS,
    val pairs: List<PairEntity> = emptyList(),
    val selection: SelectionUi = SelectionUi()
)

class PairListViewModel(private val repository: PairRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LevelFilter.ALL)
    private val sort = MutableStateFlow(SortOrder.LETTERS)
    private val selection = MutableStateFlow(SelectionUi())

    // „Cofnij": ostatnio usunięte pary (wiersze skasowane, pliki obrazków zachowane do czasu undo).
    private val _undo = MutableStateFlow(UndoState())
    val undo: StateFlow<UndoState> = _undo
    private var pendingDeleted: List<PairEntity> = emptyList()
    private var undoCounter = 0L

    val state: StateFlow<PairListState> =
        combine(repository.observeAll(), query, filter, sort, selection) { all, q, f, s, sel ->
            val filtered = all.asSequence()
                .filter { pair ->
                    q.isBlank() ||
                        pair.letters.contains(q, ignoreCase = true) ||
                        pair.word.contains(q, ignoreCase = true)
                }
                .filter { pair ->
                    when (f) {
                        LevelFilter.ALL -> true
                        LevelFilter.NEW -> pair.level == null
                        LevelFilter.DONT_KNOW -> pair.level == 0
                        LevelFilter.SOSO -> pair.level == 1
                        LevelFilter.WELL -> pair.level == 2
                        LevelFilter.HARD -> pair.hardFlag
                        LevelFilter.NO_WORD -> pair.word.isBlank()
                        LevelFilter.NO_IMAGE -> pair.imagePath.isNullOrBlank()
                        LevelFilter.REVIEW -> pair.reviewFlag
                    }
                }
                .toList()
            val sorted = when (s) {
                SortOrder.LETTERS -> filtered.sortedBy { it.letters.lowercase() }
                SortOrder.WORD -> filtered.sortedBy { it.word.lowercase() }
                SortOrder.WEAKEST -> filtered.sortedWith(
                    compareBy({ weaknessRank(it) }, { it.letters.lowercase() })
                )
                SortOrder.RECENT -> filtered.sortedByDescending { it.lastSeen ?: Long.MIN_VALUE }
            }
            // Zaznaczenie ograniczamy do par nadal widocznych (po zmianie filtra/wyszukiwania).
            val visibleIds = sorted.mapTo(HashSet()) { it.id }
            val prunedIds = sel.ids.intersect(visibleIds)
            val prunedSel = when {
                !sel.active -> sel
                prunedIds.isEmpty() -> SelectionUi()
                else -> sel.copy(ids = prunedIds)
            }
            PairListState(q, f, s, sorted, prunedSel)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PairListState())

    /** Ranking „słabości" pary do sortowania „najsłabsze na górze". */
    private fun weaknessRank(pair: PairEntity): Int = when {
        pair.level == 0 -> 0          // „nie znam" — najpilniejsze
        pair.level == null -> 1       // jeszcze nieoceniona
        pair.level == 1 -> 2          // „w miarę"
        else -> 3                     // „bardzo dobrze"
    }

    fun onQueryChange(value: String) { query.value = value }
    fun onFilterChange(value: LevelFilter) { filter.value = value }
    fun onSortChange(value: SortOrder) { sort.value = value }

    fun imageFile(name: String?): File? = name?.let { repository.imageFile(it) }

    // --- Pojedyncze akcje wiersza ---

    /** Przełącza flagę „słowo do zmiany" dla danej pary. */
    fun onToggleReview(pair: PairEntity) {
        viewModelScope.launch { repository.setReviewFlag(pair.id, !pair.reviewFlag) }
    }

    /** Przełącza flagę „nie wchodzi do głowy". */
    fun onToggleHard(pair: PairEntity) {
        viewModelScope.launch { repository.setHardFlag(pair.id, !pair.hardFlag) }
    }

    /** Szybka zmiana samego słowa (z dialogu na liście). */
    fun onUpdateWord(id: Long, word: String) {
        viewModelScope.launch { repository.updateWord(id, word.trim()) }
    }

    fun onDeleteOne(pair: PairEntity) {
        deleteWithUndo(listOf(pair))
    }

    /** Usuwa wiersze i wystawia zdarzenie „Cofnij"; pliki obrazków zostają do czasu undo. */
    private fun deleteWithUndo(pairs: List<PairEntity>) {
        if (pairs.isEmpty()) return
        pendingDeleted = pairs
        viewModelScope.launch { repository.deleteRowsOnly(pairs) }
        undoCounter += 1
        _undo.value = UndoState(id = undoCounter, count = pairs.size)
    }

    /** Przywraca ostatnio usunięte pary. */
    fun undoDelete() {
        val toRestore = pendingDeleted
        pendingDeleted = emptyList()
        if (toRestore.isNotEmpty()) {
            viewModelScope.launch { repository.restorePairs(toRestore) }
        }
    }

    // --- Tryb zaznaczania wielu par ---

    fun enterSelection(id: Long) { selection.value = SelectionUi(active = true, ids = setOf(id)) }

    fun toggleSelection(id: Long) {
        val cur = selection.value
        val ids = if (id in cur.ids) cur.ids - id else cur.ids + id
        selection.value = if (ids.isEmpty()) SelectionUi() else SelectionUi(active = true, ids = ids)
    }

    fun clearSelection() { selection.value = SelectionUi() }

    fun bulkReview(flag: Boolean) {
        val ids = selection.value.ids.toList()
        viewModelScope.launch { repository.setReviewFlagMany(ids, flag) }
        clearSelection()
    }

    fun bulkHard(flag: Boolean) {
        val ids = selection.value.ids.toList()
        viewModelScope.launch { repository.setHardFlagMany(ids, flag) }
        clearSelection()
    }

    fun bulkDelete() {
        val ids = selection.value.ids
        val toDelete = state.value.pairs.filter { it.id in ids }
        clearSelection()
        deleteWithUndo(toDelete)
    }
}
