package org.telegram.messenger.feature.messaging.draftmeasure

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.draftmeasure.data.datasource.DraftMeasureLocalDataSource
import org.telegram.messenger.feature.messaging.draftmeasure.data.datasource.DraftMeasureRemoteDataSource
import org.telegram.messenger.feature.messaging.draftmeasure.data.repository.DraftMeasureRepositoryImpl
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureViewport

class DraftMeasureRepositoryImplTest {

    private lateinit var localDataSource: DraftMeasureLocalDataSource
    private lateinit var remoteDataSource: DraftMeasureRemoteDataSource
    private lateinit var repository: DraftMeasureRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = DraftMeasureLocalDataSource(0)
        remoteDataSource = DraftMeasureRemoteDataSource(0)
        repository = DraftMeasureRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun initialTarget_isEmpty() {
        val target = repository.getTarget()
        assertEquals(0, target.messageId)
        assertEquals(0L, target.groupId)
        assertFalse(repository.hasAdditionalHeight())
    }

    @Test
    fun setTarget_updatesTargetAndConfigFlow() = runBlocking {
        val changed = repository.setTarget(42, 100L)
        assertTrue(changed)

        val target = repository.getTarget()
        assertEquals(42, target.messageId)
        assertEquals(100L, target.groupId)

        val config = repository.observeConfig().first()
        assertEquals(42, config.target.messageId)
    }

    @Test
    fun onMessageIdChanged_updatesMatchingTarget() {
        repository.setTarget(10, 5L)
        val changed = repository.onMessageIdChanged(10, 20, 5L)
        assertTrue(changed)
        assertEquals(20, repository.getTarget().messageId)

        val nonMatching = repository.onMessageIdChanged(999, 30, 5L)
        assertFalse(nonMatching)
        assertEquals(20, repository.getTarget().messageId)
    }

    @Test
    fun setPreviousMessageHeight_updatesState() {
        repository.setPreviousMessageHeight(150)
        assertEquals(150, repository.getPreviousMessageHeight())
        assertEquals(150, repository.getConfig().previousMessageHeight)
    }

    @Test
    fun calculateOverrideHeight_withMatchingTargetCalculatesAdditional() {
        repository.setTarget(100, 1L)
        repository.setPreviousMessageHeight(50)

        val viewport = DraftMeasureViewport(
            totalHeight = 1000,
            paddingTop = 50,
            paddingBottom = 50
        )
        // availHeight = 1000 - 50 - 50 - 50 = 850
        // measuredHeight = 600 -> additional = 850 - 600 = 250
        val result = repository.calculateOverrideHeight(100, 1L, 600, viewport)
        assertEquals(600, result.measuredHeight)
        assertEquals(250, result.additionalHeight)
        assertTrue(result.hasAdditionalHeight)
        assertTrue(repository.hasAdditionalHeight())
    }

    @Test
    fun resetTarget_resetsToEmpty() {
        repository.setTarget(100, 1L)
        repository.resetTarget()
        val target = repository.getTarget()
        assertEquals(0, target.messageId)
        assertFalse(repository.hasAdditionalHeight())
    }
}
