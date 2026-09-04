package com.jarvis.master.data.db

/**
 * Жизненный цикл ремонта/заказа в мастерской.
 * Хранится в БД как строковое имя (name).
 */
enum class RepairStatus(val label: String) {
    NEW("Принят в ремонт"),
    DIAGNOSED("Диагностика выполнена"),
    IN_WORK("В работе"),
    AWAITING_PART("Ждёт запчасть"),
    READY("Готов к выдаче"),
    ISSUED("Выдан клиенту"),
    CANCELLED("Отменён");

    companion object {
        fun from(name: String?): RepairStatus =
            entries.firstOrNull { it.name == name } ?: NEW
    }
}
