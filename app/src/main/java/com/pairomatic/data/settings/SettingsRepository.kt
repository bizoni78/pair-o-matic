package com.pairomatic.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Wzajemnie wykluczające się tryby powiadomień. */
enum class LearningMode { TEST, IMMERSION }

/** Poziom ważności powiadomień (mapowany na importance kanału / priorytet). */
enum class NotificationImportance { SILENT, HEADS_UP }

/**
 * Migawka ustawień aplikacji.
 *
 * @param quietStartMinute początek godzin ciszy jako minuta doby (0..1439)
 * @param quietEndMinute koniec godzin ciszy jako minuta doby (0..1439)
 */
data class AppSettings(
    val mode: LearningMode = LearningMode.TEST,
    val notificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartMinute: Int = 22 * 60,
    val quietEndMinute: Int = 7 * 60,
    val immersionIntervalMinutes: Int = 15,
    val importance: NotificationImportance = NotificationImportance.HEADS_UP
)

/**
 * Postęp/motywacja i stan kopii zapasowej — liczone z lekkich liczników w DataStore
 * (tabela zdarzeń jest poza zakresem v1).
 *
 * @param streak liczba kolejnych dni z aktywnością (0 = seria przerwana / brak)
 * @param today liczba ocen wykonanych dzisiaj
 * @param dailyGoal cel dzienny liczby ocen
 * @param daysSinceBackup dni od ostatniej kopii (null = nigdy)
 * @param backupOverdue czy warto zrobić nową kopię
 */
data class ProgressStats(
    val streak: Int = 0,
    val today: Int = 0,
    val dailyGoal: Int = 20,
    val daysSinceBackup: Int? = null,
    val backupOverdue: Boolean = false
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val IMMERSION_INTERVAL = intPreferencesKey("immersion_interval")
        val IMPORTANCE = stringPreferencesKey("importance")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val STREAK_LAST_DAY = longPreferencesKey("streak_last_day")
        val TODAY_COUNT = intPreferencesKey("today_count")
        val TODAY_DAY = longPreferencesKey("today_day")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val BACKUP_LAST_DAY = longPreferencesKey("backup_last_day")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            mode = p[Keys.MODE]?.let { runCatching { LearningMode.valueOf(it) }.getOrNull() }
                ?: LearningMode.TEST,
            notificationsEnabled = p[Keys.NOTIFICATIONS_ENABLED] ?: true,
            quietHoursEnabled = p[Keys.QUIET_ENABLED] ?: false,
            quietStartMinute = p[Keys.QUIET_START] ?: (22 * 60),
            quietEndMinute = p[Keys.QUIET_END] ?: (7 * 60),
            immersionIntervalMinutes = p[Keys.IMMERSION_INTERVAL] ?: 15,
            importance = p[Keys.IMPORTANCE]?.let {
                runCatching { NotificationImportance.valueOf(it) }.getOrNull()
            } ?: NotificationImportance.HEADS_UP
        )
    }

    suspend fun setMode(mode: LearningMode) = store.edit { it[Keys.MODE] = mode.name }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        store.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }

    suspend fun setQuietHoursEnabled(enabled: Boolean) =
        store.edit { it[Keys.QUIET_ENABLED] = enabled }

    suspend fun setQuietHours(startMinute: Int, endMinute: Int) = store.edit {
        it[Keys.QUIET_START] = startMinute
        it[Keys.QUIET_END] = endMinute
    }

    suspend fun setImmersionInterval(minutes: Int) =
        store.edit { it[Keys.IMMERSION_INTERVAL] = minutes }

    suspend fun setImportance(importance: NotificationImportance) =
        store.edit { it[Keys.IMPORTANCE] = importance.name }

    // --- Postęp / motywacja / kopia zapasowa ---

    val progress: Flow<ProgressStats> = store.data.map { p ->
        val today = LocalDate.now().toEpochDay()
        val streakLastDay = p[Keys.STREAK_LAST_DAY]
        val streakStored = p[Keys.STREAK_COUNT] ?: 0
        // Serię pokazujemy tylko, jeśli aktywność była dziś lub wczoraj; inaczej jest przerwana.
        val streak = when {
            streakLastDay == null -> 0
            streakLastDay == today || streakLastDay == today - 1 -> streakStored
            else -> 0
        }
        val todayDay = p[Keys.TODAY_DAY]
        val todayCount = if (todayDay == today) (p[Keys.TODAY_COUNT] ?: 0) else 0
        val goal = p[Keys.DAILY_GOAL] ?: 20
        val backupDay = p[Keys.BACKUP_LAST_DAY]
        val daysSince = backupDay?.let { (today - it).toInt() }
        ProgressStats(
            streak = streak,
            today = todayCount,
            dailyGoal = goal,
            daysSinceBackup = daysSince,
            backupOverdue = daysSince == null || daysSince >= 7
        )
    }

    /** Rejestruje jedną ocenę: aktualizuje serię dni i licznik „dziś". */
    suspend fun recordGrade() = store.edit { p ->
        val today = LocalDate.now().toEpochDay()
        val lastDay = p[Keys.STREAK_LAST_DAY]
        val streak = p[Keys.STREAK_COUNT] ?: 0
        p[Keys.STREAK_COUNT] = when {
            lastDay == null -> 1
            lastDay == today -> streak.coerceAtLeast(1)  // już liczone dziś
            lastDay == today - 1 -> streak + 1           // kolejny dzień z rzędu
            else -> 1                                    // przerwa — reset
        }
        p[Keys.STREAK_LAST_DAY] = today
        val todayDay = p[Keys.TODAY_DAY]
        val curCount = if (todayDay == today) (p[Keys.TODAY_COUNT] ?: 0) else 0
        p[Keys.TODAY_COUNT] = curCount + 1
        p[Keys.TODAY_DAY] = today
    }

    suspend fun setDailyGoal(goal: Int) =
        store.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(5, 200) }

    /** Zapisuje, że właśnie zrobiono kopię zapasową (dziś). */
    suspend fun recordBackupDone() =
        store.edit { it[Keys.BACKUP_LAST_DAY] = LocalDate.now().toEpochDay() }
}
