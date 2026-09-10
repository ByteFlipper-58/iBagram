package org.telegram.messenger.feature.system.fpscontent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.fpscontent.data.repository.LegacyFpsContentRepository
import org.telegram.messenger.feature.system.fpscontent.domain.model.FpsTimingUtils
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameCallbackType
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.CalculateFpsTimingUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.DispatchVsyncTickUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsContentStatsUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsSubscriptionsUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsContentStatsUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsTicksUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterFrameCallbackUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterRunnableCallbackUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestDrawableInvalidationUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestViewInvalidationUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.ResetFpsContentUseCase
import org.telegram.messenger.feature.system.fpscontent.domain.usecase.UnregisterCallbackUseCase
import org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentEvent
import org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FpsContentDomainTest {

    private lateinit var repository: LegacyFpsContentRepository

    private lateinit var registerFrameCallbackUseCase: RegisterFrameCallbackUseCase
    private lateinit var registerRunnableCallbackUseCase: RegisterRunnableCallbackUseCase
    private lateinit var unregisterCallbackUseCase: UnregisterCallbackUseCase
    private lateinit var requestViewInvalidationUseCase: RequestViewInvalidationUseCase
    private lateinit var requestDrawableInvalidationUseCase: RequestDrawableInvalidationUseCase
    private lateinit var dispatchVsyncTickUseCase: DispatchVsyncTickUseCase
    private lateinit var calculateFpsTimingUseCase: CalculateFpsTimingUseCase
    private lateinit var getFpsContentStatsUseCase: GetFpsContentStatsUseCase
    private lateinit var getFpsSubscriptionsUseCase: GetFpsSubscriptionsUseCase
    private lateinit var observeFpsContentStatsUseCase: ObserveFpsContentStatsUseCase
    private lateinit var observeFpsTicksUseCase: ObserveFpsTicksUseCase
    private lateinit var resetFpsContentUseCase: ResetFpsContentUseCase

    @Before
    fun setUp() {
        repository = LegacyFpsContentRepository()

        registerFrameCallbackUseCase = RegisterFrameCallbackUseCase(repository)
        registerRunnableCallbackUseCase = RegisterRunnableCallbackUseCase(repository)
        unregisterCallbackUseCase = UnregisterCallbackUseCase(repository)
        requestViewInvalidationUseCase = RequestViewInvalidationUseCase(repository)
        requestDrawableInvalidationUseCase = RequestDrawableInvalidationUseCase(repository)
        dispatchVsyncTickUseCase = DispatchVsyncTickUseCase(repository)
        calculateFpsTimingUseCase = CalculateFpsTimingUseCase()
        getFpsContentStatsUseCase = GetFpsContentStatsUseCase(repository)
        getFpsSubscriptionsUseCase = GetFpsSubscriptionsUseCase(repository)
        observeFpsContentStatsUseCase = ObserveFpsContentStatsUseCase(repository)
        observeFpsTicksUseCase = ObserveFpsTicksUseCase(repository)
        resetFpsContentUseCase = ResetFpsContentUseCase(repository)
    }

    @Test
    fun testFpsTimingCalculations() {
        val config60 = calculateFpsTimingUseCase(60)
        assertEquals(60, config60.fps)
        assertEquals(1, config60.stride)
        assertTrue(config60.isStrideBased)

        val config30 = calculateFpsTimingUseCase(30)
        assertEquals(30, config30.fps)
        assertEquals(2, config30.stride)
        assertTrue(config30.isStrideBased)

        val config20 = calculateFpsTimingUseCase(20)
        assertEquals(20, config20.fps)
        assertEquals(3, config20.stride)
        assertTrue(config20.isStrideBased)

        val config24 = calculateFpsTimingUseCase(24)
        assertEquals(24, config24.fps)
        assertEquals(0, config24.stride)
        assertFalse(config24.isStrideBased)

        // Clamping tests
        val config120 = calculateFpsTimingUseCase(120)
        assertEquals(60, config120.fps)

        val config0 = calculateFpsTimingUseCase(0)
        assertEquals(1, config0.fps)

        // Stride shouldFire tests
        val (fireStride0, _) = FpsTimingUtils.shouldFire(
            counter = 0L,
            stride = 2,
            currentAccumulatedNs = 0L,
            intervalNs = 33_333_333L
        )
        assertTrue(fireStride0)

        val (fireStride1, _) = FpsTimingUtils.shouldFire(
            counter = 1L,
            stride = 2,
            currentAccumulatedNs = 0L,
            intervalNs = 33_333_333L
        )
        assertFalse(fireStride1)
    }

    @Test
    fun testAddAndRemoveFrameCallbacks() {
        var callbackFired = 0
        val id = registerFrameCallbackUseCase(fps = 60, isOneShot = false) {
            callbackFired++
        }

        assertNotNull(id)
        val stats = getFpsContentStatsUseCase()
        assertEquals(1, stats.totalSubscriptionsCount)
        assertEquals(1, stats.activeGroupsCount)

        val subs = getFpsSubscriptionsUseCase()
        assertEquals(1, subs.size)
        assertEquals(FrameCallbackType.FRAME_TICK, subs[0].type)
        assertEquals(60, subs[0].fps)

        val removed = unregisterCallbackUseCase(id)
        assertTrue(removed)

        val statsAfter = getFpsContentStatsUseCase()
        assertEquals(0, statsAfter.totalSubscriptionsCount)
        assertEquals(0, statsAfter.activeGroupsCount)
    }

    @Test
    fun testDispatchVsyncAndStrideExecution() {
        var count60 = 0
        var count30 = 0

        registerRunnableCallbackUseCase(fps = 60, isOneShot = false) {
            count60++
        }
        registerRunnableCallbackUseCase(fps = 30, isOneShot = false) {
            count30++
        }

        val frameInterval = FpsTimingUtils.TARGET_FRAME_INTERVAL_NS
        var timeNs = 1_000_000_000L

        // First tick: sets lastVsyncNs baseline, no frames dispatched yet
        val firstTicks = dispatchVsyncTickUseCase(timeNs)
        assertTrue(firstTicks.isEmpty())
        assertEquals(0, count60)
        assertEquals(0, count30)

        // Second tick: +16.67ms -> Frame 0 (counter=0). Stride 1 and 2 both fire on 0 % N == 0
        timeNs += frameInterval
        val tick0 = dispatchVsyncTickUseCase(timeNs)
        assertEquals(2, tick0.size)
        assertEquals(1, count60)
        assertEquals(1, count30)

        // Third tick: +16.67ms -> Frame 1 (counter=1). 60fps fires (1 % 1 == 0), 30fps does not (1 % 2 != 0)
        timeNs += frameInterval
        val tick1 = dispatchVsyncTickUseCase(timeNs)
        assertEquals(1, tick1.size)
        assertEquals(60, tick1[0].fps)
        assertEquals(2, count60)
        assertEquals(1, count30)

        // Fourth tick: +16.67ms -> Frame 2 (counter=2). Both fire (2 % 1 == 0, 2 % 2 == 0)
        timeNs += frameInterval
        val tick2 = dispatchVsyncTickUseCase(timeNs)
        assertEquals(2, tick2.size)
        assertEquals(3, count60)
        assertEquals(2, count30)
    }

    @Test
    fun testOneShotRunnablesAndCallbacks() {
        var oneShotFired = 0
        registerRunnableCallbackUseCase(fps = 60, isOneShot = true) {
            oneShotFired++
        }

        assertEquals(1, getFpsContentStatsUseCase().totalSubscriptionsCount)

        val frameInterval = FpsTimingUtils.TARGET_FRAME_INTERVAL_NS
        var timeNs = 2_000_000_000L

        // Baseline
        dispatchVsyncTickUseCase(timeNs)

        // Dispatch frame
        timeNs += frameInterval
        dispatchVsyncTickUseCase(timeNs)

        assertEquals(1, oneShotFired)
        // Subscriptions should now be cleared
        assertEquals(0, getFpsContentStatsUseCase().totalSubscriptionsCount)

        // Subsequent frame should not fire again
        timeNs += frameInterval
        dispatchVsyncTickUseCase(timeNs)
        assertEquals(1, oneShotFired)
    }

    @Test
    fun testViewAndDrawableInvalidations() {
        requestViewInvalidationUseCase("header_view")
        requestDrawableInvalidationUseCase("avatar_drawable", 60)
        requestDrawableInvalidationUseCase("background_drawable_30", 30)

        val statsBefore = getFpsContentStatsUseCase()
        assertEquals(1, statsBefore.pendingViewsCount)
        assertEquals(1, statsBefore.pendingDrawablesCount)
        assertEquals(1, statsBefore.pendingDrawables30fpsCount)

        val frameInterval = FpsTimingUtils.TARGET_FRAME_INTERVAL_NS
        var timeNs = 3_000_000_000L

        dispatchVsyncTickUseCase(timeNs) // baseline

        timeNs += frameInterval
        dispatchVsyncTickUseCase(timeNs) // frame 0

        val statsAfter = getFpsContentStatsUseCase()
        assertEquals(0, statsAfter.pendingViewsCount)
        assertEquals(0, statsAfter.pendingDrawablesCount)
        assertEquals(0, statsAfter.pendingDrawables30fpsCount)
    }

    @Test
    fun testFpsContentViewModelFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = FpsContentViewModel(
            registerFrameCallbackUseCase = registerFrameCallbackUseCase,
            registerRunnableCallbackUseCase = registerRunnableCallbackUseCase,
            unregisterCallbackUseCase = unregisterCallbackUseCase,
            requestViewInvalidationUseCase = requestViewInvalidationUseCase,
            requestDrawableInvalidationUseCase = requestDrawableInvalidationUseCase,
            dispatchVsyncTickUseCase = dispatchVsyncTickUseCase,
            getFpsContentStatsUseCase = getFpsContentStatsUseCase,
            getFpsSubscriptionsUseCase = getFpsSubscriptionsUseCase,
            observeFpsContentStatsUseCase = observeFpsContentStatsUseCase,
            observeFpsTicksUseCase = observeFpsTicksUseCase,
            resetFpsContentUseCase = resetFpsContentUseCase,
            scope = testScope
        )

        testScheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.activeSubscriptionsCount)
        assertFalse(viewModel.uiState.value.hasActiveAnimations)

        // 1. Register a frame callback
        viewModel.onEvent(FpsContentEvent.RegisterFrameCallback(fps = 60, isOneShot = false))
        testScheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.activeSubscriptionsCount)
        assertTrue(viewModel.uiState.value.hasActiveAnimations)

        // 2. Request view invalidation
        viewModel.onEvent(FpsContentEvent.PostInvalidateView("chat_bubble_view"))
        testScheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.stats.pendingViewsCount)

        // 3. Dispatch vsync ticks
        val frameInterval = FpsTimingUtils.TARGET_FRAME_INTERVAL_NS
        var timeNs = 4_000_000_000L
        viewModel.onEvent(FpsContentEvent.DispatchVsync(timeNs))
        testScheduler.runCurrent()

        timeNs += frameInterval
        viewModel.onEvent(FpsContentEvent.DispatchVsync(timeNs))
        testScheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.recentTicks.size)
        assertEquals(1L, viewModel.uiState.value.totalDispatchedFrames)

        // 4. Reset
        viewModel.onEvent(FpsContentEvent.Reset)
        testScheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.activeSubscriptionsCount)
        assertEquals(0, viewModel.uiState.value.recentTicks.size)

        viewModel.onCleared()
    }
}
