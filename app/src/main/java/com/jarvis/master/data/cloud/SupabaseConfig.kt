package com.jarvis.master.data.cloud

/**
 * Настройки подключения к облаку Supabase.
 *
 * Значения ниже можно поменять в настройках приложения, но по умолчанию
 * используется ваш проект (URL и publishable-ключ безопасны для приложения).
 */
object SupabaseConfig {
    /** Project URL из Supabase → Settings → API → Project URL. */
    const val URL: String = "https://tzdtmrqbeagvsfnihcgi.supabase.co"

    /** Publishable key (новый формат, заменяет anon). Открыт для приложений. */
    const val KEY: String = "sb_publishable_2GcqRS06w6x4Xv4TZPhnog_OQ8JqwlY"

    /** Название бакета для фото (создан в Storage). */
    const val BUCKET: String = "repair-photos"

    const val TABLE_CLIENTS = "clients"
    const val TABLE_REPAIRS = "repairs"
    const val TABLE_PHOTOS = "repair_photos"
    const val TABLE_PARTS = "parts"
    const val TABLE_REPAIR_PARTS = "repair_parts"
    const val TABLE_TRANSACTIONS = "transactions"
}
