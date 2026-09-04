package com.jarvis.master.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RepairsViewModel(private val repository: RepairRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<RepairStatus?>(null)

    val repairs: StateFlow<List<Repair>> =
        combine(repository.observeRepairs(), query, statusFilter) { list, q, st ->
            list.filter { r ->
                val textOk = q.isBlank() ||
                    r.deviceName.contains(q, true) ||
                    r.serialNumber.contains(q, true) ||
                    r.issue.contains(q, true)
                val statusOk = st == null || r.statusEnum == st
                textOk && statusOk
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filters = statusFilter.asStateFlow()
    val queryState = query.asStateFlow()

    fun setQuery(q: String) { query.value = q }
    fun setStatusFilter(s: RepairStatus?) { statusFilter.value = s }
}
