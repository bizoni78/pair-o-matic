package com.pairomatic.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerRulesTest {

    private fun m(h: Int, min: Int = 0) = h * 60 + min

    // --- Okno przez północ (typowe: 22:00–07:00) ---

    @Test
    fun `overnight window includes late night and early morning`() {
        val start = m(22)
        val end = m(7)
        assertTrue(SchedulerRules.isWithinQuietHours(m(23), start, end))   // 23:00
        assertTrue(SchedulerRules.isWithinQuietHours(m(2), start, end))    // 02:00
        assertTrue(SchedulerRules.isWithinQuietHours(m(6, 59), start, end)) // 06:59
    }

    @Test
    fun `overnight window excludes daytime`() {
        val start = m(22)
        val end = m(7)
        assertFalse(SchedulerRules.isWithinQuietHours(m(12), start, end))  // południe
        assertFalse(SchedulerRules.isWithinQuietHours(m(21, 59), start, end))
    }

    @Test
    fun `overnight window start is inclusive and end is exclusive`() {
        val start = m(22)
        val end = m(7)
        assertTrue(SchedulerRules.isWithinQuietHours(start, start, end))   // dokładnie 22:00 → cisza
        assertFalse(SchedulerRules.isWithinQuietHours(end, start, end))    // dokładnie 07:00 → już nie
    }

    // --- Okno w ciągu dnia (start < end) ---

    @Test
    fun `daytime window basic membership`() {
        val start = m(9)
        val end = m(17)
        assertTrue(SchedulerRules.isWithinQuietHours(m(12), start, end))
        assertTrue(SchedulerRules.isWithinQuietHours(start, start, end))   // 09:00 inclusive
        assertFalse(SchedulerRules.isWithinQuietHours(end, start, end))    // 17:00 exclusive
        assertFalse(SchedulerRules.isWithinQuietHours(m(8, 59), start, end))
        assertFalse(SchedulerRules.isWithinQuietHours(m(17, 1), start, end))
    }

    // --- Okno puste (start == end) ---

    @Test
    fun `empty window is never quiet`() {
        val t = m(10)
        assertFalse(SchedulerRules.isWithinQuietHours(t, t, t))
        assertFalse(SchedulerRules.isWithinQuietHours(m(0), m(12), m(12)))
        assertFalse(SchedulerRules.isWithinQuietHours(m(23), m(12), m(12)))
    }
}
