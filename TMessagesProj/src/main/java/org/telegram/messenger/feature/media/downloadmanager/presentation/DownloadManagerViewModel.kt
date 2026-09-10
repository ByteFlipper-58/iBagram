package org.telegram.messenger.feature.media.downloadmanager.presentation

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
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.CancelDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ClearRecentDownloadsUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.EnqueueDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.MarkDownloadsAsViewedUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ObserveDownloadManagerStateUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.PauseDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ResumeDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.RetryDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.SetDownloadNetworkTypeUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.UpdateDownloadPresetUseCase

class DownloadManagerViewModel(
    private val observeDownloadManagerStateUseCase: ObserveDownloadManagerStateUseCase,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val pauseDownloadUseCase: PauseDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val retryDownloadUseCase: RetryDownloadUseCase,
    private val clearRecentDownloadsUseCase: ClearRecentDownloadsUseCase,
    private val markDownloadsAsViewedUseCase: MarkDownloadsAsViewedUseCase,
    private val setDownloadNetworkTypeUseCase: SetDownloadNetworkTypeUseCase,
    private val updateDownloadPresetUseCase: UpdateDownloadPresetUseCase,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val coroutineScope = scope ?: CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    private val _uiState = MutableStateFlow(DownloadManagerUiState())
    val uiState: StateFlow<DownloadManagerUiState> = _uiState.asStateFlow()

    init {
        observeDownloadManagerStateUseCase()
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(
                        downloadingFiles = state.downloadingFiles,
                        recentDownloadingFiles = state.recentDownloadingFiles,
                        unviewedDownloads = state.unviewedDownloads,
                        currentNetwork = state.currentNetwork,
                        activePreset = state.activePreset,
                        stats = state.stats
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: DownloadManagerEvent) {
        when (event) {
            is DownloadManagerEvent.EnqueueDownload -> {
                coroutineScope.launch {
                    try {
                        enqueueDownloadUseCase(event.item)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }
                }
            }
            is DownloadManagerEvent.PauseDownload -> {
                coroutineScope.launch {
                    pauseDownloadUseCase(event.id)
                }
            }
            is DownloadManagerEvent.ResumeDownload -> {
                coroutineScope.launch {
                    resumeDownloadUseCase(event.id)
                }
            }
            is DownloadManagerEvent.CancelDownload -> {
                coroutineScope.launch {
                    cancelDownloadUseCase(event.id)
                }
            }
            is DownloadManagerEvent.RetryDownload -> {
                coroutineScope.launch {
                    retryDownloadUseCase(event.id)
                }
            }
            is DownloadManagerEvent.DeleteRecentDownload -> {
                // Not directly exposed via usecase, or can be added if needed
            }
            is DownloadManagerEvent.ClearRecentDownloads -> {
                coroutineScope.launch {
                    clearRecentDownloadsUseCase()
                }
            }
            is DownloadManagerEvent.MarkDownloadsAsViewed -> {
                coroutineScope.launch {
                    markDownloadsAsViewedUseCase()
                }
            }
            is DownloadManagerEvent.SetNetworkType -> {
                coroutineScope.launch {
                    setDownloadNetworkTypeUseCase(event.network)
                }
            }
            is DownloadManagerEvent.UpdatePreset -> {
                coroutineScope.launch {
                    updateDownloadPresetUseCase(event.network, event.preset)
                }
            }
        }
    }
}
