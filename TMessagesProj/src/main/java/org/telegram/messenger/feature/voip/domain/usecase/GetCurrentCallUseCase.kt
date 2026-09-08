package org.telegram.messenger.feature.voip.domain.usecase

import org.telegram.messenger.feature.voip.domain.model.CallModel
import org.telegram.messenger.feature.voip.domain.repository.VoIPRepository

class GetCurrentCallUseCase(
    private val repository: VoIPRepository
) {
    suspend operator fun invoke(): CallModel? = repository.getCurrentCall()
}
