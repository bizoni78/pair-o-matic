package com.pairomatic.domain

import com.pairomatic.domain.DeckJson.DeckPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckJsonTest {

    private val deck = listOf(
        DeckPair("CT", "Cytryna", "ct.png", level = 2, lastSeen = 1_700_000_000_000L, hardFlag = true, reviewFlag = false),
        DeckPair("SK", "Skorpion", null, level = null, lastSeen = null, hardFlag = false, reviewFlag = true),
        DeckPair("AB", "Abażur", "ab.jpg", level = 0, lastSeen = 42L, hardFlag = false, reviewFlag = false)
    )

    @Test fun `round-trip ze statystykami odtwarza pary 1 do 1`() {
        val json = DeckJson.encode(deck, includeStats = true)
        val decoded = DeckJson.decode(json)
        assertEquals(deck, decoded)
    }

    @Test fun `round-trip bez statystyk zeruje statystyki, zachowuje tekst i obrazek`() {
        val json = DeckJson.encode(deck, includeStats = false)
        val decoded = DeckJson.decode(json)

        assertEquals(deck.size, decoded.size)
        decoded.forEachIndexed { i, p ->
            assertEquals(deck[i].letters, p.letters)
            assertEquals(deck[i].word, p.word)
            assertEquals(deck[i].imagePath, p.imagePath)
            // statystyki pominięte w eksporcie → wartości domyślne
            assertNull(p.level)
            assertNull(p.lastSeen)
            assertTrue(!p.hardFlag)
            assertTrue(!p.reviewFlag)
        }
    }

    @Test fun `null obrazek pozostaje nullem po round-trip`() {
        val json = DeckJson.encode(listOf(deck[1]), includeStats = true)
        val decoded = DeckJson.decode(json)
        assertNull(decoded.single().imagePath)
    }

    @Test fun `level zero jest rozroznialny od null po round-trip`() {
        val pairs = listOf(
            DeckPair("A0", "zero", null, level = 0, lastSeen = null, hardFlag = false, reviewFlag = false),
            DeckPair("AN", "null", null, level = null, lastSeen = null, hardFlag = false, reviewFlag = false)
        )
        val decoded = DeckJson.decode(DeckJson.encode(pairs, includeStats = true))
        assertEquals(0, decoded[0].level)
        assertNull(decoded[1].level)
    }

    @Test fun `decode respektuje limit`() {
        val json = DeckJson.encode(deck, includeStats = true)
        assertEquals(2, DeckJson.decode(json, limit = 2).size)
    }

    @Test fun `pusta talia round-trip`() {
        assertEquals(emptyList<DeckPair>(), DeckJson.decode(DeckJson.encode(emptyList(), includeStats = true)))
    }
}
