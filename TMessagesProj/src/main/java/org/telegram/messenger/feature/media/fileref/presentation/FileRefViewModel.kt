package org.telegram.messenger.feature.media.fileref.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefStatsModel
import org.telegram.messenger.feature.media.fileref.domain.usecase.CancelFileRefRequestUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.ClearFileRefCacheUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.GetFileRefStatsUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.NotifyReferenceRenewedUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.ObserveFileRefStatsUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.RequestReferenceRenewalUseCase

class FileRefViewModel(
    private val observeFileRefStatsUseCase: ObserveFileRefStatsUseCase,
    private val getFileRefStatsUseCase: GetFileRefStatsUseCase,
    private val requestReferenceRenewalUseCase: RequestReferenceRenewalUseCase,
    private val notifyReferenceRenewedUseCase: NotifyReferenceRenewedUseCase,
    private val cancelFileRefRequestUseCase: CancelFileRefRequestUseCase,
    private val clearFileRefCacheUseCase: ClearFileRefCacheUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileRefUiState(isLoading = true))
    val uiState: StateFlow<FileRefUiState> = _uiState.asStateFlow()

    init {
        observeFileRefStatsUseCase()
            .onEach { stats ->
                updateFromStats(stats)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: FileRefEvent) {
        when (event) {
            is FileRefEvent.RequestRenewal -> {
                val immediatelyRenewed = requestReferenceRenewalUseCase(event.item)
                if (immediatelyRenewed) {
                    _uiState.value = _uiState.value.copy(
                        infoMessage = "Renewed immediately from cache: ${event.item.locationKey}"
                    )
                }
            }
            is FileRefEvent.NotifyRenewed -> {
                notifyReferenceRenewedUseCase(event.locationKey, event.parentKey, event.refLength)
            }
            is FileRefEvent.CancelRequest -> {
                cancelFileRefRequestUseCase(event.locationKey)
            }
            is FileRefEvent.ClearCache -> {
                clearFileRefCacheUseCase()
                _uiState.value = _uiState.value.copy(infoMessage = "FileRef cache cleared")
            }
            is FileRefEvent.DismissInfo -> {
                _uiState.value = _uiState.value.copy(infoMessage = null)
            }
        }
    }

    private fun updateFromStats(stats: FileRefStatsModel) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            stats = stats
        )
    }
}
