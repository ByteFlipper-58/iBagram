package org.telegram.messenger.feature.social.joinrequests.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository

class DismissAllJoinRequestsUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(chatId: Long, inviteLink: String? = null): Result<Unit> {
        return repository.dismissAllRequests(chatId, inviteLink)
    }
}
