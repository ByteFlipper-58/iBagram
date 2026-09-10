package org.telegram.messenger.feature.social.joinrequests.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.social.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository

class ObservePendingRequestsUseCase(
    private val repository: JoinRequestsRepository
) {
    operator fun invoke(chatId: Long): Flow<ChatPendingRequestsModel> {
        return repository.observePendingRequests(chatId)
    }
}
