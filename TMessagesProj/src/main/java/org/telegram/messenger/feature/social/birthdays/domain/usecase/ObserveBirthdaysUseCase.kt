package org.telegram.messenger.feature.social.birthdays.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository

class ObserveBirthdaysUseCase(
    private val repository: BirthdaysRepository
) {
    operator fun invoke(): Flow<BirthdayStateModel?> = repository.observeBirthdays()
}
