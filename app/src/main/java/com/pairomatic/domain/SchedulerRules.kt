package com.pairomatic.domain

import java.util.Calendar

/**
 * Reguły decydujące, czy w danej chwili wolno pokazać powiadomienie
 * (godziny ciszy, główny przełącznik).
 */
object SchedulerRules {

    /**
     * Czy podana minuta doby mieści się w oknie ciszy. Obsługuje okna przechodzące przez północ
     * (np. 22:00–07:00). Gdy start == end, okno jest puste (cisza nigdy nie obowiązuje).
     */
    fun isWithinQuietHours(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean {
        if (startMinute == endMinute) return false
        return if (startMinute < endMinute) {
            minuteOfDay in startMinute until endMinute
        } else {
            minuteOfDay >= startMinute || minuteOfDay < endMinute
        }
    }

    /** Bieżąca minuta doby (0..1439) z zegara systemowego. */
    fun currentMinuteOfDay(now: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
