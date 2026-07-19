package com.pairomatic.ui.pairs

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.data.PairRepository
import com.pairomatic.data.db.PairEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairEditState(
    val id: Long = 0,
    val letters: String = "",
    val word: String = "",
    val imagePath: String? = null,
    val level: Int? = null,
    val lastSeen: Long? = null,
    val hardFlag: Boolean = false,
    val reviewFlag: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val saved: Boolean = false
)

class PairEditViewModel(
    private val repository: PairRepository,
    private val pairId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(PairEditState(id = pairId))
    val state: StateFlow<PairEditState> = _state.asStateFlow()

    init {
        if (pairId == 0L) {
            _state.value = _state.value.copy(loading = false)
        } else {
            viewModelScope.launch {
                val pair = repository.getById(pairId)
                _state.value = if (pair == null) {
                    PairEditState(loading = false, error = "Nie znaleziono pary")
                } else {
                    PairEditState(
                        id = pair.id,
                        letters = pair.letters,
                        word = pair.word,
                        imagePath = pair.imagePath,
                        level = pair.level,
                        lastSeen = pair.lastSeen,
                        hardFlag = pair.hardFlag,
                        reviewFlag = pair.reviewFlag,
                        loading = false
                    )
                }
            }
        }
    }

    fun onLettersChange(value: String) { _state.value = _state.value.copy(letters = value) }
    fun onWordChange(value: String) { _state.value = _state.value.copy(word = value) }
    fun onHardFlagChange(value: Boolean) { _state.value = _state.value.copy(hardFlag = value) }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val fileName = runCatching { repository.copyImageFromUri(uri) }.getOrNull()
            if (fileName != null) _state.value = _state.value.copy(imagePath = fileName)
            else _state.value = _state.value.copy(error = "Nie udało się wczytać obrazka")
        }
    }

    fun imageFile() = _state.value.imagePath?.let { repository.imageFile(it) }

    fun save() {
        val s = _state.value
        if (s.letters.isBlank()) {
            _state.value = s.copy(error = "Litery nie mogą być puste")
            return
        }
        viewModelScope.launch {
            val entity = PairEntity(
                id = s.id,
                letters = s.letters.trim(),
                word = s.word.trim(),
                imagePath = s.imagePath,
                level = s.level,
                lastSeen = s.lastSeen,
                hardFlag = s.hardFlag,
                reviewFlag = s.reviewFlag
            )
            val result = runCatching { repository.upsert(entity) }
            _state.value = if (result.isSuccess) {
                s.copy(saved = true, error = null)
            } else {
                s.copy(error = "Para o tych literach już istnieje")
            }
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) { _state.value = s.copy(saved = true); return }
        viewModelScope.launch {
            runCatching {
                repository.delete(
                    PairEntity(
                        id = s.id, letters = s.letters, word = s.word,
                        imagePath = s.imagePath, level = s.level,
                        lastSeen = s.lastSeen, hardFlag = s.hardFlag,
                        reviewFlag = s.reviewFlag
                    )
                )
            }
            _state.value = s.copy(saved = true)
        }
    }
}
