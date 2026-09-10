package org.telegram.messenger.feature.media.fileref.domain.usecase

import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository

class CancelFileRefRequestUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(locationKey: String) {
        repository.cancelPendingRequest(locationKey)
    }
}
