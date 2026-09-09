package org.telegram.messenger.feature.timezones.domain.usecase

import org.telegram.messenger.feature.timezones.domain.repository.TimezonesRepository

class GetTimezoneNameUseCase(
    private val repository: TimezonesRepository
) {
    operator fun invoke(id: String, withOffset: Boolean = false): String {
        return repository.getTimezoneName(id, withOffset)
    }
}
