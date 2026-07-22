package com.pairomatic.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {

    @Test
    fun `parses simple comma rows`() {
        val rows = CsvParser.parse("litery,slowo,obrazek\nCT,Cytryna,ct.png")
        assertEquals(
            listOf(
                listOf("litery", "slowo", "obrazek"),
                listOf("CT", "Cytryna", "ct.png")
            ),
            rows
        )
    }

    @Test
    fun `detects semicolon delimiter`() {
        val rows = CsvParser.parse("CT;Cytryna;ct.png")
        assertEquals(listOf(listOf("CT", "Cytryna", "ct.png")), rows)
    }

    @Test
    fun `skips blank lines and trims trailing CR`() {
        val rows = CsvParser.parse("a,b\r\n\r\nc,d")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), rows)
    }

    @Test
    fun `quoted field may contain delimiter`() {
        val fields = CsvParser.splitLine("CT,\"Cytryna, żółta\",ct.png", ',')
        assertEquals(listOf("CT", "Cytryna, żółta", "ct.png"), fields)
    }

    @Test
    fun `doubled quotes become a single quote`() {
        val fields = CsvParser.splitLine("CT,\"a\"\"b\",x", ',')
        assertEquals(listOf("CT", "a\"b", "x"), fields)
    }

    @Test
    fun `empty fields are preserved`() {
        val fields = CsvParser.splitLine("CT,,ct.png", ',')
        assertEquals(listOf("CT", "", "ct.png"), fields)
    }
}
