package org.telegram.messenger.feature.network.push.domain.usecase

import org.telegram.messenger.feature.network.push.domain.model.PushServiceType
import org.telegram.messenger.feature.network.push.domain.repository.PushRepository

class RegisterPushTokenUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(serviceType: PushServiceType, token: String?): Result<Unit> =
        repository.registerPushToken(serviceType, token)
}
