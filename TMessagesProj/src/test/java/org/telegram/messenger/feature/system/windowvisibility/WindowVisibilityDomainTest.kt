package org.telegram.messenger.feature.system.windowvisibility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.windowvisibility.data.mapper.WindowVisibilityMapper
import org.telegram.messenger.feature.system.windowvisibility.data.repository.LegacyWindowVisibilityRepository
import org.telegram.messenger.feature.system.windowvisibility.domain.model.WindowVisibilityState
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.CheckIsWindowVisibleUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.CreateVisibilityControllerUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.GetActiveHideReasonsUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.GetWindowVisibilityStateUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ObserveWindowVisibilityChangesUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ObserveWindowVisibilityStateUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ReleaseHideWindowUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.RequestHideWindowUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ResetWindowVisibilityUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ToggleWindowHideUseCase
import org.telegram.messenger.feature.system.windowvisibility.presentation.WindowVisibilityEvent
import org.telegram.messenger.feature.system.windowvisibility.presentation.WindowVisibilityViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class WindowVisibilityDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyWindowVisibilityRepository
    private lateinit var requestHideWindowUseCase: RequestHideWindowUseCase
    private lateinit var releaseHideWindowUseCase: ReleaseHideWindowUseCase
    private lateinit var toggleWindowHideUseCase: ToggleWindowHideUseCase
    private lateinit var checkIsWindowVisibleUseCase: CheckIsWindowVisibleUseCase
    private lateinit var getWindowVisibilityStateUseCase: GetWindowVisibilityStateUseCase
    private lateinit var getActiveHideReasonsUseCase: GetActiveHideReasonsUseCase
    private lateinit var resetWindowVisibilityUseCase: ResetWindowVisibilityUseCase
    private lateinit var observeWindowVisibilityStateUseCase: ObserveWindowVisibilityStateUseCase
    private lateinit var observeWindowVisibilityChangesUseCase: ObserveWindowVisibilityChangesUseCase
    private lateinit var createVisibilityControllerUseCase: CreateVisibilityControllerUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyWindowVisibilityRepository()
        requestHideWindowUseCase = RequestHideWindowUseCase(repository)
        releaseHideWindowUseCase = ReleaseHideWindowUseCase(repository)
        toggleWindowHideUseCase = ToggleWindowHideUseCase(repository)
        checkIsWindowVisibleUseCase = CheckIsWindowVisibleUseCase(repository)
        getWindowVisibilityStateUseCase = GetWindowVisibilityStateUseCase(repository)
        getActiveHideReasonsUseCase = GetActiveHideReasonsUseCase(repository)
        resetWindowVisibilityUseCase = ResetWindowVisibilityUseCase(repository)
        observeWindowVisibilityStateUseCase = ObserveWindowVisibilityStateUseCase(repository)
        observeWindowVisibilityChangesUseCase = ObserveWindowVisibilityChangesUseCase(repository)
        createVisibilityControllerUseCase = CreateVisibilityControllerUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMultipleReasonsCountAndVisibility() = runTest {
        assertTrue(checkIsWindowVisibleUseCase())
        assertEquals(0, repository.getReasonsCount())

        // Add first reason: story recorder
        val state1 = requestHideWindowUseCase("story_recorder", "Story recording active")
        assertFalse(state1.isVisible)
        assertTrue(state1.isHidden)
        assertEquals(1, state1.reasonsCount)
        assertTrue(state1.activeReasons.contains("story_recorder"))

        // Add second reason: photo viewer
        val state2 = requestHideWindowUseCase("photo_viewer", "Viewing full photo")
        assertFalse(state2.isVisible)
        assertEquals(2, state2.reasonsCount)
        assertTrue(state2.activeReasons.contains("photo_viewer"))
        assertTrue(state2.activeReasons.contains("story_recorder"))

        // Release first reason
        val state3 = releaseHideWindowUseCase("story_recorder")
        assertFalse(state3.isVisible)
        assertEquals(1, state3.reasonsCount)
        assertFalse(state3.activeReasons.contains("story_recorder"))
        assertTrue(state3.activeReasons.contains("photo_viewer"))

        // Release second reason
        val state4 = releaseHideWindowUseCase("photo_viewer")
        assertTrue(state4.isVisible)
        assertFalse(state4.isHidden)
        assertEquals(0, state4.reasonsCount)
        assertTrue(state4.activeReasons.isEmpty())
    }

    @Test
    fun testIdempotentReasonHandling() = runTest {
        // Request hide twice with the same reason tag
        requestHideWindowUseCase("overlay", "Debug overlay")
        val state = requestHideWindowUseCase("overlay", "Debug overlay duplicate")
        assertEquals(1, state.reasonsCount)
        assertEquals(setOf("overlay"), state.activeReasons)

        // Release once
        val stateAfterRelease = releaseHideWindowUseCase("overlay")
        assertEquals(0, stateAfterRelease.reasonsCount)
        assertTrue(stateAfterRelease.isVisible)

        // Release again - count shouldn't drop below 0
        val stateAfterDuplicateRelease = releaseHideWindowUseCase("overlay")
        assertEquals(0, stateAfterDuplicateRelease.reasonsCount)
        assertTrue(stateAfterDuplicateRelease.isVisible)
    }

    @Test
    fun testControllerLifecycleAndDestroy() = runTest {
        val controller = createVisibilityControllerUseCase("article_viewer")
        assertEquals("article_viewer", controller.reasonTag)
        assertFalse(controller.isHidden)
        assertFalse(controller.isDestroyed)

        controller.setHidden(true)
        assertTrue(controller.isHidden)
        assertTrue(repository.isHidden())
        assertEquals(1, repository.getReasonsCount())

        controller.setHidden(false)
        assertFalse(controller.isHidden)
        assertTrue(repository.isVisible())
        assertEquals(0, repository.getReasonsCount())

        // Re-hide then destroy
        controller.setHidden(true)
        assertTrue(repository.isHidden())

        controller.destroy()
        assertTrue(controller.isDestroyed)
        assertFalse(controller.isHidden)
        assertTrue(repository.isVisible())
        assertEquals(0, repository.getReasonsCount())

        // Calling setHidden after destroyed should have no effect
        controller.setHidden(true)
        assertFalse(controller.isHidden)
        assertTrue(repository.isVisible())
    }

    @Test
    fun testStateTransitionsAndMapper() {
        val stateVisible = WindowVisibilityMapper.toState(
            reasonsCount = 0,
            activeReasons = emptySet()
        )
        assertTrue(stateVisible.isVisible)
        assertFalse(WindowVisibilityMapper.toLegacyIsHidden(stateVisible))

        val stateHidden = WindowVisibilityMapper.toState(
            reasonsCount = 2,
            activeReasons = setOf("reason1", "reason2"),
            lastChangedReason = "reason2"
        )
        assertFalse(stateHidden.isVisible)
        assertTrue(WindowVisibilityMapper.toLegacyIsHidden(stateHidden))
        assertEquals("reason2", stateHidden.lastChangedReason)

        val changeResult = WindowVisibilityMapper.calculateChangeResult(stateVisible, stateHidden)
        assertTrue(changeResult.visibilityToggled)
        assertEquals(stateVisible, changeResult.previousState)
        assertEquals(stateHidden, changeResult.newState)

        val noToggleResult = WindowVisibilityMapper.calculateChangeResult(stateHidden, stateHidden.copy(reasonsCount = 3))
        assertFalse(noToggleResult.visibilityToggled)
    }

    @Test
    fun testResetAllReasons() = runTest {
        requestHideWindowUseCase("reasonA")
        requestHideWindowUseCase("reasonB")
        requestHideWindowUseCase("reasonC")
        assertEquals(3, repository.getReasonsCount())
        assertTrue(repository.isHidden())

        val resetState = resetWindowVisibilityUseCase()
        assertEquals(0, resetState.reasonsCount)
        assertTrue(resetState.isVisible)
        assertTrue(resetState.activeReasons.isEmpty())
        assertTrue(checkIsWindowVisibleUseCase())
    }

    @Test
    fun testWindowVisibilityViewModelFlow() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = WindowVisibilityViewModel(
            requestHideWindowUseCase = requestHideWindowUseCase,
            releaseHideWindowUseCase = releaseHideWindowUseCase,
            toggleWindowHideUseCase = toggleWindowHideUseCase,
            checkIsWindowVisibleUseCase = checkIsWindowVisibleUseCase,
            getWindowVisibilityStateUseCase = getWindowVisibilityStateUseCase,
            getActiveHideReasonsUseCase = getActiveHideReasonsUseCase,
            resetWindowVisibilityUseCase = resetWindowVisibilityUseCase,
            observeWindowVisibilityStateUseCase = observeWindowVisibilityStateUseCase
        )

        assertTrue(viewModel.uiState.value.isVisible)
        assertFalse(viewModel.uiState.value.isHidden)
        assertEquals(0, viewModel.uiState.value.reasonsCount)

        viewModel.onEvent(WindowVisibilityEvent.HideRequested("camera_preview"))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isVisible)
        assertTrue(viewModel.uiState.value.isHidden)
        assertEquals(1, viewModel.uiState.value.reasonsCount)
        assertTrue(viewModel.uiState.value.activeReasons.contains("camera_preview"))

        viewModel.onEvent(WindowVisibilityEvent.ToggleHide("gallery_picker", hide = true))
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.reasonsCount)

        viewModel.onEvent(WindowVisibilityEvent.ReleaseRequested("camera_preview"))
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.reasonsCount)

        viewModel.onEvent(WindowVisibilityEvent.ResetAll)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isVisible)
        assertEquals(0, viewModel.uiState.value.reasonsCount)
    }
}
