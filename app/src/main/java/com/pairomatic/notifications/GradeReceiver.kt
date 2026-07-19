package com.pairomatic.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pairomatic.PairOMaticApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Obsługuje kliknięcie przycisku oceny w trybie testu: zapisuje ocenę i pokazuje kolejną parę.
 */
class GradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pairId = intent.getLongExtra(NotificationHelper.EXTRA_PAIR_ID, -1L)
        val grade = intent.getIntExtra(NotificationHelper.EXTRA_GRADE, -1)
        val app = context.applicationContext as PairOMaticApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (pairId >= 0 && grade in 0..2) {
                    app.container.pairRepository.grade(pairId, grade, System.currentTimeMillis())
                    app.container.settingsRepository.recordGrade()
                }
                app.container.notificationScheduler.postNextTest()
            } finally {
                pending.finish()
            }
        }
    }
}
