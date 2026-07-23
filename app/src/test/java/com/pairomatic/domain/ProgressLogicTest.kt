package com.pairomatic.domain

import com.pairomatic.domain.ProgressLogic.StreakState
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressLogicTest {

    private val day = 20_000L  // dowolny epoch day

    // --- recordGrade: seria ---

    @Test fun `pierwsza ocena w ogole - seria startuje od 1`() {
        val s = ProgressLogic.recordGrade(
            StreakState(streakCount = 0, lastDay = null, todayCount = 0, todayDay = null),
            day
        )
        assertEquals(1, s.streakCount)
        assertEquals(day, s.lastDay)
        assertEquals(1, s.todayCount)
        assertEquals(day, s.todayDay)
    }

    @Test fun `kolejny dzien z rzedu - seria plus 1`() {
        val s = ProgressLogic.recordGrade(
            StreakState(streakCount = 3, lastDay = day - 1, todayCount = 5, todayDay = day - 1),
            day
        )
        assertEquals(4, s.streakCount)
        assertEquals(1, s.todayCount)   // nowy dzień → licznik „dziś" zresetowany do 1
    }

    @Test fun `przerwa wiecej niz dzien - seria reset do 1`() {
        val s = ProgressLogic.recordGrade(
            StreakState(streakCount = 9, lastDay = day - 3, todayCount = 0, todayDay = day - 3),
            day
        )
        assertEquals(1, s.streakCount)
        assertEquals(1, s.todayCount)
    }

    @Test fun `druga ocena tego samego dnia - seria bez zmian, licznik plus 1`() {
        val s = ProgressLogic.recordGrade(
            StreakState(streakCount = 4, lastDay = day, todayCount = 2, todayDay = day),
            day
        )
        assertEquals(4, s.streakCount)  // już liczone dziś, nie rośnie
        assertEquals(3, s.todayCount)
    }

    @Test fun `pierwsza ocena danego dnia gdy seria byla wczoraj ale licznik dzis nieustawiony`() {
        val s = ProgressLogic.recordGrade(
            StreakState(streakCount = 2, lastDay = day - 1, todayCount = 99, todayDay = day - 1),
            day
        )
        assertEquals(3, s.streakCount)
        assertEquals(1, s.todayCount)   // stary licznik z innego dnia ignorowany
    }

    // --- visibleStreak ---

    @Test fun `widoczna seria - brak aktywnosci to zero`() {
        assertEquals(0, ProgressLogic.visibleStreak(streakCount = 5, lastDay = null, today = day))
    }

    @Test fun `widoczna seria - aktywnosc dzis pokazuje serie`() {
        assertEquals(7, ProgressLogic.visibleStreak(streakCount = 7, lastDay = day, today = day))
    }

    @Test fun `widoczna seria - aktywnosc wczoraj wciaz pokazuje serie`() {
        assertEquals(7, ProgressLogic.visibleStreak(streakCount = 7, lastDay = day - 1, today = day))
    }

    @Test fun `widoczna seria - przerwa wygasza serie do zera`() {
        assertEquals(0, ProgressLogic.visibleStreak(streakCount = 7, lastDay = day - 2, today = day))
    }

    // --- todayCount ---

    @Test fun `licznik dzis - ten sam dzien zwraca zapisany`() {
        assertEquals(4, ProgressLogic.todayCount(storedCount = 4, todayDay = day, today = day))
    }

    @Test fun `licznik dzis - inny dzien zeruje`() {
        assertEquals(0, ProgressLogic.todayCount(storedCount = 4, todayDay = day - 1, today = day))
    }

    @Test fun `licznik dzis - brak zapisanego dnia zeruje`() {
        assertEquals(0, ProgressLogic.todayCount(storedCount = 4, todayDay = null, today = day))
    }
}
