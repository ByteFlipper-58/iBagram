package org.telegram.messenger.feature.messaging.autodelete.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.autodelete.data.mapper.AutoDeleteMapper
import org.telegram.messenger.feature.messaging.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.messaging.autodelete.domain.model.GlobalAutoDeleteStateModel
import org.telegram.messenger.feature.messaging.autodelete.domain.repository.AutoDeleteRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

class LegacyAutoDeleteRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : AutoDeleteRepository {

    private val userConfig: UserConfig
        get() = UserConfig.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    private val notificationCenter: NotificationCenter
        get() = NotificationCenter.getInstance(currentAccount)

    override fun observeGlobalAutoDelete(): Flow<GlobalAutoDeleteStateModel> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.didUpdateGlobalAutoDeleteTimer
        )
            .map { AutoDeleteMapper.toGlobalState(userConfig.globalTTl) }
            .onStart { emit(AutoDeleteMapper.toGlobalState(userConfig.globalTTl)) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getGlobalAutoDelete(forceRefresh: Boolean): Result<AutoDeleteTtlModel> =
        withContext(mainDispatcher) {
            if (!forceRefresh) {
                return@withContext Result.Success(AutoDeleteMapper.toModelFromMinutes(userConfig.globalTTl))
            }

            suspendCancellableCoroutine<Result<AutoDeleteTtlModel>> { continuation ->
                val req = TLRPC.TL_messages_getDefaultHistoryTTL()
                val reqId = connectionsManager.sendRequest(req) { response, error ->
                    AndroidUtilities.runOnUIThread {
                        if (error != null) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(error.text ?: "Failed to get default history TTL"))
                            }
                        } else if (response is TLRPC.TL_defaultHistoryTTL) {
                            val minutes = response.period / 60
                            userConfig.setGlobalTtl(minutes)
                            notificationCenter.postNotificationName(NotificationCenter.didUpdateGlobalAutoDeleteTimer)
                            if (continuation.isActive) {
                                continuation.resume(Result.Success(AutoDeleteMapper.toModelFromMinutes(minutes)))
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(Result.Success(AutoDeleteMapper.toModelFromMinutes(userConfig.globalTTl)))
                            }
                        }
                    }
                }
                continuation.invokeOnCancellation {
                    try {
                        connectionsManager.cancelRequest(reqId, true)
                    } catch (_: Throwable) {}
                }
            }
        }

    override suspend fun setGlobalAutoDelete(ttl: AutoDeleteTtlModel): Result<Unit> =
        withContext(mainDispatcher) {
            suspendCancellableCoroutine<Result<Unit>> { continuation ->
                val req = TLRPC.TL_messages_setDefaultHistoryTTL()
                req.period = ttl.periodSeconds
                val reqId = connectionsManager.sendRequest(req) { _, error ->
                    AndroidUtilities.runOnUIThread {
                        if (error != null) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(error.text ?: "Failed to set default history TTL"))
                            }
                        } else {
                            userConfig.setGlobalTtl(ttl.periodMinutes)
                            notificationCenter.postNotificationName(NotificationCenter.didUpdateGlobalAutoDeleteTimer)
                            if (continuation.isActive) {
                                continuation.resume(Result.Success(Unit))
                            }
                        }
                    }
                }
                continuation.invokeOnCancellation {
                    try {
                        connectionsManager.cancelRequest(reqId, true)
                    } catch (_: Throwable) {}
                }
            }
        }

    override suspend fun getChatAutoDelete(chatId: Long): Result<AutoDeleteTtlModel> =
        withContext(mainDispatcher) {
            try {
                val dialog = messagesController.dialogs_dict.get(chatId)
                val ttlSeconds = dialog?.ttl_period ?: run {
                    if (chatId > 0) {
                        messagesController.getUserFull(chatId)?.ttl_period ?: 0
                    } else {
                        messagesController.getChatFull(-chatId)?.ttl_period ?: 0
                    }
                }
                Result.Success(AutoDeleteMapper.toModel(ttlSeconds))
            } catch (e: Throwable) {
                Result.failure(e.message ?: "Failed to get chat auto-delete TTL", e)
            }
        }

    override suspend fun setChatAutoDelete(chatId: Long, ttl: AutoDeleteTtlModel): Result<Unit> =
        withContext(mainDispatcher) {
            try {
                messagesController.setDialogHistoryTTL(chatId, ttl.periodSeconds)
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.failure(e.message ?: "Failed to set chat auto-delete TTL", e)
            }
        }

    override suspend fun setChatsAutoDeleteBatch(
        chatIds: List<Long>,
        ttl: AutoDeleteTtlModel
    ): Result<Unit> = withContext(mainDispatcher) {
        try {
            for (id in chatIds) {
                messagesController.setDialogHistoryTTL(id, ttl.periodSeconds)
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to batch set chats auto-delete TTL", e)
        }
    }
}
