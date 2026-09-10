package org.telegram.messenger.feature.business.timezones.domain.usecase

import org.telegram.messenger.feature.business.timezones.domain.repository.TimezonesRepository

class GetSystemTimezoneIdUseCase(
    private val repository: TimezonesRepository
) {
    operator fun invoke(): String {
        return repository.getSystemTimezoneId()
    }
}
