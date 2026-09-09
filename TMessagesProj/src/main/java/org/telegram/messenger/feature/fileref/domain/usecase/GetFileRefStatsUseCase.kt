package org.telegram.messenger.feature.fileref.domain.usecase

import org.telegram.messenger.feature.fileref.domain.model.FileRefStatsModel
import org.telegram.messenger.feature.fileref.domain.repository.FileRefRepository

class GetFileRefStatsUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(): FileRefStatsModel = repository.getStats()
}
