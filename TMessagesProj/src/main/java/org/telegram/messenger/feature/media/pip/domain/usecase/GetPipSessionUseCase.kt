package org.telegram.messenger.feature.media.pip.domain.usecase

import org.telegram.messenger.feature.media.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class GetPipSessionUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(): PipSessionInfo = repository.getSessionInfo()
}
