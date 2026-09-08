package org.telegram.messenger.feature.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository

class SendQuickReplyUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend operator fun invoke(dialogId: Long, shortcutId: Int): Result<Unit> {
        return repository.sendQuickReply(dialogId, shortcutId)
    }
}
