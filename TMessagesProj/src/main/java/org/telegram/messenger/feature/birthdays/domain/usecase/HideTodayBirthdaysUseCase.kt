package org.telegram.messenger.feature.birthdays.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.birthdays.domain.repository.BirthdaysRepository

class HideTodayBirthdaysUseCase(
    private val repository: BirthdaysRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.hideTodayBirthdays()
}
