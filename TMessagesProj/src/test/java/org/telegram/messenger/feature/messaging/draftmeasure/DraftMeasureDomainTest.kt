package org.telegram.messenger.feature.messaging.draftmeasure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.draftmeasure.data.repository.LegacyDraftMeasureRepository
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureTarget
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureViewport
import org.telegram.messenger.feature.messaging.draftmeasure.domain.repository.DraftMeasureRepository
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.CalculateDraftMeasureOverrideUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.GetDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ObserveDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.OnDraftMessageIdChangedUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ResetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetPreviousMessageHeightUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.presentation.DraftMeasureEvent
import org.telegram.messenger.feature.messaging.draftmeasure.presentation.DraftMeasureViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class DraftMeasureDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DraftMeasureRepository
    private lateinit var calculateOverrideUseCase: CalculateDraftMeasureOverrideUseCase
    private lateinit var setTargetUseCase: SetDraftMeasureTargetUseCase
    private lateinit var onMessageIdChangedUseCase: OnDraftMessageIdChangedUseCase
    private lateinit var setPreviousHeightUseCase: SetPreviousMessageHeightUseCase
    private lateinit var resetTargetUseCase: ResetDraftMeasureTargetUseCase
    private lateinit var observeConfigUseCase: ObserveDraftMeasureConfigUseCase
    private lateinit var getConfigUseCase: GetDraftMeasureConfigUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyDraftMeasureRepository()
        calculateOverrideUseCase = CalculateDraftMeasureOverrideUseCase(repository)
        setTargetUseCase = SetDraftMeasureTargetUseCase(repository)
        onMessageIdChangedUseCase = OnDraftMessageIdChangedUseCase(repository)
        setPreviousHeightUseCase = SetPreviousMessageHeightUseCase(repository)
        resetTargetUseCase = ResetDraftMeasureTargetUseCase(repository)
        observeConfigUseCase = ObserveDraftMeasureConfigUseCase(repository)
        getConfigUseCase = GetDraftMeasureConfigUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun target_matching_and_active_logic() {
        val emptyTarget = DraftMeasureTarget.EMPTY
        assertFalse(emptyTarget.isActive)
        assertFalse(emptyTarget.matches(100, 0L))

        val singleTarget = DraftMeasureTarget(messageId = 42, groupId = 0L)
        assertTrue(singleTarget.isActive)
        assertTrue(singleTarget.matches(42, 0L))
        assertFalse(singleTarget.matches(99, 0L))

        val groupTarget = DraftMeasureTarget(messageId = 0, groupId = 9999L)
        assertTrue(groupTarget.isActive)
        assertTrue(groupTarget.matches(1, 9999L))
        assertFalse(groupTarget.matches(1, 8888L))
    }

    @Test
    fun viewport_available_height_calculation() {
        val viewport = DraftMeasureViewport(
            totalHeight = 1000,
            paddingTop = 50,
            paddingBottom = 50,
            previousMessageHeight = 200
        )
        // 1000 - 50 - 50 - 200 = 700
        assertEquals(700, viewport.availableHeight)

        val smallViewport = DraftMeasureViewport(
            totalHeight = 100,
            paddingTop = 60,
            paddingBottom = 60,
            previousMessageHeight = 0
        )
        assertEquals(0, smallViewport.availableHeight)
    }

    @Test
    fun calculate_override_with_additional_height() = runTest {
        setTargetUseCase(messageId = 77, groupId = 0L)
        setPreviousHeightUseCase(100)

        val viewport = DraftMeasureViewport(
            totalHeight = 800,
            paddingTop = 50,
            paddingBottom = 50,
            previousMessageHeight = 100
        )
        // availHeight = 800 - 50 - 50 - 100 = 600
        // measuredHeight = 200 => additionalHeight = 400 => finalHeight = 600
        val result = calculateOverrideUseCase(
            messageId = 77,
            groupId = 0L,
            measuredHeight = 200,
            viewport = viewport
        )

        assertEquals(200, result.measuredHeight)
        assertEquals(400, result.additionalHeight)
        assertEquals(600, result.finalHeight)
        assertTrue(result.hasAdditionalHeight)
        assertTrue(repository.hasAdditionalHeight())
    }

    @Test
    fun calculate_override_without_additional_height_resets_target() = runTest {
        setTargetUseCase(messageId = 77, groupId = 0L)
        setPreviousHeightUseCase(100)

        val viewport = DraftMeasureViewport(
            totalHeight = 500,
            paddingTop = 50,
            paddingBottom = 50,
            previousMessageHeight = 100
        )
        // availHeight = 500 - 50 - 50 - 100 = 300
        // measuredHeight = 400 => additionalHeight = 0
        val result = calculateOverrideUseCase(
            messageId = 77,
            groupId = 0L,
            measuredHeight = 400,
            viewport = viewport
        )

        assertEquals(400, result.measuredHeight)
        assertEquals(0, result.additionalHeight)
        assertEquals(400, result.finalHeight)
        assertFalse(result.hasAdditionalHeight)
        assertFalse(repository.hasAdditionalHeight())
        // Target was reset to EMPTY because additionalHeight == 0
        assertEquals(DraftMeasureTarget.EMPTY, repository.getTarget())
    }

    @Test
    fun calculate_override_for_non_matching_message_returns_original_height() = runTest {
        setTargetUseCase(messageId = 77, groupId = 0L)

        val viewport = DraftMeasureViewport(totalHeight = 1000)
        val result = calculateOverrideUseCase(
            messageId = 999,
            groupId = 0L,
            measuredHeight = 300,
            viewport = viewport
        )

        assertEquals(300, result.measuredHeight)
        assertEquals(0, result.additionalHeight)
        assertEquals(300, result.finalHeight)
    }

    @Test
    fun on_message_id_changed_updates_target() = runTest {
        setTargetUseCase(messageId = 10, groupId = 555L)

        val wrongOldChanged = onMessageIdChangedUseCase(oldMessageId = 99, newMessageId = 20, groupId = 555L)
        assertFalse(wrongOldChanged)
        assertEquals(10, repository.getTarget().messageId)

        val correctChanged = onMessageIdChangedUseCase(oldMessageId = 10, newMessageId = 20, groupId = 555L)
        assertTrue(correctChanged)
        assertEquals(20, repository.getTarget().messageId)
        assertEquals(555L, repository.getTarget().groupId)
    }

    @Test
    fun reset_target_clears_target_and_additional_height() = runTest {
        setTargetUseCase(messageId = 15, groupId = 0L)
        assertEquals(15, repository.getTarget().messageId)

        val resetResult = resetTargetUseCase()
        assertTrue(resetResult)
        assertEquals(DraftMeasureTarget.EMPTY, repository.getTarget())
        assertFalse(repository.hasAdditionalHeight())
    }

    @Test
    fun reactive_config_observation() = runTest {
        setPreviousHeightUseCase(120)
        setTargetUseCase(messageId = 33, groupId = 777L)

        val config = observeConfigUseCase().first()
        assertEquals(33, config.target.messageId)
        assertEquals(777L, config.target.groupId)
        assertEquals(120, config.previousMessageHeight)
    }

    @Test
    fun view_model_events_and_state() = runTest {
        val viewModel = DraftMeasureViewModel(
            calculateOverrideUseCase = calculateOverrideUseCase,
            setTargetUseCase = setTargetUseCase,
            onMessageIdChangedUseCase = onMessageIdChangedUseCase,
            setPreviousHeightUseCase = setPreviousHeightUseCase,
            resetTargetUseCase = resetTargetUseCase,
            observeConfigUseCase = observeConfigUseCase,
            getConfigUseCase = getConfigUseCase
        )

        viewModel.onEvent(DraftMeasureEvent.SetPreviousHeight(80))
        viewModel.onEvent(DraftMeasureEvent.SetTarget(messageId = 50, groupId = 100L))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(50, state.target.messageId)
        assertEquals(100L, state.target.groupId)
        assertEquals(80, state.previousMessageHeight)

        val viewport = DraftMeasureViewport(totalHeight = 900, paddingTop = 20, paddingBottom = 20, previousMessageHeight = 80)
        viewModel.onEvent(
            DraftMeasureEvent.CalculateOverride(
                messageId = 50,
                groupId = 100L,
                measuredHeight = 300,
                viewport = viewport
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertNotNull(state.lastResult)
        assertEquals(300, state.lastResult?.measuredHeight)
        // 900 - 20 - 20 - 80 = 780 availHeight => additional = 480 => final = 780
        assertEquals(480, state.lastResult?.additionalHeight)
        assertEquals(780, state.lastResult?.finalHeight)
        assertTrue(state.hasAdditionalHeight)

        viewModel.onEvent(DraftMeasureEvent.ResetTarget)
        testDispatcher.scheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(DraftMeasureTarget.EMPTY, state.target)
    }
}
