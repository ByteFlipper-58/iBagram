package org.telegram.messenger.feature.voip.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.voip.domain.repository.VoIPRepository

class StartCallUseCase(
    private val repository: VoIPRepository
) {
    suspend operator fun invoke(userId: Long, isVideo: Boolean): Result<Unit> =
        repository.startCall(userId, isVideo)
}
