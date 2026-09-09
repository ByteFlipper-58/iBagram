package org.telegram.messenger.feature.timezones.domain.usecase

import org.telegram.messenger.feature.timezones.domain.repository.TimezonesRepository

class GetSystemTimezoneIdUseCase(
    private val repository: TimezonesRepository
) {
    operator fun invoke(): String {
        return repository.getSystemTimezoneId()
    }
}
