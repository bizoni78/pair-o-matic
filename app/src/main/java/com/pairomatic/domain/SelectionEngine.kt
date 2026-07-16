package com.pairomatic.domain

import com.pairomatic.data.db.PairEntity
import kotlin.math.min
import kotlin.random.Random

/**
 * Dobór następnej pary metodą losowania ważonego. Czysta logika bez zależności od Androida —
 * w całości pokryta testami jednostkowymi.
 *
 * Waga pary = poziom znajomości × świeżość × flaga „trudna", a para w cooldownie ma wagę 0.
 */
object SelectionEngine {

    /** Waga pojedynczej pary. 0 oznacza „poza pulą" (np. świeżo pokazana). */
    fun weight(pair: PairEntity, now: Long, config: SelectionConfig = SelectionConfig.DEFAULT): Double {
        // 1. Cooldown — świeżo oceniona para chwilowo wypada z puli.
        val lastSeen = pair.lastSeen
        if (lastSeen != null && now - lastSeen < config.cooldownMillis) return 0.0

        // 2. Poziom znajomości.
        val base = config.levelWeight(pair.level)
        // 3. Świeżość — im dawniej widziana, tym większa waga (ograniczona z góry).
        val recency = recencyMultiplier(lastSeen, now, config)
        // 4. Ręczna flaga „nie wchodzi do głowy".
        val boost = if (pair.hardFlag) config.hardFlagBoost else 1.0

        return base * recency * boost
    }

    /**
     * Mnożnik świeżości: `1 + min(godziny_od_lastSeen, CAP)`.
     * Para nigdy nieoceniona (lastSeen == null) dostaje maksymalny mnożnik.
     */
    fun recencyMultiplier(lastSeen: Long?, now: Long, config: SelectionConfig): Double {
        if (lastSeen == null) return 1.0 + config.recencyCapHours
        val hours = (now - lastSeen).toDouble() / 3_600_000.0
        return 1.0 + min(hours.coerceAtLeast(0.0), config.recencyCapHours)
    }

    /**
     * Wybiera następną parę z prawdopodobieństwem proporcjonalnym do wagi.
     * Zwraca null, gdy żadna para nie kwalifikuje się do pokazania (pusta lista lub same cooldowny).
     *
     * @param exclude identyfikatory par do pominięcia (np. „ostatnio pokazane" w immersji)
     */
    fun pickNext(
        pairs: List<PairEntity>,
        now: Long,
        config: SelectionConfig = SelectionConfig.DEFAULT,
        exclude: Set<Long> = emptySet(),
        random: Random = Random.Default
    ): PairEntity? {
        val weighted = pairs.asSequence()
            .filter { it.id !in exclude }
            .map { it to weight(it, now, config) }
            .filter { it.second > 0.0 }
            .toList()

        if (weighted.isEmpty()) {
            // Awaryjnie: jeśli wykluczenia wyzerowały pulę, spróbuj bez nich.
            if (exclude.isNotEmpty()) return pickNext(pairs, now, config, emptySet(), random)
            return null
        }

        val total = weighted.sumOf { it.second }
        var threshold = random.nextDouble() * total
        for ((pair, w) in weighted) {
            threshold -= w
            if (threshold <= 0.0) return pair
        }
        return weighted.last().first
    }
}
