package com.pairomatic.domain

/**
 * Parser CSV używany przy imporcie talii. Czysta logika (bez zależności od Androida) —
 * łatwa do pokrycia testami. Wykrywa separator (`,` lub `;`) i obsługuje pola w cudzysłowach
 * (z podwojonym `""` w środku).
 */
object CsvParser {

    /** Dzieli tekst CSV na wiersze i kolumny. Puste wiersze pomijane. */
    fun parse(text: String): List<List<String>> {
        val delimiter = if (text.count { it == ';' } > text.count { it == ',' }) ';' else ','
        val result = mutableListOf<List<String>>()
        for (raw in text.split("\n")) {
            val line = raw.trimEnd('\r')
            if (line.isBlank()) continue
            result.add(splitLine(line, delimiter))
        }
        return result
    }

    /** Dzieli pojedynczy wiersz na pola, świadomy cudzysłowów. */
    fun splitLine(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> { fields.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }
}
