package com.pairomatic.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pairomatic.PairOMaticApp
import com.pairomatic.data.settings.AppSettings
import com.pairomatic.data.settings.LearningMode
import com.pairomatic.data.settings.NotificationImportance
import com.pairomatic.data.settings.ProgressStats
import com.pairomatic.data.settings.ThemeMode
import androidx.work.ExistingPeriodicWorkPolicy
import com.pairomatic.immersion.ImmersionWorker
import com.pairomatic.reminder.ReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as PairOMaticApp).container
    private val settingsRepo = container.settingsRepository

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val progress: StateFlow<ProgressStats> = settingsRepo.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressStats())

    fun setMode(mode: LearningMode) {
        viewModelScope.launch {
            settingsRepo.setMode(mode)
            val current = settings.value
            applyImmersionScheduling(mode, current.immersionIntervalMinutes, current.notificationsEnabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setNotificationsEnabled(enabled)
            val current = settings.value
            applyImmersionScheduling(current.mode, current.immersionIntervalMinutes, enabled)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setQuietHoursEnabled(enabled) }
    }

    fun setQuietHours(startMinute: Int, endMinute: Int) {
        viewModelScope.launch { settingsRepo.setQuietHours(startMinute, endMinute) }
    }

    fun setImmersionInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepo.setImmersionInterval(minutes)
            val current = settings.value
            applyImmersionScheduling(current.mode, minutes, current.notificationsEnabled)
        }
    }

    fun setImportance(importance: NotificationImportance) {
        viewModelScope.launch { settingsRepo.setImportance(importance) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    /** Ponownie pokazuje ekran onboardingu (uprawnienia + bateria). */
    fun reopenOnboarding() {
        viewModelScope.launch { settingsRepo.setOnboardingDone(false) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setReminderEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) ReminderWorker.schedule(
                context,
                settings.value.reminderMinuteOfDay,
                ExistingPeriodicWorkPolicy.UPDATE
            )
            else ReminderWorker.cancel(context)
        }
    }

    fun setReminderTime(minuteOfDay: Int) {
        viewModelScope.launch {
            settingsRepo.setReminderTime(minuteOfDay)
            if (settings.value.reminderEnabled) {
                ReminderWorker.schedule(getApplication(), minuteOfDay, ExistingPeriodicWorkPolicy.UPDATE)
            }
        }
    }

    /** Ręczne uruchomienie pierwszego powiadomienia bieżącego trybu. */
    fun startNow() {
        viewModelScope.launch {
            container.notificationScheduler.postFirst()
            _message.value = "Uruchomiono naukę"
        }
    }

    fun export(target: Uri, includeStats: Boolean) {
        viewModelScope.launch {
            val result = runCatching { container.pairRepository.exportToZip(target, includeStats) }
            if (result.isSuccess) settingsRepo.recordBackupDone()
            _message.value = if (result.isSuccess) "Wyeksportowano talię" else "Błąd eksportu"
        }
    }

    fun exportCsv(target: Uri) {
        viewModelScope.launch {
            val result = runCatching { container.pairRepository.exportToCsv(target) }
            _message.value = if (result.isSuccess) "Wyeksportowano CSV" else "Błąd eksportu CSV"
        }
    }

    fun import(source: Uri, replaceAll: Boolean) {
        viewModelScope.launch {
            val result = runCatching { container.pairRepository.importFromZip(source, replaceAll) }
            _message.value = if (result.isSuccess) "Zaimportowano talię" else "Błąd importu"
        }
    }

    fun importCsv(source: Uri, replaceAll: Boolean) {
        viewModelScope.launch {
            val result = runCatching { container.pairRepository.importFromCsv(source, replaceAll) }
            _message.value = if (result.isSuccess) "Zaimportowano pary z CSV" else "Błąd importu CSV"
        }
    }

    fun consumeMessage() { _message.value = null }

    private fun applyImmersionScheduling(mode: LearningMode, intervalMinutes: Int, enabled: Boolean) {
        val context = getApplication<Application>()
        if (mode == LearningMode.IMMERSION && enabled) {
            ImmersionWorker.schedule(context, intervalMinutes)
        } else {
            ImmersionWorker.cancel(context)
        }
    }
}
