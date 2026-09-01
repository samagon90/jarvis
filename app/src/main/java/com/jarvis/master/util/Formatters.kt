package com.jarvis.master.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Форматирование денег, дат и времени для русского интерфейса. */
object Formatters {

    fun money(value: Double, currency: String = "₽"): String {
        val rounded = if (value == value.toLong().toDouble()) value.toLong().toString()
        else String.format(Locale.ROOT, "%.2f", value).replace(',', '.')
        return "$rounded $currency"
    }

    fun date(ts: Long): String =
        SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date(ts))

    fun time(ts: Long): String =
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(ts))

    fun dateTime(ts: Long): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(ts))

    /** Относительная подпись: «сегодня», «вчера» или дата. */
    fun relativeDay(ts: Long): String {
        val c = Calendar.getInstance()
        val today = startOfDay(c.timeInMillis)
        val that = startOfDay(ts)
        val diffDays = ((today - that) / (24 * 3600_000L)).toInt()
        return when {
            diffDays <= 0 -> "Сегодня"
            diffDays == 1 -> "Вчера"
            else -> date(ts)
        }
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
