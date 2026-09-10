package org.telegram.messenger.feature.media.cachebychats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.ClearKeepMediaExceptionsUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.GetCacheByChatsConfigUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.ObserveCacheByChatsConfigUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.RemoveKeepMediaExceptionUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.SetKeepMediaDurationUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.SetKeepMediaExceptionUseCase

class CacheByChatsViewModel(
    private val observeCacheByChatsConfigUseCase: ObserveCacheByChatsConfigUseCase,
    private val getCacheByChatsConfigUseCase: GetCacheByChatsConfigUseCase,
    private val setKeepMediaDurationUseCase: SetKeepMediaDurationUseCase,
    private val setKeepMediaExceptionUseCase: SetKeepMediaExceptionUseCase,
    private val removeKeepMediaExceptionUseCase: RemoveKeepMediaExceptionUseCase,
    private val clearKeepMediaExceptionsUseCase: ClearKeepMediaExceptionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CacheByChatsUiState(isLoading = true))
    val uiState: StateFlow<CacheByChatsUiState> = _uiState.asStateFlow()

    init {
        observeCacheByChatsConfigUseCase()
            .onEach { config ->
                updateFromConfig(config)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: CacheByChatsEvent) {
        when (event) {
            is CacheByChatsEvent.SelectTab -> {
                _uiState.value = _uiState.value.copy(selectedTab = event.tab)
            }
            is CacheByChatsEvent.SetDuration -> {
                setKeepMediaDurationUseCase(event.type, event.duration)
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Updated retention for ${event.type.name} to ${event.duration.name}"
                )
            }
            is CacheByChatsEvent.SetException -> {
                setKeepMediaExceptionUseCase(event.dialogId, event.type, event.duration)
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Added exception for dialog ${event.dialogId}"
                )
            }
            is CacheByChatsEvent.RemoveException -> {
                removeKeepMediaExceptionUseCase(event.dialogId, event.type)
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Removed exception for dialog ${event.dialogId}"
                )
            }
            is CacheByChatsEvent.ClearAllExceptions -> {
                clearKeepMediaExceptionsUseCase(event.type)
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Cleared all exceptions for ${event.type.name}"
                )
            }
            is CacheByChatsEvent.DismissInfo -> {
                _uiState.value = _uiState.value.copy(infoMessage = null)
            }
        }
    }

    private fun updateFromConfig(config: CacheByChatsConfigModel) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            config = config
        )
    }
}
