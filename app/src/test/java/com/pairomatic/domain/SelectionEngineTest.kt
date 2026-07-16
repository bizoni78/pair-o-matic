package com.pairomatic.domain

import com.pairomatic.data.db.PairEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SelectionEngineTest {

    private val now = 1_000_000_000_000L
    private val config = SelectionConfig.DEFAULT

    private fun pair(
        id: Long,
        level: Int? = null,
        lastSeen: Long? = null,
        hardFlag: Boolean = false
    ) = PairEntity(id = id, letters = "P$id", word = "word$id", level = level, lastSeen = lastSeen, hardFlag = hardFlag)

    @Test
    fun `pair within cooldown has zero weight`() {
        val fresh = pair(1, level = 0, lastSeen = now - 60_000) // minutę temu
        assertEquals(0.0, SelectionEngine.weight(fresh, now, config), 0.0)
    }

    @Test
    fun `never graded pair is eligible and weighted`() {
        val p = pair(1, level = null, lastSeen = null)
        assertTrue(SelectionEngine.weight(p, now, config) > 0.0)
    }

    @Test
    fun `dont-know weighs more than very-well`() {
        val old = now - 48L * 3_600_000L // dawno, poza cooldownem i poza CAP świeżości
        val dontKnow = pair(1, level = 0, lastSeen = old)
        val veryWell = pair(2, level = 2, lastSeen = old)
        assertTrue(SelectionEngine.weight(dontKnow, now, config) > SelectionEngine.weight(veryWell, now, config))
    }

    @Test
    fun `hard flag boosts weight`() {
        val old = now - 48L * 3_600_000L
        val plain = pair(1, level = 1, lastSeen = old, hardFlag = false)
        val hard = pair(2, level = 1, lastSeen = old, hardFlag = true)
        val ratio = SelectionEngine.weight(hard, now, config) / SelectionEngine.weight(plain, now, config)
        assertEquals(config.hardFlagBoost, ratio, 0.0001)
    }

    @Test
    fun `recency multiplier is capped`() {
        val veryOld = now - 1000L * 3_600_000L // 1000 godzin
        val recent = now - 1L * 3_600_000L
        val capped = SelectionEngine.recencyMultiplier(veryOld, now, config)
        assertEquals(1.0 + config.recencyCapHours, capped, 0.0001)
        assertTrue(SelectionEngine.recencyMultiplier(recent, now, config) < capped)
    }

    @Test
    fun `pickNext returns null for empty list`() {
        assertNull(SelectionEngine.pickNext(emptyList(), now, config))
    }

    @Test
    fun `pickNext returns null when all in cooldown`() {
        val pairs = listOf(
            pair(1, level = 0, lastSeen = now - 60_000),
            pair(2, level = 1, lastSeen = now - 120_000)
        )
        assertNull(SelectionEngine.pickNext(pairs, now, config))
    }

    @Test
    fun `pickNext respects exclusion but falls back when pool empties`() {
        val only = pair(1, level = null)
        // Wykluczamy jedyną parę → fallback ignoruje wykluczenia i i tak ją zwraca.
        val picked = SelectionEngine.pickNext(listOf(only), now, config, exclude = setOf(1L))
        assertNotNull(picked)
        assertEquals(1L, picked!!.id)
    }

    @Test
    fun `weighted distribution favours heavier pairs`() {
        val old = now - 48L * 3_600_000L
        val heavy = pair(1, level = 0, lastSeen = old, hardFlag = true) // duża waga
        val light = pair(2, level = 2, lastSeen = old)                  // mała waga
        val pairs = listOf(heavy, light)
        val random = Random(42)
        var heavyCount = 0
        repeat(2000) {
            if (SelectionEngine.pickNext(pairs, now, config, random = random)?.id == 1L) heavyCount++
        }
        // Waga heavy jest wielokrotnie większa — powinna zdecydowanie dominować.
        assertTrue("heavyCount=$heavyCount", heavyCount > 1500)
    }
}
