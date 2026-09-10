package org.telegram.messenger.feature.media.fileloader.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.fileloader.domain.repository.FileLoaderRepository

class CancelAllDownloadsUseCase(
    private val repository: FileLoaderRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.cancelAllDownloads()
}
