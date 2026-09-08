package org.telegram.messenger.feature.reactions.presentation

import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel

sealed class ReactionsEvent {
    data class LoadAvailableReactions(val force: Boolean = false) : ReactionsEvent()
    object LoadRecentReactions : ReactionsEvent()
    object LoadSettings : ReactionsEvent()
    data class SetDoubleTapReaction(val reaction: String) : ReactionsEvent()
    data class SendReaction(
        val dialogId: Long,
        val messageId: Int,
        val reaction: ReactionItemModel,
        val isBig: Boolean = false,
        val addToRecent: Boolean = true
    ) : ReactionsEvent()
    data class SendMultipleReactions(
        val dialogId: Long,
        val messageId: Int,
        val reactions: List<ReactionItemModel>,
        val isBig: Boolean = false,
        val addToRecent: Boolean = true
    ) : ReactionsEvent()
    data class ClearReactions(
        val dialogId: Long,
        val messageId: Int
    ) : ReactionsEvent()
    data class SendVote(
        val dialogId: Long,
        val messageId: Int,
        val pollId: Long,
        val options: List<ByteArray>
    ) : ReactionsEvent()
    object ClearMessages : ReactionsEvent()
}
