package org.telegram.messenger.feature.messaging.dialogs.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.messaging.dialogs.data.mapper.DialogMapper
import org.telegram.messenger.feature.messaging.dialogs.domain.model.DialogModel

/**
 * Локальный источник данных для диалогов Telegram с поддержкой in-memory кэша и реактивных потоков.
 */
class DialogsLocalDataSource(
    private val currentAccount: Int
) {

    private val lock = Any()
    private val _dialogs = MutableStateFlow<Map<Int, List<DialogModel>>>(emptyMap())

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    init {
        trySyncFromLegacy(0)
    }

    private fun trySyncFromLegacy(folderId: Int) {
        if (!isLegacyAvailable) return
        try {
            val controller = MessagesController.getInstance(currentAccount) ?: return
            val legacy = controller.getDialogs(folderId) ?: return
            if (legacy.isNotEmpty()) {
                val mapped = legacy.map { dialog ->
                    val isMuted = controller.isDialogMuted(dialog.id, 0)
                    val isForum = controller.isForum(dialog.id)
                    DialogMapper.mapToDomain(dialog, isMuted = isMuted, isForum = isForum)
                }
                synchronized(lock) {
                    _dialogs.value = _dialogs.value + (folderId to mapped)
                }
            }
        } catch (_: Throwable) {
            // Headless environment
        }
    }

    fun observeDialogs(folderId: Int): Flow<List<DialogModel>> {
        return _dialogs.map { it[folderId] ?: emptyList() }
    }

    fun getDialogs(folderId: Int): List<DialogModel> {
        val cached = _dialogs.value[folderId]
        if (!cached.isNullOrEmpty()) return cached

        if (!isLegacyAvailable) return emptyList()

        return try {
            val controller = MessagesController.getInstance(currentAccount)
            val legacy = controller?.getDialogs(folderId)
            if (legacy != null && legacy.isNotEmpty()) {
                val mapped = legacy.map { dialog ->
                    val isMuted = controller.isDialogMuted(dialog.id, 0)
                    val isForum = controller.isForum(dialog.id)
                    DialogMapper.mapToDomain(dialog, isMuted = isMuted, isForum = isForum)
                }
                setDialogs(folderId, mapped)
                mapped
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun setDialogs(folderId: Int, dialogs: List<DialogModel>) = synchronized(lock) {
        _dialogs.value = _dialogs.value + (folderId to dialogs)
    }

    fun pinDialog(dialogId: Long, pin: Boolean): Boolean = synchronized(lock) {
        var updated = false
        val newMap = _dialogs.value.mapValues { (_, list) ->
            list.map { item ->
                if (item.id == dialogId) {
                    updated = true
                    item.copy(isPinned = pin)
                } else {
                    item
                }
            }
        }
        if (updated) {
            _dialogs.value = newMap
        }
        updated
    }

    fun deleteDialog(dialogId: Long): Boolean = synchronized(lock) {
        var removed = false
        val newMap = _dialogs.value.mapValues { (_, list) ->
            val filtered = list.filter { it.id != dialogId }
            if (filtered.size != list.size) removed = true
            filtered
        }
        if (removed) {
            _dialogs.value = newMap
        }
        removed
    }

    fun markAsRead(dialogId: Long): Boolean = synchronized(lock) {
        var updated = false
        val newMap = _dialogs.value.mapValues { (_, list) ->
            list.map { item ->
                if (item.id == dialogId) {
                    updated = true
                    item.copy(unreadCount = 0)
                } else {
                    item
                }
            }
        }
        if (updated) {
            _dialogs.value = newMap
        }
        updated
    }
}
