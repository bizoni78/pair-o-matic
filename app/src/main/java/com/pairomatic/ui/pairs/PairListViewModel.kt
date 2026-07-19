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

/** Filtr listy par (null = wszystkie). NO_WORD = pary bez słowa, REVIEW = oznaczone „do zmiany". */
enum class LevelFilter { ALL, NEW, DONT_KNOW, SOSO, WELL, HARD, NO_WORD, REVIEW }

data class PairListState(
    val query: String = "",
    val filter: LevelFilter = LevelFilter.ALL,
    val pairs: List<PairEntity> = emptyList()
)

class PairListViewModel(private val repository: PairRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LevelFilter.ALL)

    val state: StateFlow<PairListState> =
        combine(repository.observeAll(), query, filter) { all, q, f ->
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
                        LevelFilter.REVIEW -> pair.reviewFlag
                    }
                }
                .toList()
            PairListState(q, f, filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PairListState())

    fun onQueryChange(value: String) { query.value = value }
    fun onFilterChange(value: LevelFilter) { filter.value = value }

    /** Przełącza flagę „słowo do zmiany" dla danej pary. */
    fun onToggleReview(pair: PairEntity) {
        viewModelScope.launch { repository.setReviewFlag(pair.id, !pair.reviewFlag) }
    }
}
