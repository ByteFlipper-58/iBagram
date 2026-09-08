package org.telegram.messenger.feature.birthdays.domain.usecase

import org.telegram.messenger.feature.birthdays.domain.repository.BirthdaysRepository

class HasBirthdaysTodayUseCase(
    private val repository: BirthdaysRepository
) {
    suspend operator fun invoke(): Boolean = repository.hasBirthdaysToday()
}
