package org.telegram.messenger.feature.sendmessages.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.sendmessages.domain.model.PendingSendModel
import org.telegram.messenger.feature.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.sendmessages.domain.model.SendMediaType
import org.telegram.messenger.feature.sendmessages.domain.model.SendOptionsModel
import org.telegram.messenger.feature.sendmessages.domain.model.SendStatus
import org.telegram.messenger.feature.sendmessages.domain.repository.SendMessagesRepository
import java.util.concurrent.atomic.AtomicLong

class LegacySendMessagesRepository(
    private val currentAccount: Int = 0
) : SendMessagesRepository {

    private val idGenerator = AtomicLong(1000L)
    private val _pendingSends = MutableStateFlow<List<PendingSendModel>>(emptyList())

    override fun observePendingSends(dialogId: Long?): Flow<List<PendingSendModel>> {
        return _pendingSends.map { list ->
            if (dialogId != null) {
                list.filter { it.dialogId == dialogId }
            } else {
                list
            }
        }
    }

    override fun getPendingSends(dialogId: Long?): List<PendingSendModel> {
        val list = _pendingSends.value
        return if (dialogId != null) {
            list.filter { it.dialogId == dialogId }
        } else {
            list
        }
    }

    override suspend fun sendText(
        dialogId: Long,
        text: String,
        options: SendOptionsModel
    ): PendingSendModel {
        val item = PendingSendModel(
            localId = idGenerator.incrementAndGet(),
            dialogId = dialogId,
            type = SendMediaType.TEXT,
            text = text,
            status = SendStatus.SENDING,
            progress = 0f,
            options = options
        )
        _pendingSends.update { it + item }
        return item
    }

    override suspend fun sendMedia(
        dialogId: Long,
        item: SendMediaItem,
        options: SendOptionsModel
    ): PendingSendModel {
        val pending = PendingSendModel(
            localId = idGenerator.incrementAndGet(),
            dialogId = dialogId,
            type = item.type,
            text = item.caption,
            status = SendStatus.UPLOADING,
            progress = 0f,
            uploadedBytes = 0L,
            totalBytes = item.sizeBytes,
            options = options
        )
        _pendingSends.update { it + pending }
        return pending
    }

    override suspend fun sendAlbum(
        dialogId: Long,
        items: List<SendMediaItem>,
        options: SendOptionsModel
    ): List<PendingSendModel> {
        val created = items.map { item ->
            PendingSendModel(
                localId = idGenerator.incrementAndGet(),
                dialogId = dialogId,
                type = item.type,
                text = item.caption,
                status = SendStatus.UPLOADING,
                progress = 0f,
                uploadedBytes = 0L,
                totalBytes = item.sizeBytes,
                options = options
            )
        }
        _pendingSends.update { it + created }
        return created
    }

    override suspend fun forwardMessages(request: ForwardRequestModel): List<PendingSendModel> {
        val created = request.messageIds.map { msgId ->
            PendingSendModel(
                localId = idGenerator.incrementAndGet(),
                dialogId = request.targetDialogId,
                type = SendMediaType.TEXT,
                text = "Forwarded message #$msgId",
                status = SendStatus.SENDING,
                progress = 0f,
                options = request.options
            )
        }
        _pendingSends.update { it + created }
        return created
    }

    override suspend fun retrySend(localId: Long): Boolean {
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
        return found
    }

    override suspend fun cancelSend(localId: Long): Boolean {
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
        return found
    }

    override suspend fun cancelAll(dialogId: Long?) {
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

    override fun updateProgress(localId: Long, progress: Float, uploadedBytes: Long) {
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

    override fun markSuccess(localId: Long) {
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

    override fun markFailed(localId: Long, reason: String) {
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

    override fun reset() {
        _pendingSends.value = emptyList()
    }
}
