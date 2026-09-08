package org.telegram.messenger.feature.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.reactions.domain.repository.ReactionsRepository

class SendReactionUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        messageId: Int,
        reactions: List<ReactionItemModel>,
        isBig: Boolean = false,
        addToRecent: Boolean = true
    ): Result<Unit> {
        return repository.sendReaction(dialogId, messageId, reactions, isBig, addToRecent)
    }

    suspend fun single(
        dialogId: Long,
        messageId: Int,
        reaction: ReactionItemModel,
        isBig: Boolean = false,
        addToRecent: Boolean = true
    ): Result<Unit> {
        return repository.sendReaction(dialogId, messageId, listOf(reaction), isBig, addToRecent)
    }
}
