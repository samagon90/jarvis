package com.jarvis.master.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.Client
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientsViewModel(
    private val repository: RepairRepository
) : ViewModel() {

    val clients: StateFlow<List<Client>> = repository.observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(client: Client, onDone: () -> Unit) {
        if (client.name.isBlank()) return
        viewModelScope.launch {
            repository.saveClient(client)
            onDone()
        }
    }
}
