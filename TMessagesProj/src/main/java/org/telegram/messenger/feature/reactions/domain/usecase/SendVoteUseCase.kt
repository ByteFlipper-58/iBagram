package org.telegram.messenger.feature.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.reactions.domain.repository.ReactionsRepository

class SendVoteUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        messageId: Int,
        pollId: Long,
        options: List<ByteArray>
    ): Result<Unit> {
        return repository.sendVote(dialogId, messageId, pollId, options)
    }
}
