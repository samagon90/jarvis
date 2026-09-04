package com.jarvis.master.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.DashboardStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(repository: RepairRepository) : ViewModel() {

    val stats: StateFlow<DashboardStats> = repository.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}
