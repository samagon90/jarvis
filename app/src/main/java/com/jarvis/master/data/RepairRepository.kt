package com.jarvis.master.data

import com.jarvis.master.data.db.AppDatabase
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.Part
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairDao
import com.jarvis.master.data.db.RepairPart
import com.jarvis.master.data.db.RepairStatus
import com.jarvis.master.data.db.Transaction
import com.jarvis.master.data.db.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

/**
 * Единая точка доступа к данным и бизнес-логике мастерской.
 */
class RepairRepository private constructor(
    private val db: AppDatabase
) {
    private val repairDao: RepairDao = db.repairDao()
    private val partDao = db.partDao()
    private val transactionDao = db.transactionDao()
    private val clientDao = db.clientDao()

    // ---- Клиенты ----
    fun observeClients(): Flow<List<Client>> = clientDao.observeAll()
    fun observeClient(id: Long): Flow<Client?> = clientDao.observeById(id)
    suspend fun saveClient(client: Client): Long =
        if (client.id == 0L) clientDao.insert(client) else { clientDao.update(client); client.id }
    suspend fun deleteClient(client: Client) = clientDao.delete(client)

    // ---- Запчасти ----
    fun observeParts(): Flow<List<Part>> = partDao.observeAll()
    fun observeLowStock(): Flow<List<Part>> = partDao.observeLowStock()
    suspend fun savePart(part: Part): Long =
        if (part.id == 0L) partDao.insert(part) else { partDao.update(part); part.id }
    suspend fun deletePart(part: Part) = partDao.delete(part)
    fun observePartsForRepair(repairId: Long): Flow<List<RepairPart>> =
        partDao.observePartsForRepair(repairId)

    /** Списывает запчасти в заказ: уменьшает остаток, добавляет связи, начисляет себестоимость. */
    suspend fun addPartToRepair(repairId: Long, partId: Long, quantity: Int) {
        val part = partDao.getById(partId) ?: return
        val qty = quantity.coerceAtLeast(1)
        partDao.insertRepairPart(
            RepairPart(repairId = repairId, partId = partId, quantity = qty, priceAtTime = part.purchasePrice)
        )
        partDao.changeQuantity(partId, -qty)
        repairDao.update(
            repairDao.getById(repairId)?.copy(partsCost = partsCostNow(repairId)) ?: return
        )
    }

    suspend fun removePartFromRepair(repairPart: RepairPart) {
        partDao.changeQuantity(repairPart.partId, repairPart.quantity)
        partDao.deleteRepairPart(repairPart)
    }

    private suspend fun partsCostNow(repairId: Long): Double =
        partDao.getPartsForRepair(repairId).sumOf { it.quantity * it.priceAtTime }

    // ---- Ремонты ----
    fun observeRepairs(): Flow<List<Repair>> = repairDao.observeAll()
    fun observeRepair(id: Long): Flow<Repair?> = repairDao.observeById(id)
    fun observeRepairsByClient(clientId: Long): Flow<List<Repair>> = repairDao.observeByClient(clientId)
    fun observeReadyOverdue(now: Long): Flow<List<Repair>> = repairDao.observeReadyOverdue(now)

    suspend fun saveRepair(repair: Repair): Long =
        if (repair.id == 0L) repairDao.insert(repair) else { repairDao.update(repair); repair.id }

    suspend fun deleteRepair(repair: Repair) = repairDao.delete(repair)

    /** Перевести заказ в статус «Готов». */
    suspend fun markReady(repair: Repair) {
        repairDao.update(
            repair.copy(status = RepairStatus.READY.name, readyDate = System.currentTimeMillis())
        )
    }

    /**
     * Выдать устройство клиенту. Создаёт транзакцию дохода в размере фактически полученной суммы.
     */
    suspend fun issueRepair(repair: Repair, received: Double) {
        val receivedSum = received.coerceIn(0.0, repair.price)
        val updated = repair.copy(
            status = RepairStatus.ISSUED.name,
            issuedDate = System.currentTimeMillis(),
            receivedPayment = receivedSum
        )
        repairDao.update(updated)
        transactionDao.insert(
            Transaction(
                type = TransactionType.INCOME.name,
                category = "Ремонт",
                amount = receivedSum,
                date = System.currentTimeMillis(),
                repairId = repair.id,
                description = "Оплата ремонта: ${repair.deviceName}"
            )
        )
    }

    // ---- Финансы ----
    fun observeTransactions(): Flow<List<Transaction>> = transactionDao.observeAll()

    fun observeIncomeBetween(from: Long, to: Long): Flow<Double> = transactionDao.incomeBetween(from, to)
    fun observeExpenseBetween(from: Long, to: Long): Flow<Double> = transactionDao.expenseBetween(from, to)

    suspend fun addExpense(category: String, amount: Double, description: String) {
        transactionDao.insert(
            Transaction(
                type = TransactionType.EXPENSE.name,
                category = category,
                amount = amount,
                date = System.currentTimeMillis(),
                description = description
            )
        )
    }

    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)

    fun observeDaily(from: Long, to: Long) = transactionDao.dailyAggregates(from, to)

    // ---- Дашборд ----
    /** Сводные показатели для главного экрана. */
    fun observeDashboard(): Flow<DashboardStats> {
        val now = System.currentTimeMillis()
        val dayStart = startOfDay(now)
        val todayEnd = now + 24 * 3600_000L
        val weekStart = startOfDay(now - 6L * 24 * 3600_000L)

        return combine(
            repairDao.countAll(),
            repairDao.countInProgress(),
            repairDao.countReady(),
            partDao.observeLowStock(),
            transactionDao.incomeBetween(dayStart, todayEnd),
            transactionDao.incomeBetween(weekStart, now),
            transactionDao.expenseBetween(weekStart, now),
            repairDao.observeAll()
        ) { a: Array<Any> ->
            val all = a[0] as Int
            val inProgress = a[1] as Int
            val ready = a[2] as Int
            @Suppress("UNCHECKED_CAST")
            val lowParts = a[3] as List<Part>
            val todayIncome = a[4] as Double
            val weekIncome = a[5] as Double
            val weekExpense = a[6] as Double
            @Suppress("UNCHECKED_CAST")
            val repairs = a[7] as List<Repair>
            val readyOverdue = repairs.filter {
                it.statusEnum == RepairStatus.READY && (it.readyDate ?: 0) <= now
            }
            val totalDebt = repairs.filter { it.statusEnum != RepairStatus.CANCELLED }
                .sumOf { it.debt }
            DashboardStats(
                totalRepairs = all,
                inProgress = inProgress,
                ready = ready,
                lowStockCount = lowParts.size,
                lowStockParts = lowParts,
                todayIncome = todayIncome,
                weekIncome = weekIncome,
                weekExpense = weekExpense,
                weekProfit = weekIncome - weekExpense,
                readyOverdue = readyOverdue,
                totalDebt = totalDebt
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RepairRepository? = null

        fun getInstance(db: AppDatabase): RepairRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RepairRepository(db).also { INSTANCE = it }
            }

        private fun startOfDay(ts: Long): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = ts
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
    }
}

/** Сводные показатели главного экрана. */
data class DashboardStats(
    val totalRepairs: Int = 0,
    val inProgress: Int = 0,
    val ready: Int = 0,
    val lowStockCount: Int = 0,
    val lowStockParts: List<Part> = emptyList(),
    val todayIncome: Double = 0.0,
    val weekIncome: Double = 0.0,
    val weekExpense: Double = 0.0,
    val weekProfit: Double = 0.0,
    val readyOverdue: List<Repair> = emptyList(),
    val totalDebt: Double = 0.0
)
