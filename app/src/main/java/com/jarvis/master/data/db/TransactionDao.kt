package com.jarvis.master.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<Transaction>>

    /** Сумма доходов за период [from, to]. */
    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='INCOME' AND date BETWEEN :from AND :to")
    fun incomeBetween(from: Long, to: Long): Flow<Double>

    /** Сумма расходов за период [from, to]. */
    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='EXPENSE' AND date BETWEEN :from AND :to")
    fun expenseBetween(from: Long, to: Long): Flow<Double>

    /** Сгруппированные суммы по категориям и дням (для графиков). */
    @Query(
        "SELECT date, type, SUM(amount) as sum FROM transactions " +
            "WHERE date BETWEEN :from AND :to GROUP BY date, type ORDER BY date"
    )
    fun dailyAggregates(from: Long, to: Long): Flow<List<DailyAggregate>>
}

/** Агрегат «день + тип + сумма» для графика. */
data class DailyAggregate(
    val date: Long,
    val type: String,
    val sum: Double
)
