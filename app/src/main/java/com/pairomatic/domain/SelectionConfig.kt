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
    val recencyCapHours: Double = 24.0,
    /**
     * Maks. liczba par nigdy nieocenionych (`level == null`) dopuszczonych naraz do losowania.
     * Chroni przed zalaniem rotacji wszystkimi nowymi parami przy dużej talii. `null` = brak limitu.
     * Dopuszczane są najstarsze (po `id`) nowe pary; kolejne wchodzą, gdy wcześniejsze zostaną ocenione.
     */
    val newPairLimit: Int? = 30
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
