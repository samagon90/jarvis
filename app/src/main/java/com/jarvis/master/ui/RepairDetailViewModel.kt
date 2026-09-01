package com.jarvis.master.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairPart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RepairDetailViewModel(
    private val repository: RepairRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repairId: Long = savedStateHandle["repairId"] ?: 0L

    /** Объединяет заказ, привязанного клиента и использованные запчасти. */
    val uiState: StateFlow<RepairDetailUi> = combine(
        repository.observeRepair(repairId),
        repository.observeClients(),
        repository.observePartsForRepair(repairId)
    ) { repair, clients, parts ->
        val client = repair?.clientId?.let { id -> clients.firstOrNull { it.id == id } }
        RepairDetailUi(repair, client, parts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RepairDetailUi())

    fun markReady() {
        viewModelScope.launch {
            uiState.value.repair?.let { repository.markReady(it) }
        }
    }

    fun issue(received: Double) {
        viewModelScope.launch {
            uiState.value.repair?.let { repository.issueRepair(it, received) }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            uiState.value.repair?.let { repository.deleteRepair(it) }
            onDone()
        }
    }
}

data class RepairDetailUi(
    val repair: Repair? = null,
    val client: Client? = null,
    val parts: List<RepairPart> = emptyList()
)
