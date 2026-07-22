package com.pairomatic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pairomatic.PairOMaticApp
import com.pairomatic.R
import com.pairomatic.data.db.PairEntity
import com.pairomatic.domain.SelectionConfig
import com.pairomatic.domain.SelectionEngine
import com.pairomatic.util.boldPairLettersSpannable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Mały widget (pojedyncza wysokość): duże litery pary + słowo obok, na jasnofioletowym tle.
 * Tap = kolejna para.
 */
class PairWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = (context.applicationContext as PairOMaticApp).container.pairRepository
                val pairs = repo.getAllPairs()
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, pairs, id))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(context: Context, pairs: List<PairEntity>, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_pair)
        if (pairs.isEmpty()) {
            views.setTextViewText(R.id.pair_letters, "—")
            views.setTextViewText(R.id.pair_word, "Dodaj pary w aplikacji")
        } else {
            val pair = SelectionEngine.pickNext(pairs, System.currentTimeMillis(), SelectionConfig.NO_COOLDOWN)
                ?: pairs.random()
            views.setTextViewText(R.id.pair_letters, pair.letters)
            views.setTextViewText(R.id.pair_word, boldPairLettersSpannable(pair.word, pair.letters, "—"))
        }

        // Tap w widget → odśwież (kolejna para). Wysyłamy do siebie APPWIDGET_UPDATE dla tego id.
        val intent = Intent(context, PairWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val pi = PendingIntent.getBroadcast(
            context, appWidgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.pair_root, pi)
        return views
    }
}
