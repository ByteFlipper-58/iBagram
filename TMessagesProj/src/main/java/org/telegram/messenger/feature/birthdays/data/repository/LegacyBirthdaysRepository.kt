package org.telegram.messenger.feature.birthdays.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.BirthdayController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.events.NotificationEvent
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.birthdays.data.mapper.BirthdayMapper
import org.telegram.messenger.feature.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.birthdays.domain.repository.BirthdaysRepository

class LegacyBirthdaysRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BirthdaysRepository {

    private val birthdayController: BirthdayController
        get() = BirthdayController.getInstance(currentAccount)

    override fun observeBirthdays(): Flow<BirthdayStateModel?> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.premiumPromoUpdated)
            .onStart {
                emit(NotificationEvent(NotificationCenter.premiumPromoUpdated, currentAccount, emptyArray()))
            }
            .map {
                BirthdayMapper.mapState(birthdayController.state)
            }
            .flowOn(mainDispatcher)
    }

    override suspend fun getBirthdaysState(): BirthdayStateModel? = withContext(mainDispatcher) {
        BirthdayMapper.mapState(birthdayController.state)
    }

    override suspend fun checkBirthdays(force: Boolean): Result<BirthdayStateModel?> =
        withContext(mainDispatcher) {
            try {
                birthdayController.check()
                Result.Success(BirthdayMapper.mapState(birthdayController.state))
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to check birthdays", e)
            }
        }

    override suspend fun hideTodayBirthdays(): Result<Unit> = withContext(mainDispatcher) {
        try {
            birthdayController.hide()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.failure(e.message ?: "Failed to hide today birthdays", e)
        }
    }

    override suspend fun isBirthdayToday(userId: Long): Boolean = withContext(mainDispatcher) {
        birthdayController.isToday(userId)
    }

    override suspend fun hasBirthdaysToday(): Boolean = withContext(mainDispatcher) {
        birthdayController.contains()
    }
}
