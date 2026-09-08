package org.telegram.messenger.feature.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository

class CanAddNewQuickReplyUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return repository.canAddNew()
    }
}
