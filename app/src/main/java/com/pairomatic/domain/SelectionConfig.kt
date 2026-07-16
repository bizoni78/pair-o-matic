package com.pairomatic.domain

/**
 * Strojalne stałe algorytmu doboru pary. Wartości domyślne pochodzą ze specyfikacji.
 */
data class SelectionConfig(
    val levelWeightNull: Double = 8.0,
    val levelWeight0: Double = 10.0,
    val levelWeight1: Double = 4.0,
    val levelWeight2: Double = 1.0,
    val hardFlagBoost: Double = 3.0,
    val cooldownMillis: Long = 30L * 60L * 1000L,
    val recencyCapHours: Double = 24.0
) {
    fun levelWeight(level: Int?): Double = when (level) {
        0 -> levelWeight0
        1 -> levelWeight1
        2 -> levelWeight2
        else -> levelWeightNull // null oraz wartości nieoczekiwane
    }

    companion object {
        val DEFAULT = SelectionConfig()
    }
}
