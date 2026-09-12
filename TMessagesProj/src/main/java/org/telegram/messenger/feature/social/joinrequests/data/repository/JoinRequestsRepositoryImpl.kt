package org.telegram.messenger.feature.social.joinrequests.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.events.NotificationEvent
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.joinrequests.data.datasource.JoinRequestsLocalDataSource
import org.telegram.messenger.feature.social.joinrequests.data.datasource.JoinRequestsRemoteDataSource
import org.telegram.messenger.feature.social.joinrequests.data.mapper.JoinRequestMapper
import org.telegram.messenger.feature.social.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestModel
import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestsListModel
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository
import org.telegram.tgnet.TLRPC

/**
 * Clean repository implementation coordinating local and remote data sources for chat join requests operations,
 * strangling legacy monolithic logic in MemberRequestsController.
 */
class JoinRequestsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: JoinRequestsLocalDataSource,
    private val remoteDataSource: JoinRequestsRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : JoinRequestsRepository {

    override fun observePendingRequests(chatId: Long): Flow<ChatPendingRequestsModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.chatInfoDidLoad)
            .onStart {
                emit(NotificationEvent(NotificationCenter.chatInfoDidLoad, currentAccount, emptyArray()))
            }
            .map {
                val chatFull = localDataSource.getChatFull(chatId)
                JoinRequestMapper.mapChatPendingRequests(chatId, chatFull)
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getPendingRequestsCount(chatId: Long): Result<ChatPendingRequestsModel> = withContext(ioDispatcher) {
        try {
            val chatFull = localDataSource.getChatFull(chatId)
            Result.Success(JoinRequestMapper.mapChatPendingRequests(chatId, chatFull))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get pending requests count", e))
        }
    }

    override suspend fun getCachedRequests(chatId: Long): List<JoinRequestModel>? = withContext(ioDispatcher) {
        try {
            val cached = localDataSource.getCachedImporters(chatId)
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
    ): Result<JoinRequestsListModel> = withContext(ioDispatcher) {
        val inputPeer = localDataSource.getInputPeer(-chatId)
            ?: return@withContext Result.failure("Failed to resolve input peer for chatId $chatId")

        val offsetUser: TLRPC.InputUser = if (offsetUserId != null && offsetUserId != 0L) {
            val user = localDataSource.getUser(offsetUserId)
            if (user != null) localDataSource.getInputUser(user) ?: TLRPC.TL_inputUserEmpty() else TLRPC.TL_inputUserEmpty()
        } else {
            TLRPC.TL_inputUserEmpty()
        }

        when (val remoteResult = remoteDataSource.getChatInviteImporters(
            peer = inputPeer,
            requested = true,
            limit = limit,
            query = query,
            offsetUser = offsetUser,
            offsetDate = offsetDate ?: 0
        )) {
            is Result.Success -> {
                val response = remoteResult.data
                if (offsetUserId == null && query.isNullOrEmpty()) {
                    localDataSource.putCachedImporters(chatId, response)
                }
                Result.Success(JoinRequestMapper.mapImportersList(chatId, response))
            }
            is Result.Failure -> Result.Failure(remoteResult.error)
        }
    }

    override suspend fun approveRequest(chatId: Long, userId: Long): Result<Unit> =
        hideSingleJoinRequest(chatId, userId, approved = true)

    override suspend fun dismissRequest(chatId: Long, userId: Long): Result<Unit> =
        hideSingleJoinRequest(chatId, userId, approved = false)

    override suspend fun approveAllRequests(chatId: Long, inviteLink: String?): Result<Unit> =
        hideAllJoinRequests(chatId, inviteLink, approved = true)

    override suspend fun dismissAllRequests(chatId: Long, inviteLink: String?): Result<Unit> =
        hideAllJoinRequests(chatId, inviteLink, approved = false)

    private suspend fun hideSingleJoinRequest(
        chatId: Long,
        userId: Long,
        approved: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        val inputPeer = localDataSource.getInputPeer(-chatId)
            ?: return@withContext Result.failure("Failed to resolve input peer for chatId $chatId")

        val user = localDataSource.getUser(userId)
        val inputUser = if (user != null) localDataSource.getInputUser(user) ?: TLRPC.TL_inputUserEmpty() else TLRPC.TL_inputUserEmpty()

        when (val remoteResult = remoteDataSource.hideChatJoinRequest(inputPeer, inputUser, approved)) {
            is Result.Success -> {
                localDataSource.processUpdates(remoteResult.data)
                Result.Success(Unit)
            }
            is Result.Failure -> Result.Failure(remoteResult.error)
        }
    }

    private suspend fun hideAllJoinRequests(
        chatId: Long,
        inviteLink: String?,
        approved: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        val inputPeer = localDataSource.getInputPeer(-chatId)
            ?: return@withContext Result.failure("Failed to resolve input peer for chatId $chatId")

        when (val remoteResult = remoteDataSource.hideAllChatJoinRequests(inputPeer, inviteLink, approved)) {
            is Result.Success -> {
                localDataSource.processUpdates(remoteResult.data)
                Result.Success(Unit)
            }
            is Result.Failure -> Result.Failure(remoteResult.error)
        }
    }
}
