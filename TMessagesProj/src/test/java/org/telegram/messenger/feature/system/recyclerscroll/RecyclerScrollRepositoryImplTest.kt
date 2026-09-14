package org.telegram.messenger.feature.system.recyclerscroll

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.recyclerscroll.data.datasource.RecyclerScrollLocalDataSource
import org.telegram.messenger.feature.system.recyclerscroll.data.datasource.RecyclerScrollRemoteDataSource
import org.telegram.messenger.feature.system.recyclerscroll.data.repository.RecyclerScrollRepositoryImpl
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollDirection

class RecyclerScrollRepositoryImplTest {

    private lateinit var localDataSource: RecyclerScrollLocalDataSource
    private lateinit var remoteDataSource: RecyclerScrollRemoteDataSource
    private lateinit var repository: RecyclerScrollRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = RecyclerScrollLocalDataSource()
        remoteDataSource = RecyclerScrollRemoteDataSource()
        repository = RecyclerScrollRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() {
        val state = repository.getState()
        assertFalse(state.isRunning)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun testScrollLifecycle() {
        repository.startScroll(position = 10, offset = 50, direction = ScrollDirection.DOWN, durationMs = 250)
        var state = repository.getState()
        assertTrue(state.isRunning)
        assertEquals(10, state.targetPosition)
        assertEquals(50, state.targetOffset)
        assertEquals(ScrollDirection.DOWN, state.direction)

        repository.updateProgress(0.5f)
        assertEquals(0.5f, repository.getState().progress, 0.001f)

        repository.finishScroll()
        state = repository.getState()
        assertFalse(state.isRunning)
        assertEquals(1f, state.progress, 0.001f)

        repository.reset()
        assertEquals(0f, repository.getState().progress, 0.001f)
    }

    @Test
    fun testCancelScroll() {
        repository.startScroll(position = 5, offset = 0, direction = ScrollDirection.UP, durationMs = 200)
        assertTrue(repository.getState().isRunning)
        repository.cancelScroll()
        assertFalse(repository.getState().isRunning)
        assertEquals(0f, repository.getState().progress, 0.001f)
    }

    @Test
    fun testCalculations() {
        val eligibility = repository.evaluateEligibility(
            fastScrollRunning = false,
            itemAnimatorRunning = false,
            childCount = 10,
            viewAnimationsEnabled = true,
            smooth = true,
            direction = ScrollDirection.DOWN
        )
        assertNotNull(eligibility)

        val plan = repository.calculatePlan(
            ScrollAnimationSpec(
                scrollDirection = ScrollDirection.DOWN,
                isDialogs = false,
                hasSameViews = true,
                scrollLength = 500,
                containerHeight = 1000
            )
        )
        assertNotNull(plan)

        val translations = repository.computeViewTranslations(
            scrollLength = 500,
            isScrollDown = true,
            progress = 0.5f,
            additionalY = 0
        )
        assertNotNull(translations)
    }

    @Test
    fun testRemoteDataSource() = runBlocking {
        val result = remoteDataSource.syncRecyclerScrollConfig()
        assertTrue(result.isSuccess)
    }
}
