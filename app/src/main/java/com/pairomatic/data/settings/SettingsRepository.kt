package com.pairomatic.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}
