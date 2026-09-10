package org.telegram.messenger.feature.media.fileref.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefStatsModel
import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository

class ObserveFileRefStatsUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(): Flow<FileRefStatsModel> = repository.observeStats()
}
