package org.telegram.messenger.feature.voip.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.voip.domain.repository.VoIPRepository

class HangUpCallUseCase(
    private val repository: VoIPRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.hangUp()
}
