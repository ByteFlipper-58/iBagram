package org.telegram.messenger.feature.aitones.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.aitones.domain.model.AiTonesStateModel
import org.telegram.messenger.feature.aitones.domain.repository.AiTonesRepository

class LoadAiTonesUseCase(
    private val repository: AiTonesRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<AiTonesStateModel> =
        repository.loadTones(force)
}
