package org.telegram.messenger.feature.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository

class LoadQuickRepliesUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<Unit> {
        return repository.loadQuickReplies(force)
    }
}
