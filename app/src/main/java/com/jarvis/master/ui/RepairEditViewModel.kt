package com.jarvis.master.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RepairEditViewModel(
    private val repository: RepairRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repairId: Long = savedStateHandle["repairId"] ?: 0L

    val clients: StateFlow<List<Client>> = repository.observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(
        RepairForm(
            deviceName = "",
            serialNumber = "",
            issue = "",
            diagnosis = "",
            clientId = null,
            price = "",
            status = RepairStatus.NEW
        )
    )
    val form: StateFlow<RepairForm> = _form.asStateFlow()

    init {
        if (repairId != 0L) {
            viewModelScope.launch {
                repository.observeRepair(repairId).collect { r ->
                    if (r != null) {
                        _form.value = r.toForm()
                    }
                }
            }
        }
    }

    fun update(f: (RepairForm) -> RepairForm) {
        _form.value = f(_form.value)
    }

    fun save(onDone: (Long) -> Unit) {
        val f = _form.value
        if (f.deviceName.isBlank()) return
        viewModelScope.launch {
            val repair = Repair(
                id = repairId,
                clientId = f.clientId,
                deviceName = f.deviceName.trim(),
                serialNumber = f.serialNumber.trim(),
                issue = f.issue.trim(),
                diagnosis = f.diagnosis.trim(),
                status = f.status.name,
                price = f.price.toDoubleOrNull() ?: 0.0,
                warrantyDays = 30
            )
            val id = repository.saveRepair(repair)
            onDone(id)
        }
    }

    private fun Repair.toForm() = RepairForm(
        deviceName = deviceName,
        serialNumber = serialNumber,
        issue = issue,
        diagnosis = diagnosis,
        clientId = clientId,
        price = if (price == 0.0) "" else price.toString(),
        status = statusEnum
    )
}

/** Данные формы создания/редактирования ремонта. */
data class RepairForm(
    val deviceName: String = "",
    val serialNumber: String = "",
    val issue: String = "",
    val diagnosis: String = "",
    val clientId: Long? = null,
    val price: String = "",
    val status: RepairStatus = RepairStatus.NEW
)
