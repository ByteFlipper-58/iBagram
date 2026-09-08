package org.telegram.messenger.feature.stories.domain.model

data class PeerStoriesModel(
    val dialogId: Long,
    val maxReadId: Int = 0,
    val stories: List<StoryModel> = emptyList(),
    val hasUnread: Boolean = false,
    val lastStoryDate: Long = 0L
)
