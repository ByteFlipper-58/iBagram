package org.telegram.messenger.feature.quickreplies.domain.model

data class QuickReplyModel(
    val id: Int,
    val name: String,
    val order: Int,
    val topMessageId: Int,
    val messagesCount: Int,
    val isSpecial: Boolean
)
