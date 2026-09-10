package org.telegram.messenger.feature.social.joinrequests.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository

class GetPendingRequestsCountUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(chatId: Long): Result<ChatPendingRequestsModel> {
        return repository.getPendingRequestsCount(chatId)
    }
}
