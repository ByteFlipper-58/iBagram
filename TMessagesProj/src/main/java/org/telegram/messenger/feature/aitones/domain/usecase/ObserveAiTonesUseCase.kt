package org.telegram.messenger.feature.aitones.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.aitones.domain.model.AiTonesStateModel
import org.telegram.messenger.feature.aitones.domain.repository.AiTonesRepository

class ObserveAiTonesUseCase(
    private val repository: AiTonesRepository
) {
    operator fun invoke(): Flow<AiTonesStateModel> = repository.observeTones()
}
