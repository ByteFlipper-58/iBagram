package org.telegram.messenger.feature.media.stories.domain.model

data class StoryLimitModel(
    val limit: Int,
    val currentCount: Int,
    val isOverLimit: Boolean = currentCount >= limit
)
