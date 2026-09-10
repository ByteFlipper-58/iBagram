package org.telegram.messenger.feature.media.fileloader.presentation

import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferModel

data class FileLoaderUiState(
    val isLoading: Boolean = false,
    val activeTransfers: List<FileTransferModel> = emptyList(),
    val recentTransfers: List<FileTransferModel> = emptyList(),
    val selectedTransfer: FileTransferModel? = null,
    val errorMessage: String? = null
)
