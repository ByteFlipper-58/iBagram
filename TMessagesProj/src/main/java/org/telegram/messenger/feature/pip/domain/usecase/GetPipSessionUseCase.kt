package org.telegram.messenger.feature.pip.domain.usecase

import org.telegram.messenger.feature.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.pip.domain.repository.PipRepository

class GetPipSessionUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(): PipSessionInfo = repository.getSessionInfo()
}
