package org.telegram.messenger.feature.network.push.domain.usecase

import org.telegram.messenger.feature.network.push.domain.repository.PushRepository

class ResetPushTokenUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.resetPushToken()
}
