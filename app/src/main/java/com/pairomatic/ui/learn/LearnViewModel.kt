package com.pairomatic.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.data.PairRepository
import com.pairomatic.data.db.PairEntity
import com.pairomatic.domain.SelectionConfig
import com.pairomatic.domain.SelectionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class LearnState(
    val pair: PairEntity? = null,
    val revealed: Boolean = false,
    val loading: Boolean = true,
    val empty: Boolean = false
)

/**
 * Nauka „na żywo" w aplikacji — ta sama logika co tryb testu w powiadomieniach:
 * pokaż parę, odsłoń odpowiedź, oceń, a po ocenie od razu pojawia się kolejna para.
 * Cooldown wyłączony (w aplikacji chcemy ciągłego strumienia), świeżo pokazana para
 * jest tylko wykluczana z najbliższego losowania.
 */
class LearnViewModel(private val repository: PairRepository) : ViewModel() {

    private val config = SelectionConfig.DEFAULT.copy(cooldownMillis = 0L)

    private val _state = MutableStateFlow(LearnState())
    val state: StateFlow<LearnState> = _state.asStateFlow()

    init {
        loadNext(null)
    }

    fun reveal() {
        _state.value = _state.value.copy(revealed = true)
    }

    /** Zapisuje ocenę (0/1/2) bieżącej pary i pokazuje kolejną. */
    fun grade(level: Int) {
        val current = _state.value.pair ?: return
        viewModelScope.launch {
            repository.grade(current.id, level, System.currentTimeMillis())
            loadNextSuspend(excludeId = current.id)
        }
    }

    /** „Dalej" bez oceny — pokazuje kolejną parę, nie zmieniając oceny bieżącej. */
    fun skip() {
        val current = _state.value.pair
        loadNext(excludeId = current?.id)
    }

    fun imageFile(name: String?): File? = name?.let { repository.imageFile(it) }

    private fun loadNext(excludeId: Long?) {
        viewModelScope.launch { loadNextSuspend(excludeId) }
    }

    private suspend fun loadNextSuspend(excludeId: Long?) {
        val pairs = repository.getAllPairs()
        if (pairs.isEmpty()) {
            _state.value = LearnState(loading = false, empty = true)
            return
        }
        val exclude = excludeId?.let { setOf(it) } ?: emptySet()
        val next = SelectionEngine.pickNext(pairs, System.currentTimeMillis(), config, exclude)
            ?: pairs.random()
        _state.value = LearnState(pair = next, revealed = false, loading = false, empty = false)
    }
}
