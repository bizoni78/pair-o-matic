package com.pairomatic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pairomatic.MainActivity
import com.pairomatic.PairOMaticApp
import com.pairomatic.R
import com.pairomatic.data.settings.ProgressStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Widget na ekran główny: seria dni + dziś/cel + przycisk „Ucz się". Dane czytane z DataStore.
 */
class LearnWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as PairOMaticApp
                val progress = app.container.settingsRepository.progress.first()
                val views = buildViews(context, progress)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(context: Context, progress: ProgressStats): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_learn)
        views.setTextViewText(
            R.id.widget_streak,
            if (progress.streak > 0) "🔥 Seria: ${progress.streak} ${dayWord(progress.streak)}"
            else "Zacznij serię dziś!"
        )
        views.setTextViewText(R.id.widget_today, "Dziś: ${progress.today} / ${progress.dailyGoal}")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_learn_button, pi)
        views.setOnClickPendingIntent(R.id.widget_root, pi)
        return views
    }

    private fun dayWord(n: Int): String = if (n == 1) "dzień" else "dni"
}
