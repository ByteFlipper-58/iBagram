package org.telegram.messenger.feature.media.fileloader.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.fileloader.domain.model.FileUploadRequest
import org.telegram.messenger.feature.media.fileloader.domain.repository.FileLoaderRepository

class UploadFileUseCase(
    private val repository: FileLoaderRepository
) {
    suspend operator fun invoke(request: FileUploadRequest): Result<String> = repository.uploadFile(request)
}
