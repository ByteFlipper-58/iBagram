package org.telegram.messenger.feature.joinrequests.domain.usecase

import org.telegram.messenger.feature.joinrequests.domain.model.JoinRequestModel
import org.telegram.messenger.feature.joinrequests.domain.repository.JoinRequestsRepository

class GetCachedJoinRequestsUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(chatId: Long): List<JoinRequestModel>? {
        return repository.getCachedRequests(chatId)
    }
}
