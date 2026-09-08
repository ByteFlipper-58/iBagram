package org.telegram.messenger.feature.joinrequests.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.joinrequests.domain.repository.JoinRequestsRepository

class ApproveAllJoinRequestsUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(chatId: Long, inviteLink: String? = null): Result<Unit> {
        return repository.approveAllRequests(chatId, inviteLink)
    }
}
