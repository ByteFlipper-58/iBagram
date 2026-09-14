package org.telegram.messenger.feature.system.adjustpan

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.adjustpan.data.datasource.AdjustPanLocalDataSource
import org.telegram.messenger.feature.system.adjustpan.data.datasource.AdjustPanRemoteDataSource
import org.telegram.messenger.feature.system.adjustpan.data.repository.AdjustPanRepositoryImpl
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan

class AdjustPanRepositoryImplTest {

    private lateinit var localDataSource: AdjustPanLocalDataSource
    private lateinit var remoteDataSource: AdjustPanRemoteDataSource
    private lateinit var repository: AdjustPanRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = AdjustPanLocalDataSource()
        remoteDataSource = AdjustPanRemoteDataSource()
        repository = AdjustPanRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() {
        val state = repository.getState()
        assertTrue(state.isEnabled)
        assertFalse(state.isAnimationInProgress)
        assertEquals(0f, state.currentTranslationY, 0.001f)
    }

    @Test
    fun testCalculatePlanAndTransition() {
        val spec = PanCalculationSpec(
            previousHeight = 800,
            contentHeight = 500,
            isHeightAnimationEnabled = true
        )
        val plan = repository.calculatePlan(spec)
        assertNotNull(plan)

        repository.startTransition(plan)
        val state = repository.getState()
        assertEquals(plan.shouldAnimate, state.isAnimationInProgress)

        repository.updateTransition(0.5f)
        assertEquals(0.5f, repository.getState().currentProgress, 0.001f)

        repository.stopTransition(1.0f, true)
        assertFalse(repository.getState().isAnimationInProgress)
        assertEquals(0f, repository.getState().currentTranslationY, 0.001f)
    }

    @Test
    fun testSetEnabledAndReset() {
        repository.setEnabled(false)
        assertFalse(repository.getState().isEnabled)

        val spec = PanCalculationSpec(
            previousHeight = 800,
            contentHeight = 500,
            isHeightAnimationEnabled = true
        )
        val plan = repository.calculatePlan(spec)
        assertFalse(plan.shouldAnimate)

        repository.reset()
        assertTrue(repository.getState().isEnabled)
    }

    @Test
    fun testRemoteDataSource() = runBlocking {
        val result = remoteDataSource.syncAdjustPanConfig()
        assertTrue(result.isSuccess)
    }
}
