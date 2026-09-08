package org.telegram.messenger.feature.fileloader.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.fileloader.domain.repository.FileLoaderRepository

class CancelFileUploadUseCase(
    private val repository: FileLoaderRepository
) {
    suspend operator fun invoke(location: String, isEncrypted: Boolean): Result<Unit> =
        repository.cancelFileUpload(location, isEncrypted)
}
