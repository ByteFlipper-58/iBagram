package org.telegram.messenger.feature.push.domain.usecase

import org.telegram.messenger.feature.push.domain.model.PushServiceType
import org.telegram.messenger.feature.push.domain.repository.PushRepository

class RegisterPushTokenUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(serviceType: PushServiceType, token: String?): Result<Unit> =
        repository.registerPushToken(serviceType, token)
}
