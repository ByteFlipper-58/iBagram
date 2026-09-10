package org.telegram.messenger.feature.business.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository

class GetQuickRepliesUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend operator fun invoke(): Result<List<QuickReplyModel>> {
        return repository.getQuickReplies()
    }
}
