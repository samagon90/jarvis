package com.jarvis.master.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Связь «ремонт ↔ запчасть»: сколько единиц конкретной запчасти ушло в заказ. */
@Entity(
    tableName = "repair_parts",
    primaryKeys = ["repairId", "partId"],
    foreignKeys = [
        ForeignKey(
            entity = Repair::class,
            parentColumns = ["id"],
            childColumns = ["repairId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Part::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partId")]
)
data class RepairPart(
    val repairId: Long,
    val partId: Long,
    val quantity: Int = 1,
    /** Цена за единицу на момент списания. */
    val priceAtTime: Double = 0.0
)
