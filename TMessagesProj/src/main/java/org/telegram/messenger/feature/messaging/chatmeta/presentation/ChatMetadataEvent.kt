package org.telegram.messenger.feature.messaging.chatmeta.presentation

import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem

/**
 * UI intents for chat messages metadata operations.
 */
sealed interface ChatMetadataEvent {
    data class CheckMessages(
        val dialogId: Long,
        val items: List<MessageMetadataCheckItem>,
        val currentTime: Long = System.currentTimeMillis()
    ) : ChatMetadataEvent

    data class LoadReactions(val dialogId: Long, val messageIds: List<Int>) : ChatMetadataEvent
    data class LoadExtendedMedia(val dialogId: Long, val messageIds: List<Int>) : ChatMetadataEvent
    data object CancelPending : ChatMetadataEvent
    data object DismissInfo : ChatMetadataEvent
}
