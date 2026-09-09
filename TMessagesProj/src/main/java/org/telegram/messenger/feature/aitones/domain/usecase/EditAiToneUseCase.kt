package org.telegram.messenger.feature.aitones.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.aitones.domain.repository.AiTonesRepository

class EditAiToneUseCase(
    private val repository: AiTonesRepository
) {
    suspend operator fun invoke(tone: AiToneModel): Result<Unit> = repository.editTone(tone)
}
