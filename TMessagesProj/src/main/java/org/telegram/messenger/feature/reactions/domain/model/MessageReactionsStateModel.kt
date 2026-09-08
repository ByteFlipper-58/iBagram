package org.telegram.messenger.feature.reactions.domain.model

data class MessageReactionsStateModel(
    val dialogId: Long,
    val messageId: Int,
    val canSeeList: Boolean = false,
    val reactions: List<MessageReactionCountModel> = emptyList()
)
