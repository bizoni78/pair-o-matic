package com.pairomatic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.RemoteViews
import com.pairomatic.PairOMaticApp
import com.pairomatic.R
import com.pairomatic.domain.SelectionConfig
import com.pairomatic.domain.SelectionEngine
import com.pairomatic.ui.components.pairLetterIndices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Mały widget: para liter + słowo obok (z pogrubionymi literami pary). Tap = kolejna para.
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
                val app = context.applicationContext as PairOMaticApp
                val pairs = app.container.pairRepository.getAllPairs()
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, pairs, id))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(
        context: Context,
        pairs: List<com.pairomatic.data.db.PairEntity>,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_pair)
        if (pairs.isEmpty()) {
            views.setTextViewText(R.id.pair_letters, "—")
            views.setTextViewText(R.id.pair_word, "Dodaj pary w aplikacji")
        } else {
            val pair = SelectionEngine.pickNext(pairs, System.currentTimeMillis(), NO_COOLDOWN)
                ?: pairs.random()
            views.setTextViewText(R.id.pair_letters, pair.letters)
            views.setTextViewText(R.id.pair_word, boldWord(pair.word, pair.letters))
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

    /** Słowo z pogrubionymi literami pary (np. „CT" + „Cytryna" → **C**y**t**ryna). */
    private fun boldWord(word: String, letters: String): CharSequence {
        if (word.isBlank()) return "—"
        val indices = pairLetterIndices(word, letters)
        if (indices.isEmpty()) return word
        val spannable = SpannableString(word)
        for (i in indices) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    companion object {
        private val NO_COOLDOWN = SelectionConfig.DEFAULT.copy(cooldownMillis = 0L)
    }
}
