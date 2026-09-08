package org.telegram.messenger.feature.joinrequests.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.joinrequests.domain.model.JoinRequestModel
import org.telegram.messenger.feature.joinrequests.domain.model.JoinRequestsListModel

interface JoinRequestsRepository {
    fun observePendingRequests(chatId: Long): Flow<ChatPendingRequestsModel>
    suspend fun getPendingRequestsCount(chatId: Long): Result<ChatPendingRequestsModel>
    suspend fun getCachedRequests(chatId: Long): List<JoinRequestModel>?
    suspend fun loadRequests(
        chatId: Long,
        query: String? = null,
        offsetUserId: Long? = null,
        offsetDate: Int? = null,
        limit: Int = 30
    ): Result<JoinRequestsListModel>
    suspend fun approveRequest(chatId: Long, userId: Long): Result<Unit>
    suspend fun dismissRequest(chatId: Long, userId: Long): Result<Unit>
    suspend fun approveAllRequests(chatId: Long, inviteLink: String? = null): Result<Unit>
    suspend fun dismissAllRequests(chatId: Long, inviteLink: String? = null): Result<Unit>
}
