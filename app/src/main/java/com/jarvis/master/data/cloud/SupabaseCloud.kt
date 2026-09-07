package com.jarvis.master.data.cloud

import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.Repair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Типизированный доступ к облачной базе Supabase (PostgREST).
 *
 * Облако — ЕДИНЫЙ источник данных: здесь читаются все строки и пишутся изменения
 * с получением сгенерированного сервером id. Методы работают в IO-потоке
 * (CloudHttp синхронный) и кидают исключение при ошибке сети/сервера.
 */
object SupabaseCloud {

    // ============================= Клиенты =============================

    suspend fun fetchClients(): List<Client> =
        onIo { decodeList<Client>(CloudHttp.select(SupabaseConfig.TABLE_CLIENTS)) }

    suspend fun createClient(client: Client): Client =
        onIo { insertOne(SupabaseConfig.TABLE_CLIENTS, client) }

    suspend fun updateClient(client: Client): Client =
        onIo { updateOne(SupabaseConfig.TABLE_CLIENTS, client.id, client) }

    suspend fun deleteClient(id: Long) {
        onIo { CloudHttp.delete(SupabaseConfig.TABLE_CLIENTS, CloudHttp.eq("id", id)) }
    }

    // ============================= Ремонты =============================

    suspend fun fetchRepairs(): List<Repair> =
        onIo { decodeList<Repair>(CloudHttp.select(SupabaseConfig.TABLE_REPAIRS)) }

    suspend fun createRepair(repair: Repair): Repair =
        onIo { insertOne(SupabaseConfig.TABLE_REPAIRS, repair) }

    suspend fun updateRepair(repair: Repair): Repair =
        onIo { updateOne(SupabaseConfig.TABLE_REPAIRS, repair.id, repair) }

    suspend fun deleteRepair(id: Long) {
        onIo { CloudHttp.delete(SupabaseConfig.TABLE_REPAIRS, CloudHttp.eq("id", id)) }
    }
}

/** Общий JSON для сериализации (см. работавший ранее облачный вариант). */
private val json = Json { ignoreUnknownKeys = true }

private suspend fun <T> onIo(block: () -> T): T =
    withContext(Dispatchers.IO) { block() }

private inline fun <reified T> decodeList(text: String): List<T> =
    json.decodeFromString(text)

/** Вставка без поля id (сервер генерирует его сам) и возврат созданной строки. */
private inline fun <reified T> insertOne(table: String, entity: T): T {
    val body = json.encodeToJsonElement(entity).jsonObject.dropId().toString()
    val resp = CloudHttp.insert(table, body)
    return json.decodeFromString<List<T>>(resp).first()
}

/** Обновление строки по id и возврат обновлённой строки. */
private inline fun <reified T> updateOne(table: String, id: Long, entity: T): T {
    val body = json.encodeToJsonElement(entity).jsonObject.toString()
    val resp = CloudHttp.update(table, CloudHttp.eq("id", id), body)
    return json.decodeFromString<List<T>>(resp).first()
}

/** Возвращает копию объекта без поля id (чтобы не вставлять его вручную). */
private fun JsonObject.dropId(): JsonObject {
    val m = toMutableMap()
    m.remove("id")
    return JsonObject(m)
}
