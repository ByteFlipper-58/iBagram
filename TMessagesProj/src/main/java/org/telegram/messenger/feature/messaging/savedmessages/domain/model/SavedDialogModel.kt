package org.telegram.messenger.feature.messaging.savedmessages.domain.model

/**
 * Pure domain model representing a conversation item inside "Saved Messages".
 * Completely decoupled from Android UI, SQLite, and TLRPC classes.
 */
data class SavedDialogModel(
    val dialogId: Long,
    val title: String,
    val isPinned: Boolean,
    val unreadCount: Long,
    val messagesCount: Int,
    val lastMessageDate: Int,
    val topMessageId: Int,
    val topMessageSnippet: String? = null
)
