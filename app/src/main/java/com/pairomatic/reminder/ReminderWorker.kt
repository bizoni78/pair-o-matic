package com.pairomatic.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pairomatic.PairOMaticApp
import com.pairomatic.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Codzienne przypomnienie o nauce: jeśli o wyznaczonej porze nie było dziś żadnej oceny,
 * wysyła delikatne powiadomienie „poćwicz, żeby nie stracić serii".
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PairOMaticApp
        val settings = app.container.settingsRepository.settings.first()
        if (!settings.reminderEnabled || !settings.notificationsEnabled) return Result.success()

        // Nie przypominamy, jeśli użytkownik już dziś ćwiczył.
        val progress = app.container.settingsRepository.progress.first()
        if (progress.today > 0) return Result.success()

        NotificationHelper.showReminder(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "study_reminder"

        /** Planuje codzienne przypomnienie na wskazaną minutę doby (pierwsze uruchomienie o tej porze). */
        fun schedule(context: Context, minuteOfDay: Int) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                set(Calendar.MINUTE, minuteOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = target.timeInMillis - now.timeInMillis
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
