package org.telegram.messenger.feature.business.timezones.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.business.timezones.domain.repository.TimezonesRepository

class LoadTimezonesUseCase(
    private val repository: TimezonesRepository
) {
    suspend operator fun invoke(forceReload: Boolean = false): Result<List<TimezoneModel>> {
        return repository.loadTimezones(forceReload)
    }
}
