package com.jarvis.master.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.Part
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.Transaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Резервная копия всех данных в формате JSON. */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val clients: List<Client> = emptyList(),
    val repairs: List<Repair> = emptyList(),
    val parts: List<Part> = emptyList(),
    val transactions: List<Transaction> = emptyList()
)

object ExportUtils {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Собирает резервную копию и открывает системное окно «Поделиться».
     * @return путь к созданному файлу (null, если не удалось).
     */
    fun shareBackup(context: Context, backup: BackupData): Uri? {
        val file = File(context.cacheDir, "jarvis_backup.json")
        file.writeText(json.encodeToString(backup))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        shareFile(context, uri, "application/json")
        return uri
    }

    /** Экспорт справочника запчастей в CSV для удобного переноса/печати. */
    fun sharePartsCsv(context: Context, parts: List<Part>) {
        val sb = StringBuilder()
        sb.append("Название;Категория;Артикул;Остаток;Мин.остаток;Закупка;Продажа;Место\n")
        parts.forEach {
            sb.append("${it.name};${it.category};${it.barcode};${it.quantity};" +
                "${it.minQuantity};${it.purchasePrice};${it.sellPrice};${it.location}\n")
        }
        val file = File(context.cacheDir, "parts_${stamp()}.csv")
        file.writeText(sb.toString())
        shareFile(context, FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file), "text/csv")
    }

    private fun shareFile(context: Context, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date())
}
