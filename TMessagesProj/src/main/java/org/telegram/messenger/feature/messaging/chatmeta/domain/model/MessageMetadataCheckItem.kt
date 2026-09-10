package org.telegram.messenger.feature.messaging.chatmeta.domain.model

/**
 * Pure domain descriptor representing a message item checked for metadata updates.
 */
data class MessageMetadataCheckItem(
    val messageId: Int,
    val dialogId: Long,
    val canHaveReactions: Boolean = false,
    val hasExtendedMedia: Boolean = false,
    val hasStory: Boolean = false,
    val storyId: Int = 0,
    val storyPeerId: Long = 0L,
    val lastReactionsCheckTime: Long = 0L,
    val lastExtendedMediaCheckTime: Long = 0L,
    val lastStoryCheckTime: Long = 0L
) {
    fun needsReactionsCheck(currentTime: Long, intervalMs: Long = 15_000L): Boolean {
        return canHaveReactions && messageId > 0 && (currentTime - lastReactionsCheckTime > intervalMs)
    }

    fun needsExtendedMediaCheck(currentTime: Long, intervalMs: Long = 30_000L): Boolean {
        return hasExtendedMedia && messageId > 0 && (currentTime - lastExtendedMediaCheckTime > intervalMs)
    }

    fun needsStoryCheck(currentTime: Long, intervalMs: Long = 300_000L): Boolean {
        return hasStory && storyId > 0 && (currentTime - lastStoryCheckTime > intervalMs)
    }
}
