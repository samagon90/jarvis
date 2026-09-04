package com.jarvis.master.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.Part
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PartEditViewModel(
    private val repository: RepairRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val partId: Long = savedStateHandle["partId"] ?: 0L

    private val _form = MutableStateFlow(PartForm())
    val form = _form.asStateFlow()

    init {
        if (partId != 0L) {
            viewModelScope.launch {
                repository.observeParts().collect { list ->
                    val p = list.firstOrNull { it.id == partId }
                    if (p != null) _form.value = p.toForm()
                }
            }
        }
    }

    fun update(f: (PartForm) -> PartForm) {
        _form.value = f(_form.value)
    }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (f.name.isBlank()) return
        viewModelScope.launch {
            repository.savePart(
                Part(
                    id = partId,
                    name = f.name.trim(),
                    category = f.category,
                    barcode = f.barcode.trim(),
                    quantity = f.quantity,
                    minQuantity = f.minQuantity,
                    purchasePrice = f.purchase,
                    sellPrice = f.sell,
                    location = f.location.trim(),
                    note = f.note.trim()
                )
            )
            onDone()
        }
    }

    private fun Part.toForm() = PartForm(
        name = name,
        category = category,
        barcode = barcode,
        quantity = quantity,
        minQuantity = minQuantity,
        purchase = purchasePrice,
        sell = sellPrice,
        location = location,
        note = note
    )
}

data class PartForm(
    val name: String = "",
    val category: String = "Дисплей",
    val barcode: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 1,
    val purchase: Double = 0.0,
    val sell: Double = 0.0,
    val location: String = "",
    val note: String = ""
)
