package org.telegram.messenger.feature.messaging.reactions

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.data.datasource.ReactionsLocalDataSource
import org.telegram.messenger.feature.messaging.reactions.data.datasource.ReactionsRemoteDataSource
import org.telegram.messenger.feature.messaging.reactions.data.repository.ReactionsRepositoryImpl
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionItemModel

class ReactionsRepositoryImplTest {

    private lateinit var localDataSource: ReactionsLocalDataSource
    private lateinit var remoteDataSource: ReactionsRemoteDataSource
    private lateinit var repository: ReactionsRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = ReactionsLocalDataSource(0)
        remoteDataSource = ReactionsRemoteDataSource(0)
        repository = ReactionsRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialAvailableReactions() = runBlocking {
        val result = repository.getAvailableReactions()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertNotNull(list)
    }

    @Test
    fun testSetAndGetAvailableReactions() = runBlocking {
        val item = ReactionItemModel(reaction = "👍")
        localDataSource.setAvailableReactions(listOf(item))

        val result = repository.getAvailableReactions()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("👍", list[0].reaction)

        val observed = repository.observeAvailableReactions().first()
        assertEquals(1, observed.size)
        assertEquals("👍", observed[0].reaction)
    }

    @Test
    fun testSetAndGetRecentReactions() = runBlocking {
        val item = ReactionItemModel(reaction = "❤️")
        localDataSource.setRecentReactions(listOf(item))

        val result = repository.getRecentReactions()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("❤️", list[0].reaction)

        val observed = repository.observeRecentReactions().first()
        assertEquals(1, observed.size)
        assertEquals("❤️", observed[0].reaction)
    }

    @Test
    fun testDoubleTapReaction() = runBlocking {
        repository.setDoubleTapReaction("🔥")

        val result = repository.getDoubleTapReaction()
        assertTrue(result is Result.Success)
        assertEquals("🔥", (result as Result.Success).data)
    }

    @Test
    fun testGetReactionsSettings() = runBlocking {
        localDataSource.setAvailableReactions(listOf(ReactionItemModel(reaction = "🎉")))
        localDataSource.setRecentReactions(listOf(ReactionItemModel(reaction = "🚀")))
        repository.setDoubleTapReaction("🎉")

        val result = repository.getReactionsSettings()
        assertTrue(result is Result.Success)
        val settings = (result as Result.Success).data
        assertEquals("🎉", settings.doubleTapReaction)
        assertEquals(1, settings.availableReactions.size)
        assertEquals(1, settings.recentReactions.size)
    }
}
