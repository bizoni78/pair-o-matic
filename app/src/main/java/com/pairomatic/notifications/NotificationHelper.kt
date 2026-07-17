package com.pairomatic.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pairomatic.R
import com.pairomatic.data.db.PairEntity
import com.pairomatic.data.settings.NotificationImportance
import java.io.File

/**
 * Buduje i wyświetla powiadomienia obu trybów. Cała komunikacja z systemem powiadomień
 * jest w jednym miejscu.
 */
object NotificationHelper {

    const val CHANNEL_ID = "letter_pairs"
    const val NOTIFICATION_ID = 1001

    const val EXTRA_PAIR_ID = "extra_pair_id"
    const val EXTRA_GRADE = "extra_grade"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
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
            .setSummaryText(pair.word)                      // rozwinięte: słowo pod obrazkiem
        if (bitmap != null) style.bigPicture(bitmap)
        builder.setStyle(style)

        builder.addAction(0, context.getString(R.string.grade_dont_know), gradeIntent(context, pair.id, 0))
        builder.addAction(0, context.getString(R.string.grade_soso), gradeIntent(context, pair.id, 1))
        builder.addAction(0, context.getString(R.string.grade_well), gradeIntent(context, pair.id, 2))

        // Swipe → DismissReceiver pokazuje kolejną parę.
        builder.setDeleteIntent(dismissIntent(context))

        notify(context, builder.build())
    }

    /**
     * Tryb immersji: od razu obrazek, bez przycisków i bez zbierania statystyk.
     */
    fun showImmersion(context: Context, pair: PairEntity, imageDir: File, importance: NotificationImportance) {
        val bitmap = loadBitmap(pair.imagePath, imageDir)
        val builder = baseBuilder(context, importance)
            .setContentTitle(pair.letters)
            .setContentText(pair.word)

        if (bitmap != null) {
            // Miniatura widoczna od razu w widoku zwiniętym (także na ekranie blokady).
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .setBigContentTitle(pair.letters)
                    .setSummaryText(pair.word)
                    .bigPicture(bitmap)
                    // Po rozwinięciu miniatura zamienia się w duży obraz.
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
            .setOnlyAlertOnce(importance == NotificationImportance.SILENT)
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

    private fun loadBitmap(fileName: String?, imageDir: File): Bitmap? {
        if (fileName.isNullOrBlank()) return null
        val file = File(imageDir, fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
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

    private fun dismissIntent(context: Context): PendingIntent {
        val intent = Intent(context, DismissReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
