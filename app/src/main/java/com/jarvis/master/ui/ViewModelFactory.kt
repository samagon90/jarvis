package com.jarvis.master.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.settings.SettingsRepository

/**
 * Фабрика ViewModel-ов, снабжающая их репозиториями.
 */
class ViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val repository: RepairRepository = RepairRepository.getInstance(appContext)
    private val settingsRepository: SettingsRepository = SettingsRepository(appContext)

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val handle: SavedStateHandle = extras.createSavedStateHandle()
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(RepairsViewModel::class.java) ->
                RepairsViewModel(repository) as T
            modelClass.isAssignableFrom(RepairEditViewModel::class.java) ->
                RepairEditViewModel(repository, handle) as T
            modelClass.isAssignableFrom(RepairDetailViewModel::class.java) ->
                RepairDetailViewModel(repository, handle) as T
            modelClass.isAssignableFrom(PartsViewModel::class.java) ->
                PartsViewModel(repository, appContext) as T
            modelClass.isAssignableFrom(PartEditViewModel::class.java) ->
                PartEditViewModel(repository, handle) as T
            modelClass.isAssignableFrom(MoneyViewModel::class.java) ->
                MoneyViewModel(repository, appContext) as T
            modelClass.isAssignableFrom(ClientsViewModel::class.java) ->
                ClientsViewModel(repository) as T
            modelClass.isAssignableFrom(ClientDetailViewModel::class.java) ->
                ClientDetailViewModel(repository, handle) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository, settingsRepository, appContext) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
