package org.telegram.messenger.feature.chattheme.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.chattheme.domain.model.DialogThemeStateModel
import org.telegram.messenger.feature.chattheme.domain.usecase.GetAvailableChatThemesUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.GetDialogThemeStateUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.ObserveDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.ResetDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.SaveChatWallpaperUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.SetDialogThemeUseCase

class ChatThemeViewModel(
    private val observeDialogThemeUseCase: ObserveDialogThemeUseCase,
    private val getDialogThemeStateUseCase: GetDialogThemeStateUseCase,
    private val getAvailableThemesUseCase: GetAvailableChatThemesUseCase,
    private val setDialogThemeUseCase: SetDialogThemeUseCase,
    private val resetDialogThemeUseCase: ResetDialogThemeUseCase,
    private val saveChatWallpaperUseCase: SaveChatWallpaperUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatThemeUiState>(ChatThemeUiState.Initial)
    val uiState: StateFlow<ChatThemeUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun onEvent(event: ChatThemeEvent) {
        when (event) {
            is ChatThemeEvent.LoadThemes -> loadThemes(event.dialogId)
            is ChatThemeEvent.SelectTheme -> selectTheme(event.theme)
            is ChatThemeEvent.ApplyTheme -> applyTheme(event.dialogId, event.theme)
            is ChatThemeEvent.ResetTheme -> resetTheme(event.dialogId)
        }
    }

    fun loadThemes(dialogId: Long) {
        observeJob?.cancel()
        _uiState.value = ChatThemeUiState.Loading

        viewModelScope.launch {
            val themesResult = getAvailableThemesUseCase()
            val availableThemes = if (themesResult is Result.Success) themesResult.data else emptyList()

            observeJob = launch {
                observeDialogThemeUseCase(dialogId)
                    .catch { e ->
                        _uiState.value = ChatThemeUiState.Error(e.message ?: "Failed to observe chat theme")
                    }
                    .collect { dialogState ->
                        val currentState = _uiState.value
                        val selected = if (currentState is ChatThemeUiState.Success) {
                            currentState.selectedTheme ?: dialogState.currentTheme
                        } else {
                            dialogState.currentTheme
                        }
                        _uiState.value = ChatThemeUiState.Success(
                            dialogThemeState = dialogState,
                            availableThemes = availableThemes,
                            selectedTheme = selected,
                            isSaving = false
                        )
                    }
            }
        }
    }

    fun selectTheme(theme: ChatThemeModel) {
        val currentState = _uiState.value
        if (currentState is ChatThemeUiState.Success) {
            _uiState.value = currentState.copy(selectedTheme = theme)
        }
    }

    fun applyTheme(dialogId: Long, theme: ChatThemeModel) {
        val currentState = _uiState.value
        if (currentState is ChatThemeUiState.Success) {
            _uiState.value = currentState.copy(isSaving = true)
        }

        viewModelScope.launch {
            val result = if (theme.isDefault) {
                resetDialogThemeUseCase(dialogId)
            } else {
                setDialogThemeUseCase(dialogId, theme.emoticon, theme.giftSlug)
            }

            val current = _uiState.value
            if (current is ChatThemeUiState.Success) {
                when (result) {
                    is Result.Success -> {
                        _uiState.value = current.copy(
                            selectedTheme = theme,
                            isSaving = false,
                            error = null
                        )
                    }
                    is Result.Failure -> {
                        _uiState.value = current.copy(
                            isSaving = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun resetTheme(dialogId: Long) {
        val currentState = _uiState.value
        if (currentState is ChatThemeUiState.Success) {
            _uiState.value = currentState.copy(isSaving = true)
        }

        viewModelScope.launch {
            val result = resetDialogThemeUseCase(dialogId)
            val current = _uiState.value
            if (current is ChatThemeUiState.Success) {
                when (result) {
                    is Result.Success -> {
                        _uiState.value = current.copy(
                            selectedTheme = null,
                            isSaving = false,
                            error = null
                        )
                    }
                    is Result.Failure -> {
                        _uiState.value = current.copy(
                            isSaving = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }
}
