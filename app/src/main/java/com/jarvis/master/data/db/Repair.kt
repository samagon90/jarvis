package com.jarvis.master.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Заказ-наряд на ремонт устройства. */
@Serializable
@Entity(
    tableName = "repairs",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("clientId")]
)
data class Repair(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long? = null,
    val deviceName: String = "",
    val serialNumber: String = "",
    val issue: String = "",
    val diagnosis: String = "",
    val status: String = RepairStatus.NEW.name,
    /** Стоимость работы для клиента. */
    val price: Double = 0.0,
    /** Себестоимость запчастей по этому заказу. */
    val partsCost: Double = 0.0,
    val receivedDate: Long = System.currentTimeMillis(),
    val readyDate: Long? = null,
    val issuedDate: Long? = null,
    /** Дней гарантии после выдачи. */
    val warrantyDays: Int = 30,
    /** Сколько фактически заплатил клиент (может быть меньше [price] — тогда остаётся долг). */
    val receivedPayment: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val statusEnum: RepairStatus
        get() = RepairStatus.from(status)

    /** Оставшийся долг клиента по этому заказу. */
    val debt: Double
        get() = if (receivedPayment < price) price - receivedPayment else 0.0
}
