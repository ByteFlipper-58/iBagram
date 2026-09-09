package org.telegram.messenger.feature.push.domain.usecase

import org.telegram.messenger.feature.push.domain.repository.PushRepository

class IsPushAvailableUseCase(
    private val repository: PushRepository
) {
    operator fun invoke(): Boolean = repository.isPushServiceAvailable()
}
