package com.jarvis.master.data.db

/** Тип финансовой операции. */
enum class TransactionType(val label: String) {
    INCOME("Доход"),
    EXPENSE("Расход");

    companion object {
        fun from(name: String?): TransactionType =
            entries.firstOrNull { it.name == name } ?: INCOME
    }
}
