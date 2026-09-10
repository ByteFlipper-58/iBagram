package org.telegram.messenger.feature.media.fileloader.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.media.fileloader.domain.model.FileUploadRequest
import org.telegram.messenger.feature.media.fileloader.domain.usecase.CancelAllDownloadsUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.CancelFileUploadUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.CancelLoadFileUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.GetActiveDownloadsUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.GetRecentDownloadsUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.LoadFileUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.ObserveTransfersUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.UploadFileUseCase

/**
 * Clean ViewModel for managing file downloads, uploads, transfer progress, and lifecycle events.
 */
class FileLoaderViewModel(
    private val observeTransfersUseCase: ObserveTransfersUseCase,
    private val getActiveDownloadsUseCase: GetActiveDownloadsUseCase,
    private val getRecentDownloadsUseCase: GetRecentDownloadsUseCase,
    private val loadFileUseCase: LoadFileUseCase,
    private val cancelLoadFileUseCase: CancelLoadFileUseCase,
    private val cancelAllDownloadsUseCase: CancelAllDownloadsUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val cancelFileUploadUseCase: CancelFileUploadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileLoaderUiState(isLoading = true))
    val uiState: StateFlow<FileLoaderUiState> = _uiState.asStateFlow()

    init {
        observeTransfers()
    }

    private fun observeTransfers() {
        viewModelScope.launch {
            combine(
                getActiveDownloadsUseCase(),
                getRecentDownloadsUseCase()
            ) { active, recent ->
                Pair(active, recent)
            }.collect { (active, recent) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeTransfers = active,
                    recentTransfers = recent
                )
            }
        }
    }

    fun onEvent(event: FileLoaderEvent) {
        when (event) {
            is FileLoaderEvent.LoadFile -> loadFile(event.request)
            is FileLoaderEvent.CancelLoad -> cancelLoad(event.fileName)
            FileLoaderEvent.CancelAllDownloads -> cancelAllDownloads()
            is FileLoaderEvent.UploadFile -> uploadFile(event.request)
            is FileLoaderEvent.CancelUpload -> cancelUpload(event.location, event.isEncrypted)
            is FileLoaderEvent.SelectTransfer -> selectTransfer(event.fileId)
            FileLoaderEvent.DismissError -> dismissError()
        }
    }

    private fun loadFile(request: FileDownloadRequest) {
        viewModelScope.launch {
            when (val result = loadFileUseCase(request)) {
                is Result.Success -> {
                    // Transfer will be observed via Flow
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private fun cancelLoad(fileName: String) {
        viewModelScope.launch {
            when (val result = cancelLoadFileUseCase(fileName)) {
                is Result.Success -> {
                    // State updated via Flow
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private fun cancelAllDownloads() {
        viewModelScope.launch {
            when (val result = cancelAllDownloadsUseCase()) {
                is Result.Success -> {
                    // State updated via Flow
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private fun uploadFile(request: FileUploadRequest) {
        viewModelScope.launch {
            when (val result = uploadFileUseCase(request)) {
                is Result.Success -> {
                    // Transfer will be observed via Flow
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private fun cancelUpload(location: String, isEncrypted: Boolean) {
        viewModelScope.launch {
            when (val result = cancelFileUploadUseCase(location, isEncrypted)) {
                is Result.Success -> {
                    // State updated via Flow
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private fun selectTransfer(fileId: String?) {
        val selected = if (fileId != null) {
            _uiState.value.activeTransfers.firstOrNull { it.id == fileId }
                ?: _uiState.value.recentTransfers.firstOrNull { it.id == fileId }
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(selectedTransfer = selected)
    }

    private fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
