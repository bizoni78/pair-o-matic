package com.pairomatic.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PairLettersTest {

    @Test
    fun `basic pair in word`() {
        // C na 0, t na 2 → {0, 2}
        assertEquals(setOf(0, 2), pairLetterIndices("Cytryna", "CT"))
    }

    @Test
    fun `case insensitive matching`() {
        assertEquals(setOf(0, 2), pairLetterIndices("cytryna", "CT"))
        assertEquals(setOf(0, 1), pairLetterIndices("Skorpion", "SK"))
    }

    @Test
    fun `duplicate letters match distinct non-overlapping positions`() {
        // "Sos" + "SS": pierwsze S na 0, drugie s na 2 (bez nakładania).
        assertEquals(setOf(0, 2), pairLetterIndices("Sos", "SS"))
    }

    @Test
    fun `letters matched in order, later letter only after previous match`() {
        // "Taxi" + "XT": X na 2, potem T szukane od indeksu 3 → brak (T jest na 0).
        assertEquals(setOf(2), pairLetterIndices("Taxi", "XT"))
    }

    @Test
    fun `no match returns empty`() {
        assertEquals(emptySet<Int>(), pairLetterIndices("abc", "XY"))
    }

    @Test
    fun `partial match returns only found letter`() {
        // "Kot" + "KZ": K na 0, Z brak → {0}
        assertEquals(setOf(0), pairLetterIndices("Kot", "KZ"))
    }

    @Test
    fun `empty inputs return empty`() {
        assertEquals(emptySet<Int>(), pairLetterIndices("", "CT"))
        assertEquals(emptySet<Int>(), pairLetterIndices("Cytryna", ""))
    }
}
