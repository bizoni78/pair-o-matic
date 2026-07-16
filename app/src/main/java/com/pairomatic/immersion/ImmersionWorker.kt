package com.pairomatic.immersion

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pairomatic.PairOMaticApp
import java.util.concurrent.TimeUnit

/**
 * Cyklicznie (na timerze WorkManagera) pokazuje kolejny obrazek w trybie immersji.
 * To jedyny element czasowy aplikacji — tryb testu jest zdarzeniowy.
 */
class ImmersionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PairOMaticApp
        app.container.notificationScheduler.postNextImmersion()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "immersion_rotation"

        /** Planuje cykliczne pokazywanie. WorkManager wymusza minimum 15 minut. */
        fun schedule(context: Context, intervalMinutes: Int) {
            val safeInterval = intervalMinutes.coerceAtLeast(15).toLong()
            val request = PeriodicWorkRequestBuilder<ImmersionWorker>(
                safeInterval, TimeUnit.MINUTES
            ).build()
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
