package org.telegram.messenger.feature.business.payments.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTopupOptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarsBalanceModel

interface PaymentsRepository {
    fun observeBalance(): Flow<StarsBalanceModel>
    fun observeTransactions(): Flow<List<StarTransactionModel>>
    fun observeSubscriptions(): Flow<List<StarSubscriptionModel>>

    suspend fun getBalance(): Result<StarsBalanceModel>
    suspend fun getTransactions(type: Int): Result<List<StarTransactionModel>>
    suspend fun getSubscriptions(): Result<List<StarSubscriptionModel>>
    suspend fun getTopupOptions(): Result<List<StarTopupOptionModel>>

    suspend fun refreshBalance(): Result<Unit>
    suspend fun refreshTransactions(): Result<Unit>
    suspend fun refreshSubscriptions(): Result<Unit>
}
