package org.telegram.messenger.feature.social.birthdays.domain.usecase

import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository

class GetBirthdaysStateUseCase(
    private val repository: BirthdaysRepository
) {
    suspend operator fun invoke(): BirthdayStateModel? = repository.getBirthdaysState()
}
