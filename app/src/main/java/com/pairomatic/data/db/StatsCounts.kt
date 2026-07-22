package com.pairomatic.data.db

/**
 * Liczniki statystyk policzone jednym zapytaniem agregującym (SQL COUNT/SUM),
 * zamiast liczenia w pamięci po pełnej liście par (PERF-4).
 */
data class StatsCounts(
    val total: Int = 0,
    val veryWell: Int = 0,
    val soso: Int = 0,
    val dontKnow: Int = 0,
    val hard: Int = 0,
    val neverGraded: Int = 0,
    val noWord: Int = 0,
    val noImage: Int = 0,
    val review: Int = 0
)
