package org.telegram.messenger.feature.messaging.chat.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chat.data.datasource.ChatLocalDataSource
import org.telegram.messenger.feature.messaging.chat.data.datasource.ChatRemoteDataSource
import org.telegram.messenger.feature.messaging.chat.domain.model.MessageModel
import org.telegram.messenger.feature.messaging.chat.domain.repository.ChatRepository

/**
 * Чистая реализация [ChatRepository], управляющая сообщениями чата, историей и отправкой.
 */
class ChatRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: ChatLocalDataSource,
    private val remoteDataSource: ChatRemoteDataSource
) : ChatRepository {

    override fun observeMessages(dialogId: Long): Flow<List<MessageModel>> =
        localDataSource.observeMessages(dialogId)

    override suspend fun getMessages(dialogId: Long): Result<List<MessageModel>> {
        val cached = localDataSource.getMessages(dialogId)
        return Result.success(cached)
    }

    override suspend fun loadHistory(dialogId: Long, count: Int): Result<Unit> =
        remoteDataSource.loadHistory(dialogId, count)

    override suspend fun sendMessage(dialogId: Long, text: String): Result<Unit> {
        val res = remoteDataSource.sendMessage(dialogId, text)
        if (res is Result.Success) {
            val syntheticMessage = MessageModel(
                id = (System.currentTimeMillis() % 1000000).toInt(),
                dialogId = dialogId,
                senderId = 0L,
                text = text,
                date = (System.currentTimeMillis() / 1000).toInt(),
                isOut = true,
                isUnread = true
            )
            localDataSource.addMessage(dialogId, syntheticMessage)
        }
        return res
    }

    override suspend fun deleteMessages(dialogId: Long, messageIds: List<Int>, revoke: Boolean): Result<Unit> {
        localDataSource.deleteMessages(dialogId, messageIds)
        return remoteDataSource.deleteMessages(dialogId, messageIds, revoke)
    }
}
