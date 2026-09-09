package org.telegram.messenger.feature.billing.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.billing.domain.model.BillingProductType
import org.telegram.messenger.feature.billing.domain.model.BillingPurchaseModel
import org.telegram.messenger.feature.billing.domain.repository.BillingRepository

class QueryBillingPurchasesUseCase(
    private val repository: BillingRepository
) {
    suspend operator fun invoke(productType: BillingProductType): Result<List<BillingPurchaseModel>> {
        return repository.queryPurchases(productType)
    }
}
