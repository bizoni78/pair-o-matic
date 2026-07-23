package com.pairomatic.domain

import com.pairomatic.data.db.PairWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class WeightedPickerTest {

    @Test fun `pusta lista zwraca null`() {
        assertNull(WeightedPicker.pick(emptyList()))
    }

    @Test fun `same nie-dodatnie wagi zwracaja null`() {
        val candidates = listOf(PairWeight(1, 0.0), PairWeight(2, -3.0))
        assertNull(WeightedPicker.pick(candidates))
    }

    @Test fun `jedyny dodatni kandydat jest zawsze wybierany`() {
        val candidates = listOf(PairWeight(1, 0.0), PairWeight(7, 5.0), PairWeight(3, 0.0))
        repeat(50) { assertEquals(7L, WeightedPicker.pick(candidates, Random(it.toLong()))) }
    }

    @Test fun `rozklad jest proporcjonalny do wag`() {
        // Wagi 1:3 → oczekiwane ~25% / ~75%.
        val candidates = listOf(PairWeight(1, 1.0), PairWeight(2, 3.0))
        val rng = Random(12_345)
        val counts = HashMap<Long, Int>()
        val n = 60_000
        repeat(n) {
            val id = WeightedPicker.pick(candidates, rng)!!
            counts.merge(id, 1, Int::plus)
        }
        val p1 = counts.getValue(1L) / n.toDouble()
        val p2 = counts.getValue(2L) / n.toDouble()
        assertTrue("p1=$p1 poza tolerancją", abs(p1 - 0.25) < 0.02)
        assertTrue("p2=$p2 poza tolerancją", abs(p2 - 0.75) < 0.02)
    }

    @Test fun `kandydat z zerowa waga nigdy nie jest wybierany`() {
        val candidates = listOf(PairWeight(1, 0.0), PairWeight(2, 2.0), PairWeight(3, 0.0))
        val rng = Random(999)
        repeat(5_000) {
            assertEquals(2L, WeightedPicker.pick(candidates, rng))
        }
    }
}
