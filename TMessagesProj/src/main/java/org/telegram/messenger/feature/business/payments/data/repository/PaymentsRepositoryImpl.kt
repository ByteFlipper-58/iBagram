package org.telegram.messenger.feature.business.payments.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.payments.data.datasource.PaymentsLocalDataSource
import org.telegram.messenger.feature.business.payments.data.datasource.PaymentsRemoteDataSource
import org.telegram.messenger.feature.business.payments.data.mapper.PaymentMapper
import org.telegram.messenger.feature.business.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTopupOptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarsBalanceModel
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository

/**
 * Repository implementation coordinating PaymentsLocalDataSource and PaymentsRemoteDataSource.
 */
class PaymentsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: PaymentsLocalDataSource,
    private val remoteDataSource: PaymentsRemoteDataSource,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PaymentsRepository {

    override fun observeBalance(): Flow<StarsBalanceModel> {
        return localDataSource.observeBalanceUpdated()
            .map {
                val balance = localDataSource.getBalance()
                val isAvailable = localDataSource.isBalanceAvailable()
                PaymentMapper.mapBalance(balance, isAvailable)
            }
            .onStart {
                val balance = localDataSource.getBalance()
                val isAvailable = localDataSource.isBalanceAvailable()
                emit(PaymentMapper.mapBalance(balance, isAvailable))
            }
            .flowOn(defaultDispatcher)
    }

    override fun observeTransactions(): Flow<List<StarTransactionModel>> {
        return localDataSource.observeTransactionsUpdated()
            .map {
                val list = localDataSource.getTransactions(0)
                list.map { PaymentMapper.mapTransaction(it) }
            }
            .onStart {
                val list = localDataSource.getTransactions(0)
                emit(list.map { PaymentMapper.mapTransaction(it) })
            }
            .flowOn(defaultDispatcher)
    }

    override fun observeSubscriptions(): Flow<List<StarSubscriptionModel>> {
        return localDataSource.observeSubscriptionsUpdated()
            .map {
                val list = localDataSource.getSubscriptions()
                list.map { PaymentMapper.mapSubscription(it) }
            }
            .onStart {
                val list = localDataSource.getSubscriptions()
                emit(list.map { PaymentMapper.mapSubscription(it) })
            }
            .flowOn(defaultDispatcher)
    }

    override suspend fun getBalance(): Result<StarsBalanceModel> = withContext(defaultDispatcher) {
        if (localDataSource.isBalanceAvailable()) {
            return@withContext Result.Success(
                PaymentMapper.mapBalance(
                    balance = localDataSource.getBalance(),
                    isAvailable = true
                )
            )
        }

        when (val res = remoteDataSource.fetchBalance()) {
            is Result.Success -> {
                val (balance, isAvailable) = res.data
                localDataSource.saveBalance(balance, isAvailable)
                Result.Success(PaymentMapper.mapBalance(balance, isAvailable))
            }
            is Result.Failure -> {
                Result.Success(
                    PaymentMapper.mapBalance(
                        balance = localDataSource.getBalance(),
                        isAvailable = localDataSource.isBalanceAvailable()
                    )
                )
            }
        }
    }

    override suspend fun getTransactions(type: Int): Result<List<StarTransactionModel>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getTransactions(type)
        if (cached.isNotEmpty()) {
            return@withContext Result.Success(cached.map { PaymentMapper.mapTransaction(it) })
        }

        when (val res = remoteDataSource.fetchTransactions(type)) {
            is Result.Success -> {
                localDataSource.saveTransactions(type, res.data)
                Result.Success(res.data.map { PaymentMapper.mapTransaction(it) })
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(cached.map { PaymentMapper.mapTransaction(it) })
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun getSubscriptions(): Result<List<StarSubscriptionModel>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getSubscriptions()
        if (cached.isNotEmpty()) {
            return@withContext Result.Success(cached.map { PaymentMapper.mapSubscription(it) })
        }

        when (val res = remoteDataSource.fetchSubscriptions()) {
            is Result.Success -> {
                localDataSource.saveSubscriptions(res.data)
                Result.Success(res.data.map { PaymentMapper.mapSubscription(it) })
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(cached.map { PaymentMapper.mapSubscription(it) })
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun getTopupOptions(): Result<List<StarTopupOptionModel>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getTopupOptions()
        if (cached.isNotEmpty()) {
            return@withContext Result.Success(cached.map { PaymentMapper.mapTopupOption(it) })
        }

        when (val res = remoteDataSource.fetchTopupOptions()) {
            is Result.Success -> {
                localDataSource.saveTopupOptions(res.data)
                Result.Success(res.data.map { PaymentMapper.mapTopupOption(it) })
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(cached.map { PaymentMapper.mapTopupOption(it) })
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun refreshBalance(): Result<Unit> = withContext(defaultDispatcher) {
        remoteDataSource.refreshBalance()
    }

    override suspend fun refreshTransactions(): Result<Unit> = withContext(defaultDispatcher) {
        remoteDataSource.refreshTransactions()
    }

    override suspend fun refreshSubscriptions(): Result<Unit> = withContext(defaultDispatcher) {
        remoteDataSource.refreshSubscriptions()
    }
}
