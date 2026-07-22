package com.pairomatic.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pairomatic.R
import com.pairomatic.data.db.PairEntity
import com.pairomatic.data.settings.NotificationImportance
import com.pairomatic.domain.pairLetterIndices
import java.io.File

/**
 * Buduje i wyświetla powiadomienia obu trybów. Cała komunikacja z systemem powiadomień
 * jest w jednym miejscu.
 */
object NotificationHelper {

    const val CHANNEL_ID = "letter_pairs"
    const val REMINDER_CHANNEL_ID = "letter_pairs_reminder"
    const val NOTIFICATION_ID = 1001
    const val REMINDER_NOTIFICATION_ID = 2001

    const val EXTRA_PAIR_ID = "extra_pair_id"
    const val EXTRA_GRADE = "extra_grade"

    // Docelowa maks. krawędź obrazka w powiadomieniu (downsampling — ochrona przed OOM).
    private const val MAX_IMAGE_EDGE_PX = 1024

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_description)
        }
        val reminderChannel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(reminderChannel)
    }

    /** Delikatne przypomnienie o nauce (osobny kanał, tap otwiera aplikację). */
    fun showReminder(context: Context) {
        val tapIntent = Intent(context, com.pairomatic.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // brak uprawnienia — ignorujemy
        }
    }

    /**
     * Tryb testu: zwinięte pokazuje tylko litery (test), rozwinięte — słowo, obrazek i 3 oceny.
     */
    fun showTest(context: Context, pair: PairEntity, imageDir: File, importance: NotificationImportance) {
        val bitmap = loadBitmap(pair.imagePath, imageDir)

        val builder = baseBuilder(context, importance)
            .setContentTitle(pair.letters)                 // zwinięte: wyłącznie para liter (to jest test)
        // Celowo NIE ustawiamy contentText — w widoku zwiniętym ma być widoczna sama para,
        // bez słowa (test polega na przypomnieniu sobie skojarzenia).

        val style = NotificationCompat.BigPictureStyle()
            .setBigContentTitle(pair.letters)
            .setSummaryText(boldedWord(pair))               // rozwinięte: słowo pod obrazkiem (litery pary pogrubione)
        if (bitmap != null) style.bigPicture(bitmap)
        builder.setStyle(style)

        builder.addAction(0, context.getString(R.string.grade_dont_know), gradeIntent(context, pair.id, 0))
        builder.addAction(0, context.getString(R.string.grade_soso), gradeIntent(context, pair.id, 1))
        builder.addAction(0, context.getString(R.string.grade_well), gradeIntent(context, pair.id, 2))

        // Swipe → DismissReceiver pokazuje kolejną parę (z pominięciem tej właśnie odrzuconej).
        builder.setDeleteIntent(dismissIntent(context, pair.id))

        notify(context, builder.build())
    }

    /**
     * Tryb immersji: od razu obrazek, bez przycisków i bez zbierania statystyk.
     */
    fun showImmersion(context: Context, pair: PairEntity, imageDir: File, importance: NotificationImportance) {
        val bitmap = loadBitmap(pair.imagePath, imageDir)
        val builder = baseBuilder(context, importance)
            .setContentTitle(pair.letters)
            .setContentText(boldedWord(pair))

        if (bitmap != null) {
            // Miniatura jako fallback dla Androida < 12 (tam duży obraz tylko po rozwinięciu).
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .setBigContentTitle(pair.letters)
                    .setSummaryText(boldedWord(pair))
                    .bigPicture(bitmap)
                    // Android 12+: duży obraz widoczny od razu również w widoku zwiniętym.
                    .showBigPictureWhenCollapsed(true)
                    // Nie dubluj miniatury po rozwinięciu.
                    .bigLargeIcon(null as Bitmap?)
            )
        }
        notify(context, builder.build())
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun baseBuilder(context: Context, importance: NotificationImportance) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(false)
            // setSilent(true) wycisza dźwięk i wibracje NIEZALEŻNIE od kanału (kanał jest HIGH,
            // więc bez tego „Cichy" i tak by dzwonił na Androidzie 8+). To realny przełącznik dźwięku.
            .setSilent(importance == NotificationImportance.SILENT)
            .setPriority(
                if (importance == NotificationImportance.HEADS_UP) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_LOW
            )

    private fun notify(context: Context, notification: android.app.Notification) {
        // Uprawnienie POST_NOTIFICATIONS jest sprawdzane przy starcie aplikacji; jeśli go brak,
        // system po prostu odrzuci powiadomienie.
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // brak uprawnienia — ignorujemy
        }
    }

    /**
     * Słowo pary z pogrubionymi literami pary (np. „CT" + „Cytryna" → **C**y**t**ryna).
     * Zwraca zwykły String, jeśli słowo jest puste lub nie da się dopasować liter.
     */
    private fun boldedWord(pair: PairEntity): CharSequence {
        val word = pair.word
        if (word.isBlank()) return word
        val indices = pairLetterIndices(word, pair.letters)
        if (indices.isEmpty()) return word
        val spannable = SpannableString(word)
        for (i in indices) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    private fun loadBitmap(fileName: String?, imageDir: File): Bitmap? {
        if (fileName.isNullOrBlank()) return null
        val file = File(imageDir, fileName)
        if (!file.exists()) return null
        return decodeSampledBitmap(file.absolutePath, MAX_IMAGE_EDGE_PX)
    }

    /**
     * Dekoduje obrazek z próbkowaniem (inSampleSize), żeby duże zdjęcia (np. z aparatu)
     * nie tworzyły ogromnych bitmap → ochrona przed OOM i limitem rozmiaru powiadomienia.
     */
    private fun decodeSampledBitmap(path: String, maxEdgePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxEdgePx || bounds.outHeight / sample > maxEdgePx) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, opts)
    }

    private fun gradeIntent(context: Context, pairId: Long, grade: Int): PendingIntent {
        val intent = Intent(context, GradeReceiver::class.java).apply {
            putExtra(EXTRA_PAIR_ID, pairId)
            putExtra(EXTRA_GRADE, grade)
        }
        // requestCode unikalny per (para, ocena), by PendingIntenty się nie nadpisywały.
        val requestCode = (pairId.toInt() * 10) + grade
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dismissIntent(context: Context, pairId: Long): PendingIntent {
        val intent = Intent(context, DismissReceiver::class.java).apply {
            putExtra(EXTRA_PAIR_ID, pairId)
        }
        // FLAG_UPDATE_CURRENT odświeża EXTRA_PAIR_ID przy każdym kolejnym powiadomieniu.
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
