package org.telegram.messenger.feature.timezones.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.timezones.domain.repository.TimezonesRepository

class ObserveTimezonesUseCase(
    private val repository: TimezonesRepository
) {
    operator fun invoke(): Flow<List<TimezoneModel>> {
        return repository.observeTimezones()
    }
}
