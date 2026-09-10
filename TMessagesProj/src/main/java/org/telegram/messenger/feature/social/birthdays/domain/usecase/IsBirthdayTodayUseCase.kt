package org.telegram.messenger.feature.social.birthdays.domain.usecase

import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository

class IsBirthdayTodayUseCase(
    private val repository: BirthdaysRepository
) {
    suspend operator fun invoke(userId: Long): Boolean = repository.isBirthdayToday(userId)
}
