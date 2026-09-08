package org.telegram.messenger.feature.joinrequests.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.joinrequests.domain.repository.JoinRequestsRepository

class ApproveJoinRequestUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(chatId: Long, userId: Long): Result<Unit> {
        return repository.approveRequest(chatId, userId)
    }
}
