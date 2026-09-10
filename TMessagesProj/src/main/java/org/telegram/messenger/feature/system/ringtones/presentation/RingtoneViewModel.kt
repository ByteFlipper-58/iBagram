package org.telegram.messenger.feature.system.ringtones.presentation

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
import org.telegram.messenger.feature.system.ringtones.domain.usecase.CancelRingtoneUploadUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.ObserveRingtoneStateUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.RefreshRingtonesUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.RemoveRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.SaveRingtoneFromDocumentUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.SelectRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.UploadRingtoneUseCase

class RingtoneViewModel(
    private val observeRingtoneStateUseCase: ObserveRingtoneStateUseCase,
    private val refreshRingtonesUseCase: RefreshRingtonesUseCase,
    private val selectRingtoneUseCase: SelectRingtoneUseCase,
    private val removeRingtoneUseCase: RemoveRingtoneUseCase,
    private val saveRingtoneFromDocumentUseCase: SaveRingtoneFromDocumentUseCase,
    private val uploadRingtoneUseCase: UploadRingtoneUseCase,
    private val cancelRingtoneUploadUseCase: CancelRingtoneUploadUseCase,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val coroutineScope = scope ?: CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    private val _uiState = MutableStateFlow(RingtoneUiState())
    val uiState: StateFlow<RingtoneUiState> = _uiState.asStateFlow()

    init {
        observeRingtoneStateUseCase()
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(
                        ringtones = state.ringtones,
                        selectedRingtoneId = state.selectedRingtoneId,
                        isLoading = state.isLoading,
                        activeUploadsCount = state.ringtones.count { it.isUploading },
                        limits = state.limits
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: RingtoneEvent) {
        when (event) {
            is RingtoneEvent.LoadRingtones -> {
                coroutineScope.launch {
                    refreshRingtonesUseCase(event.force)
                }
            }
            is RingtoneEvent.SelectRingtone -> {
                coroutineScope.launch {
                    selectRingtoneUseCase(event.id)
                }
            }
            is RingtoneEvent.TogglePreview -> {
                _uiState.update { current ->
                    val nextPreview = if (current.previewPlayingId == event.id) null else event.id
                    current.copy(previewPlayingId = nextPreview)
                }
            }
            is RingtoneEvent.UploadFile -> {
                coroutineScope.launch {
                    val result = uploadRingtoneUseCase(
                        filePath = event.filePath,
                        fileName = event.fileName,
                        durationSec = event.durationSec,
                        sizeBytes = event.sizeBytes
                    )
                    result.onFailure { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                }
            }
            is RingtoneEvent.CancelUpload -> {
                coroutineScope.launch {
                    cancelRingtoneUploadUseCase(event.filePath)
                }
            }
            is RingtoneEvent.DeleteRingtone -> {
                coroutineScope.launch {
                    removeRingtoneUseCase(event.id)
                }
            }
            is RingtoneEvent.SaveFromDocument -> {
                coroutineScope.launch {
                    val result = saveRingtoneFromDocumentUseCase(
                        documentId = event.documentId,
                        title = event.title,
                        durationSec = event.durationSec,
                        sizeBytes = event.sizeBytes,
                        mimeType = event.mimeType
                    )
                    if (!result.isValid) {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            }
            is RingtoneEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }
}
