package com.jarvis.master.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Запчасть / расходник на складе. */
@Serializable
@Entity(tableName = "parts")
data class Part(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val category: String = "Дисплей",
    /** Штрих-код / артикул поставщика. */
    val barcode: String = "",
    val quantity: Int = 0,
    /** Минимальный остаток, при котором показывается алерт. */
    val minQuantity: Int = 1,
    val purchasePrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    /** Где лежит на складе (полка/ящик). */
    val location: String = "",
    val note: String = ""
) {
    val isLow: Boolean
        get() = quantity <= minQuantity
}
