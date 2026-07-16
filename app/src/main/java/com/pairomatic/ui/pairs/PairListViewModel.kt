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

/** Filtr po poziomie znajomości (null = wszystkie). */
enum class LevelFilter { ALL, NEW, DONT_KNOW, SOSO, WELL, HARD }

data class PairListState(
    val query: String = "",
    val filter: LevelFilter = LevelFilter.ALL,
    val pairs: List<PairEntity> = emptyList()
)

class PairListViewModel(repository: PairRepository) : ViewModel() {

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
                    }
                }
                .toList()
            PairListState(q, f, filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PairListState())

    fun onQueryChange(value: String) { query.value = value }
    fun onFilterChange(value: LevelFilter) { filter.value = value }
}
