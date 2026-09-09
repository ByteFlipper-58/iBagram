package org.telegram.messenger.feature.push.domain.usecase

import org.telegram.messenger.feature.push.domain.repository.PushRepository

class ResetPushTokenUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.resetPushToken()
}
