package org.telegram.messenger.feature.messaging.sendmessages.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.PendingSendModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Локальный источник данных для отслеживания очереди и статусов отправки сообщений.
 */
class SendMessagesLocalDataSource(
    private val currentAccount: Int
) {

    private val lock = Any()
    private val idGenerator = AtomicLong(1000L)
    private val _pendingSends = MutableStateFlow<List<PendingSendModel>>(emptyList())
    private val albumQueue = ConcurrentHashMap<Long, AlbumEntry>()

    data class AlbumEntry(val albumId: Long, val dialogId: Long, val localIds: List<Long>)

    fun nextId(): Long = idGenerator.incrementAndGet()

    fun observePendingSends(dialogId: Long? = null): Flow<List<PendingSendModel>> {
        return _pendingSends.map { list ->
            if (dialogId != null) {
                list.filter { it.dialogId == dialogId }
            } else {
                list
            }
        }
    }

    fun getPendingSends(dialogId: Long? = null): List<PendingSendModel> {
        val list = _pendingSends.value
        return if (dialogId != null) {
            list.filter { it.dialogId == dialogId }
        } else {
            list
        }
    }

    fun getPendingSend(localId: Long): PendingSendModel? {
        return _pendingSends.value.firstOrNull { it.localId == localId }
    }

    fun addPendingSend(item: PendingSendModel) = synchronized(lock) {
        _pendingSends.update { it + item }
    }

    fun addPendingSends(items: List<PendingSendModel>) = synchronized(lock) {
        _pendingSends.update { it + items }
    }

    fun updateProgress(localId: Long, progress: Float, uploadedBytes: Long) = synchronized(lock) {
        _pendingSends.update { list ->
            list.map { item ->
                if (item.localId == localId) {
                    item.copy(
                        progress = progress.coerceIn(0f, 1f),
                        uploadedBytes = uploadedBytes,
                        status = if (progress >= 1f) SendStatus.SENDING else SendStatus.UPLOADING
                    )
                } else {
                    item
                }
            }
        }
    }

    fun markSuccess(localId: Long) = synchronized(lock) {
        _pendingSends.update { list ->
            list.map { item ->
                if (item.localId == localId) {
                    item.copy(
                        status = SendStatus.SUCCESS,
                        progress = 1.0f
                    )
                } else {
                    item
                }
            }
        }
    }

    fun markFailed(localId: Long, reason: String) = synchronized(lock) {
        _pendingSends.update { list ->
            list.map { item ->
                if (item.localId == localId) {
                    item.copy(
                        status = SendStatus.FAILED,
                        errorReason = reason
                    )
                } else {
                    item
                }
            }
        }
    }

    fun retrySend(localId: Long): Boolean = synchronized(lock) {
        var found = false
        _pendingSends.update { list ->
            list.map { item ->
                if (item.localId == localId) {
                    found = true
                    item.copy(
                        status = SendStatus.PENDING,
                        errorReason = null,
                        retryCount = item.retryCount + 1
                    )
                } else {
                    item
                }
            }
        }
        found
    }

    fun cancelSend(localId: Long): Boolean = synchronized(lock) {
        var found = false
        _pendingSends.update { list ->
            list.map { item ->
                if (item.localId == localId) {
                    found = true
                    item.copy(status = SendStatus.CANCELLED)
                } else {
                    item
                }
            }
        }
        found
    }

    fun cancelAll(dialogId: Long? = null) = synchronized(lock) {
        _pendingSends.update { list ->
            list.map { item ->
                if (dialogId == null || item.dialogId == dialogId) {
                    if (!item.isFinished) {
                        item.copy(status = SendStatus.CANCELLED)
                    } else {
                        item
                    }
                } else {
                    item
                }
            }
        }
    }

    fun reset() = synchronized(lock) {
        _pendingSends.value = emptyList()
        albumQueue.clear()
    }

    // Phase 4: Synchronous legacy strangler methods
    fun registerSending(localId: Long, dialogId: Long, isUploading: Boolean) = synchronized(lock) {
        val existing = _pendingSends.value.firstOrNull { it.localId == localId }
        if (existing != null) {
            _pendingSends.update { list ->
                list.map { item ->
                    if (item.localId == localId) {
                        item.copy(
                            status = if (isUploading) SendStatus.UPLOADING else SendStatus.SENDING
                        )
                    } else item
                }
            }
        } else {
            val newSend = PendingSendModel(
                localId = localId,
                dialogId = dialogId,
                type = org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaType.TEXT,
                status = if (isUploading) SendStatus.UPLOADING else SendStatus.SENDING
            )
            _pendingSends.update { it + newSend }
        }
    }

    fun unregisterSending(localId: Long, isSuccess: Boolean) = synchronized(lock) {
        if (isSuccess) {
            markSuccess(localId)
        } else {
            cancelSend(localId)
        }
    }

    fun isSendingMessage(localId: Long): Boolean {
        return _pendingSends.value.any { it.localId == localId && !it.isFinished }
    }

    fun isSendingDialog(dialogId: Long): Boolean {
        return _pendingSends.value.any { it.dialogId == dialogId && !it.isFinished }
    }

    // Phase 4: Media album queue & batch dispatching
    fun registerMediaAlbum(albumId: Long, dialogId: Long, localIds: List<Long>) = synchronized(lock) {
        albumQueue[albumId] = AlbumEntry(albumId, dialogId, localIds)
        for (id in localIds) {
            registerSending(id, dialogId, isUploading = true)
        }
    }

    fun unregisterMediaAlbum(albumId: Long, isSuccess: Boolean) = synchronized(lock) {
        val entry = albumQueue.remove(albumId)
        if (entry != null) {
            for (id in entry.localIds) {
                unregisterSending(id, isSuccess)
            }
        }
    }

    fun isSendingAlbum(albumId: Long): Boolean {
        return albumQueue.containsKey(albumId)
    }

    fun getAlbumLocalIds(albumId: Long): List<Long> {
        return albumQueue[albumId]?.localIds ?: emptyList()
    }

    fun getSendingAlbumsCount(dialogId: Long? = null): Int {
        return if (dialogId == null) {
            albumQueue.size
        } else {
            albumQueue.values.count { it.dialogId == dialogId }
        }
    }
}
