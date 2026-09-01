package com.jarvis.master.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Финансовая операция: доход или расход. */
@Serializable
@Entity(tableName = "transactions", indices = [Index("repairId")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = TransactionType.INCOME.name,
    val category: String = "Ремонт",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val repairId: Long? = null,
    val description: String = ""
) {
    val typeEnum: TransactionType
        get() = TransactionType.from(type)
}
