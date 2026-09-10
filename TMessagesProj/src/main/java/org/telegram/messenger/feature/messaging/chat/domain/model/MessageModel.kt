package org.telegram.messenger.feature.messaging.chat.domain.model

/**
 * Pure domain model representing a message in a chat or channel.
 * Independent of Android SDK and legacy Telegram data structures.
 */
data class MessageModel(
    val id: Int,
    val dialogId: Long,
    val senderId: Long,
    val text: String,
    val date: Int,
    val isOut: Boolean,
    val isUnread: Boolean,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val replyToMsgId: Int? = null
)
