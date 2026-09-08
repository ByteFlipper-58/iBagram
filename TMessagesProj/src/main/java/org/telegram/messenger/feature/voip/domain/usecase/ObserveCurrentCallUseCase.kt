package org.telegram.messenger.feature.voip.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.voip.domain.model.CallModel
import org.telegram.messenger.feature.voip.domain.repository.VoIPRepository

class ObserveCurrentCallUseCase(
    private val repository: VoIPRepository
) {
    operator fun invoke(): Flow<CallModel?> = repository.observeCurrentCall()
}
