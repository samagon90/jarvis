package com.jarvis.master.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Клиент мастерской. */
@Serializable
@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val telegram: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
