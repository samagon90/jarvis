package com.jarvis.master.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.settings.Settings
import com.jarvis.master.data.settings.SettingsRepository
import com.jarvis.master.util.BackupData
import com.jarvis.master.util.ExportUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: RepairRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    fun save(f: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(f) }
    }

    /** Экспортирует резервную копию всех данных в системное окно «Поделиться». */
    fun exportBackup() {
        viewModelScope.launch {
            val backup = BackupData(
                clients = repository.observeClients().snapshot(),
                repairs = repository.observeRepairs().snapshot(),
                parts = repository.observeParts().snapshot(),
                transactions = repository.observeTransactions().snapshot()
            )
            ExportUtils.shareBackup(context, backup)
        }
    }

    private suspend fun <T> Flow<T>.snapshot(): T = first()
}
