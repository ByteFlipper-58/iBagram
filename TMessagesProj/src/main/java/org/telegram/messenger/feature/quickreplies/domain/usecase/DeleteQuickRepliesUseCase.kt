package org.telegram.messenger.feature.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository

class DeleteQuickRepliesUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend operator fun invoke(ids: List<Int>): Result<Unit> {
        return repository.deleteReplies(ids)
    }
}
