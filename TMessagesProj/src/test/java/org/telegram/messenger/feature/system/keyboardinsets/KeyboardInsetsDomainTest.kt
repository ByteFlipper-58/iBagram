package org.telegram.messenger.feature.system.keyboardinsets

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
import org.telegram.messenger.feature.system.keyboardinsets.data.repository.LegacyKeyboardInsetsRepository
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.InAppImeMode
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardInsetsModel
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardVisibilityState
import org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.GetKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ObserveKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightWithNavbarUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ResetInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.UpdateSystemInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.presentation.KeyboardInsetsEvent
import org.telegram.messenger.feature.system.keyboardinsets.presentation.KeyboardInsetsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class KeyboardInsetsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: KeyboardInsetsRepository
    private lateinit var requestInAppKeyboardHeightUseCase: RequestInAppKeyboardHeightUseCase
    private lateinit var resetInAppKeyboardHeightUseCase: ResetInAppKeyboardHeightUseCase
    private lateinit var requestInAppKeyboardHeightWithNavbarUseCase: RequestInAppKeyboardHeightWithNavbarUseCase
    private lateinit var updateSystemInsetsUseCase: UpdateSystemInsetsUseCase
    private lateinit var getKeyboardInsetsUseCase: GetKeyboardInsetsUseCase
    private lateinit var observeKeyboardInsetsUseCase: ObserveKeyboardInsetsUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyKeyboardInsetsRepository()
        requestInAppKeyboardHeightUseCase = RequestInAppKeyboardHeightUseCase(repository)
        resetInAppKeyboardHeightUseCase = ResetInAppKeyboardHeightUseCase(repository)
        requestInAppKeyboardHeightWithNavbarUseCase = RequestInAppKeyboardHeightWithNavbarUseCase(repository)
        updateSystemInsetsUseCase = UpdateSystemInsetsUseCase(repository)
        getKeyboardInsetsUseCase = GetKeyboardInsetsUseCase(repository)
        observeKeyboardInsetsUseCase = ObserveKeyboardInsetsUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun model_effective_bottom_inset_and_visibility() {
        val model = KeyboardInsetsModel(
            inAppKeyboardHeight = 250,
            navigationBarHeight = 48,
            systemBarsTop = 24,
            systemBarsBottom = 48,
            imeBottom = 300,
            keyboardVisibility = 1.0f,
            keyboardState = KeyboardVisibilityState.FULLY_VISIBLE
        )

        assertEquals(300, model.effectiveBottomInset)
        assertTrue(model.isKeyboardVisible)

        val inAppOnly = KeyboardInsetsModel(
            inAppKeyboardHeight = 280,
            systemBarsBottom = 50,
            imeBottom = 0
        )
        assertEquals(280, inAppOnly.effectiveBottomInset)
        assertFalse(inAppOnly.isKeyboardVisible)
    }

    @Test
    fun ime_mode_codes() {
        assertEquals(InAppImeMode.HIDDEN, InAppImeMode.fromCode(0))
        assertEquals(InAppImeMode.VISIBLE, InAppImeMode.fromCode(1))
        assertEquals(InAppImeMode.HIDE_AFTER_ANIMATION_END, InAppImeMode.fromCode(2))
        assertEquals(InAppImeMode.HIDE_AFTER_KEYBOARD_OPEN, InAppImeMode.fromCode(3))
        assertEquals(InAppImeMode.HIDDEN, InAppImeMode.fromCode(99))
    }

    @Test
    fun request_and_reset_in_app_keyboard_height() {
        requestInAppKeyboardHeightUseCase(320)
        var insets = getKeyboardInsetsUseCase()
        assertEquals(320, insets.inAppKeyboardHeight)
        assertEquals(InAppImeMode.VISIBLE, insets.inAppImeMode)

        resetInAppKeyboardHeightUseCase(waitKeyboardOpen = true)
        insets = getKeyboardInsetsUseCase()
        assertEquals(0, insets.inAppKeyboardHeight)
        assertEquals(InAppImeMode.HIDE_AFTER_KEYBOARD_OPEN, insets.inAppImeMode)

        resetInAppKeyboardHeightUseCase(waitKeyboardOpen = false)
        insets = getKeyboardInsetsUseCase()
        assertEquals(0, insets.inAppKeyboardHeight)
        assertEquals(InAppImeMode.HIDDEN, insets.inAppImeMode)
    }

    @Test
    fun request_in_app_height_with_navbar_compensation() {
        requestInAppKeyboardHeightWithNavbarUseCase(height = 200, navigationBarHeight = 50)
        var insets = getKeyboardInsetsUseCase()
        assertEquals(250, insets.inAppKeyboardHeight)
        assertEquals(50, insets.navigationBarHeight)

        requestInAppKeyboardHeightWithNavbarUseCase(height = 0, navigationBarHeight = 50)
        insets = getKeyboardInsetsUseCase()
        assertEquals(0, insets.inAppKeyboardHeight)
    }

    @Test
    fun update_system_insets_state() {
        updateSystemInsetsUseCase(top = 30, bottom = 45, imeBottom = 350, animated = true)
        var insets = getKeyboardInsetsUseCase()
        assertEquals(30, insets.systemBarsTop)
        assertEquals(45, insets.systemBarsBottom)
        assertEquals(350, insets.imeBottom)
        assertEquals(KeyboardVisibilityState.ANIMATING_TO_FULLY_VISIBLE, insets.keyboardState)
        assertTrue(insets.isKeyboardVisible)

        updateSystemInsetsUseCase(top = 30, bottom = 45, imeBottom = 0, animated = false)
        insets = getKeyboardInsetsUseCase()
        assertEquals(0, insets.imeBottom)
        assertEquals(KeyboardVisibilityState.FULLY_HIDDEN, insets.keyboardState)
    }

    @Test
    fun reactive_insets_observation() = runTest {
        requestInAppKeyboardHeightUseCase(260)
        val insets = observeKeyboardInsetsUseCase().first()
        assertEquals(260, insets.inAppKeyboardHeight)
    }

    @Test
    fun view_model_events_and_state() = runTest {
        val viewModel = KeyboardInsetsViewModel(
            observeKeyboardInsetsUseCase = observeKeyboardInsetsUseCase,
            getKeyboardInsetsUseCase = getKeyboardInsetsUseCase,
            requestInAppKeyboardHeightUseCase = requestInAppKeyboardHeightUseCase,
            resetInAppKeyboardHeightUseCase = resetInAppKeyboardHeightUseCase,
            requestInAppKeyboardHeightWithNavbarUseCase = requestInAppKeyboardHeightWithNavbarUseCase,
            updateSystemInsetsUseCase = updateSystemInsetsUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.insets.inAppKeyboardHeight)

        viewModel.onEvent(KeyboardInsetsEvent.RequestHeight(300))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(300, viewModel.uiState.value.insets.inAppKeyboardHeight)

        viewModel.onEvent(KeyboardInsetsEvent.UpdateInsets(top = 20, bottom = 40, imeBottom = 320, animated = false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(320, viewModel.uiState.value.insets.imeBottom)
        assertEquals(320, viewModel.uiState.value.insets.effectiveBottomInset)

        viewModel.onEvent(KeyboardInsetsEvent.ResetHeight(waitKeyboardOpen = false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.insets.inAppKeyboardHeight)
    }
}
