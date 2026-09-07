package com.jarvis.master.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairPhotoDao {

    @Insert
    suspend fun insert(photo: RepairPhoto): Long

    @Delete
    suspend fun delete(photo: RepairPhoto)

    @Query("SELECT * FROM repair_photos WHERE repairId = :repairId ORDER BY createdAt DESC")
    fun observeByRepair(repairId: Long): Flow<List<RepairPhoto>>

    @Query("SELECT * FROM repair_photos WHERE repairId = :repairId")
    suspend fun getByRepair(repairId: Long): List<RepairPhoto>
}
