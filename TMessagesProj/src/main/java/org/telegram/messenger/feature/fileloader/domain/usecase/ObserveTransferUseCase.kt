package org.telegram.messenger.feature.fileloader.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.fileloader.domain.model.FileTransferModel
import org.telegram.messenger.feature.fileloader.domain.repository.FileLoaderRepository

class ObserveTransferUseCase(
    private val repository: FileLoaderRepository
) {
    operator fun invoke(fileId: String): Flow<FileTransferModel?> = repository.observeTransfer(fileId)
}
