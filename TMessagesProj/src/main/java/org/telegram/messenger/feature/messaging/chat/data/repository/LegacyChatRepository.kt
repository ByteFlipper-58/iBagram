package org.telegram.messenger.feature.messaging.chat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chat.data.mapper.ChatMessageMapper
import org.telegram.messenger.feature.messaging.chat.domain.model.MessageModel
import org.telegram.messenger.feature.messaging.chat.domain.repository.ChatRepository

/**
 * Adapter implementing [ChatRepository] on top of legacy [MessagesController]
 * and [SendMessagesHelper].
 *
 * Adheres strictly to the Telegram threading policy: all reads from the in-memory
 * [MessagesController.dialogMessage] cache occur on [Dispatchers.Main].
 */
class LegacyChatRepository(
    private val account: Int
) : ChatRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(account)

    private val sendMessagesHelper: SendMessagesHelper
        get() = SendMessagesHelper.getInstance(account)

    override fun observeMessages(dialogId: Long): Flow<List<MessageModel>> = callbackFlow {
        val emitCurrentState = {
            val legacyMessages = messagesController.dialogMessage.get(dialogId)
            val list = legacyMessages?.map { msgObj ->
                ChatMessageMapper.mapToDomain(msgObj)
            } ?: emptyList()
            trySend(list)
        }

        val observer = NotificationCenter.NotificationCenterDelegate { id, acc, args ->
            if (acc == account) {
                when (id) {
                    NotificationCenter.didReceiveNewMessages,
                    NotificationCenter.messagesDidLoad -> {
                        val eventDialogId = args?.getOrNull(0) as? Long
                        if (eventDialogId == null || eventDialogId == dialogId) {
                            emitCurrentState()
                        }
                    }
                    NotificationCenter.messagesDeleted,
                    NotificationCenter.messageReceivedByAck -> {
                        emitCurrentState()
                    }
                }
            }
        }

        NotificationCenterFlowBridge.runOnMainThread {
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.didReceiveNewMessages)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.messagesDidLoad)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.messagesDeleted)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.messageReceivedByAck)
            emitCurrentState()
        }

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.didReceiveNewMessages)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.messagesDidLoad)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.messagesDeleted)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.messageReceivedByAck)
            }
        }
    }

    override suspend fun getMessages(dialogId: Long): Result<List<MessageModel>> = withContext(Dispatchers.Main) {
        try {
            val legacyMessages = messagesController.dialogMessage.get(dialogId)
            val list = legacyMessages?.map { msgObj ->
                ChatMessageMapper.mapToDomain(msgObj)
            } ?: emptyList()
            Result.success(list)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to get messages for dialog $dialogId", e))
        }
    }

    override suspend fun loadHistory(dialogId: Long, count: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            messagesController.loadMessages(dialogId, 0, false, count, 0, 0, true, 0, 0, 0, 0, 0, 0, 0, 0, false)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to load history for dialog $dialogId", e))
        }
    }

    override suspend fun sendMessage(dialogId: Long, text: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val params = SendMessagesHelper.SendMessageParams.of(text, dialogId)
            sendMessagesHelper.sendMessage(params)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to send message to dialog $dialogId", e))
        }
    }

    override suspend fun deleteMessages(dialogId: Long, messageIds: List<Int>, revoke: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val ids = ArrayList(messageIds)
            messagesController.deleteMessages(ids, null, null, dialogId, 0, revoke, 0)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to delete messages for dialog $dialogId", e))
        }
    }
}
