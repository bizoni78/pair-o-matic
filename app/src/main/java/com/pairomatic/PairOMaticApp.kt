package com.pairomatic

import android.app.Application
import com.pairomatic.notifications.NotificationHelper
import com.pairomatic.reminder.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PairOMaticApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)

        // Upewnij się, że przypomnienie o nauce jest zaplanowane, jeśli włączone.
        CoroutineScope(Dispatchers.IO).launch {
            val settings = container.settingsRepository.settings.first()
            if (settings.reminderEnabled) {
                ReminderWorker.schedule(this@PairOMaticApp, settings.reminderMinuteOfDay)
            }
        }
    }
}
