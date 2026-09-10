package org.telegram.messenger.feature.social.birthdays.domain.usecase

import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository

class HasBirthdaysTodayUseCase(
    private val repository: BirthdaysRepository
) {
    suspend operator fun invoke(): Boolean = repository.hasBirthdaysToday()
}
