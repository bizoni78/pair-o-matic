package com.pairomatic.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Zwraca słowo z pogrubionymi literami pary. Dla każdej litery pary (po kolei) pogrubia jej
 * pierwsze wystąpienie w słowie, bez nakładania się (np. `CT` + „Cytryna" → **C**y**t**ryna).
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

/** Indeksy w słowie, które odpowiadają kolejnym literom pary (pierwsze wystąpienia, bez nakładania). */
fun pairLetterIndices(word: String, letters: String): Set<Int> {
    if (word.isEmpty() || letters.isEmpty()) return emptySet()
    val result = LinkedHashSet<Int>()
    var from = 0
    for (ch in letters) {
        val idx = word.indexOf(ch, startIndex = from, ignoreCase = true)
        if (idx >= 0) {
            result.add(idx)
            from = idx + 1
        }
    }
    return result
}
