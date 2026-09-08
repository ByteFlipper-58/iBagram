package org.telegram.messenger.feature.fileloader.presentation

import org.telegram.messenger.feature.fileloader.domain.model.FileTransferModel

data class FileLoaderUiState(
    val isLoading: Boolean = false,
    val activeTransfers: List<FileTransferModel> = emptyList(),
    val recentTransfers: List<FileTransferModel> = emptyList(),
    val selectedTransfer: FileTransferModel? = null,
    val errorMessage: String? = null
)
