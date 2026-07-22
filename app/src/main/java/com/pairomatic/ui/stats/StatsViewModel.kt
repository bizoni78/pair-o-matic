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
    val noImage: Int = 0,
    val review: Int = 0
)

class StatsViewModel(
    repository: PairRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // PERF-4: liczniki liczone w SQL (COUNT/SUM), bez ładowania całej tabeli do pamięci.
    val state: StateFlow<StatsState> = repository.observeStats()
        .map { c ->
            StatsState(
                total = c.total,
                veryWell = c.veryWell,
                soso = c.soso,
                dontKnow = c.dontKnow,
                hard = c.hard,
                neverGraded = c.neverGraded,
                noWord = c.noWord,
                noImage = c.noImage,
                review = c.review
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    val progress: StateFlow<ProgressStats> = settingsRepository.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressStats())

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal) }
    }
}
