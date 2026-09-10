package org.telegram.messenger.feature.business.timezones.domain.usecase

import org.telegram.messenger.feature.business.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.business.timezones.domain.repository.TimezonesRepository

class GetTimezonesUseCase(
    private val repository: TimezonesRepository
) {
    suspend operator fun invoke(): List<TimezoneModel> {
        return repository.getTimezones()
    }
}
