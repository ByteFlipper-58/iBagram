package org.telegram.messenger.feature.push.domain.usecase

import org.telegram.messenger.feature.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.push.domain.repository.PushRepository

class RequestPushTokenUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(): Result<PushRegistrationResult> = repository.requestPushToken()
}
