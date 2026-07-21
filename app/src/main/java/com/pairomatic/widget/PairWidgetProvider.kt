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
import android.view.View
import android.widget.RemoteViews
import com.pairomatic.PairOMaticApp
import com.pairomatic.R
import com.pairomatic.data.db.PairEntity
import com.pairomatic.domain.SelectionConfig
import com.pairomatic.domain.SelectionEngine
import com.pairomatic.ui.components.pairLetterIndices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Mały widget: duże litery pary + słowo obok (jasnofioletowe tło). Tap na parę odsłania
 * trzy przyciski oceny; tap w przycisk zapisuje ocenę i pokazuje kolejną parę. Tap poza
 * przyciskami (gdy odsłonięte) pomija bez oceny.
 *
 * Uwaga: widgety Androida nie obsługują długiego przytrzymania (launcher przechwytuje je
 * na przenoszenie), dlatego odsłanianie działa na zwykłe tapnięcie.
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
                    appWidgetManager.updateAppWidget(id, compactViews(context, id, pickPair(pairs)))
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action != ACTION_REVEAL && action != ACTION_GRADE && action != ACTION_NEXT) return
        val appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as PairOMaticApp
                val repo = app.container.pairRepository
                val manager = AppWidgetManager.getInstance(context)
                when (action) {
                    ACTION_REVEAL -> {
                        val pair = repo.getById(intent.getLongExtra(EXTRA_PAIR_ID, -1L))
                        if (pair != null) manager.updateAppWidget(appWidgetId, revealedViews(context, appWidgetId, pair))
                    }
                    ACTION_GRADE -> {
                        val pairId = intent.getLongExtra(EXTRA_PAIR_ID, -1L)
                        val grade = intent.getIntExtra(EXTRA_GRADE, -1)
                        if (pairId >= 0 && grade in 0..2) {
                            repo.grade(pairId, grade, System.currentTimeMillis())
                            app.container.settingsRepository.recordGrade()
                        }
                        val next = pickPair(repo.getAllPairs(), excludeId = pairId)
                        manager.updateAppWidget(appWidgetId, compactViews(context, appWidgetId, next))
                    }
                    ACTION_NEXT -> {
                        val next = pickPair(repo.getAllPairs())
                        manager.updateAppWidget(appWidgetId, compactViews(context, appWidgetId, next))
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun pickPair(pairs: List<PairEntity>, excludeId: Long? = null): PairEntity? {
        if (pairs.isEmpty()) return null
        val exclude = excludeId?.takeIf { it >= 0 }?.let { setOf(it) } ?: emptySet()
        return SelectionEngine.pickNext(pairs, System.currentTimeMillis(), NO_COOLDOWN, exclude)
            ?: pairs.random()
    }

    /** Widok zwinięty: para + słowo, przyciski ukryte; tap odsłania oceny. */
    private fun compactViews(context: Context, appWidgetId: Int, pair: PairEntity?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_pair)
        views.setViewVisibility(R.id.buttons_row, View.GONE)
        if (pair == null) {
            views.setTextViewText(R.id.pair_letters, "—")
            views.setTextViewText(R.id.pair_word, "Dodaj pary w aplikacji")
            views.setOnClickPendingIntent(R.id.pair_root, null)
            return views
        }
        views.setTextViewText(R.id.pair_letters, pair.letters)
        views.setTextViewText(R.id.pair_word, boldWord(pair.word, pair.letters))
        views.setOnClickPendingIntent(
            R.id.pair_root,
            broadcast(context, appWidgetId, ACTION_REVEAL, reqCode(appWidgetId, 0), pairId = pair.id)
        )
        return views
    }

    /** Widok odsłonięty: para + słowo + trzy przyciski oceny; tap poza nimi = pomiń. */
    private fun revealedViews(context: Context, appWidgetId: Int, pair: PairEntity): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_pair)
        views.setTextViewText(R.id.pair_letters, pair.letters)
        views.setTextViewText(R.id.pair_word, boldWord(pair.word, pair.letters))
        views.setViewVisibility(R.id.buttons_row, View.VISIBLE)
        views.setOnClickPendingIntent(
            R.id.btn_dont_know,
            broadcast(context, appWidgetId, ACTION_GRADE, reqCode(appWidgetId, 1), pairId = pair.id, grade = 0)
        )
        views.setOnClickPendingIntent(
            R.id.btn_soso,
            broadcast(context, appWidgetId, ACTION_GRADE, reqCode(appWidgetId, 2), pairId = pair.id, grade = 1)
        )
        views.setOnClickPendingIntent(
            R.id.btn_well,
            broadcast(context, appWidgetId, ACTION_GRADE, reqCode(appWidgetId, 3), pairId = pair.id, grade = 2)
        )
        // Tap poza przyciskami — pomiń (kolejna para bez oceny).
        views.setOnClickPendingIntent(
            R.id.pair_root,
            broadcast(context, appWidgetId, ACTION_NEXT, reqCode(appWidgetId, 4))
        )
        return views
    }

    private fun broadcast(
        context: Context,
        appWidgetId: Int,
        action: String,
        requestCode: Int,
        pairId: Long = -1L,
        grade: Int = -1
    ): PendingIntent {
        val intent = Intent(context, PairWidgetProvider::class.java).apply {
            this.action = action
            putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            if (pairId >= 0) putExtra(EXTRA_PAIR_ID, pairId)
            if (grade >= 0) putExtra(EXTRA_GRADE, grade)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reqCode(appWidgetId: Int, slot: Int): Int = appWidgetId * 10 + slot

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
        private const val ACTION_REVEAL = "com.pairomatic.widget.REVEAL"
        private const val ACTION_GRADE = "com.pairomatic.widget.GRADE"
        private const val ACTION_NEXT = "com.pairomatic.widget.NEXT"
        private const val EXTRA_APPWIDGET_ID = "widget_id"
        private const val EXTRA_PAIR_ID = "pair_id"
        private const val EXTRA_GRADE = "grade"
        private val NO_COOLDOWN = SelectionConfig.DEFAULT.copy(cooldownMillis = 0L)
    }
}
