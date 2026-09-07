package com.jarvis.master.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Фото устройства, привязанное к ремонту.
 * Изображение хранится локально в приватной папке приложения, в таблице
 * сохраняется путь к файлу (url). Также при доступном облаке копия уходит
 * в фоне в Supabase Storage (бесшумно, не блокируя работу).
 */
@Entity(tableName = "repair_photos")
data class RepairPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repairId: Long = 0,
    /** Локальный путь к файлу изображения. */
    val url: String = "",
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
