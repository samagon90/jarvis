package com.jarvis.master.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {

    @Insert
    suspend fun insert(repair: Repair): Long

    @Update
    suspend fun update(repair: Repair)

    @Delete
    suspend fun delete(repair: Repair)

    @Query("SELECT * FROM repairs ORDER BY receivedDate DESC")
    fun observeAll(): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE id = :id")
    fun observeById(id: Long): Flow<Repair?>

    @Query("SELECT * FROM repairs WHERE id = :id")
    suspend fun getById(id: Long): Repair?

    @Query("SELECT * FROM repairs WHERE clientId = :clientId ORDER BY receivedDate DESC")
    fun observeByClient(clientId: Long): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE status = :status ORDER BY receivedDate DESC")
    fun observeByStatus(status: String): Flow<List<Repair>>

    @Query("SELECT COUNT(*) FROM repairs")
    fun countAll(): Flow<Int>

    @Query("SELECT COUNT(*) FROM repairs WHERE status IN ('NEW','DIAGNOSED','IN_WORK','AWAITING_PART')")
    fun countInProgress(): Flow<Int>

    @Query("SELECT COUNT(*) FROM repairs WHERE status = 'READY'")
    fun countReady(): Flow<Int>

    @Query("SELECT * FROM repairs WHERE status = 'READY' AND readyDate <= :now ORDER BY readyDate ASC")
    fun observeReadyOverdue(now: Long): Flow<List<Repair>>
}
