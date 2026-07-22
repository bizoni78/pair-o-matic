package com.pairomatic.util

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import com.pairomatic.domain.pairLetterIndices

/**
 * Słowo z pogrubionymi literami pary jako [CharSequence] (Spannable) — do powiadomień i widgetów
 * (android.text). Wersję dla Compose (AnnotatedString) daje `boldPairLetters`. Wspólne indeksy
 * liczy [pairLetterIndices]. Puste słowo → [blankPlaceholder]; brak dopasowania → samo słowo.
 */
fun boldPairLettersSpannable(
    word: String,
    letters: String,
    blankPlaceholder: String
): CharSequence {
    if (word.isBlank()) return blankPlaceholder
    val indices = pairLetterIndices(word, letters)
    if (indices.isEmpty()) return word
    val spannable = SpannableString(word)
    for (i in indices) {
        spannable.setSpan(StyleSpan(Typeface.BOLD), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}
