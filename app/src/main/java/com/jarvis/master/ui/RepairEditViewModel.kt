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

    /** Нужен для автоподсказок по уже введённым устройствам/серийникам. */
    val repairs: StateFlow<List<Repair>> = repository.observeRepairs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(
        RepairForm(
            deviceName = "",
            serialNumber = "",
            issue = "",
            diagnosis = "",
            clientId = null,
            clientName = "",
            clientPhone = "",
            price = "",
            status = RepairStatus.NEW
        )
    )
    val form: StateFlow<RepairForm> = _form.asStateFlow()

    init {
        if (repairId != 0L) {
            viewModelScope.launch {
                repository.observeRepair(repairId).collect { r ->
                    if (r != null) _form.value = r.toForm()
                }
            }
        }
    }

    fun update(f: (RepairForm) -> RepairForm) {
        _form.value = f(_form.value)
    }

    /** Находит клиента по имени; если такой есть — привязывает и подставляет телефон. */
    fun onClientNameChange(name: String) {
        val c = clients.value.firstOrNull { it.name.equals(name.trim(), true) }
        if (c != null) {
            _form.value = _form.value.copy(clientName = c.name, clientPhone = c.phone, clientId = c.id)
        } else {
            _form.value = _form.value.copy(clientName = name, clientId = null)
        }
    }

    fun onClientSelect(c: Client) {
        _form.value = _form.value.copy(clientName = c.name, clientPhone = c.phone, clientId = c.id)
    }

    fun save(onDone: (Long) -> Unit) {
        val f = _form.value
        if (f.deviceName.isBlank()) return
        viewModelScope.launch {
            var cid = f.clientId
            // Если имя клиента введено и не совпадает с выбранным — создаём нового клиента
            if (f.clientName.isNotBlank()) {
                val existing = cid?.let { id -> clients.value.firstOrNull { it.id == id } }
                if (existing == null || !existing.name.equals(f.clientName.trim(), true)) {
                    cid = repository.saveClient(
                        Client(name = f.clientName.trim(), phone = f.clientPhone.trim())
                    )
                }
            }
            // Нового клиента не удалось сохранить в облако — ремонт не создаём.
            if (f.clientName.isNotBlank() && cid == 0L) return@launch
            val repair = Repair(
                id = repairId,
                clientId = cid,
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
        clientName = "",
        clientPhone = "",
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
    val clientName: String = "",
    val clientPhone: String = "",
    val price: String = "",
    val status: RepairStatus = RepairStatus.NEW
)
