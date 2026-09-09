package org.telegram.messenger.feature.aitones.domain.usecase

import org.telegram.messenger.feature.aitones.domain.model.AiTonesStateModel
import org.telegram.messenger.feature.aitones.domain.repository.AiTonesRepository

class GetAiTonesStateUseCase(
    private val repository: AiTonesRepository
) {
    operator fun invoke(): AiTonesStateModel = repository.getTonesState()
}
