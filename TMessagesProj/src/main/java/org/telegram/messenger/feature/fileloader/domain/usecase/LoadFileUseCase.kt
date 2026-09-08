package org.telegram.messenger.feature.fileloader.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.fileloader.domain.repository.FileLoaderRepository

class LoadFileUseCase(
    private val repository: FileLoaderRepository
) {
    suspend operator fun invoke(request: FileDownloadRequest): Result<Unit> = repository.loadFile(request)
}
