package com.pairomatic.domain

/**
 * Czysta, testowalna logika serii dni (streak) i licznika „dziś" — bez zależności od DataStore
 * czy zegara systemowego. Dni wyrażone jako `epoch day` (LocalDate.toEpochDay()).
 */
object ProgressLogic {

    /**
     * Migawka liczników postępu.
     *
     * @param streakCount zapisana długość serii
     * @param lastDay dzień ostatniej aktywności (epoch day) lub null, gdy nigdy
     * @param todayCount zapisany licznik ocen dla [todayDay]
     * @param todayDay dzień, którego dotyczy [todayCount], lub null
     */
    data class StreakState(
        val streakCount: Int,
        val lastDay: Long?,
        val todayCount: Int,
        val todayDay: Long?
    )

    /** Aktualizuje stan po jednej ocenie wykonanej dnia [today]. */
    fun recordGrade(state: StreakState, today: Long): StreakState {
        val newStreak = when {
            state.lastDay == null -> 1                              // pierwsza aktywność
            state.lastDay == today -> state.streakCount.coerceAtLeast(1)  // już liczone dziś
            state.lastDay == today - 1 -> state.streakCount + 1     // kolejny dzień z rzędu
            else -> 1                                               // przerwa — reset
        }
        val curCount = if (state.todayDay == today) state.todayCount else 0
        return StreakState(
            streakCount = newStreak,
            lastDay = today,
            todayCount = curCount + 1,
            todayDay = today
        )
    }

    /** Widoczna seria: pokazujemy ją tylko, jeśli aktywność była dziś lub wczoraj. */
    fun visibleStreak(streakCount: Int, lastDay: Long?, today: Long): Int = when {
        lastDay == null -> 0
        lastDay == today || lastDay == today - 1 -> streakCount
        else -> 0
    }

    /** Licznik „dziś": zeruje się automatycznie przy zmianie dnia. */
    fun todayCount(storedCount: Int, todayDay: Long?, today: Long): Int =
        if (todayDay == today) storedCount else 0
}
