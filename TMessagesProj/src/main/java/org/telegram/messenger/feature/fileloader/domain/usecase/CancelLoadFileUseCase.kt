package org.telegram.messenger.feature.fileloader.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.fileloader.domain.repository.FileLoaderRepository

class CancelLoadFileUseCase(
    private val repository: FileLoaderRepository
) {
    suspend operator fun invoke(fileName: String): Result<Unit> = repository.cancelLoadFile(fileName)
}
