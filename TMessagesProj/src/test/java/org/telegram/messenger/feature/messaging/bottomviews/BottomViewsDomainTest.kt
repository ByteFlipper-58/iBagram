package org.telegram.messenger.feature.messaging.bottomviews

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
import org.telegram.messenger.feature.messaging.bottomviews.data.mapper.BottomViewsVisibilityMapper
import org.telegram.messenger.feature.messaging.bottomviews.data.repository.LegacyBottomViewsVisibilityRepository
import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomContainerType
import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomViewsVisibilityState
import org.telegram.messenger.feature.messaging.bottomviews.domain.repository.BottomViewsVisibilityRepository
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetBottomViewVisibilityUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetBottomViewsStateUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetPriorityBottomContainerUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.ObserveBottomViewsVisibilityUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.SetBottomViewVisibleUseCase
import org.telegram.messenger.feature.messaging.bottomviews.presentation.BottomViewsEvent
import org.telegram.messenger.feature.messaging.bottomviews.presentation.BottomViewsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class BottomViewsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: BottomViewsVisibilityRepository
    private lateinit var getBottomViewVisibilityUseCase: GetBottomViewVisibilityUseCase
    private lateinit var setBottomViewVisibleUseCase: SetBottomViewVisibleUseCase
    private lateinit var getPriorityBottomContainerUseCase: GetPriorityBottomContainerUseCase
    private lateinit var getBottomViewsStateUseCase: GetBottomViewsStateUseCase
    private lateinit var observeBottomViewsVisibilityUseCase: ObserveBottomViewsVisibilityUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyBottomViewsVisibilityRepository()
        getBottomViewVisibilityUseCase = GetBottomViewVisibilityUseCase(repository)
        setBottomViewVisibleUseCase = SetBottomViewVisibleUseCase(repository)
        getPriorityBottomContainerUseCase = GetPriorityBottomContainerUseCase(repository)
        getBottomViewsStateUseCase = GetBottomViewsStateUseCase(repository)
        observeBottomViewsVisibilityUseCase = ObserveBottomViewsVisibilityUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun container_type_resolution_and_constants() {
        assertEquals(BottomContainerType.DEFAULT, BottomContainerType.fromId(0))
        assertEquals(BottomContainerType.MESSAGE_INPUT, BottomContainerType.fromId(1))
        assertEquals(BottomContainerType.BOTTOM_OVERLAY_TEXT, BottomContainerType.fromId(2))
        assertEquals(BottomContainerType.BOTTOM_OVERLAY_CHAT, BottomContainerType.fromId(3))
        assertEquals(BottomContainerType.MESSAGE_SEARCH, BottomContainerType.fromId(4))
        assertEquals(BottomContainerType.MESSAGE_ACTION, BottomContainerType.fromId(5))
        assertEquals(null, BottomContainerType.fromId(99))
    }

    @Test
    fun priority_calculation_highest_bit_wins() {
        // Flag 1 (bit 0) -> priority 0
        assertEquals(0, BottomViewsVisibilityMapper.calculatePriorityContainerId(1))

        // Bits 0 and 1 -> flag 3 -> priority 1
        assertEquals(1, BottomViewsVisibilityMapper.calculatePriorityContainerId(3))

        // Bits 0, 1, 4 -> flag 1 | 2 | 16 = 19 -> priority 4
        assertEquals(4, BottomViewsVisibilityMapper.calculatePriorityContainerId(19))

        // Bits 0, 1, 4, 5 -> flag 1 | 2 | 16 | 32 = 51 -> priority 5
        assertEquals(5, BottomViewsVisibilityMapper.calculatePriorityContainerId(51))
    }

    @Test
    fun visibility_state_predicates() {
        val state = BottomViewsVisibilityState(
            visibilityFlags = (1 shl BottomContainerType.MESSAGE_INPUT.id) or (1 shl BottomContainerType.MESSAGE_ACTION.id),
            priorityContainerId = BottomContainerType.MESSAGE_ACTION.id,
            visibilities = mapOf(
                BottomContainerType.MESSAGE_ACTION.id to 1.0f,
                BottomContainerType.MESSAGE_INPUT.id to 0.0f
            )
        )

        assertTrue(state.isInputVisible)
        assertTrue(state.isActionVisible)
        assertFalse(state.isSearchVisible)
        assertEquals(1.0f, state.getVisibility(BottomContainerType.MESSAGE_ACTION.id), 0.001f)
        assertEquals(0.0f, state.getVisibility(BottomContainerType.MESSAGE_INPUT.id), 0.001f)
        assertEquals(0.0f, state.getVisibility(BottomContainerType.MESSAGE_SEARCH.id), 0.001f)
    }

    @Test
    fun repository_visibility_transitions_and_priority_arbitration() {
        // Initial state: default container 0 is active
        assertEquals(0, getPriorityBottomContainerUseCase())
        assertEquals(1.0f, getBottomViewVisibilityUseCase(0), 0.001f)

        // Show input view (container 1)
        setBottomViewVisibleUseCase(containerId = BottomContainerType.MESSAGE_INPUT.id, isVisible = true)
        assertEquals(BottomContainerType.MESSAGE_INPUT.id, getPriorityBottomContainerUseCase())
        assertEquals(1.0f, getBottomViewVisibilityUseCase(BottomContainerType.MESSAGE_INPUT.id), 0.001f)
        assertEquals(0.0f, getBottomViewVisibilityUseCase(0), 0.001f)

        // Show action view (container 5, higher priority)
        setBottomViewVisibleUseCase(containerId = BottomContainerType.MESSAGE_ACTION.id, isVisible = true)
        assertEquals(BottomContainerType.MESSAGE_ACTION.id, getPriorityBottomContainerUseCase())
        assertEquals(1.0f, getBottomViewVisibilityUseCase(BottomContainerType.MESSAGE_ACTION.id), 0.001f)
        assertEquals(0.0f, getBottomViewVisibilityUseCase(BottomContainerType.MESSAGE_INPUT.id), 0.001f)

        // Hide action view -> priority falls back to input view
        setBottomViewVisibleUseCase(containerId = BottomContainerType.MESSAGE_ACTION.id, isVisible = false)
        assertEquals(BottomContainerType.MESSAGE_INPUT.id, getPriorityBottomContainerUseCase())
        assertEquals(1.0f, getBottomViewVisibilityUseCase(BottomContainerType.MESSAGE_INPUT.id), 0.001f)

        // Hide input view -> priority falls back to default container 0
        setBottomViewVisibleUseCase(containerId = BottomContainerType.MESSAGE_INPUT.id, isVisible = false)
        assertEquals(0, getPriorityBottomContainerUseCase())
        assertEquals(1.0f, getBottomViewVisibilityUseCase(0), 0.001f)
    }

    @Test
    fun reactive_state_observation() = runTest {
        setBottomViewVisibleUseCase(containerId = BottomContainerType.MESSAGE_SEARCH.id, isVisible = true)

        val state = observeBottomViewsVisibilityUseCase().first()
        assertEquals(BottomContainerType.MESSAGE_SEARCH.id, state.priorityContainerId)
        assertTrue(state.isSearchVisible)
    }

    @Test
    fun view_model_events_and_state() = runTest {
        val viewModel = BottomViewsViewModel(
            observeBottomViewsVisibilityUseCase = observeBottomViewsVisibilityUseCase,
            getBottomViewsStateUseCase = getBottomViewsStateUseCase,
            setBottomViewVisibleUseCase = setBottomViewVisibleUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(BottomContainerType.DEFAULT, viewModel.uiState.value.priorityContainerType)

        // Show Message Action bar
        viewModel.onEvent(
            BottomViewsEvent.SetViewVisible(
                containerId = BottomContainerType.MESSAGE_ACTION.id,
                isVisible = true
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(BottomContainerType.MESSAGE_ACTION, state.priorityContainerType)
        assertTrue(state.state.isActionVisible)
        assertEquals(1.0f, state.state.getVisibility(BottomContainerType.MESSAGE_ACTION.id), 0.001f)

        // Reset to default
        viewModel.onEvent(BottomViewsEvent.ResetToDefault)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(BottomContainerType.DEFAULT, state.priorityContainerType)
        assertFalse(state.state.isActionVisible)
        assertEquals(1.0f, state.state.getVisibility(0), 0.001f)
    }
}
