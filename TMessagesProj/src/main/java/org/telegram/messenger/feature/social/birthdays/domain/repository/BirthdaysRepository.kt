package org.telegram.messenger.feature.social.birthdays.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayStateModel

interface BirthdaysRepository {
    fun observeBirthdays(): Flow<BirthdayStateModel?>
    suspend fun getBirthdaysState(): BirthdayStateModel?
    suspend fun checkBirthdays(force: Boolean = false): Result<BirthdayStateModel?>
    suspend fun hideTodayBirthdays(): Result<Unit>
    suspend fun isBirthdayToday(userId: Long): Boolean
    suspend fun hasBirthdaysToday(): Boolean
}
