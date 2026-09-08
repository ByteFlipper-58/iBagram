package org.telegram.messenger.feature.joinrequests.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.joinrequests.domain.repository.JoinRequestsRepository

class GetPendingRequestsCountUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(chatId: Long): Result<ChatPendingRequestsModel> {
        return repository.getPendingRequestsCount(chatId)
    }
}
