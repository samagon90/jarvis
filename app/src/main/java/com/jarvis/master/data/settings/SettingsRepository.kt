package com.jarvis.master.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Настройки мастерской и приложения. */
data class Settings(
    val shopName: String = "Моя мастерская",
    val shopAddress: String = "",
    val shopPhone: String = "",
    val warrantyDays: Int = 30,
    val currency: String = "₽",
    /** 0 = система, 1 = светлая, 2 = тёмная. */
    val themeMode: Int = 0
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val WARRANTY_DAYS = intPreferencesKey("warranty_days")
        val CURRENCY = stringPreferencesKey("currency")
        val THEME_MODE = intPreferencesKey("theme_mode")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            shopName = p[Keys.SHOP_NAME] ?: "Моя мастерская",
            shopAddress = p[Keys.SHOP_ADDRESS] ?: "",
            shopPhone = p[Keys.SHOP_PHONE] ?: "",
            warrantyDays = p[Keys.WARRANTY_DAYS] ?: 30,
            currency = p[Keys.CURRENCY] ?: "₽",
            themeMode = p[Keys.THEME_MODE] ?: 0
        )
    }

    suspend fun update(transform: (Settings) -> Settings) {
        val current = context.dataStore.data.first().toSettings()
        val updated = transform(current)
        context.dataStore.edit { p ->
            p[Keys.SHOP_NAME] = updated.shopName
            p[Keys.SHOP_ADDRESS] = updated.shopAddress
            p[Keys.SHOP_PHONE] = updated.shopPhone
            p[Keys.WARRANTY_DAYS] = updated.warrantyDays
            p[Keys.CURRENCY] = updated.currency
            p[Keys.THEME_MODE] = updated.themeMode
        }
    }

    private fun Preferences.toSettings(): Settings = Settings(
        shopName = this[Keys.SHOP_NAME] ?: "Моя мастерская",
        shopAddress = this[Keys.SHOP_ADDRESS] ?: "",
        shopPhone = this[Keys.SHOP_PHONE] ?: "",
        warrantyDays = this[Keys.WARRANTY_DAYS] ?: 30,
        currency = this[Keys.CURRENCY] ?: "₽",
        themeMode = this[Keys.THEME_MODE] ?: 0
    )
}
