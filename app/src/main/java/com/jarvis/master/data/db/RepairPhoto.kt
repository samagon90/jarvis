package com.jarvis.master.data.db

import kotlinx.serialization.Serializable

/**
 * Фото устройства, привязанное к ремонту. Само изображение хранится в облачном
 * хранилище Supabase (Storage), в таблице хранится его публичный URL.
 */
@Serializable
data class RepairPhoto(
    val id: Long = 0,
    val repairId: Long = 0,
    val url: String = "",
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
