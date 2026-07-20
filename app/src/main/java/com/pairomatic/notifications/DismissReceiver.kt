package com.pairomatic.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pairomatic.PairOMaticApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Obsługuje odrzucenie (swipe) powiadomienia w trybie testu: pokazuje kolejną parę,
 * NIE zmieniając oceny odrzuconej pary.
 */
class DismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dismissedId = intent.getLongExtra(NotificationHelper.EXTRA_PAIR_ID, -1L)
        val app = context.applicationContext as PairOMaticApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.notificationScheduler.postNextTest(
                    excludeId = dismissedId.takeIf { it >= 0 }
                )
            } finally {
                pending.finish()
            }
        }
    }
}
