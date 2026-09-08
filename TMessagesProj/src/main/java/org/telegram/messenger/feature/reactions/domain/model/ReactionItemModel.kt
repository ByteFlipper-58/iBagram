package org.telegram.messenger.feature.reactions.domain.model

data class ReactionItemModel(
    val reaction: String,
    val title: String = "",
    val isCustom: Boolean = false,
    val documentId: Long = 0L,
    val isInactive: Boolean = false,
    val isPremium: Boolean = false,
    val isPaid: Boolean = false,
    val order: Int = 0
)
