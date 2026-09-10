package org.telegram.messenger.feature.messaging.aitones.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.messaging.aitones.domain.repository.AiTonesRepository

class AddAiToneUseCase(
    private val repository: AiTonesRepository
) {
    suspend operator fun invoke(tone: AiToneModel): Result<Unit> = repository.addTone(tone)
}
