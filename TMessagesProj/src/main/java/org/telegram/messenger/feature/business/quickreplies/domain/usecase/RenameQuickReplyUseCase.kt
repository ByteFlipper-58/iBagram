package org.telegram.messenger.feature.business.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository

class RenameQuickReplyUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend operator fun invoke(id: Int, newName: String): Result<Unit> {
        return repository.renameReply(id, newName)
    }
}
