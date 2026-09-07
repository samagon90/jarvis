package com.jarvis.master.data

import android.content.Context
import android.util.Log
import com.jarvis.master.data.cloud.CloudHttp
import com.jarvis.master.data.cloud.SupabaseConfig
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.DailyAggregate
import com.jarvis.master.data.db.Part
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairPart
import com.jarvis.master.data.db.RepairPhoto
import com.jarvis.master.data.db.RepairStatus
import com.jarvis.master.data.db.Transaction
import com.jarvis.master.data.db.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.util.Calendar

/**
 * Единая точка доступа к данным мастерской.
 *
 * Данные хранятся в облаке Supabase и кэшируются в памяти. Обе копии приложения
 * (ваша и второго человека) читают и пишут одну и ту же облачную базу; кэш
 * периодически обновляется, поэтому изменения появляются на обоих устройствах.
 *
 * Имя класса и API сохранены как в старой Room-версии, чтобы не менять ViewModel.
 */
class RepairRepository private constructor(context: Context) {

    private val tag = "RepairRepository"

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    private val _repairs = MutableStateFlow<List<Repair>>(emptyList())
    private val _repairParts = MutableStateFlow<List<RepairPart>>(emptyList())
    private val _parts = MutableStateFlow<List<Part>>(emptyList())
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _photos = MutableStateFlow<List<RepairPhoto>>(emptyList())

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (true) {
                runCatching { refreshAll() }
                delay(10_000)
            }
        }
    }

    fun observePhotosForRepair(repairId: Long): Flow<List<RepairPhoto>> =
        _photos.map { list -> list.filter { it.repairId == repairId }.sortedByDescending { it.createdAt } }

    /**
     * Загружает фото в облачное хранилище Supabase и записывает запись о нём
     * в таблицу repair_photos, привязанную к ремонту.
     */
    suspend fun addPhotoToRepair(repairId: Long, bytes: ByteArray, mime: String, ext: String) =
        withContext(Dispatchers.IO) {
            try {
                val path = "${repairId}_${System.currentTimeMillis()}.$ext"
                CloudHttp.upload(SupabaseConfig.BUCKET, path, bytes, mime)
                val photo = RepairPhoto(
                    repairId = repairId,
                    url = CloudHttp.publicUrl(SupabaseConfig.BUCKET, path),
                    caption = "",
                    createdAt = System.currentTimeMillis()
                )
                CloudHttp.insert(
                    SupabaseConfig.TABLE_PHOTOS,
                    _json.encodeToJsonElement(photo).jsonObject.dropId().toString()
                )
                refreshPhotos()
            } catch (e: Exception) {
                Log.w(tag, "addPhoto: ${e.message}")
            }
        }

    // ============================= Чтение =============================

    fun observeClients(): Flow<List<Client>> = _clients.asStateFlow()
    fun observeClient(id: Long): Flow<Client?> =
        _clients.map { list -> list.firstOrNull { it.id == id } }
    fun observeRepairs(): Flow<List<Repair>> = _repairs.asStateFlow()
    fun observeRepair(id: Long): Flow<Repair?> =
        _repairs.map { list -> list.firstOrNull { it.id == id } }
    fun observeRepairsByClient(clientId: Long): Flow<List<Repair>> =
        _repairs.map { list -> list.filter { it.clientId == clientId } }
    fun observeParts(): Flow<List<Part>> = _parts.asStateFlow()
    fun observeLowStock(): Flow<List<Part>> =
        _parts.map { list -> list.filter { it.quantity <= it.minQuantity } }
    fun observePartsForRepair(repairId: Long): Flow<List<RepairPart>> =
        _repairParts.map { list -> list.filter { it.repairId == repairId } }
    fun observeTransactions(): Flow<List<Transaction>> = _transactions.asStateFlow()
    fun observeReadyOverdue(now: Long): Flow<List<Repair>> =
        _repairs.map { list ->
            list.filter { it.statusEnum == RepairStatus.READY && (it.readyDate ?: 0) <= now }
        }

    fun observeIncomeBetween(from: Long, to: Long): Flow<Double> =
        _transactions.map { list ->
            list.filter { it.typeEnum == TransactionType.INCOME && it.date in from..to }
                .sumOf { it.amount }
        }

    fun observeExpenseBetween(from: Long, to: Long): Flow<Double> =
        _transactions.map { list ->
            list.filter { it.typeEnum == TransactionType.EXPENSE && it.date in from..to }
                .sumOf { it.amount }
        }

    fun observeDaily(from: Long, to: Long): Flow<List<DailyAggregate>> =
        _transactions.map { list ->
            list.filter { it.date in from..to }
                .groupBy { it.date to it.type }
                .map { (k, v) -> DailyAggregate(k.first, k.second, v.sumOf { it.amount }) }
        }

    fun observeDashboard(): Flow<DashboardStats> =
        combine(_repairs, _parts, _transactions) { repairs, parts, txs ->
            val now = System.currentTimeMillis()
            val dayStart = startOfDay(now)
            val weekStart = startOfDay(now - 6L * 24 * 3600_000L)
            val active = setOf(
                RepairStatus.NEW, RepairStatus.DIAGNOSED,
                RepairStatus.IN_WORK, RepairStatus.AWAITING_PART
            )
            val weekIncome = txs.filter { it.typeEnum == TransactionType.INCOME && it.date >= weekStart }
                .sumOf { it.amount }
            val weekExpense = txs.filter { it.typeEnum == TransactionType.EXPENSE && it.date >= weekStart }
                .sumOf { it.amount }
            DashboardStats(
                totalRepairs = repairs.size,
                inProgress = repairs.count { it.statusEnum in active },
                ready = repairs.count { it.statusEnum == RepairStatus.READY },
                lowStockCount = parts.count { it.quantity <= it.minQuantity },
                lowStockParts = parts.filter { it.quantity <= it.minQuantity },
                todayIncome = txs.filter { it.typeEnum == TransactionType.INCOME && it.date >= dayStart }
                    .sumOf { it.amount },
                weekIncome = weekIncome,
                weekExpense = weekExpense,
                weekProfit = weekIncome - weekExpense,
                readyOverdue = repairs.filter {
                    it.statusEnum == RepairStatus.READY && (it.readyDate ?: 0) <= now
                },
                totalDebt = repairs.filter { it.statusEnum != RepairStatus.CANCELLED }.sumOf { it.debt }
            )
        }

    // ============================= Клиенты =============================

    suspend fun saveClient(client: Client): Long = withContext(Dispatchers.IO) {
        try {
            val id = if (client.id == 0L) insertClient(client) else updateClient(client)
            refreshClients()
            id
        } catch (e: Exception) {
            Log.w(tag, "saveClient: ${e.message}")
            client.id
        }
    }

    private suspend fun insertClient(c: Client): Long {
        val body = _json.encodeToJsonElement(c).jsonObject.dropId().toString()
        val text = CloudHttp.insert(SupabaseConfig.TABLE_CLIENTS, body)
        return decodeList<Client>(text).firstOrNull()?.id ?: 0L
    }

    private suspend fun updateClient(c: Client): Long {
        val body = _json.encodeToJsonElement(c).jsonObject.dropId().toString()
        CloudHttp.update(SupabaseConfig.TABLE_CLIENTS, CloudHttp.eq("id", c.id), body)
        return c.id
    }

    suspend fun deleteClient(client: Client) = withContext(Dispatchers.IO) {
        try {
            CloudHttp.delete(SupabaseConfig.TABLE_CLIENTS, CloudHttp.eq("id", client.id))
            refreshClients()
        } catch (e: Exception) { Log.w(tag, "deleteClient: ${e.message}") }
    }

    // ============================= Запчасти =============================

    suspend fun savePart(part: Part): Long = withContext(Dispatchers.IO) {
        try {
            val id = if (part.id == 0L) insertPart(part) else updatePart(part)
            refreshParts()
            id
        } catch (e: Exception) {
            Log.w(tag, "savePart: ${e.message}")
            part.id
        }
    }

    private suspend fun insertPart(p: Part): Long {
        val body = _json.encodeToJsonElement(p).jsonObject.dropId().toString()
        val text = CloudHttp.insert(SupabaseConfig.TABLE_PARTS, body)
        return decodeList<Part>(text).firstOrNull()?.id ?: 0L
    }

    private suspend fun updatePart(p: Part): Long {
        val body = _json.encodeToJsonElement(p).jsonObject.dropId().toString()
        CloudHttp.update(SupabaseConfig.TABLE_PARTS, CloudHttp.eq("id", p.id), body)
        return p.id
    }

    suspend fun deletePart(part: Part) = withContext(Dispatchers.IO) {
        try {
            CloudHttp.delete(SupabaseConfig.TABLE_PARTS, CloudHttp.eq("id", part.id))
            refreshParts()
        } catch (e: Exception) { Log.w(tag, "deletePart: ${e.message}") }
    }

    // ============================= Ремонты =============================

    suspend fun saveRepair(repair: Repair): Long = withContext(Dispatchers.IO) {
        try {
            val id = if (repair.id == 0L) insertRepair(repair) else updateRepair(repair)
            refreshRepairs()
            id
        } catch (e: Exception) {
            Log.w(tag, "saveRepair: ${e.message}")
            repair.id
        }
    }

    private suspend fun insertRepair(r: Repair): Long {
        val body = _json.encodeToJsonElement(r).jsonObject.dropId().toString()
        val text = CloudHttp.insert(SupabaseConfig.TABLE_REPAIRS, body)
        return decodeList<Repair>(text).firstOrNull()?.id ?: 0L
    }

    private suspend fun updateRepair(r: Repair): Long {
        val body = _json.encodeToJsonElement(r).jsonObject.dropId().toString()
        CloudHttp.update(SupabaseConfig.TABLE_REPAIRS, CloudHttp.eq("id", r.id), body)
        return r.id
    }

    suspend fun deleteRepair(repair: Repair) = withContext(Dispatchers.IO) {
        try {
            CloudHttp.delete(SupabaseConfig.TABLE_REPAIRS, CloudHttp.eq("id", repair.id))
            refreshRepairs()
            refreshRepairParts()
        } catch (e: Exception) { Log.w(tag, "deleteRepair: ${e.message}") }
    }

    suspend fun markReady(repair: Repair) = withContext(Dispatchers.IO) {
        try {
            updateRepair(
                repair.copy(status = RepairStatus.READY.name, readyDate = System.currentTimeMillis())
            )
            refreshRepairs()
        } catch (e: Exception) { Log.w(tag, "markReady: ${e.message}") }
    }

    suspend fun issueRepair(repair: Repair, received: Double) = withContext(Dispatchers.IO) {
        try {
            val receivedSum = received.coerceIn(0.0, repair.price)
            val updated = repair.copy(
                status = RepairStatus.ISSUED.name,
                issuedDate = System.currentTimeMillis(),
                receivedPayment = receivedSum
            )
            updateRepair(updated)
            val tx = Transaction(
                type = TransactionType.INCOME.name,
                category = "Ремонт",
                amount = receivedSum,
                date = System.currentTimeMillis(),
                repairId = repair.id,
                description = "Оплата ремонта: ${repair.deviceName}"
            )
            insertTransaction(tx)
            refreshRepairs()
            refreshTransactions()
        } catch (e: Exception) { Log.w(tag, "issueRepair: ${e.message}") }
    }

    /** Списывает запчасть в заказ и пересчитывает себестоимость. */
    suspend fun addPartToRepair(repairId: Long, partId: Long, quantity: Int) = withContext(Dispatchers.IO) {
        try {
            val part = _parts.value.firstOrNull { it.id == partId } ?: return@withContext
            val qty = quantity.coerceAtLeast(1)
            val link = RepairPart(repairId, partId, qty, part.purchasePrice)
            CloudHttp.insert(
                SupabaseConfig.TABLE_REPAIR_PARTS,
                _json.encodeToJsonElement(link).jsonObject.dropId().toString()
            )
            updatePart(part.copy(quantity = part.quantity - qty))
            recomputePartsCost(repairId)
            refreshParts()
            refreshRepairParts()
            refreshRepairs()
        } catch (e: Exception) { Log.w(tag, "addPartToRepair: ${e.message}") }
    }

    suspend fun removePartFromRepair(repairPart: RepairPart) = withContext(Dispatchers.IO) {
        try {
            val f = "${CloudHttp.eq("repairId", repairPart.repairId)}&${CloudHttp.eq("partId", repairPart.partId)}"
            CloudHttp.delete(SupabaseConfig.TABLE_REPAIR_PARTS, f)
            val part = _parts.value.firstOrNull { it.id == repairPart.partId }
            if (part != null) updatePart(part.copy(quantity = part.quantity + repairPart.quantity))
            recomputePartsCost(repairPart.repairId)
            refreshParts()
            refreshRepairParts()
            refreshRepairs()
        } catch (e: Exception) { Log.w(tag, "removePartFromRepair: ${e.message}") }
    }

    private suspend fun recomputePartsCost(repairId: Long) {
        val links = _repairParts.value.filter { it.repairId == repairId }
        val cost = links.sumOf { it.quantity * it.priceAtTime }
        val repair = _repairs.value.firstOrNull { it.id == repairId } ?: return
        updateRepair(repair.copy(partsCost = cost))
    }

    // ============================= Финансы =============================

    suspend fun addExpense(category: String, amount: Double, description: String) = withContext(Dispatchers.IO) {
        try {
            insertTransaction(
                Transaction(
                    type = TransactionType.EXPENSE.name,
                    category = category,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    description = description
                )
            )
            refreshTransactions()
        } catch (e: Exception) { Log.w(tag, "addExpense: ${e.message}") }
    }

    private suspend fun insertTransaction(t: Transaction): Long {
        val body = _json.encodeToJsonElement(t).jsonObject.dropId().toString()
        val text = CloudHttp.insert(SupabaseConfig.TABLE_TRANSACTIONS, body)
        return decodeList<Transaction>(text).firstOrNull()?.id ?: 0L
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            CloudHttp.delete(SupabaseConfig.TABLE_TRANSACTIONS, CloudHttp.eq("id", transaction.id))
            refreshTransactions()
        } catch (e: Exception) { Log.w(tag, "deleteTransaction: ${e.message}") }
    }

    // ============================= Загрузка из облака =============================

    suspend fun refreshAll() {
        refreshClients()
        refreshParts()
        refreshRepairs()
        refreshRepairParts()
        refreshTransactions()
        refreshPhotos()
    }

    private suspend fun refreshPhotos() {
        _photos.value = loadList { CloudHttp.select(SupabaseConfig.TABLE_PHOTOS) }
            .let { _json.decodeFromString<List<RepairPhoto>>(it) }
    }

    private suspend fun refreshClients() {
        _clients.value = loadList { CloudHttp.select(SupabaseConfig.TABLE_CLIENTS) }
            .let { _json.decodeFromString<List<Client>>(it) }
            .sortedBy { it.name.lowercase() }
    }

    private suspend fun refreshParts() {
        _parts.value = loadList { CloudHttp.select(SupabaseConfig.TABLE_PARTS) }
            .let { _json.decodeFromString<List<Part>>(it) }
    }

    private suspend fun refreshRepairs() {
        _repairs.value = loadList { CloudHttp.select(SupabaseConfig.TABLE_REPAIRS) }
            .let { _json.decodeFromString<List<Repair>>(it) }
            .sortedByDescending { it.receivedDate }
    }

    private suspend fun refreshRepairParts() {
        _repairParts.value = loadList { CloudHttp.select(SupabaseConfig.TABLE_REPAIR_PARTS) }
            .let { _json.decodeFromString<List<RepairPart>>(it) }
    }

    private suspend fun refreshTransactions() {
        _transactions.value = loadList { CloudHttp.select(SupabaseConfig.TABLE_TRANSACTIONS) }
            .let { _json.decodeFromString<List<Transaction>>(it) }
            .sortedByDescending { it.date }
    }

    /** Выполняет блокирующий сетевой запрос в IO-потоке, при ошибке возвращает "[]". */
    private suspend fun loadList(block: () -> String): String =
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            Log.w(tag, "loadList: ${e.message}")
            "[]"
        }

    private fun JsonObject.dropId(): JsonObject {
        val m = toMutableMap()
        m.remove("id")
        return JsonObject(m)
    }

    companion object {
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

/** Общий JSON для декодирования списков. */
private val _json = Json { ignoreUnknownKeys = true }

private inline fun <reified T> decodeList(text: String): List<T> =
    _json.decodeFromString(text)
