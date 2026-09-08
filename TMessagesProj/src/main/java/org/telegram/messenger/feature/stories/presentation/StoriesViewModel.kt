package org.telegram.messenger.feature.stories.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stories.domain.usecase.ActivateStealthModeUseCase
import org.telegram.messenger.feature.stories.domain.usecase.DeleteStoryUseCase
import org.telegram.messenger.feature.stories.domain.usecase.GetStoryLimitUseCase
import org.telegram.messenger.feature.stories.domain.usecase.MarkStoryAsReadUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveHiddenStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveSelfStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveStealthModeUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.RefreshStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ToggleStoryHiddenUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ToggleStoryPinUseCase

class StoriesViewModel(
    private val observeStoriesUseCase: ObserveStoriesUseCase,
    private val observeHiddenStoriesUseCase: ObserveHiddenStoriesUseCase,
    private val observeStealthModeUseCase: ObserveStealthModeUseCase,
    private val observeSelfStoriesUseCase: ObserveSelfStoriesUseCase,
    private val markStoryAsReadUseCase: MarkStoryAsReadUseCase,
    private val deleteStoryUseCase: DeleteStoryUseCase,
    private val toggleStoryPinUseCase: ToggleStoryPinUseCase,
    private val toggleStoryHiddenUseCase: ToggleStoryHiddenUseCase,
    private val activateStealthModeUseCase: ActivateStealthModeUseCase,
    private val getStoryLimitUseCase: GetStoryLimitUseCase,
    private val refreshStoriesUseCase: RefreshStoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoriesUiState(isLoading = true))
    val uiState: StateFlow<StoriesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StoriesEvent>()
    val events: SharedFlow<StoriesEvent> = _events.asSharedFlow()

    init {
        observeStories()
        observeHiddenStories()
        observeStealthMode()
        observeSelfStories()
        loadStoryLimit()
    }

    private fun observeStories() {
        viewModelScope.launch {
            observeStoriesUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { stories ->
                    _uiState.update { it.copy(peerStories = stories, isLoading = false) }
                }
        }
    }

    private fun observeHiddenStories() {
        viewModelScope.launch {
            observeHiddenStoriesUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { hidden ->
                    _uiState.update { it.copy(hiddenStories = hidden) }
                }
        }
    }

    private fun observeStealthMode() {
        viewModelScope.launch {
            observeStealthModeUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { stealth ->
                    _uiState.update { it.copy(stealthMode = stealth) }
                }
        }
    }

    private fun observeSelfStories() {
        viewModelScope.launch {
            observeSelfStoriesUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { self ->
                    _uiState.update { it.copy(selfStories = self) }
                }
        }
    }

    fun loadStoryLimit() {
        viewModelScope.launch {
            when (val result = getStoryLimitUseCase()) {
                is Result.Success -> _uiState.update { it.copy(storyLimit = result.data) }
                is Result.Failure -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun onEvent(event: StoriesEvent) {
        viewModelScope.launch {
            _events.emit(event)
            when (event) {
                is StoriesEvent.Refresh -> {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    when (val result = refreshStoriesUseCase()) {
                        is Result.Success -> {
                            loadStoryLimit()
                            _uiState.update { it.copy(isLoading = false) }
                        }
                        is Result.Failure -> _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.error.message)
                        }
                    }
                }
                is StoriesEvent.MarkAsRead -> {
                    markStoryAsReadUseCase(event.dialogId, event.storyId)
                }
                is StoriesEvent.DeleteStory -> {
                    deleteStoryUseCase(event.dialogId, event.storyId)
                }
                is StoriesEvent.TogglePin -> {
                    toggleStoryPinUseCase(event.dialogId, event.storyId, event.pin)
                }
                is StoriesEvent.ToggleHide -> {
                    toggleStoryHiddenUseCase(event.dialogId, event.hide)
                }
                is StoriesEvent.ActivateStealthMode -> {
                    activateStealthModeUseCase(event.future, event.past)
                }
            }
        }
    }
}
