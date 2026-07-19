package com.pairomatic.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.data.PairRepository
import com.pairomatic.data.settings.ProgressStats
import com.pairomatic.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatsState(
    val total: Int = 0,
    val veryWell: Int = 0,
    val soso: Int = 0,
    val dontKnow: Int = 0,
    val hard: Int = 0,
    val neverGraded: Int = 0,
    val noWord: Int = 0,
    val review: Int = 0
)

class StatsViewModel(
    repository: PairRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val state: StateFlow<StatsState> = repository.observeAll()
        .map { pairs ->
            StatsState(
                total = pairs.size,
                veryWell = pairs.count { it.level == 2 },
                soso = pairs.count { it.level == 1 },
                dontKnow = pairs.count { it.level == 0 },
                hard = pairs.count { it.hardFlag },
                neverGraded = pairs.count { it.level == null },
                noWord = pairs.count { it.word.isBlank() },
                review = pairs.count { it.reviewFlag }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    val progress: StateFlow<ProgressStats> = settingsRepository.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressStats())

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal) }
    }
}
