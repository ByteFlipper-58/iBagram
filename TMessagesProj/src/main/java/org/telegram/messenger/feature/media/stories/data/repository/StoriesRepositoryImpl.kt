package org.telegram.messenger.feature.media.stories.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.data.datasource.StoriesLocalDataSource
import org.telegram.messenger.feature.media.stories.data.datasource.StoriesRemoteDataSource
import org.telegram.messenger.feature.media.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.media.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryLimitModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryModel
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository

class StoriesRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: StoriesLocalDataSource,
    private val remoteDataSource: StoriesRemoteDataSource
) : StoriesRepository {

    override fun observeStories(): Flow<List<PeerStoriesModel>> = localDataSource.dialogStoriesFlow

    override fun observeHiddenStories(): Flow<List<PeerStoriesModel>> = localDataSource.hiddenStoriesFlow

    override fun observeStealthMode(): Flow<StealthModeModel> = localDataSource.stealthModeFlow

    override fun observeSelfStories(): Flow<PeerStoriesModel?> = localDataSource.selfStoriesFlow

    override suspend fun getStories(dialogId: Long): Result<List<StoryModel>> {
        val stories = localDataSource.getStories(dialogId)
        return Result.Success(stories)
    }

    override suspend fun markStoryAsRead(dialogId: Long, storyId: Int): Result<Unit> {
        localDataSource.markStoryAsRead(dialogId, storyId)
        remoteDataSource.markStoryAsRead(dialogId, storyId)
        return Result.Success(Unit)
    }

    override suspend fun deleteStory(dialogId: Long, storyId: Int): Result<Unit> {
        localDataSource.deleteStory(dialogId, storyId)
        remoteDataSource.deleteStory(dialogId, storyId)
        return Result.Success(Unit)
    }

    override suspend fun toggleStoryPin(dialogId: Long, storyId: Int, pin: Boolean): Result<Unit> {
        localDataSource.toggleStoryPin(dialogId, storyId, pin)
        remoteDataSource.toggleStoryPin(dialogId, storyId, pin)
        return Result.Success(Unit)
    }

    override suspend fun toggleStoryHidden(dialogId: Long, hide: Boolean): Result<Unit> {
        remoteDataSource.toggleStoryHidden(dialogId, hide)
        return Result.Success(Unit)
    }

    override suspend fun activateStealthMode(future: Boolean, past: Boolean): Result<Unit> {
        localDataSource.setStealthMode(active = true)
        remoteDataSource.activateStealthMode(future, past)
        return Result.Success(Unit)
    }

    override suspend fun getStoryLimit(): Result<StoryLimitModel> {
        return Result.Success(localDataSource.getStoryLimit())
    }

    override suspend fun refreshStories(): Result<Unit> {
        remoteDataSource.refreshStories()
        return Result.Success(Unit)
    }
}
