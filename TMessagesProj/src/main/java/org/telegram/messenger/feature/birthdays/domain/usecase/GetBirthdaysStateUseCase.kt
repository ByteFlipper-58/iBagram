package org.telegram.messenger.feature.birthdays.domain.usecase

import org.telegram.messenger.feature.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.birthdays.domain.repository.BirthdaysRepository

class GetBirthdaysStateUseCase(
    private val repository: BirthdaysRepository
) {
    suspend operator fun invoke(): BirthdayStateModel? = repository.getBirthdaysState()
}
