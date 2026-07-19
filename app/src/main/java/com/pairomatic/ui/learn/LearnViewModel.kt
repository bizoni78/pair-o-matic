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

/** Kierunek testu: litery→słowo (domyślny) lub słowo→litery. */
enum class LearnDirection { LETTERS_TO_WORD, WORD_TO_LETTERS }

/** Grupa par objęta nauką (zawężenie puli losowania). */
enum class LearnGroup { ALL, NEW, DONT_KNOW, SOSO, WELL, HARD, REVIEW }

data class LearnState(
    val pair: PairEntity? = null,
    val revealed: Boolean = false,
    val loading: Boolean = true,
    val empty: Boolean = false,        // brak jakichkolwiek par w bazie
    val groupEmpty: Boolean = false,   // wybrana grupa/kierunek nie ma par
    val direction: LearnDirection = LearnDirection.LETTERS_TO_WORD,
    val group: LearnGroup = LearnGroup.ALL
)

/**
 * Nauka „na żywo" w aplikacji — ta sama logika co tryb testu w powiadomieniach:
 * pokaż zagadkę, odsłoń odpowiedź, oceń, a po ocenie od razu pojawia się kolejna para.
 * Można zawęzić pulę do wybranej grupy oraz odwrócić kierunek (słowo → litery).
 * Cooldown wyłączony (chcemy ciągłego strumienia), świeżo pokazana para jest tylko
 * wykluczana z najbliższego losowania.
 */
class LearnViewModel(private val repository: PairRepository) : ViewModel() {

    private val config = SelectionConfig.DEFAULT.copy(cooldownMillis = 0L)

    private var direction = LearnDirection.LETTERS_TO_WORD
    private var group = LearnGroup.ALL

    private val _state = MutableStateFlow(LearnState())
    val state: StateFlow<LearnState> = _state.asStateFlow()

    init {
        loadNext(null)
    }

    fun reveal() {
        _state.value = _state.value.copy(revealed = true)
    }

    fun setDirection(value: LearnDirection) {
        if (value == direction) return
        direction = value
        val current = _state.value.pair
        // Jeśli bieżąca para nie nadaje się do nowego kierunku (słowo→litery bez słowa),
        // dobierz nową; w innym wypadku tylko odwracamy widok tej samej pary.
        if (value == LearnDirection.WORD_TO_LETTERS && (current == null || current.word.isBlank())) {
            loadNext(null)
        } else {
            _state.value = _state.value.copy(revealed = false, direction = value)
        }
    }

    fun setGroup(value: LearnGroup) {
        if (value == group) return
        group = value
        loadNext(null)
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
        val all = repository.getAllPairs()
        if (all.isEmpty()) {
            _state.value = LearnState(loading = false, empty = true, direction = direction, group = group)
            return
        }
        var pool = applyGroup(all, group)
        // W kierunku słowo→litery zagadką jest słowo — pary bez słowa nie mają sensu.
        if (direction == LearnDirection.WORD_TO_LETTERS) {
            pool = pool.filter { it.word.isNotBlank() }
        }
        if (pool.isEmpty()) {
            _state.value = LearnState(loading = false, groupEmpty = true, direction = direction, group = group)
            return
        }
        val exclude = excludeId?.let { setOf(it) } ?: emptySet()
        val next = SelectionEngine.pickNext(pool, System.currentTimeMillis(), config, exclude)
            ?: pool.random()
        _state.value = LearnState(
            pair = next, revealed = false, loading = false,
            direction = direction, group = group
        )
    }

    private fun applyGroup(pairs: List<PairEntity>, group: LearnGroup): List<PairEntity> = when (group) {
        LearnGroup.ALL -> pairs
        LearnGroup.NEW -> pairs.filter { it.level == null }
        LearnGroup.DONT_KNOW -> pairs.filter { it.level == 0 }
        LearnGroup.SOSO -> pairs.filter { it.level == 1 }
        LearnGroup.WELL -> pairs.filter { it.level == 2 }
        LearnGroup.HARD -> pairs.filter { it.hardFlag }
        LearnGroup.REVIEW -> pairs.filter { it.reviewFlag }
    }
}
