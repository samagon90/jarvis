package com.jarvis.master.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {

    @Insert
    suspend fun insert(part: Part): Long

    @Update
    suspend fun update(part: Part)

    @Delete
    suspend fun delete(part: Part)

    @Query("SELECT * FROM parts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Part>>

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getById(id: Long): Part?

    @Query("SELECT * FROM parts WHERE quantity <= minQuantity ORDER BY quantity ASC")
    fun observeLowStock(): Flow<List<Part>>

    @Query("SELECT * FROM repair_parts WHERE repairId = :repairId")
    fun observePartsForRepair(repairId: Long): Flow<List<RepairPart>>

    @Query("SELECT * FROM repair_parts WHERE repairId = :repairId")
    suspend fun getPartsForRepair(repairId: Long): List<RepairPart>

    @Insert
    suspend fun insertRepairPart(repairPart: RepairPart)

    @Delete
    suspend fun deleteRepairPart(repairPart: RepairPart)

    @Query("UPDATE parts SET quantity = quantity + :delta WHERE id = :partId")
    suspend fun changeQuantity(partId: Long, delta: Int)
}
