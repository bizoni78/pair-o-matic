package com.pairomatic.notifications

import android.content.Context
import com.pairomatic.data.PairRepository
import com.pairomatic.data.db.PairEntity
import com.pairomatic.data.settings.AppSettings
import com.pairomatic.data.settings.LearningMode
import com.pairomatic.data.settings.SettingsRepository
import com.pairomatic.domain.SchedulerRules
import com.pairomatic.domain.SelectionConfig
import com.pairomatic.domain.SelectionEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Spina dobór pary z regułami planowania i wyświetlaniem powiadomień.
 * Wywoływany zdarzeniowo (odbiorniki trybu testu) oraz z timera (immersja).
 *
 * STA-6: `postMutex` serializuje pokazywanie kolejnego powiadomienia — szybkie taps/oceny
 * nie wywołają kilku `postNext*` naraz (brak podwójnych powiadomień).
 * STA-3: dzięki temu dostęp do [recentImmersion] jest też bezpieczny wątkowo.
 */
class NotificationScheduler(
    private val context: Context,
    private val pairRepository: PairRepository,
    private val settingsRepository: SettingsRepository
) {
    private val postMutex = Mutex()

    // Ulotna lista „ostatnio pokazanych" w immersji — tylko w pamięci, chroniona przez postMutex.
    private val recentImmersion = LinkedHashSet<Long>()

    /**
     * Pokazuje kolejną parę w trybie testu (po ocenie lub odrzuceniu).
     * @param excludeId para do pominięcia w najbliższym losowaniu (np. właśnie odrzucona przez swipe),
     *   żeby nie pokazać od razu tej samej.
     */
    suspend fun postNextTest(excludeId: Long? = null) = postMutex.withLock {
        val settings = settingsRepository.settings.first()
        if (!canPost(settings)) {
            NotificationHelper.cancel(context)
            return@withLock
        }
        val pairs = pairRepository.getAllPairs()
        if (pairs.isEmpty()) {
            NotificationHelper.cancel(context)
            return@withLock
        }
        // WAŻNE: nigdy nie urywamy łańcucha. Jeśli cooldown wyzeruje całą pulę (mała talia,
        // wszystko świeżo ocenione), dobieramy parę mimo wszystko (bez cooldownu) — inaczej
        // po swipe/ocenie kolejne powiadomienie nie pojawiłoby się aż do wygaśnięcia cooldownu.
        val exclude = excludeId?.let { setOf(it) } ?: emptySet()
        val next = pickWithFallback(pairs, exclude = exclude)
        NotificationHelper.showTest(
            context, next, pairRepository.imagesDirectory, settings.importance
        )
    }

    /** Pokazuje kolejną parę w trybie immersji (tick timera). Nie zmienia statystyk. */
    suspend fun postNextImmersion() = postMutex.withLock {
        val settings = settingsRepository.settings.first()
        if (!canPost(settings)) {
            NotificationHelper.cancel(context)
            return@withLock
        }
        val pairs = pairRepository.getAllPairs()
        if (pairs.isEmpty()) {
            NotificationHelper.cancel(context)
            return@withLock
        }
        val next = pickWithFallback(pairs, exclude = recentImmersion.toSet())
        rememberImmersion(next.id)
        NotificationHelper.showImmersion(
            context, next, pairRepository.imagesDirectory, settings.importance
        )
    }

    /**
     * Dobiera parę, gwarantując wynik dla niepustej talii: najpierw normalnie (z cooldownem),
     * potem — jeśli cała pula jest w cooldownie — bez cooldownu, a w ostateczności losowo.
     */
    private fun pickWithFallback(pairs: List<PairEntity>, exclude: Set<Long>): PairEntity {
        val now = System.currentTimeMillis()
        return SelectionEngine.pickNext(pairs, now, SelectionConfig.DEFAULT, exclude)
            ?: SelectionEngine.pickNext(pairs, now, SelectionConfig.NO_COOLDOWN, exclude)
            ?: pairs.random()
    }

    /** Uruchamia pierwszą parę zależnie od aktywnego trybu (np. z ekranu ustawień). */
    suspend fun postFirst() {
        val mode = settingsRepository.settings.first().mode
        when (mode) {
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

    /** Wywoływane pod [postMutex]. */
    private fun rememberImmersion(id: Long) {
        recentImmersion.add(id)
        while (recentImmersion.size > RECENT_CAP) {
            recentImmersion.iterator().let { if (it.hasNext()) { it.next(); it.remove() } }
        }
    }

    companion object {
        private const val RECENT_CAP = 10
    }
}
