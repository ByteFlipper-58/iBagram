package org.telegram.messenger.feature.media.fileloader.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferModel
import org.telegram.messenger.feature.media.fileloader.domain.repository.FileLoaderRepository

class ObserveTransfersUseCase(
    private val repository: FileLoaderRepository
) {
    operator fun invoke(): Flow<List<FileTransferModel>> = repository.observeTransfers()
}
