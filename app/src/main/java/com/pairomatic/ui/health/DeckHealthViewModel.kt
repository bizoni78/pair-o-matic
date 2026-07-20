package com.pairomatic.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.data.PairRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Grupa par mających identyczne słowo (potencjalny duplikat do rozdzielenia). */
data class DuplicateWordGroup(val word: String, val letters: List<String>)

data class DeckHealthState(
    val loading: Boolean = true,
    val total: Int = 0,
    val duplicateWords: List<DuplicateWordGroup> = emptyList(),
    val noWord: Int = 0,
    val noImage: Int = 0,
    val orphanImages: Int = 0,
    val message: String? = null
)

/**
 * „Zdrowie talii" — wykrywa problemy przy dużej bazie: powtórzone słowa, braki słowa/obrazka
 * oraz osierocone pliki obrazków (do których nie odwołuje się żadna para).
 */
class DeckHealthViewModel(private val repository: PairRepository) : ViewModel() {

    private val _state = MutableStateFlow(DeckHealthState())
    val state: StateFlow<DeckHealthState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val pairs = repository.getAllPairs()
            val duplicates = pairs
                .filter { it.word.isNotBlank() }
                .groupBy { it.word.trim().lowercase() }
                .filter { it.value.size > 1 }
                .map { (_, group) -> DuplicateWordGroup(group.first().word, group.map { it.letters }) }
                .sortedByDescending { it.letters.size }
            _state.value = DeckHealthState(
                loading = false,
                total = pairs.size,
                duplicateWords = duplicates,
                noWord = pairs.count { it.word.isBlank() },
                noImage = pairs.count { it.imagePath.isNullOrBlank() },
                orphanImages = repository.countOrphanImages(),
                message = _state.value.message
            )
        }
    }

    fun cleanOrphanImages() {
        viewModelScope.launch {
            val removed = repository.deleteOrphanImages()
            _state.value = _state.value.copy(
                message = if (removed > 0) "Usunięto $removed niepotrzebnych plików" else "Brak plików do usunięcia"
            )
            refresh()
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
