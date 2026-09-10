package org.telegram.messenger.feature.system.floatingdebug

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.floatingdebug.data.mapper.FloatingDebugMapper
import org.telegram.messenger.feature.system.floatingdebug.data.repository.LegacyFloatingDebugRepository
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemKind
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ClearFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.GetFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.GetFloatingDebugStateUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.IsFloatingDebugActiveUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ObserveFloatingDebugStateUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.RegisterFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.SetFloatingDebugActiveUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ToggleFloatingDebugActiveUseCase
import org.telegram.messenger.feature.system.floatingdebug.presentation.FloatingDebugEvent
import org.telegram.messenger.feature.system.floatingdebug.presentation.FloatingDebugViewModel
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController

@OptIn(ExperimentalCoroutinesApi::class)
class FloatingDebugDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FloatingDebugRepository
    private lateinit var isFloatingDebugActiveUseCase: IsFloatingDebugActiveUseCase
    private lateinit var setFloatingDebugActiveUseCase: SetFloatingDebugActiveUseCase
    private lateinit var toggleFloatingDebugActiveUseCase: ToggleFloatingDebugActiveUseCase
    private lateinit var getFloatingDebugItemsUseCase: GetFloatingDebugItemsUseCase
    private lateinit var registerFloatingDebugItemsUseCase: RegisterFloatingDebugItemsUseCase
    private lateinit var clearFloatingDebugItemsUseCase: ClearFloatingDebugItemsUseCase
    private lateinit var observeFloatingDebugStateUseCase: ObserveFloatingDebugStateUseCase
    private lateinit var getFloatingDebugStateUseCase: GetFloatingDebugStateUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyFloatingDebugRepository()
        isFloatingDebugActiveUseCase = IsFloatingDebugActiveUseCase(repository)
        setFloatingDebugActiveUseCase = SetFloatingDebugActiveUseCase(repository)
        toggleFloatingDebugActiveUseCase = ToggleFloatingDebugActiveUseCase(repository)
        getFloatingDebugItemsUseCase = GetFloatingDebugItemsUseCase(repository)
        registerFloatingDebugItemsUseCase = RegisterFloatingDebugItemsUseCase(repository)
        clearFloatingDebugItemsUseCase = ClearFloatingDebugItemsUseCase(repository)
        observeFloatingDebugStateUseCase = ObserveFloatingDebugStateUseCase(repository)
        getFloatingDebugStateUseCase = GetFloatingDebugStateUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mapper_item_types() {
        assertEquals(DebugItemKind.SIMPLE, FloatingDebugMapper.toDomainKind(FloatingDebugController.DebugItemType.SIMPLE))
        assertEquals(DebugItemKind.HEADER, FloatingDebugMapper.toDomainKind(FloatingDebugController.DebugItemType.HEADER))
        assertEquals(DebugItemKind.SEEKBAR, FloatingDebugMapper.toDomainKind(FloatingDebugController.DebugItemType.SEEKBAR))
    }

    @Test
    fun active_toggle_and_state_transitions() {
        assertFalse(isFloatingDebugActiveUseCase())

        setFloatingDebugActiveUseCase(true)
        assertTrue(isFloatingDebugActiveUseCase())
        assertTrue(getFloatingDebugStateUseCase().isActive)

        val toggledOff = toggleFloatingDebugActiveUseCase()
        assertFalse(toggledOff)
        assertFalse(isFloatingDebugActiveUseCase())

        val toggledOn = toggleFloatingDebugActiveUseCase()
        assertTrue(toggledOn)
        assertTrue(isFloatingDebugActiveUseCase())
    }

    @Test
    fun items_registration_execution_and_clearing() {
        var actionExecuted = false
        val items = listOf(
            DebugItemModel(title = "Header 1", kind = DebugItemKind.HEADER),
            DebugItemModel(title = "Test Action", kind = DebugItemKind.SIMPLE, action = { actionExecuted = true }),
            DebugItemModel(title = "Alpha", kind = DebugItemKind.SEEKBAR, from = 0f, to = 1f, currentValue = 0.5f)
        )

        registerFloatingDebugItemsUseCase(items)
        val registered = getFloatingDebugItemsUseCase()
        assertEquals(3, registered.size)
        assertEquals("Header 1", registered[0].title)
        assertEquals(DebugItemKind.HEADER, registered[0].kind)

        registered[1].action?.invoke()
        assertTrue(actionExecuted)

        clearFloatingDebugItemsUseCase()
        assertEquals(0, getFloatingDebugItemsUseCase().size)
    }

    @Test
    fun reactive_state_observation() = runTest {
        setFloatingDebugActiveUseCase(true)
        registerFloatingDebugItemsUseCase(listOf(DebugItemModel("Item", DebugItemKind.SIMPLE)))

        val state = observeFloatingDebugStateUseCase().first()
        assertTrue(state.isActive)
        assertEquals(1, state.itemsCount)
    }

    @Test
    fun view_model_events_and_state() = runTest {
        val viewModel = FloatingDebugViewModel(
            observeFloatingDebugStateUseCase = observeFloatingDebugStateUseCase,
            getFloatingDebugStateUseCase = getFloatingDebugStateUseCase,
            setFloatingDebugActiveUseCase = setFloatingDebugActiveUseCase,
            toggleFloatingDebugActiveUseCase = toggleFloatingDebugActiveUseCase,
            registerFloatingDebugItemsUseCase = registerFloatingDebugItemsUseCase,
            clearFloatingDebugItemsUseCase = clearFloatingDebugItemsUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.state.isActive)
        assertFalse(viewModel.uiState.value.isMenuOpen)

        // Toggle active
        viewModel.onEvent(FloatingDebugEvent.ToggleActive(saveConfig = false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.state.isActive)

        // Register items
        viewModel.onEvent(
            FloatingDebugEvent.RegisterItems(
                listOf(DebugItemModel("Debug Setting", DebugItemKind.SIMPLE))
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.state.itemsCount)

        // Open menu
        viewModel.onEvent(FloatingDebugEvent.SetMenuOpen(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMenuOpen)

        // Clear items
        viewModel.onEvent(FloatingDebugEvent.ClearItems)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.state.itemsCount)
    }
}
