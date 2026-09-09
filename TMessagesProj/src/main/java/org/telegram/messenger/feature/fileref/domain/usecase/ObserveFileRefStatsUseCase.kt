package org.telegram.messenger.feature.fileref.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.fileref.domain.model.FileRefStatsModel
import org.telegram.messenger.feature.fileref.domain.repository.FileRefRepository

class ObserveFileRefStatsUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(): Flow<FileRefStatsModel> = repository.observeStats()
}
