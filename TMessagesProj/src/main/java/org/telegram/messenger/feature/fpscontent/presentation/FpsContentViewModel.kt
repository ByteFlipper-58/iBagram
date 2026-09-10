package org.telegram.messenger.feature.fpscontent.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.fpscontent.domain.usecase.DispatchVsyncTickUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.GetFpsContentStatsUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.GetFpsSubscriptionsUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.ObserveFpsContentStatsUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.ObserveFpsTicksUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.RegisterFrameCallbackUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.RegisterRunnableCallbackUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.RequestDrawableInvalidationUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.RequestViewInvalidationUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.ResetFpsContentUseCase
import org.telegram.messenger.feature.fpscontent.domain.usecase.UnregisterCallbackUseCase

/**
 * ViewModel for inspecting and controlling 60fps arbitration.
 */
class FpsContentViewModel(
    private val registerFrameCallbackUseCase: RegisterFrameCallbackUseCase,
    private val registerRunnableCallbackUseCase: RegisterRunnableCallbackUseCase,
    private val unregisterCallbackUseCase: UnregisterCallbackUseCase,
    private val requestViewInvalidationUseCase: RequestViewInvalidationUseCase,
    private val requestDrawableInvalidationUseCase: RequestDrawableInvalidationUseCase,
    private val dispatchVsyncTickUseCase: DispatchVsyncTickUseCase,
    private val getFpsContentStatsUseCase: GetFpsContentStatsUseCase,
    private val getFpsSubscriptionsUseCase: GetFpsSubscriptionsUseCase,
    private val observeFpsContentStatsUseCase: ObserveFpsContentStatsUseCase,
    private val observeFpsTicksUseCase: ObserveFpsTicksUseCase,
    private val resetFpsContentUseCase: ResetFpsContentUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private val _uiState = MutableStateFlow(
        FpsContentUiState(
            stats = getFpsContentStatsUseCase(),
            subscriptions = getFpsSubscriptionsUseCase()
        )
    )
    val uiState: StateFlow<FpsContentUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeFpsContentStatsUseCase().collectLatest { stats ->
                _uiState.update { current ->
                    current.copy(
                        stats = stats,
                        subscriptions = getFpsSubscriptionsUseCase()
                    )
                }
            }
        }

        scope.launch {
            observeFpsTicksUseCase().collect { tick ->
                _uiState.update { current ->
                    val updatedTicks = (listOf(tick) + current.recentTicks).take(20)
                    current.copy(recentTicks = updatedTicks)
                }
            }
        }
    }

    fun onEvent(event: FpsContentEvent) {
        when (event) {
            is FpsContentEvent.RegisterFrameCallback -> {
                registerFrameCallbackUseCase(
                    fps = event.fps,
                    isOneShot = event.isOneShot,
                    onFrame = { /* no-op debug listener */ }
                )
            }
            is FpsContentEvent.RegisterRunnableCallback -> {
                registerRunnableCallbackUseCase(
                    fps = event.fps,
                    isOneShot = event.isOneShot,
                    action = { /* no-op debug action */ }
                )
            }
            is FpsContentEvent.UnregisterCallback -> {
                unregisterCallbackUseCase(event.subscriptionId)
            }
            is FpsContentEvent.PostInvalidateView -> {
                requestViewInvalidationUseCase(event.viewId)
            }
            is FpsContentEvent.PostInvalidateDrawable -> {
                requestDrawableInvalidationUseCase(event.drawableId, event.fps)
            }
            is FpsContentEvent.DispatchVsync -> {
                dispatchVsyncTickUseCase(event.frameTimeNanos)
            }
            is FpsContentEvent.Reset -> {
                resetFpsContentUseCase()
                _uiState.update { current ->
                    current.copy(
                        recentTicks = emptyList(),
                        subscriptions = emptyList()
                    )
                }
            }
            is FpsContentEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
