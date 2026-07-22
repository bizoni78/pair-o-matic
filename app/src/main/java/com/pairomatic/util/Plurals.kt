package com.pairomatic.util

/**
 * Polska odmiana liczebników: 1 → [one], 2–4 → [few], 5+ → [many]
 * (z wyjątkiem 12–14, które używają formy [many]).
 */
fun plural(n: Int, one: String, few: String, many: String): String {
    val lastTwo = n % 100
    val last = n % 10
    return when {
        n == 1 -> one
        last in 2..4 && lastTwo !in 12..14 -> few
        else -> many
    }
}

/** „dzień" / „dni". */
fun dayWord(n: Int): String = plural(n, "dzień", "dni", "dni")

/** „parę" / „pary" / „par". */
fun pairsWord(n: Int): String = plural(n, "parę", "pary", "par")
