package org.telegram.messenger.feature.media.pip.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class ObservePipSessionUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(): Flow<PipSessionInfo> = repository.observeSessionInfo()
}
