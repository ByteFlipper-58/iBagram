package org.telegram.messenger.feature.social.joinrequests.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MemberRequestsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.events.NotificationEvent
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.joinrequests.data.mapper.JoinRequestMapper
import org.telegram.messenger.feature.social.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestModel
import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestsListModel
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

class LegacyJoinRequestsRepository(
    private val currentAccount: Int
) : JoinRequestsRepository {

    private val memberRequestsController: MemberRequestsController
        get() = MemberRequestsController.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    override fun observePendingRequests(chatId: Long): Flow<ChatPendingRequestsModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.chatInfoDidLoad)
            .onStart {
                emit(NotificationEvent(NotificationCenter.chatInfoDidLoad, currentAccount, emptyArray()))
            }
            .map {
                val chatFull = messagesController.getChatFull(chatId)
                JoinRequestMapper.mapChatPendingRequests(chatId, chatFull)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Main)
    }

    override suspend fun getPendingRequestsCount(chatId: Long): Result<ChatPendingRequestsModel> =
        withContext(Dispatchers.Main) {
            try {
                val chatFull = messagesController.getChatFull(chatId)
                Result.Success(JoinRequestMapper.mapChatPendingRequests(chatId, chatFull))
            } catch (e: Exception) {
                Result.Failure(AppError.Generic(e.message ?: "Failed to get pending requests count", e))
            }
        }

    override suspend fun getCachedRequests(chatId: Long): List<JoinRequestModel>? =
        withContext(Dispatchers.Main) {
            try {
                val cached = memberRequestsController.getCachedImporters(chatId)
                JoinRequestMapper.mapImportersList(chatId, cached).requests.takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun loadRequests(
        chatId: Long,
        query: String?,
        offsetUserId: Long?,
        offsetDate: Int?,
        limit: Int
    ): Result<JoinRequestsListModel> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val req = TLRPC.TL_messages_getChatInviteImporters().apply {
                peer = messagesController.getInputPeer(-chatId)
                requested = true
                this.limit = limit
                if (!query.isNullOrEmpty()) {
                    q = query
                    flags = flags or 4
                }
                if (offsetUserId != null && offsetUserId != 0L) {
                    val user = messagesController.getUser(offsetUserId)
                    offset_user = if (user != null) messagesController.getInputUser(user) else TLRPC.TL_inputUserEmpty()
                    offset_date = offsetDate ?: 0
                } else {
                    offset_user = TLRPC.TL_inputUserEmpty()
                    offset_date = 0
                }
            }

            val requestId = connectionsManager.sendRequest(req) { response, error ->
                if (continuation.isActive) {
                    if (error == null && response is TLRPC.TL_messages_chatInviteImporters) {
                        continuation.resume(Result.Success(JoinRequestMapper.mapImportersList(chatId, response)))
                    } else {
                        val message = error?.text ?: "Failed to load join requests"
                        continuation.resume(Result.Failure(AppError.Network(message)))
                    }
                }
            }

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(requestId, false)
            }
        }
    }

    override suspend fun approveRequest(chatId: Long, userId: Long): Result<Unit> =
        hideSingleJoinRequest(chatId, userId, isApproved = true)

    override suspend fun dismissRequest(chatId: Long, userId: Long): Result<Unit> =
        hideSingleJoinRequest(chatId, userId, isApproved = false)

    override suspend fun approveAllRequests(chatId: Long, inviteLink: String?): Result<Unit> =
        hideAllJoinRequests(chatId, inviteLink, isApproved = true)

    override suspend fun dismissAllRequests(chatId: Long, inviteLink: String?): Result<Unit> =
        hideAllJoinRequests(chatId, inviteLink, isApproved = false)

    private suspend fun hideSingleJoinRequest(
        chatId: Long,
        userId: Long,
        isApproved: Boolean
    ): Result<Unit> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val user = messagesController.getUser(userId)
            val req = TLRPC.TL_messages_hideChatJoinRequest().apply {
                approved = isApproved
                peer = messagesController.getInputPeer(-chatId)
                user_id = if (user != null) messagesController.getInputUser(user) else TLRPC.TL_inputUserEmpty()
            }

            val requestId = connectionsManager.sendRequest(req) { response, error ->
                if (continuation.isActive) {
                    if (error == null) {
                        if (response is TLRPC.TL_updates) {
                            messagesController.processUpdates(response, false)
                            if (response.chats.isNotEmpty()) {
                                messagesController.loadFullChat(response.chats[0].id, 0, true)
                            }
                        }
                        continuation.resume(Result.Success(Unit))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network(error.text ?: "Failed to update join request")))
                    }
                }
            }

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(requestId, false)
            }
        }
    }

    private suspend fun hideAllJoinRequests(
        chatId: Long,
        inviteLink: String?,
        isApproved: Boolean
    ): Result<Unit> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val req = TLRPC.TL_messages_hideAllChatJoinRequests().apply {
                approved = isApproved
                peer = messagesController.getInputPeer(-chatId)
                if (!inviteLink.isNullOrEmpty()) {
                    link = inviteLink
                    flags = flags or 1
                }
            }

            val requestId = connectionsManager.sendRequest(req) { response, error ->
                if (continuation.isActive) {
                    if (error == null) {
                        if (response is TLRPC.TL_updates) {
                            messagesController.processUpdates(response, false)
                            if (response.chats.isNotEmpty()) {
                                messagesController.loadFullChat(response.chats[0].id, 0, true)
                            }
                        }
                        continuation.resume(Result.Success(Unit))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network(error.text ?: "Failed to update all join requests")))
                    }
                }
            }

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(requestId, false)
            }
        }
    }
}
