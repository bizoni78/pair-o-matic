package com.pairomatic.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.data.PairRepository
import com.pairomatic.data.db.PairEntity
import com.pairomatic.data.settings.SettingsRepository
import com.pairomatic.domain.SelectionConfig
import com.pairomatic.domain.SelectionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Kierunek testu: litery→słowo (domyślny) lub słowo→litery. */
enum class LearnDirection { LETTERS_TO_WORD, WORD_TO_LETTERS }

/** Grupa par objęta nauką (zawężenie puli losowania). MISTAKES = powtórka błędów. */
enum class LearnGroup { ALL, MISTAKES, NEW, DONT_KNOW, SOSO, WELL, HARD, REVIEW }

/** Wynik sprawdzenia wpisanej odpowiedzi. */
enum class AnswerResult { CORRECT, INCORRECT }

data class LearnState(
    val pair: PairEntity? = null,
    val revealed: Boolean = false,
    val loading: Boolean = true,
    val empty: Boolean = false,        // brak jakichkolwiek par w bazie
    val groupEmpty: Boolean = false,   // wybrana grupa/kierunek nie ma par
    val direction: LearnDirection = LearnDirection.LETTERS_TO_WORD,
    val group: LearnGroup = LearnGroup.ALL,
    val inputMode: Boolean = false,           // wpisywanie odpowiedzi zamiast samooceny
    val answerResult: AnswerResult? = null    // wynik po sprawdzeniu wpisanej odpowiedzi
)

/**
 * Nauka „na żywo" w aplikacji — ta sama logika co tryb testu w powiadomieniach:
 * pokaż zagadkę, odsłoń odpowiedź, oceń, a po ocenie od razu pojawia się kolejna para.
 * Można zawęzić pulę do wybranej grupy, odwrócić kierunek (słowo → litery) oraz przełączyć
 * się na wpisywanie odpowiedzi (automatyczna ocena). Cooldown wyłączony (ciągły strumień).
 */
class LearnViewModel(
    private val repository: PairRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val config = SelectionConfig.DEFAULT.copy(cooldownMillis = 0L)

    private var direction = LearnDirection.LETTERS_TO_WORD
    private var group = LearnGroup.ALL
    private var inputMode = false

    private val _state = MutableStateFlow(LearnState())
    val state: StateFlow<LearnState> = _state.asStateFlow()

    // Jednorazowe zdarzenie: osiągnięto cel dzienny (do animacji/konfetti). Licznik = trigger.
    private val _celebrate = MutableStateFlow(0)
    val celebrate: StateFlow<Int> = _celebrate.asStateFlow()
    private var lastTodayCount = -1

    init {
        loadNext(null)
        viewModelScope.launch {
            settingsRepository.progress.collect { p ->
                // Świętujemy tylko moment przekroczenia celu (z goal-1 na goal), nie każdą ocenę.
                if (p.dailyGoal > 0 && lastTodayCount in 0 until p.dailyGoal && p.today >= p.dailyGoal) {
                    _celebrate.value += 1
                }
                lastTodayCount = p.today
            }
        }
    }

    fun reveal() {
        _state.value = _state.value.copy(revealed = true)
    }

    fun setDirection(value: LearnDirection) {
        if (value == direction) return
        direction = value
        val current = _state.value.pair
        if (value == LearnDirection.WORD_TO_LETTERS && (current == null || current.word.isBlank())) {
            loadNext(null)
        } else {
            _state.value = _state.value.copy(revealed = false, answerResult = null, direction = value)
        }
    }

    fun setGroup(value: LearnGroup) {
        if (value == group) return
        group = value
        loadNext(null)
    }

    fun setInputMode(value: Boolean) {
        if (value == inputMode) return
        inputMode = value
        _state.value = _state.value.copy(inputMode = value, revealed = false, answerResult = null)
    }

    /** Sprawdza wpisaną odpowiedź, zapisuje ocenę (poprawnie=2, błędnie=0) i odsłania kartę. */
    fun submitAnswer(text: String) {
        val current = _state.value.pair ?: return
        val expected = if (direction == LearnDirection.LETTERS_TO_WORD) current.word else current.letters
        val correct = expected.isNotBlank() && normalize(text) == normalize(expected)
        viewModelScope.launch {
            repository.grade(current.id, if (correct) 2 else 0, System.currentTimeMillis())
            settingsRepository.recordGrade()
            _state.value = _state.value.copy(
                revealed = true,
                answerResult = if (correct) AnswerResult.CORRECT else AnswerResult.INCORRECT
            )
        }
    }

    private fun normalize(s: String): String = s.trim().replace(" ", "").lowercase()

    /** Zapisuje ocenę (0/1/2) bieżącej pary i pokazuje kolejną (tryb samooceny). */
    fun grade(level: Int) {
        val current = _state.value.pair ?: return
        viewModelScope.launch {
            repository.grade(current.id, level, System.currentTimeMillis())
            settingsRepository.recordGrade()
            loadNextSuspend(excludeId = current.id)
        }
    }

    /** „Dalej"/„Następna" bez zmiany oceny — pokazuje kolejną parę. */
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
            _state.value = LearnState(loading = false, empty = true, direction = direction, group = group, inputMode = inputMode)
            return
        }
        var pool = applyGroup(all, group)
        if (direction == LearnDirection.WORD_TO_LETTERS) {
            pool = pool.filter { it.word.isNotBlank() }
        }
        if (pool.isEmpty()) {
            _state.value = LearnState(loading = false, groupEmpty = true, direction = direction, group = group, inputMode = inputMode)
            return
        }
        val exclude = excludeId?.let { setOf(it) } ?: emptySet()
        val next = SelectionEngine.pickNext(pool, System.currentTimeMillis(), config, exclude)
            ?: pool.random()
        _state.value = LearnState(
            pair = next, revealed = false, loading = false,
            direction = direction, group = group, inputMode = inputMode, answerResult = null
        )
    }

    private fun applyGroup(pairs: List<PairEntity>, group: LearnGroup): List<PairEntity> = when (group) {
        LearnGroup.ALL -> pairs
        LearnGroup.MISTAKES -> pairs.filter { it.level == 0 || it.hardFlag }
        LearnGroup.NEW -> pairs.filter { it.level == null }
        LearnGroup.DONT_KNOW -> pairs.filter { it.level == 0 }
        LearnGroup.SOSO -> pairs.filter { it.level == 1 }
        LearnGroup.WELL -> pairs.filter { it.level == 2 }
        LearnGroup.HARD -> pairs.filter { it.hardFlag }
        LearnGroup.REVIEW -> pairs.filter { it.reviewFlag }
    }
}
