package com.jarvis.master.data

import android.content.Context
import android.util.Log
import com.jarvis.master.data.cloud.SupabaseCloud
import com.jarvis.master.data.db.AppDatabase
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.DailyAggregate
import com.jarvis.master.data.db.Part
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairDao
import com.jarvis.master.data.db.RepairPart
import com.jarvis.master.data.db.RepairPhoto
import com.jarvis.master.data.db.RepairPhotoDao
import com.jarvis.master.data.db.RepairStatus
import com.jarvis.master.data.db.Transaction
import com.jarvis.master.data.db.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

/**
 * Единая точка доступа к данным мастерской.
 *
 * **Облако (Supabase) — единый источник данных.** Ремонты и клиенты пишутся в облако,
 * а локальная база Room служит только быстрым кэшем для мгновенного чтения и офлайн-просмотра.
 * Кэш периодически (и после каждого запуска) обновляется из облака, поэтому ремонт,
 * добавленный на одном телефоне, появляется на втором.
 *
 * Запчасти, финансы и фото пока хранятся локально (переносятся в облако на следующем этапе).
 * Создание/изменение ремонтов и клиентов требует интернета; при его отсутствии операция
 * не выполняется и пользователь получает сообщение об ошибке.
 */
class RepairRepository private constructor(context: Context) {

    private val db: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val appContext = context.applicationContext
    private val repairDao: RepairDao = db.repairDao()
    private val partDao = db.partDao()
    private val transactionDao = db.transactionDao()
    private val clientDao = db.clientDao()
    private val photoDao: RepairPhotoDao = db.repairPhotoDao()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Запускает фоновое обновление кэша из облака каждые N секунд. */
    fun startAutoSync() {
        appScope.launch {
            // Один раз переносим уже существующие локальные ремонты/клиентов в облако,
            // чтобы переход на единую базу не потерял прежние данные.
            firstRunPushToCloud()
            while (true) {
                try { refreshCloud() } catch (e: Exception) { /* тихо на фоне */ }
                delay(AUTO_SYNC_MS)
            }
        }
    }

