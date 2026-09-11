package org.telegram.messenger.feature.messaging.reactions.domain.model

data class ReactionItemModel @JvmOverloads constructor(
    val reaction: String,
    val title: String = "",
    val isCustom: Boolean = false,
    val documentId: Long = 0L,
    val isInactive: Boolean = false,
    val isPremium: Boolean = false,
    val isPaid: Boolean = false,
    val order: Int = 0
)
