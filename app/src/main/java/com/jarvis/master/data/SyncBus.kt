package com.jarvis.master.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Глобальная шина коротких уведомлений для пользователя (обычно сообщения об ошибках
 * сети при работе с облаком). Показывается единым Snackbar в JarvisRoot.
 */
object SyncBus {
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    fun notify(message: String) {
        _notices.tryEmit(message)
    }
}
