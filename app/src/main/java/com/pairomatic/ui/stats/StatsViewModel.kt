package com.pairomatic.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.data.PairRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsState(
    val total: Int = 0,
    val veryWell: Int = 0,
    val soso: Int = 0,
    val dontKnow: Int = 0,
    val hard: Int = 0,
    val neverGraded: Int = 0
)

class StatsViewModel(repository: PairRepository) : ViewModel() {

    val state: StateFlow<StatsState> = repository.observeAll()
        .map { pairs ->
            StatsState(
                total = pairs.size,
                veryWell = pairs.count { it.level == 2 },
                soso = pairs.count { it.level == 1 },
                dontKnow = pairs.count { it.level == 0 },
                hard = pairs.count { it.hardFlag },
                neverGraded = pairs.count { it.level == null }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}
