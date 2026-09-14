package org.telegram.messenger.feature.media.stories

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.data.datasource.StoriesLocalDataSource
import org.telegram.messenger.feature.media.stories.data.datasource.StoriesRemoteDataSource
import org.telegram.messenger.feature.media.stories.data.repository.StoriesRepositoryImpl
import org.telegram.messenger.feature.media.stories.domain.model.StoryModel

class StoriesRepositoryImplTest {

    private lateinit var localDataSource: StoriesLocalDataSource
    private lateinit var remoteDataSource: StoriesRemoteDataSource
    private lateinit var repository: StoriesRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = StoriesLocalDataSource(0)
        remoteDataSource = StoriesRemoteDataSource(0)
        repository = StoriesRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun getStories_returnsCorrectItems() = runBlocking {
        val s1 = StoryModel(id = 10, dialogId = 100L, date = 1000L)
        val s2 = StoryModel(id = 11, dialogId = 100L, date = 2000L)
        localDataSource.setStories(100L, listOf(s1, s2))

        val result = repository.getStories(100L)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(2, list.size)
        assertEquals(10, list[0].id)
    }

    @Test
    fun markStoryAsRead_updatesStateFlow() = runBlocking {
        val s1 = StoryModel(id = 5, dialogId = 200L, date = 1000L, isUnread = true)
        localDataSource.setStories(200L, listOf(s1))

        val markResult = repository.markStoryAsRead(200L, 5)
        assertTrue(markResult is Result.Success)

        val peers = repository.observeStories().first()
        val story = peers.first().stories.first()
        assertFalse(story.isUnread)
        assertEquals(5, peers.first().maxReadId)
    }

    @Test
    fun deleteStory_removesFromLocalDataSource() = runBlocking {
        val s1 = StoryModel(id = 1, dialogId = 300L, date = 1000L)
        val s2 = StoryModel(id = 2, dialogId = 300L, date = 2000L)
        localDataSource.setStories(300L, listOf(s1, s2))

        val deleteResult = repository.deleteStory(300L, 1)
        assertTrue(deleteResult is Result.Success)

        val remaining = repository.getStories(300L)
        assertTrue(remaining is Result.Success)
        assertEquals(1, (remaining as Result.Success).data.size)
        assertEquals(2, remaining.data.first().id)
    }

    @Test
    fun activateStealthMode_updatesStealthFlow() = runBlocking {
        assertFalse(repository.observeStealthMode().first().isActive)

        val result = repository.activateStealthMode(future = true, past = true)
        assertTrue(result is Result.Success)
        assertTrue(repository.observeStealthMode().first().isActive)
    }

    @Test
    fun getStoryLimit_returnsExpectedLimits() = runBlocking {
        val limitResult = repository.getStoryLimit()
        assertTrue(limitResult is Result.Success)
        val limit = (limitResult as Result.Success).data
        assertEquals(100, limit.limit)
    }
}
