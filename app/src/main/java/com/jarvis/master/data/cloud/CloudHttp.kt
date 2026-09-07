package com.jarvis.master.data.cloud

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Минимальный REST-клиент к Supabase (PostgREST + Storage) поверх HttpURLConnection.
 * Работает в IO-потоке; JSON обрабатывается kotlinx.serialization.
 */
object CloudHttp {

    private const val CONNECT_TIMEOUT = 15_000
    private const val READ_TIMEOUT = 30_000

    /** GET строки таблицы с фильтром (например "id=eq.5") и сортировкой. */
    fun select(table: String, filter: String? = null, order: String? = null): String {
        val q = buildString {
            if (filter != null) {
                if (isNotEmpty()) append('&')
                append(filter)
            }
            if (order != null) {
                if (isNotEmpty()) append('&')
                append("order=").append(URLEncoder.encode(order, "UTF-8"))
            }
        }
        val sep = if (q.isEmpty()) "" else "?$q"
        return request("GET", "/rest/v1/$table$sep", null, "return=minimal")
    }

    /** INSERT и возврат созданных строк. */
    fun insert(table: String, body: String): String =
        request("POST", "/rest/v1/$table", body, "return=representation")

    /** UPDATE строк по фильтру и возврат строк. */
    fun update(table: String, filter: String, body: String): String =
        request("PATCH", "/rest/v1/$table?$filter", body, "return=representation")

    /** DELETE строк по фильтру. */
    fun delete(table: String, filter: String): String =
        request("DELETE", "/rest/v1/$table?$filter", null, "return=minimal")

    /** Загрузка бинарного файла (фото) в Storage. Возвращает ответ сервера. */
    fun upload(bucket: String, path: String, bytes: ByteArray, mime: String): String {
        return request(
            "POST",
            "/storage/v1/object/$bucket/$path",
            bytes = bytes,
            prefer = null,
            contentType = mime
        )
    }

    /** Публичный URL для чтения файла из public-бакета. */
    fun publicUrl(bucket: String, path: String): String =
        "${SupabaseConfig.URL}/storage/v1/object/public/$bucket/$path"

    private fun request(
        method: String,
        endpoint: String,
        body: String?,
        prefer: String?,
        contentType: String = "application/json"
    ): String {
        return request(method, endpoint, body?.toByteArray(Charsets.UTF_8), prefer, contentType)
    }

    private fun request(
        method: String,
        endpoint: String,
        bytes: ByteArray?,
        prefer: String?,
        contentType: String
    ): String {
        val conn = URL(SupabaseConfig.URL + endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("apikey", SupabaseConfig.KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.KEY}")
            conn.setRequestProperty("Content-Type", contentType)
            if (prefer != null) conn.setRequestProperty("Prefer", prefer)
            if (bytes != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(bytes) }
            }
            val code = conn.responseCode
            val stream: InputStream =
                if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let(::readAll) ?: ""
            if (code !in 200..299) {
                throw RuntimeException("HTTP $code для $method $endpoint: $text")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun readAll(stream: InputStream): String {
        val buf = ByteArrayOutputStream()
        stream.use { input ->
            val chunk = ByteArray(8192)
            while (true) {
                val n = input.read(chunk)
                if (n < 0) break
                buf.write(chunk, 0, n)
            }
        }
        return String(buf.toByteArray(), Charsets.UTF_8)
    }

    /** Кодирование значения для PostgREST-фильтра eq. */
    fun eq(column: String, value: Any): String =
        "$column=eq.${URLEncoder.encode(value.toString(), "UTF-8")}"
}
