package org.telegram.messenger.feature.chatmeta.domain.model

/**
 * Result of checking a viewport batch of messages for outdated metadata.
 */
data class ChatMetadataBatchResult(
    val reactionMessageIds: List<Int> = emptyList(),
    val extendedMediaMessageIds: List<Int> = emptyList(),
    val storyIds: List<Int> = emptyList()
) {
    val isEmpty: Boolean
        get() = reactionMessageIds.isEmpty() && extendedMediaMessageIds.isEmpty() && storyIds.isEmpty()

    val totalCount: Int
        get() = reactionMessageIds.size + extendedMediaMessageIds.size + storyIds.size
}
