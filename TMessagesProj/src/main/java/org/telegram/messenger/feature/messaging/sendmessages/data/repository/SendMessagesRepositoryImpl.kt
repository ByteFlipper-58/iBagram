package org.telegram.messenger.feature.messaging.sendmessages.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.sendmessages.data.datasource.SendMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.sendmessages.data.datasource.SendMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.PendingSendModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaType
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendOptionsModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendStatus
import org.telegram.messenger.feature.messaging.sendmessages.domain.repository.SendMessagesRepository

/**
 * Чистая реализация [SendMessagesRepository], координирующая локальную очередь отправки
 * и удаленный транспорт Telegram MTProto.
 */
class SendMessagesRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: SendMessagesLocalDataSource,
    private val remoteDataSource: SendMessagesRemoteDataSource
) : SendMessagesRepository {

    override fun observePendingSends(dialogId: Long?): Flow<List<PendingSendModel>> =
        localDataSource.observePendingSends(dialogId)

    override fun getPendingSends(dialogId: Long?): List<PendingSendModel> =
        localDataSource.getPendingSends(dialogId)

    override suspend fun sendText(
        dialogId: Long,
        text: String,
        options: SendOptionsModel
    ): PendingSendModel {
        val item = PendingSendModel(
            localId = localDataSource.nextId(),
            dialogId = dialogId,
            type = SendMediaType.TEXT,
            text = text,
            status = SendStatus.SENDING,
            progress = 0f,
            options = options
        )
        localDataSource.addPendingSend(item)
        val res = remoteDataSource.sendText(dialogId, text, options)
        if (res is Result.Failure) {
            localDataSource.markFailed(item.localId, res.error.message)
        }
        return localDataSource.getPendingSend(item.localId) ?: item
    }

    override suspend fun sendMedia(
        dialogId: Long,
        item: SendMediaItem,
        options: SendOptionsModel
    ): PendingSendModel {
        val pending = PendingSendModel(
            localId = localDataSource.nextId(),
            dialogId = dialogId,
            type = item.type,
            text = item.caption,
            status = SendStatus.UPLOADING,
            progress = 0f,
            uploadedBytes = 0L,
            totalBytes = item.sizeBytes,
            options = options
        )
        localDataSource.addPendingSend(pending)
        val res = remoteDataSource.sendMedia(dialogId, item, options)
        if (res is Result.Failure) {
            localDataSource.markFailed(pending.localId, res.error.message)
        }
        return localDataSource.getPendingSend(pending.localId) ?: pending
    }

    override suspend fun sendAlbum(
        dialogId: Long,
        items: List<SendMediaItem>,
        options: SendOptionsModel
    ): List<PendingSendModel> {
        val created = items.map { item ->
            PendingSendModel(
                localId = localDataSource.nextId(),
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
        localDataSource.addPendingSends(created)
        val res = remoteDataSource.sendAlbum(dialogId, items, options)
        if (res is Result.Failure) {
            created.forEach { localDataSource.markFailed(it.localId, res.error.message) }
        }
        return created.mapNotNull { localDataSource.getPendingSend(it.localId) }
    }

    override suspend fun forwardMessages(request: ForwardRequestModel): List<PendingSendModel> {
        val created = request.messageIds.map { msgId ->
            PendingSendModel(
                localId = localDataSource.nextId(),
                dialogId = request.targetDialogId,
                type = SendMediaType.TEXT,
                text = "Forwarded message #$msgId",
                status = SendStatus.SENDING,
                progress = 0f,
                options = request.options
            )
        }
        localDataSource.addPendingSends(created)
        val res = remoteDataSource.forwardMessages(request)
        if (res is Result.Failure) {
            created.forEach { localDataSource.markFailed(it.localId, res.error.message) }
        }
        return created.mapNotNull { localDataSource.getPendingSend(it.localId) }
    }

    override suspend fun retrySend(localId: Long): Boolean {
        val success = localDataSource.retrySend(localId)
        if (success) {
            remoteDataSource.retrySend(localId)
        }
        return success
    }

    override suspend fun cancelSend(localId: Long): Boolean {
        val success = localDataSource.cancelSend(localId)
        if (success) {
            remoteDataSource.cancelSend(localId)
        }
        return success
    }

    override suspend fun cancelAll(dialogId: Long?) {
        localDataSource.cancelAll(dialogId)
    }

    override fun updateProgress(localId: Long, progress: Float, uploadedBytes: Long) {
        localDataSource.updateProgress(localId, progress, uploadedBytes)
    }

    override fun markSuccess(localId: Long) {
        localDataSource.markSuccess(localId)
    }

    override fun markFailed(localId: Long, reason: String) {
        localDataSource.markFailed(localId, reason)
    }

    override fun reset() {
        localDataSource.reset()
    }

    // Phase 4: Synchronous legacy strangler methods
    override fun registerSending(localId: Long, dialogId: Long, isUploading: Boolean) {
        localDataSource.registerSending(localId, dialogId, isUploading)
    }

    override fun unregisterSending(localId: Long, isSuccess: Boolean) {
        localDataSource.unregisterSending(localId, isSuccess)
    }

    override fun cancelSendSync(localId: Long): Boolean {
        return localDataSource.cancelSend(localId)
    }

    override fun retrySendSync(localId: Long): Boolean {
        return localDataSource.retrySend(localId)
    }

    override fun isSendingMessage(localId: Long): Boolean {
        return localDataSource.isSendingMessage(localId)
    }

    override fun isSendingDialog(dialogId: Long): Boolean {
        return localDataSource.isSendingDialog(dialogId)
    }

    // Phase 4: Media album queue & batch dispatching
    override fun registerMediaAlbum(albumId: Long, dialogId: Long, localIds: List<Long>) {
        localDataSource.registerMediaAlbum(albumId, dialogId, localIds)
    }

    override fun unregisterMediaAlbum(albumId: Long, isSuccess: Boolean) {
        localDataSource.unregisterMediaAlbum(albumId, isSuccess)
    }

    override fun isSendingAlbum(albumId: Long): Boolean {
        return localDataSource.isSendingAlbum(albumId)
    }

    override fun getAlbumLocalIds(albumId: Long): List<Long> {
        return localDataSource.getAlbumLocalIds(albumId)
    }

    override fun getSendingAlbumsCount(dialogId: Long?): Int {
        return localDataSource.getSendingAlbumsCount(dialogId)
    }
}
