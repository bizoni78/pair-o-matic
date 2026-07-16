package com.pairomatic.notifications

import android.content.Context
import com.pairomatic.data.PairRepository
import com.pairomatic.data.settings.AppSettings
import com.pairomatic.data.settings.LearningMode
import com.pairomatic.data.settings.SettingsRepository
import com.pairomatic.domain.SchedulerRules
import com.pairomatic.domain.SelectionConfig
import com.pairomatic.domain.SelectionEngine
import kotlinx.coroutines.flow.first

/**
 * Spina dobór pary z regułami planowania i wyświetlaniem powiadomień.
 * Wywoływany zdarzeniowo (odbiorniki trybu testu) oraz z timera (immersja).
 */
class NotificationScheduler(
    private val context: Context,
    private val pairRepository: PairRepository,
    private val settingsRepository: SettingsRepository
) {
    /** Pokazuje kolejną parę w trybie testu (po ocenie lub odrzuceniu). */
    suspend fun postNextTest() {
        val settings = settingsRepository.settings.first()
        if (!canPost(settings)) {
            NotificationHelper.cancel(context)
            return
        }
        val pairs = pairRepository.getAllPairs()
        val next = SelectionEngine.pickNext(pairs, System.currentTimeMillis(), SelectionConfig.DEFAULT)
            ?: return
        NotificationHelper.showTest(
            context, next, pairRepository.imagesDirectory, settings.importance
        )
    }

    /** Pokazuje kolejną parę w trybie immersji (tick timera). Nie zmienia statystyk. */
    suspend fun postNextImmersion() {
        val settings = settingsRepository.settings.first()
        if (!canPost(settings)) {
            NotificationHelper.cancel(context)
            return
        }
        val pairs = pairRepository.getAllPairs()
        val next = SelectionEngine.pickNext(
            pairs, System.currentTimeMillis(), SelectionConfig.DEFAULT, exclude = recentImmersion
        ) ?: return
        rememberImmersion(next.id)
        NotificationHelper.showImmersion(
            context, next, pairRepository.imagesDirectory, settings.importance
        )
    }

    /** Uruchamia pierwszą parę zależnie od aktywnego trybu (np. z ekranu ustawień). */
    suspend fun postFirst() {
        val settings = settingsRepository.settings.first()
        when (settings.mode) {
            LearningMode.TEST -> postNextTest()
            LearningMode.IMMERSION -> postNextImmersion()
        }
    }

    private fun canPost(settings: AppSettings): Boolean {
        if (!settings.notificationsEnabled) return false
        if (settings.quietHoursEnabled) {
            val nowMinute = SchedulerRules.currentMinuteOfDay()
            if (SchedulerRules.isWithinQuietHours(nowMinute, settings.quietStartMinute, settings.quietEndMinute)) {
                return false
            }
        }
        return true
    }

    private fun rememberImmersion(id: Long) {
        recentImmersion.add(id)
        while (recentImmersion.size > RECENT_CAP) {
            recentImmersion.iterator().let { if (it.hasNext()) { it.next(); it.remove() } }
        }
    }

    companion object {
        private const val RECENT_CAP = 10
        // Ulotna lista „ostatnio pokazanych" w immersji — celowo tylko w pamięci.
        private val recentImmersion = LinkedHashSet<Long>()
    }
}
