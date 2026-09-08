package org.telegram.messenger.feature.reactions.domain.model

data class MessageReactionCountModel(
    val reaction: ReactionItemModel,
    val count: Int,
    val isChosen: Boolean = false,
    val chosenOrder: Int = 0
)
