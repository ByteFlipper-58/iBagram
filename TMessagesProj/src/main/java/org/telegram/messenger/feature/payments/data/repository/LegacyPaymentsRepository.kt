package org.telegram.messenger.feature.payments.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.payments.data.mapper.PaymentMapper
import org.telegram.messenger.feature.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.payments.domain.model.StarTopupOptionModel
import org.telegram.messenger.feature.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.payments.domain.model.StarsBalanceModel
import org.telegram.messenger.feature.payments.domain.repository.PaymentsRepository
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.StarsController

class LegacyPaymentsRepository(
    private val currentAccount: Int
) : PaymentsRepository {

    private val starsController: StarsController
        get() = StarsController.getInstance(currentAccount)

    override fun observeBalance(): Flow<StarsBalanceModel> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.starBalanceUpdated
        )
            .map { Unit }
            .onStart { emit(Unit) }
            .map {
                withContext(Dispatchers.Main) {
                    PaymentMapper.mapBalance(
                        balance = starsController.getBalance(false),
                        isAvailable = starsController.balanceAvailable()
                    )
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeTransactions(): Flow<List<StarTransactionModel>> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.starTransactionsLoaded
        )
            .map { Unit }
            .onStart { emit(Unit) }
            .map {
                withContext(Dispatchers.Main) {
                    val list = starsController.transactions[StarsController.ALL_TRANSACTIONS] ?: emptyList<TL_stars.StarsTransaction>()
                    list.map { PaymentMapper.mapTransaction(it) }
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeSubscriptions(): Flow<List<StarSubscriptionModel>> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.starSubscriptionsLoaded
        )
            .map { Unit }
            .onStart { emit(Unit) }
            .map {
                withContext(Dispatchers.Main) {
                    val list = starsController.subscriptions ?: emptyList<TL_stars.StarsSubscription>()
                    list.map { PaymentMapper.mapSubscription(it) }
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override suspend fun getBalance(): Result<StarsBalanceModel> {
        return withContext(Dispatchers.Main) {
            try {
                val balance = starsController.getBalance(false)
                val isAvailable = starsController.balanceAvailable()
                Result.Success(PaymentMapper.mapBalance(balance, isAvailable))
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to get stars balance", e))
            }
        }
    }

    override suspend fun getTransactions(type: Int): Result<List<StarTransactionModel>> {
        return withContext(Dispatchers.Main) {
            try {
                val safeType = if (type in 0..2) type else StarsController.ALL_TRANSACTIONS
                val list = starsController.transactions[safeType] ?: emptyList<TL_stars.StarsTransaction>()
                Result.Success(list.map { PaymentMapper.mapTransaction(it) })
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to get transactions for type $type", e))
            }
        }
    }

    override suspend fun getSubscriptions(): Result<List<StarSubscriptionModel>> {
        return withContext(Dispatchers.Main) {
            try {
                val list = starsController.subscriptions ?: emptyList<TL_stars.StarsSubscription>()
                Result.Success(list.map { PaymentMapper.mapSubscription(it) })
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to get subscriptions", e))
            }
        }
    }

    override suspend fun getTopupOptions(): Result<List<StarTopupOptionModel>> {
        return withContext(Dispatchers.Main) {
            try {
                val list = starsController.options ?: emptyList<TL_stars.TL_starsTopupOption>()
                Result.Success(list.map { PaymentMapper.mapTopupOption(it) })
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to get topup options", e))
            }
        }
    }

    override suspend fun refreshBalance(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                starsController.invalidateBalance()
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to refresh balance", e))
            }
        }
    }

    override suspend fun refreshTransactions(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                starsController.invalidateTransactions(true)
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to refresh transactions", e))
            }
        }
    }

    override suspend fun refreshSubscriptions(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                starsController.invalidateSubscriptions(true)
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to refresh subscriptions", e))
            }
        }
    }
}
