package com.pairomatic.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.pairomatic.domain.pairLetterIndices

/**
 * Zwraca słowo z pogrubionymi literami pary. Dla każdej litery pary (po kolei) pogrubia jej
 * pierwsze wystąpienie w słowie, bez nakładania się (np. `CT` + „Cytryna" → **C**y**t**ryna).
 * Logika indeksów jest w [pairLetterIndices] (czysta, przetestowana jednostkowo).
 */
fun boldPairLetters(word: String, letters: String): AnnotatedString {
    val boldIndices = pairLetterIndices(word, letters)
    return buildAnnotatedString {
        word.forEachIndexed { index, char ->
            if (index in boldIndices) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(char) }
            } else {
                append(char)
            }
        }
    }
}
