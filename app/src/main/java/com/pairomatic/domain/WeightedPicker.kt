package com.pairomatic.domain

import com.pairomatic.data.db.PairWeight
import kotlin.random.Random

/**
 * Czysta, testowalna część doboru ważonego z gotowych wag (PERF-2): wagi liczy SQL,
 * a losowy wybór proporcjonalny do wagi robimy tu. Ta sama logika co w [SelectionEngine],
 * który pozostaje referencją/fallbackiem na pełnej liście encji.
 */
object WeightedPicker {

    /**
     * Wybiera `id` z prawdopodobieństwem proporcjonalnym do wagi.
     * Zwraca null, gdy brak kandydatów z dodatnią wagą.
     */
    fun pick(candidates: List<PairWeight>, random: Random = Random.Default): Long? {
        val positive = candidates.filter { it.weight > 0.0 }
        if (positive.isEmpty()) return null

        val total = positive.sumOf { it.weight }
        var threshold = random.nextDouble() * total
        for (c in positive) {
            threshold -= c.weight
            if (threshold <= 0.0) return c.id
        }
        return positive.last().id
    }
}
