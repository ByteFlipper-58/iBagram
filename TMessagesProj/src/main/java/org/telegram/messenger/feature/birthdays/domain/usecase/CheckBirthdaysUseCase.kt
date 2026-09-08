package org.telegram.messenger.feature.birthdays.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.birthdays.domain.repository.BirthdaysRepository

class CheckBirthdaysUseCase(
    private val repository: BirthdaysRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<BirthdayStateModel?> =
        repository.checkBirthdays(force)
}
