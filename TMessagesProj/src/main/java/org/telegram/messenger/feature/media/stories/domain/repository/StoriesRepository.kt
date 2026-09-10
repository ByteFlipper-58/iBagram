package org.telegram.messenger.feature.media.stories.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.media.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryLimitModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryModel

interface StoriesRepository {
    fun observeStories(): Flow<List<PeerStoriesModel>>
    fun observeHiddenStories(): Flow<List<PeerStoriesModel>>
    fun observeStealthMode(): Flow<StealthModeModel>
    fun observeSelfStories(): Flow<PeerStoriesModel?>
    
    suspend fun getStories(dialogId: Long): Result<List<StoryModel>>
    suspend fun markStoryAsRead(dialogId: Long, storyId: Int): Result<Unit>
    suspend fun deleteStory(dialogId: Long, storyId: Int): Result<Unit>
    suspend fun toggleStoryPin(dialogId: Long, storyId: Int, pin: Boolean): Result<Unit>
    suspend fun toggleStoryHidden(dialogId: Long, hide: Boolean): Result<Unit>
    suspend fun activateStealthMode(future: Boolean, past: Boolean): Result<Unit>
    suspend fun getStoryLimit(): Result<StoryLimitModel>
    suspend fun refreshStories(): Result<Unit>
}
