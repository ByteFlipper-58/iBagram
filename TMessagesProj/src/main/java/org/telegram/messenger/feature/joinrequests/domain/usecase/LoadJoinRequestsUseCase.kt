package org.telegram.messenger.feature.joinrequests.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.joinrequests.domain.model.JoinRequestsListModel
import org.telegram.messenger.feature.joinrequests.domain.repository.JoinRequestsRepository

class LoadJoinRequestsUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(
        chatId: Long,
        query: String? = null,
        offsetUserId: Long? = null,
        offsetDate: Int? = null,
        limit: Int = 30
    ): Result<JoinRequestsListModel> {
        return repository.loadRequests(chatId, query, offsetUserId, offsetDate, limit)
    }
}
