package org.telegram.messenger.feature.media.imageloader.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.media.imageloader.domain.usecase.CancelImageRequestUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ClearImageCacheUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.EnqueueImageRequestUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ObserveImageLoaderStateUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.TrimImageMemoryUseCase

class ImageLoaderViewModel(
    private val observeImageLoaderStateUseCase: ObserveImageLoaderStateUseCase,
    private val enqueueImageRequestUseCase: EnqueueImageRequestUseCase,
    private val cancelImageRequestUseCase: CancelImageRequestUseCase,
    private val trimImageMemoryUseCase: TrimImageMemoryUseCase,
    private val clearImageCacheUseCase: ClearImageCacheUseCase,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val coroutineScope = scope ?: CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    private val _uiState = MutableStateFlow(ImageLoaderUiState())
    val uiState: StateFlow<ImageLoaderUiState> = _uiState.asStateFlow()

    init {
        observeImageLoaderStateUseCase()
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(
                        requests = state.requests,
                        activeCount = state.activeCount,
                        completedCount = state.completedCount,
                        failedCount = state.failedCount,
                        cacheStats = state.cacheStats
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: ImageLoaderEvent) {
        when (event) {
            is ImageLoaderEvent.EnqueueRequest -> {
                coroutineScope.launch {
                    try {
                        enqueueImageRequestUseCase(
                            key = event.key,
                            url = event.url,
                            filter = event.filter,
                            filterSpec = event.filterSpec,
                            priority = event.priority,
                            canForce8888 = event.canForce8888
                        )
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }
                }
            }
            is ImageLoaderEvent.CancelRequest -> {
                coroutineScope.launch {
                    cancelImageRequestUseCase(event.key)
                }
            }
            is ImageLoaderEvent.SelectCacheTier -> {
                _uiState.update { it.copy(selectedTier = event.tier) }
            }
            is ImageLoaderEvent.ClearCache -> {
                coroutineScope.launch {
                    clearImageCacheUseCase(event.tier)
                }
            }
            is ImageLoaderEvent.TrimMemory -> {
                coroutineScope.launch {
                    _uiState.update { it.copy(isTrimming = true, lastTrimmedLevel = event.level) }
                    trimImageMemoryUseCase(event.level)
                    _uiState.update { it.copy(isTrimming = false) }
                }
            }
        }
    }
}
