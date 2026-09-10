package org.telegram.messenger.feature.messaging.sendmessages.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.PendingSendModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendOptionsModel

interface SendMessagesRepository {
    fun observePendingSends(dialogId: Long? = null): Flow<List<PendingSendModel>>
    fun getPendingSends(dialogId: Long? = null): List<PendingSendModel>
    suspend fun sendText(dialogId: Long, text: String, options: SendOptionsModel): PendingSendModel
    suspend fun sendMedia(dialogId: Long, item: SendMediaItem, options: SendOptionsModel): PendingSendModel
    suspend fun sendAlbum(dialogId: Long, items: List<SendMediaItem>, options: SendOptionsModel): List<PendingSendModel>
    suspend fun forwardMessages(request: ForwardRequestModel): List<PendingSendModel>
    suspend fun retrySend(localId: Long): Boolean
    suspend fun cancelSend(localId: Long): Boolean
    suspend fun cancelAll(dialogId: Long? = null)
    fun updateProgress(localId: Long, progress: Float, uploadedBytes: Long)
    fun markSuccess(localId: Long)
    fun markFailed(localId: Long, reason: String)
    fun reset()
}
