package org.telegram.messenger.feature.messaging.bottomviews

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.bottomviews.data.datasource.BottomViewsLocalDataSource
import org.telegram.messenger.feature.messaging.bottomviews.data.datasource.BottomViewsRemoteDataSource
import org.telegram.messenger.feature.messaging.bottomviews.data.repository.BottomViewsVisibilityRepositoryImpl
import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomContainerType

class BottomViewsVisibilityRepositoryImplTest {

    private lateinit var localDataSource: BottomViewsLocalDataSource
    private lateinit var remoteDataSource: BottomViewsRemoteDataSource
    private lateinit var repository: BottomViewsVisibilityRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = BottomViewsLocalDataSource(0)
        remoteDataSource = BottomViewsRemoteDataSource(0)
        repository = BottomViewsVisibilityRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun initialState_hasDefaultVisible() {
        val state = repository.getState()
        assertEquals(0, state.priorityContainerId)
        assertEquals(1.0f, repository.getVisibility(0), 0.01f)
    }

    @Test
    fun setViewVisible_updatesPriorityAndVisibility() = runBlocking {
        repository.setViewVisible(BottomContainerType.MESSAGE_INPUT.id, true)

        val state = repository.observeState().first()
        assertTrue(state.isInputVisible)
        assertEquals(BottomContainerType.MESSAGE_INPUT.id, state.priorityContainerId)
        assertEquals(1.0f, repository.getVisibility(BottomContainerType.MESSAGE_INPUT.id), 0.01f)
    }

    @Test
    fun setViewVisible_false_clearsVisibility() {
        repository.setViewVisible(BottomContainerType.MESSAGE_INPUT.id, true)
        assertTrue(repository.getState().isInputVisible)

        repository.setViewVisible(BottomContainerType.MESSAGE_INPUT.id, false)
        assertFalse(repository.getState().isInputVisible)
        assertEquals(0.0f, repository.getVisibility(BottomContainerType.MESSAGE_INPUT.id), 0.01f)
    }

    @Test
    fun higherContainerId_takesPriority() {
        repository.setViewVisible(BottomContainerType.MESSAGE_INPUT.id, true)
        repository.setViewVisible(BottomContainerType.MESSAGE_SEARCH.id, true)

        assertEquals(BottomContainerType.MESSAGE_SEARCH.id, repository.getCurrentPriorityContainerId())
        assertEquals(1.0f, repository.getVisibility(BottomContainerType.MESSAGE_SEARCH.id), 0.01f)
    }
}
