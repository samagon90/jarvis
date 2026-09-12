package com.jarvis.master.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.SyncBus
import com.jarvis.master.util.Formatters
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairPart
import com.jarvis.master.data.db.RepairPhoto
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

    /** Объединяет заказ, клиента, запчасти и фото. */
    val uiState: StateFlow<RepairDetailUi> = combine(
        repository.observeRepair(repairId),
        repository.observeClients(),
        repository.observePartsForRepair(repairId),
        repository.observePhotosForRepair(repairId)
    ) { repair, clients, parts, photos ->
        val client = repair?.clientId?.let { id -> clients.firstOrNull { it.id == id } }
        RepairDetailUi(repair, client, parts, photos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RepairDetailUi())

    fun addPhoto(bytes: ByteArray, mime: String, ext: String) {
        if (repairId == 0L) return
        viewModelScope.launch {
            repository.addPhotoToRepair(repairId, bytes, mime, ext)
        }
    }

    fun markReady() {
        viewModelScope.launch {
            uiState.value.repair?.let { repository.markReady(it) }
        }
    }

    /** Закрывает ремонт с указанной полученной суммой (0 — в долг). */
    fun issue(received: Double) {
        viewModelScope.launch {
            val r = uiState.value.repair ?: return@launch
            val ok = repository.issueRepair(r, received)
            if (ok) {
                SyncBus.notify(
                    if (received > 0) "Ремонт закрыт. Приход: ${Formatters.money(received)}"
                    else "Ремонт закрыт в долг: ${Formatters.money(r.price - received)}"
                )
            }
        }
    }

    /** Принимает доплату (гасит долг) по выданному ремонту. */
    fun acceptPayment(amount: Double) {
        viewModelScope.launch {
            val r = uiState.value.repair ?: return@launch
            val ok = repository.acceptPayment(r, amount)
            if (ok) SyncBus.notify("Принята оплата: ${Formatters.money(amount)}")
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
    val parts: List<RepairPart> = emptyList(),
    val photos: List<RepairPhoto> = emptyList()
)
