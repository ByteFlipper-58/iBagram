package org.telegram.messenger.feature.business.timezones.domain.usecase

import org.telegram.messenger.feature.business.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.business.timezones.domain.repository.TimezonesRepository

class FindTimezoneUseCase(
    private val repository: TimezonesRepository
) {
    operator fun invoke(id: String): TimezoneModel? {
        return repository.findTimezone(id)
    }
}
