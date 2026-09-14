package org.telegram.messenger.feature.system.fpscontent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.fpscontent.data.datasource.FpsContentLocalDataSource
import org.telegram.messenger.feature.system.fpscontent.data.datasource.FpsContentRemoteDataSource
import org.telegram.messenger.feature.system.fpscontent.data.repository.FpsContentRepositoryImpl

class FpsContentRepositoryImplTest {

    private lateinit var localDataSource: FpsContentLocalDataSource
    private lateinit var remoteDataSource: FpsContentRemoteDataSource
    private lateinit var repository: FpsContentRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = FpsContentLocalDataSource()
        remoteDataSource = FpsContentRemoteDataSource(currentAccount = 0)
        repository = FpsContentRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun initialStats_areCleanAndDefaults() {
        val stats = repository.getStats()
        assertTrue(stats.isRunning)
        assertEquals(60, stats.targetFps)
        assertEquals(0, stats.activeGroupsCount)
        assertEquals(0, stats.totalSubscriptionsCount)
        assertEquals(0L, stats.totalDispatchedFrames)
        assertTrue(repository.getSubscriptions().isEmpty())
    }

    @Test
    fun addFrameCallback_andRemove_updatesSubscriptions() {
        var frameReceived = 0L
        val id = repository.addFrameCallback(fps = 60, isOneShot = false) { ts ->
            frameReceived = ts
        }

        assertEquals(1, repository.getStats().totalSubscriptionsCount)
        assertEquals(1, repository.getSubscriptions().size)

        repository.dispatchVsync(100_000_000L)
        assertEquals(100_000_000L, frameReceived)

        val removed = repository.removeCallback(id)
        assertTrue(removed)
        assertEquals(0, repository.getStats().totalSubscriptionsCount)
    }

    @Test
    fun addRunnableCallback_oneShot_firesAndRemoves() {
        var firedCount = 0
        val id = repository.addRunnableCallback(fps = 60, isOneShot = true) {
            firedCount++
        }

        assertEquals(1, repository.getStats().totalSubscriptionsCount)

        repository.dispatchVsync(100_000_000L)
        assertEquals(1, firedCount)
        assertEquals(0, repository.getStats().totalSubscriptionsCount)

        // Subsequent vsync should not fire again
        repository.dispatchVsync(200_000_000L)
        assertEquals(1, firedCount)
    }

    @Test
    fun postInvalidateViewAndDrawable_incrementsPendingCount() {
        repository.postInvalidateView("view_avatar")
        repository.postInvalidateDrawable("drawable_icon", fps = 60)
        repository.postInvalidateDrawable("drawable_badge", fps = 30)

        val stats = repository.getStats()
        assertEquals(1, stats.pendingViewsCount)
        assertEquals(1, stats.pendingDrawablesCount)
        assertEquals(1, stats.pendingDrawables30fpsCount)

        // After vsync, invalidations are cleared
        repository.dispatchVsync(100_000_000L)
        val statsAfter = repository.getStats()
        assertEquals(0, statsAfter.pendingViewsCount)
        assertEquals(0, statsAfter.pendingDrawablesCount)
    }

    @Test
    fun dispatchVsync_firesExpectedTicksFor60FpsAnd30Fps() {
        var count60 = 0
        var count30 = 0

        repository.addFrameCallback(fps = 60) { count60++ }
        repository.addFrameCallback(fps = 30) { count30++ }

        // Tick 1 (stride 1 for 60fps, stride 2 for 30fps -> odd tick: only 60fps)
        repository.dispatchVsync(16_666_666L)
        assertEquals(1, count60)

        // Tick 2 (stride 1 and stride 2 match -> both fire)
        repository.dispatchVsync(33_333_333L)
        assertEquals(2, count60)
        assertEquals(1, count30)
    }

    @Test
    fun multipleVsyncDispatches_accumulatesFramesAndEmitsTicks() {
        val ticks = repository.dispatchVsync(100_000_000L)
        assertTrue(repository.getStats().totalDispatchedFrames >= 1)
        assertEquals(1L, repository.getStats().totalDispatchedFrames)
    }

    @Test
    fun reset_clearsAllStateAndStats() {
        repository.addFrameCallback(fps = 60) {}
        repository.postInvalidateView("view_1")
        repository.dispatchVsync(100_000_000L)

        assertTrue(repository.getStats().totalDispatchedFrames > 0)

        repository.reset()
        val stats = repository.getStats()
        assertEquals(0L, stats.totalDispatchedFrames)
        assertEquals(0, stats.activeGroupsCount)
        assertEquals(0, stats.totalSubscriptionsCount)
        assertEquals(0, stats.pendingViewsCount)
    }
}
