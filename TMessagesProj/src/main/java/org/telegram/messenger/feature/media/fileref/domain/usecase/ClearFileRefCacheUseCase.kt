package org.telegram.messenger.feature.media.fileref.domain.usecase

import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository

class ClearFileRefCacheUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke() {
        repository.clearCache()
    }
}
