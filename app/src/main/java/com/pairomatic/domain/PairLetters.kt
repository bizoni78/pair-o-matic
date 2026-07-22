package com.pairomatic.domain

/**
 * Indeksy w słowie odpowiadające kolejnym literom pary — pierwsze wystąpienie każdej litery
 * (po kolei, bez nakładania), ignorując wielkość liter. Np. `CT` + „Cytryna" → {0, 2}.
 *
 * Czysta logika (bez zależności od Androida/Compose) — używana w Compose, powiadomieniach i widgecie,
 * łatwa do pokrycia testami jednostkowymi.
 */
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
