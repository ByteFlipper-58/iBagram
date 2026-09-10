package org.telegram.messenger.feature.system.recyclerscroll

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.recyclerscroll.data.mapper.RecyclerScrollMapper
import org.telegram.messenger.feature.system.recyclerscroll.data.repository.LegacyRecyclerScrollRepository
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.CalculateScrollAnimationPlanUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.CalculateScrollLengthUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.CancelRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.ComputeScrollViewTranslationsUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.EvaluateScrollEligibilityUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.FinishRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.GetRecyclerScrollStateUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.ObserveRecyclerScrollStateUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.ResetRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.StartRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.UpdateRecyclerScrollProgressUseCase
import org.telegram.messenger.feature.system.recyclerscroll.presentation.RecyclerScrollEvent
import org.telegram.messenger.feature.system.recyclerscroll.presentation.RecyclerScrollViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class RecyclerScrollDomainTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LegacyRecyclerScrollRepository

    private lateinit var observeScrollStateUseCase: ObserveRecyclerScrollStateUseCase
    private lateinit var getScrollStateUseCase: GetRecyclerScrollStateUseCase
    private lateinit var evaluateScrollEligibilityUseCase: EvaluateScrollEligibilityUseCase
    private lateinit var calculateScrollAnimationPlanUseCase: CalculateScrollAnimationPlanUseCase
    private lateinit var calculateScrollLengthUseCase: CalculateScrollLengthUseCase
    private lateinit var computeScrollViewTranslationsUseCase: ComputeScrollViewTranslationsUseCase
    private lateinit var startRecyclerScrollUseCase: StartRecyclerScrollUseCase
    private lateinit var updateRecyclerScrollProgressUseCase: UpdateRecyclerScrollProgressUseCase
    private lateinit var finishRecyclerScrollUseCase: FinishRecyclerScrollUseCase
    private lateinit var cancelRecyclerScrollUseCase: CancelRecyclerScrollUseCase
    private lateinit var resetRecyclerScrollUseCase: ResetRecyclerScrollUseCase

    private lateinit var viewModel: RecyclerScrollViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyRecyclerScrollRepository()

        observeScrollStateUseCase = ObserveRecyclerScrollStateUseCase(repository)
        getScrollStateUseCase = GetRecyclerScrollStateUseCase(repository)
        evaluateScrollEligibilityUseCase = EvaluateScrollEligibilityUseCase(repository)
        calculateScrollAnimationPlanUseCase = CalculateScrollAnimationPlanUseCase(repository)
        calculateScrollLengthUseCase = CalculateScrollLengthUseCase(repository)
        computeScrollViewTranslationsUseCase = ComputeScrollViewTranslationsUseCase(repository)
        startRecyclerScrollUseCase = StartRecyclerScrollUseCase(repository)
        updateRecyclerScrollProgressUseCase = UpdateRecyclerScrollProgressUseCase(repository)
        finishRecyclerScrollUseCase = FinishRecyclerScrollUseCase(repository)
        cancelRecyclerScrollUseCase = CancelRecyclerScrollUseCase(repository)
        resetRecyclerScrollUseCase = ResetRecyclerScrollUseCase(repository)

        viewModel = RecyclerScrollViewModel(
            observeScrollStateUseCase = observeScrollStateUseCase,
            getScrollStateUseCase = getScrollStateUseCase,
            evaluateScrollEligibilityUseCase = evaluateScrollEligibilityUseCase,
            calculateScrollAnimationPlanUseCase = calculateScrollAnimationPlanUseCase,
            calculateScrollLengthUseCase = calculateScrollLengthUseCase,
            computeScrollViewTranslationsUseCase = computeScrollViewTranslationsUseCase,
            startRecyclerScrollUseCase = startRecyclerScrollUseCase,
            updateRecyclerScrollProgressUseCase = updateRecyclerScrollProgressUseCase,
            finishRecyclerScrollUseCase = finishRecyclerScrollUseCase,
            cancelRecyclerScrollUseCase = cancelRecyclerScrollUseCase,
            resetRecyclerScrollUseCase = resetRecyclerScrollUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mapper_evaluateEligibility_shouldCheckPreconditions() {
        // Fast scroll running -> cannot animate, no instant fallback
        val r1 = RecyclerScrollMapper.evaluateEligibility(
            fastScrollRunning = true,
            itemAnimatorRunning = false,
            childCount = 10,
            viewAnimationsEnabled = true,
            smooth = true,
            direction = ScrollDirection.DOWN
        )
        assertFalse(r1.canAnimate)
        assertFalse(r1.fallbackToInstant)

        // Item animator running -> cannot animate, no instant fallback
        val r2 = RecyclerScrollMapper.evaluateEligibility(
            fastScrollRunning = false,
            itemAnimatorRunning = true,
            childCount = 10,
            viewAnimationsEnabled = true,
            smooth = true,
            direction = ScrollDirection.DOWN
        )
        assertFalse(r2.canAnimate)
        assertFalse(r2.fallbackToInstant)

        // Smooth is false -> fallback to instant
        val r3 = RecyclerScrollMapper.evaluateEligibility(
            fastScrollRunning = false,
            itemAnimatorRunning = false,
            childCount = 10,
            viewAnimationsEnabled = true,
            smooth = false,
            direction = ScrollDirection.DOWN
        )
        assertFalse(r3.canAnimate)
        assertTrue(r3.fallbackToInstant)

        // Direction unset -> fallback to instant
        val r4 = RecyclerScrollMapper.evaluateEligibility(
            fastScrollRunning = false,
            itemAnimatorRunning = false,
            childCount = 10,
            viewAnimationsEnabled = true,
            smooth = true,
            direction = ScrollDirection.UNSET
        )
        assertFalse(r4.canAnimate)
        assertTrue(r4.fallbackToInstant)

        // All conditions satisfied -> can animate!
        val r5 = RecyclerScrollMapper.evaluateEligibility(
            fastScrollRunning = false,
            itemAnimatorRunning = false,
            childCount = 10,
            viewAnimationsEnabled = true,
            smooth = true,
            direction = ScrollDirection.DOWN
        )
        assertTrue(r5.canAnimate)
        assertFalse(r5.fallbackToInstant)
    }

    @Test
    fun mapper_calculatePlan_shouldComputeDurationsAccurately() {
        // Dialogs with same views -> 150ms
        val planDialogsSame = RecyclerScrollMapper.calculatePlan(
            ScrollAnimationSpec(
                scrollDirection = ScrollDirection.DOWN,
                isDialogs = true,
                hasSameViews = true,
                scrollLength = 500,
                containerHeight = 1000
            )
        )
        assertEquals(150L, planDialogsSame.durationMs)
        assertTrue(planDialogsSame.isScrollDown)

        // Chat with same views -> 600ms
        val planChatSame = RecyclerScrollMapper.calculatePlan(
            ScrollAnimationSpec(
                scrollDirection = ScrollDirection.UP,
                isDialogs = false,
                hasSameViews = true,
                scrollLength = 500,
                containerHeight = 1000
            )
        )
        assertEquals(600L, planChatSame.durationMs)
        assertFalse(planChatSame.isScrollDown)

        // Different views: duration = (((500 / 1000) + 1) * 200) = 1.5 * 200 = 300ms
        val planDynamic = RecyclerScrollMapper.calculatePlan(
            ScrollAnimationSpec(
                scrollDirection = ScrollDirection.DOWN,
                isDialogs = false,
                hasSameViews = false,
                scrollLength = 500,
                containerHeight = 1000
            )
        )
        assertEquals(300L, planDynamic.durationMs)

        // Very large scroll length clamped to 1300ms
        val planLarge = RecyclerScrollMapper.calculatePlan(
            ScrollAnimationSpec(
                scrollDirection = ScrollDirection.DOWN,
                isDialogs = false,
                hasSameViews = false,
                scrollLength = 20000,
                containerHeight = 1000
            )
        )
        assertEquals(1300L, planLarge.durationMs)
    }

    @Test
    fun mapper_calculateScrollLength_shouldHandleBothDirections() {
        // Empty old views -> abs(scrollDiff)
        val lenEmpty = RecyclerScrollMapper.calculateScrollLength(
            scrollDown = true,
            containerHeight = 1000,
            oldViewsCount = 0,
            scrollDiff = -250,
            oldTop = 0,
            oldBottom = 1000,
            incomingTop = 0,
            incomingBottom = 1000
        )
        assertEquals(250, lenEmpty)

        // Scroll Down with old views: oldBottom = 800, incomingTop = -200 -> 800 + (-(-200)) = 1000
        val lenDown = RecyclerScrollMapper.calculateScrollLength(
            scrollDown = true,
            containerHeight = 1000,
            oldViewsCount = 5,
            scrollDiff = 0,
            oldTop = 0,
            oldBottom = 800,
            incomingTop = -200,
            incomingBottom = 800
        )
        assertEquals(1000, lenDown)

        // Scroll Up with old views: height = 1000, oldTop = 200, incomingBottom = 1200
        // finalHeight = 1000 - 200 = 800, offset = 1200 - 1000 = 200 -> 800 + 200 = 1000
        val lenUp = RecyclerScrollMapper.calculateScrollLength(
            scrollDown = false,
            containerHeight = 1000,
            oldViewsCount = 5,
            scrollDiff = 0,
            oldTop = 200,
            oldBottom = 1000,
            incomingTop = 200,
            incomingBottom = 1200
        )
        assertEquals(1000, lenUp)
    }

    @Test
    fun mapper_computeViewTranslations_shouldInterpolateBasedOnDirectionAndProgress() {
        // Scroll DOWN: progress 0.25, length 400
        // oldViewsTranslation = -400 * 0.25 = -100
        // incomingViewsTranslation = 400 * (1 - 0.25) + 10 = 310
        val transDown = RecyclerScrollMapper.computeViewTranslations(
            scrollLength = 400,
            isScrollDown = true,
            progress = 0.25f,
            additionalY = 10
        )
        assertEquals(-100f, transDown.oldViewsTranslationY, 0.01f)
        assertEquals(310f, transDown.incomingViewsTranslationY, 0.01f)

        // Scroll UP: progress 0.5, length 600
        // oldViewsTranslation = 600 * 0.5 = 300
        // incomingViewsTranslation = -600 * (1 - 0.5) = -300
        val transUp = RecyclerScrollMapper.computeViewTranslations(
            scrollLength = 600,
            isScrollDown = false,
            progress = 0.5f
        )
        assertEquals(300f, transUp.oldViewsTranslationY, 0.01f)
        assertEquals(-300f, transUp.incomingViewsTranslationY, 0.01f)
    }

    @Test
    fun repository_lifecycle_shouldUpdateStateAccurately() {
        var state = repository.getState()
        assertFalse(state.isRunning)
        assertEquals(ScrollDirection.UNSET, state.direction)

        repository.startScroll(position = 42, offset = 15, direction = ScrollDirection.DOWN, durationMs = 500L)
        state = repository.getState()
        assertTrue(state.isRunning)
        assertEquals(42, state.targetPosition)
        assertEquals(15, state.targetOffset)
        assertEquals(ScrollDirection.DOWN, state.direction)
        assertEquals(500L, state.durationMs)
        assertEquals(0f, state.progress, 0.01f)

        repository.updateProgress(0.6f)
        state = repository.getState()
        assertEquals(0.6f, state.progress, 0.01f)

        repository.finishScroll()
        state = repository.getState()
        assertFalse(state.isRunning)
        assertEquals(1.0f, state.progress, 0.01f)

        repository.cancelScroll()
        state = repository.getState()
        assertFalse(state.isRunning)
        assertEquals(0f, state.progress, 0.01f)

        repository.reset()
        state = repository.getState()
        assertEquals(-1, state.targetPosition)
    }

    @Test
    fun viewModel_shouldHandleMviEventsAndComputeTranslations() = runTest {
        val spec = ScrollAnimationSpec(
            scrollDirection = ScrollDirection.DOWN,
            isDialogs = true,
            hasSameViews = true,
            scrollLength = 600,
            containerHeight = 1200
        )

        viewModel.onEvent(
            RecyclerScrollEvent.PrepareAndStartScroll(
                position = 10,
                offset = 0,
                spec = spec
            )
        )
        advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertTrue(uiState.scrollState.isRunning)
        assertEquals(10, uiState.scrollState.targetPosition)
        assertNotNull(uiState.currentPlan)
        assertEquals(150L, uiState.currentPlan?.durationMs)

        // Progress update -> computes translations
        viewModel.onEvent(RecyclerScrollEvent.UpdateProgress(0.5f))
        advanceUntilIdle()

        uiState = viewModel.uiState.value
        assertEquals(0.5f, uiState.scrollState.progress, 0.01f)
        assertNotNull(uiState.currentTranslation)
        assertEquals(-300f, uiState.currentTranslation?.oldViewsTranslationY ?: 0f, 0.01f)
        assertEquals(300f, uiState.currentTranslation?.incomingViewsTranslationY ?: 0f, 0.01f)

        // Finish scroll
        viewModel.onEvent(RecyclerScrollEvent.FinishScroll)
        advanceUntilIdle()

        uiState = viewModel.uiState.value
        assertFalse(uiState.scrollState.isRunning)
        assertEquals(1.0f, uiState.scrollState.progress, 0.01f)
        assertNull(uiState.currentTranslation)

        // Reset
        viewModel.onEvent(RecyclerScrollEvent.Reset)
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertNull(uiState.currentPlan)
    }
}
