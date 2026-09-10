package org.telegram.messenger.feature.media.fileref.domain.usecase

import org.telegram.messenger.feature.media.fileref.domain.model.FileRefStatsModel
import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository

class GetFileRefStatsUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(): FileRefStatsModel = repository.getStats()
}
