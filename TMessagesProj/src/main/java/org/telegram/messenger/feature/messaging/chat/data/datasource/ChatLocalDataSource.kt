package org.telegram.messenger.feature.messaging.chat.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.messaging.chat.data.mapper.ChatMessageMapper
import org.telegram.messenger.feature.messaging.chat.domain.model.MessageModel

/**
 * Локальный источник данных для сообщений чата с поддержкой реактивных потоков и in-memory кэша.
 */
class ChatLocalDataSource(
    private val currentAccount: Int
) {

    private val lock = Any()
    private val _messages = MutableStateFlow<Map<Long, List<MessageModel>>>(emptyMap())

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    fun observeMessages(dialogId: Long): Flow<List<MessageModel>> {
        return _messages.map { it[dialogId] ?: emptyList() }
    }

    fun getMessages(dialogId: Long): List<MessageModel> {
        val cached = _messages.value[dialogId]
        if (!cached.isNullOrEmpty()) return cached

        if (!isLegacyAvailable) return emptyList()

        return try {
            val controller = MessagesController.getInstance(currentAccount)
            val legacyMessages = controller?.dialogMessage?.get(dialogId)
            if (legacyMessages != null && legacyMessages.isNotEmpty()) {
                val mapped = legacyMessages.map { ChatMessageMapper.mapToDomain(it) }
                setMessages(dialogId, mapped)
                mapped
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun setMessages(dialogId: Long, messages: List<MessageModel>) = synchronized(lock) {
        _messages.value = _messages.value + (dialogId to messages)
    }

    fun addMessage(dialogId: Long, message: MessageModel) = synchronized(lock) {
        val current = _messages.value[dialogId] ?: emptyList()
        _messages.value = _messages.value + (dialogId to (listOf(message) + current))
    }

    fun deleteMessages(dialogId: Long, messageIds: List<Int>): Boolean = synchronized(lock) {
        val current = _messages.value[dialogId] ?: return false
        val filtered = current.filter { it.id !in messageIds }
        val modified = filtered.size != current.size
        if (modified) {
            _messages.value = _messages.value + (dialogId to filtered)
        }
        modified
    }

    fun clear(dialogId: Long? = null) = synchronized(lock) {
        if (dialogId != null) {
            _messages.value = _messages.value - dialogId
        } else {
            _messages.value = emptyMap()
        }
    }
}
