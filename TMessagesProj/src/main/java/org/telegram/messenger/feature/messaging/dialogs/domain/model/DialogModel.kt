package org.telegram.messenger.feature.messaging.dialogs.domain.model

/**
 * Pure domain model representing a Telegram dialog (chat, channel, direct message, or group).
 * Independent of Android framework and legacy Telegram TLRPC data structures.
 */
data class DialogModel(
    val id: Long,
    val unreadCount: Int = 0,
    val unreadMentionsCount: Int = 0,
    val unreadReactionsCount: Int = 0,
    val lastMessageDate: Int = 0,
    val lastMessageId: Int = 0,
    val isPinned: Boolean = false,
    val pinnedNum: Int = 0,
    val isMuted: Boolean = false,
    val folderId: Int = 0,
    val isForum: Boolean = false,
    val draftText: String? = null
)
