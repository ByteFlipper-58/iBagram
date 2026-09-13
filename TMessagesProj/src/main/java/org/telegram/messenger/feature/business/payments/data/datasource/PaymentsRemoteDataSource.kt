package org.telegram.messenger.feature.business.payments.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.StarsController

/**
 * Remote data source for requesting and refreshing Stars balance, transactions, subscriptions, and topup options.
 */
open class PaymentsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchBalance(): Result<Pair<Long, Boolean>> = withContext(Dispatchers.Main) {
        try {
            val controller = StarsController.getInstance(currentAccount)
            val balance = controller.getBalance(false)
            val isAvailable = controller.balanceAvailable()
            Result.Success(Pair(balance, isAvailable))
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch balance", e))
        }
    }

    open suspend fun fetchTransactions(type: Int): Result<List<TL_stars.StarsTransaction>> = withContext(Dispatchers.Main) {
        try {
            val controller = StarsController.getInstance(currentAccount)
            val safeType = if (type in 0..2) type else StarsController.ALL_TRANSACTIONS
            val list = controller.transactions[safeType] ?: emptyList()
            Result.Success(list)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch transactions", e))
        }
    }

    open suspend fun fetchSubscriptions(): Result<List<TL_stars.StarsSubscription>> = withContext(Dispatchers.Main) {
        try {
            val controller = StarsController.getInstance(currentAccount)
            val list = controller.subscriptions ?: emptyList()
            Result.Success(list)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch subscriptions", e))
        }
    }

    open suspend fun fetchTopupOptions(): Result<List<TL_stars.TL_starsTopupOption>> = withContext(Dispatchers.Main) {
        try {
            val controller = StarsController.getInstance(currentAccount)
            val list = controller.options ?: emptyList()
            Result.Success(list)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch topup options", e))
        }
    }

    open suspend fun refreshBalance(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val controller = StarsController.getInstance(currentAccount)
            controller.invalidateBalance()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to refresh balance", e))
        }
    }

    open suspend fun refreshTransactions(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val controller = StarsController.getInstance(currentAccount)
            controller.invalidateTransactions(true)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to refresh transactions", e))
        }
    }

    open suspend fun refreshSubscriptions(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val controller = StarsController.getInstance(currentAccount)
            controller.invalidateSubscriptions(true)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to refresh subscriptions", e))
        }
    }
}
