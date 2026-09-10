package org.telegram.messenger.feature.media.voip.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.voip.domain.repository.VoIPRepository

class HangUpCallUseCase(
    private val repository: VoIPRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.hangUp()
}
