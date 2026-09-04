package com.jarvis.master.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.Part
import com.jarvis.master.util.ExportUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PartsViewModel(
    private val repository: RepairRepository,
    private val context: Context
) : ViewModel() {

    val parts: StateFlow<List<Part>> = repository.observeParts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStock: StateFlow<List<Part>> = repository.observeLowStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(part: Part) {
        viewModelScope.launch { repository.deletePart(part) }
    }

    fun exportCsv() {
        viewModelScope.launch {
            ExportUtils.sharePartsCsv(context, parts.value)
        }
    }
}
