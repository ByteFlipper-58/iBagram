package org.telegram.messenger.feature.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.domain.model.QuickReplyModel
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository

class FindQuickReplyUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend fun byId(id: Int): Result<QuickReplyModel?> {
        return repository.findReplyById(id)
    }

    suspend fun byName(name: String): Result<QuickReplyModel?> {
        return repository.findReplyByName(name)
    }
}
