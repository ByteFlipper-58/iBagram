package org.telegram.messenger.feature.joinrequests.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.joinrequests.domain.repository.JoinRequestsRepository

class DismissJoinRequestUseCase(
    private val repository: JoinRequestsRepository
) {
    suspend operator fun invoke(chatId: Long, userId: Long): Result<Unit> {
        return repository.dismissRequest(chatId, userId)
    }
}
