package com.pairomatic.data.db

/**
 * Lekki wynik zapytania doboru (PERF-2): tylko `id` pary i policzona po stronie SQL waga.
 * Pozwala losować kolejną parę bez ładowania całych encji do pamięci.
 */
data class PairWeight(
    val id: Long,
    val weight: Double
)
