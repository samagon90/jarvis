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

    /** anon public key (JWT, начинается на eyJ…). */
    const val KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InR6ZHRtcnFiZWFndnNmbmloY2dpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg3NTk3MTAsImV4cCI6MjEwNDMzNTcxMH0.SPozPXtYJFPn3IUKKpYaCscTOTQvkG0azQT10yh6o_Q"

    /** Название бакета для фото (создан в Storage). */
    const val BUCKET: String = "repair-photos"

    const val TABLE_CLIENTS = "clients"
    const val TABLE_REPAIRS = "repairs"
    const val TABLE_PHOTOS = "repair_photos"
    const val TABLE_PARTS = "parts"
    const val TABLE_REPAIR_PARTS = "repair_parts"
    const val TABLE_TRANSACTIONS = "transactions"
}
