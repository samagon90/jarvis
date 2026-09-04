package com.jarvis.master.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.Repair
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientDetailViewModel(
    private val repository: RepairRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clientId: Long = savedStateHandle["clientId"] ?: 0L

    val uiState: StateFlow<ClientDetailUi> = combine(
        repository.observeClient(clientId),
        repository.observeRepairsByClient(clientId)
    ) { client, repairs ->
        ClientDetailUi(client, repairs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientDetailUi())

    fun save(client: Client, onDone: () -> Unit) {
        if (client.name.isBlank()) return
        viewModelScope.launch {
            repository.saveClient(client)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            uiState.value.client?.let { repository.deleteClient(it) }
            onDone()
        }
    }
}

data class ClientDetailUi(
    val client: Client? = null,
    val repairs: List<Repair> = emptyList()
)
