package org.telegram.messenger.feature.media.stories.presentation

import org.telegram.messenger.feature.media.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.media.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryLimitModel

data class StoriesUiState(
    val peerStories: List<PeerStoriesModel> = emptyList(),
    val hiddenStories: List<PeerStoriesModel> = emptyList(),
    val selfStories: PeerStoriesModel? = null,
    val stealthMode: StealthModeModel? = null,
    val storyLimit: StoryLimitModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
