package org.telegram.messenger.feature.timezones.domain.usecase

import org.telegram.messenger.feature.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.timezones.domain.repository.TimezonesRepository

class FindTimezoneUseCase(
    private val repository: TimezonesRepository
) {
    operator fun invoke(id: String): TimezoneModel? {
        return repository.findTimezone(id)
    }
}
