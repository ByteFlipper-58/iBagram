package org.telegram.messenger.feature.system.keyboardinsets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.GetKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ObserveKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightWithNavbarUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ResetInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.UpdateSystemInsetsUseCase

class KeyboardInsetsViewModel(
    private val observeKeyboardInsetsUseCase: ObserveKeyboardInsetsUseCase,
    private val getKeyboardInsetsUseCase: GetKeyboardInsetsUseCase,
    private val requestInAppKeyboardHeightUseCase: RequestInAppKeyboardHeightUseCase,
    private val resetInAppKeyboardHeightUseCase: ResetInAppKeyboardHeightUseCase,
    private val requestInAppKeyboardHeightWithNavbarUseCase: RequestInAppKeyboardHeightWithNavbarUseCase,
    private val updateSystemInsetsUseCase: UpdateSystemInsetsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        KeyboardInsetsUiState(insets = getKeyboardInsetsUseCase())
    )
    val uiState: StateFlow<KeyboardInsetsUiState> = _uiState.asStateFlow()

    init {
        observeKeyboardInsetsUseCase()
            .onEach { insets ->
                _uiState.value = KeyboardInsetsUiState(insets = insets)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: KeyboardInsetsEvent) {
        when (event) {
            is KeyboardInsetsEvent.RequestHeight -> {
                requestInAppKeyboardHeightUseCase(event.height)
            }
            is KeyboardInsetsEvent.ResetHeight -> {
                resetInAppKeyboardHeightUseCase(event.waitKeyboardOpen)
            }
            is KeyboardInsetsEvent.RequestHeightWithNavbar -> {
                requestInAppKeyboardHeightWithNavbarUseCase(event.height, event.navigationBarHeight)
            }
            is KeyboardInsetsEvent.UpdateInsets -> {
                updateSystemInsetsUseCase(event.top, event.bottom, event.imeBottom, event.animated)
            }
        }
    }
}
