package org.telegram.messenger.feature.social.birthdays.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.events.NotificationEvent
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.birthdays.data.datasource.BirthdayLocalDataSource
import org.telegram.messenger.feature.social.birthdays.data.datasource.BirthdayRemoteDataSource
import org.telegram.messenger.feature.social.birthdays.data.mapper.BirthdayMapper
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository

/**
 * Clean repository implementation coordinating local and remote data sources for birthdays operations,
 * strangling legacy monolithic logic in BirthdayController.
 */
class BirthdaysRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BirthdayLocalDataSource,
    private val remoteDataSource: BirthdayRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BirthdaysRepository {

    override fun observeBirthdays(): Flow<BirthdayStateModel?> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.premiumPromoUpdated)
            .onStart {
                emit(NotificationEvent(NotificationCenter.premiumPromoUpdated, currentAccount, emptyArray()))
            }
            .map {
                BirthdayMapper.mapState(localDataSource.getBirthdaysState())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getBirthdaysState(): BirthdayStateModel? = withContext(ioDispatcher) {
        BirthdayMapper.mapState(localDataSource.getBirthdaysState())
    }

    override suspend fun checkBirthdays(force: Boolean): Result<BirthdayStateModel?> = withContext(ioDispatcher) {
        try {
            if (!localDataSource.shouldCheckBirthdays(force)) {
                return@withContext Result.Success(BirthdayMapper.mapState(localDataSource.getBirthdaysState()))
            }
            when (val remoteResult = remoteDataSource.getBirthdays()) {
                is Result.Success -> {
                    localDataSource.saveBirthdays(remoteResult.data)
                    Result.Success(BirthdayMapper.mapState(localDataSource.getBirthdaysState()))
                }
                is Result.Failure -> Result.Failure(remoteResult.error)
            }
        } catch (e: Exception) {
            Result.failure(e.message ?: "Failed to check birthdays", e)
        }
    }

    override suspend fun hideTodayBirthdays(): Result<Unit> = withContext(ioDispatcher) {
        try {
            localDataSource.hideTodayBirthdays()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.failure(e.message ?: "Failed to hide today birthdays", e)
        }
    }

    override suspend fun isBirthdayToday(userId: Long): Boolean = withContext(ioDispatcher) {
        localDataSource.isToday(userId)
    }

    override suspend fun hasBirthdaysToday(): Boolean = withContext(ioDispatcher) {
        localDataSource.hasBirthdaysToday()
    }
}