    /**
     * Одноразовый перенос прежних локальных ремонтов и клиентов в облако (с перепривязкой
     * clientId на новые серверные id). Выполняется только если ещё не выполнен.
     */
    private suspend fun firstRunPushToCloud() {
        val prefs = appContext.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_CLOUD_INITIALIZED, false)) return
        try {
            val oldToNewClientId = HashMap<Long, Long>()
            clientDao.observeAll().first().forEach { c ->
                if (c.id != 0L) {
                    val created = SupabaseCloud.createClient(c)
                    oldToNewClientId[c.id] = created.id
                }
            }
            repairDao.observeAll().first().forEach { r ->
                if (r.id != 0L) {
                    val newClientId = r.clientId?.let { oldToNewClientId[it] } ?: r.clientId
                    val toPush = if (newClientId == r.clientId) r else r.copy(clientId = newClientId)
                    SupabaseCloud.createRepair(toPush)
                }
            }
            // Локальный кэш приводим в соответствие с облаком (серверные id).
            refreshCloud()
            prefs.edit().putBoolean(KEY_CLOUD_INITIALIZED, true).apply()
        } catch (e: Exception) {
            Log.w("RepairRepository", "firstRunPushToCloud: ${e.message}")
        }
    }

    /**
     * Обновляет локальный кэш ремонтов и клиентов из облака (diff-слияние по id).
     */
    suspend fun refreshCloud() {
        val remoteClients = SupabaseCloud.fetchClients()
        val remoteRepairs = SupabaseCloud.fetchRepairs()
        withContext(Dispatchers.IO) {
            diffClients(remoteClients)
            diffRepairs(remoteRepairs, remoteClients.map { it.id }.toSet())
        }
    }

    private suspend fun diffClients(remote: List<Client>) {
        val current = clientDao.observeAll().first()
        val curById = current.associateBy { it.id }
        for (rc in remote) {
            val cur = curById[rc.id]
            when {
                cur == null -> clientDao.insert(rc)
                cur != rc -> clientDao.update(rc)
            }
        }
        val remoteIds = remote.map { it.id }.toSet()
        current.filter { it.id !in remoteIds }.forEach { clientDao.delete(it) }
    }

    private suspend fun diffRepairs(remote: List<Repair>, remoteClientIds: Set<Long>) {
        val current = repairDao.observeAll().first()
        val curById = current.associateBy { it.id }
        for (raw in remote) {
            // Если клиент удалён и в облаке ремонт остался со ссылкой на него — снимаем ссылку,
            // чтобы не нарушить локальный внешний ключ.
            val rr = if (raw.clientId != null && raw.clientId !in remoteClientIds)
                raw.copy(clientId = null) else raw
            val cur = curById[rr.id]
            when {
                cur == null -> repairDao.insert(rr)
                cur != rr -> repairDao.update(rr)
            }
        }
        val remoteIds = remote.map { it.id }.toSet()
        current.filter { it.id !in remoteIds }.forEach { repairDao.delete(it) }
    }

    /** Выполняет облачную операцию; при ошибке сообщает пользователю и возвращает default. */
    private suspend fun <T> safeCloud(default: T, message: String, block: suspend () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            Log.w("RepairRepository", "$message: ${e.message}")
            SyncBus.notify("$message (${cloudReason(e)})")
            default
        }

    /** Короткая техническая причина ошибки, чтобы пользователь мог её переслать. */
    private fun cloudReason(e: Exception): String {
        val m = e.message ?: ""
        val code = Regex("HTTP (\\d{3})").find(m)?.groupValues?.get(1)
        return if (code != null) "код $code" else m.take(160)
    }

    // ============================= Клиенты =============================

    fun observeClients(): Flow<List<Client>> = clientDao.observeAll()
    fun observeClient(id: Long): Flow<Client?> = clientDao.observeById(id)

    /**
     * Создаёт/изменяет клиента В ОБЛАКЕ и кэширует результат локально.
     * Возвращает id из облака (0 — если не удалось, пользователь уже уведомлён).
     */
    suspend fun saveClient(client: Client): Long = safeCloud(0L, CLOUD_SAVE_CLIENT_MSG) {
        if (client.id == 0L) {
            val created = SupabaseCloud.createClient(client)
            clientDao.insert(created)
            created.id
        } else {
            val updated = SupabaseCloud.updateClient(client)
            clientDao.update(updated)
            updated.id
        }
    }

    /** Удаляет клиента в облаке и локально. */
    suspend fun deleteClient(client: Client) {
        safeCloud(Unit, CLOUD_DELETE_CLIENT_MSG) {
            SupabaseCloud.deleteClient(client.id)
            clientDao.delete(client)
        }
    }

    // ============================= Запчасти =============================

    fun observeParts(): Flow<List<Part>> = partDao.observeAll()
    fun observeLowStock(): Flow<List<Part>> = partDao.observeLowStock()

    suspend fun savePart(part: Part): Long =
        if (part.id == 0L) partDao.insert(part) else { partDao.update(part); part.id }

    suspend fun deletePart(part: Part) = partDao.delete(part)

    fun observePartsForRepair(repairId: Long): Flow<List<RepairPart>> =
        partDao.observePartsForRepair(repairId)

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

    // ============================= Ремонты =============================

    fun observeRepairs(): Flow<List<Repair>> = repairDao.observeAll()
    fun observeRepair(id: Long): Flow<Repair?> = repairDao.observeById(id)
    fun observeRepairsByClient(clientId: Long): Flow<List<Repair>> = repairDao.observeByClient(clientId)
    fun observeReadyOverdue(now: Long): Flow<List<Repair>> = repairDao.observeReadyOverdue(now)

    /**
     * Создаёт/изменяет ремонт В ОБЛАКЕ и кэширует результат локально.
     * Возвращает id из облака (0 — если не удалось, пользователь уже уведомлён).
     */
    suspend fun saveRepair(repair: Repair): Long = safeCloud(0L, CLOUD_SAVE_REPAIR_MSG) {
        if (repair.id == 0L) {
            val created = SupabaseCloud.createRepair(repair)
            repairDao.insert(created)
            created.id
        } else {
            val updated = SupabaseCloud.updateRepair(repair)
            repairDao.update(updated)
            updated.id
        }
    }

    /** Удаляет ремонт в облаке и локально. */
    suspend fun deleteRepair(repair: Repair) {
        safeCloud(Unit, CLOUD_DELETE_REPAIR_MSG) {
            SupabaseCloud.deleteRepair(repair.id)
            repairDao.delete(repair)
        }
    }

    /** Помечает ремонт готовым в облаке и локально. */
    suspend fun markReady(repair: Repair) {
        val updated = repair.copy(
            status = RepairStatus.READY.name,
            readyDate = System.currentTimeMillis()
        )
        safeCloud(Unit, CLOUD_SAVE_REPAIR_MSG) {
            SupabaseCloud.updateRepair(updated)
            repairDao.update(updated)
        }
    }

    /** Выдаёт ремонт: статус и оплата в облаке, проводка по деньгам — локально. */
    suspend fun issueRepair(repair: Repair, received: Double) {
        val receivedSum = received.coerceIn(0.0, repair.price)
        val updated = repair.copy(
            status = RepairStatus.ISSUED.name,
            issuedDate = System.currentTimeMillis(),
            receivedPayment = receivedSum
        )
        safeCloud(Unit, CLOUD_SAVE_REPAIR_MSG) {
            SupabaseCloud.updateRepair(updated)
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
    }

    // ============================= Фото =============================

    fun observePhotosForRepair(repairId: Long): Flow<List<RepairPhoto>> =
        photoDao.observeByRepair(repairId)

    /** Сохраняет фото локально и привязывает к ремонту. */
    suspend fun addPhotoToRepair(repairId: Long, bytes: ByteArray, mime: String, ext: String) =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(appContext.filesDir, "photos").apply { mkdirs() }
                val name = "${repairId}_${System.currentTimeMillis()}.$ext"
                val file = File(dir, name)
                file.writeBytes(bytes)
                photoDao.insert(
                    RepairPhoto(
                        repairId = repairId,
                        url = file.absolutePath,
                        caption = "",
                        createdAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w("RepairRepository", "addPhoto: ${e.message}")
            }
        }

    suspend fun deletePhoto(photo: RepairPhoto) = withContext(Dispatchers.IO) {
        try {
            runCatching { File(photo.url).delete() }
            photoDao.delete(photo)
        } catch (e: Exception) { Log.w("RepairRepository", "deletePhoto: ${e.message}") }
    }

    // ============================= Финансы =============================

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

    fun observeDaily(from: Long, to: Long): Flow<List<DailyAggregate>> = transactionDao.dailyAggregates(from, to)

    // ============================= Дашборд =============================

    fun observeDashboard(): Flow<DashboardStats> {
        val now = System.currentTimeMillis()
        val dayStart = startOfDay(now)
        val todayEnd = now + 24 * 3600_000L
        val weekStart = startOfDay(now - 6L * 24 * 3600_000L)
        val active = setOf(
            RepairStatus.NEW, RepairStatus.DIAGNOSED,
            RepairStatus.IN_WORK, RepairStatus.AWAITING_PART
        )

        // Типизированный combine (до 5 потоков разных типов); счётчики считаем
        // из полного списка ремонтов — отдельные count-потоки не нужны.
        return combine(
            partDao.observeLowStock(),
            transactionDao.incomeBetween(dayStart, todayEnd),
            transactionDao.incomeBetween(weekStart, now),
            transactionDao.expenseBetween(weekStart, now),
            repairDao.observeAll()
        ) { lowParts, todayIncome, weekIncome, weekExpense, repairs ->
            DashboardStats(
                totalRepairs = repairs.size,
                inProgress = repairs.count { it.statusEnum in active },
                ready = repairs.count { it.statusEnum == RepairStatus.READY },
                lowStockCount = lowParts.size,
                lowStockParts = lowParts,
                todayIncome = todayIncome,
                weekIncome = weekIncome,
                weekExpense = weekExpense,
                weekProfit = weekIncome - weekExpense,
                readyOverdue = repairs.filter {
                    it.statusEnum == RepairStatus.READY && (it.readyDate ?: 0L) <= now
                },
                totalDebt = repairs.filter { it.statusEnum != RepairStatus.CANCELLED }
                    .sumOf { it.debt }
            )
        }
    }

    companion object {
        private const val AUTO_SYNC_MS = 10_000L

        private const val SYNC_PREFS = "jarvis_sync"
        private const val KEY_CLOUD_INITIALIZED = "cloud_initialized"

        private const val CLOUD_SAVE_CLIENT_MSG = "Не удалось сохранить клиента — нет связи с облаком"
        private const val CLOUD_DELETE_CLIENT_MSG = "Не удалось удалить клиента — нет связи с облаком"
        private const val CLOUD_SAVE_REPAIR_MSG = "Не удалось сохранить ремонт — нет связи с облаком"
        private const val CLOUD_DELETE_REPAIR_MSG = "Не удалось удалить ремонт — нет связи с облаком"

        @Volatile
        private var INSTANCE: RepairRepository? = null

        fun getInstance(context: Context): RepairRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RepairRepository(context.applicationContext).also { INSTANCE = it }
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
