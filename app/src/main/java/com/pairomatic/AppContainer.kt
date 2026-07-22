package com.pairomatic

import android.content.Context
import com.pairomatic.data.PairRepository
import com.pairomatic.data.db.AppDatabase
import com.pairomatic.data.settings.SettingsRepository
import com.pairomatic.notifications.NotificationScheduler

/**
 * Prosty ręczny kontener zależności (Service Locator). Trzymamy jeden egzemplarz w [PairOMaticApp],
 * bez bibliotek DI — wystarczające dla aplikacji tej wielkości.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)

    val pairRepository = PairRepository(database, context)
    val settingsRepository = SettingsRepository(context)
    val notificationScheduler = NotificationScheduler(
        context.applicationContext, pairRepository, settingsRepository
    )
}
