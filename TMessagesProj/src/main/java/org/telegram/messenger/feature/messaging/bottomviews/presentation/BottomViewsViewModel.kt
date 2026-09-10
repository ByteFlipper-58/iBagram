package org.telegram.messenger.feature.messaging.bottomviews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomContainerType
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetBottomViewsStateUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.ObserveBottomViewsVisibilityUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.SetBottomViewVisibleUseCase

class BottomViewsViewModel(
    private val observeBottomViewsVisibilityUseCase: ObserveBottomViewsVisibilityUseCase,
    private val getBottomViewsStateUseCase: GetBottomViewsStateUseCase,
    private val setBottomViewVisibleUseCase: SetBottomViewVisibleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val s = getBottomViewsStateUseCase()
            BottomViewsUiState(
                state = s,
                priorityContainerType = BottomContainerType.fromId(s.priorityContainerId)
            )
        }
    )
    val uiState: StateFlow<BottomViewsUiState> = _uiState.asStateFlow()

    init {
        observeBottomViewsVisibilityUseCase()
            .onEach { state ->
                _uiState.value = BottomViewsUiState(
                    state = state,
                    priorityContainerType = BottomContainerType.fromId(state.priorityContainerId)
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BottomViewsEvent) {
        when (event) {
            is BottomViewsEvent.SetViewVisible -> {
                setBottomViewVisibleUseCase(
                    containerId = event.containerId,
                    isVisible = event.isVisible,
                    animated = event.animated
                )
            }
            is BottomViewsEvent.ResetToDefault -> {
                for (i in 1..31) {
                    setBottomViewVisibleUseCase(containerId = i, isVisible = false, animated = false)
                }
                setBottomViewVisibleUseCase(containerId = 0, isVisible = true, animated = false)
            }
        }
    }
}
