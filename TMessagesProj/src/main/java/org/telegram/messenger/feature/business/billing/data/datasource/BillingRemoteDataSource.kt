package org.telegram.messenger.feature.business.billing.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC

/**
 * Remote data source for MTProto payment and purchase assignment RPCs.
 */
open class BillingRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun assignPlayMarketTransaction(
        receiptJson: String,
        purpose: TLRPC.InputStorePaymentPurpose
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_payments_assignPlayMarketTransaction().apply {
            this.receipt = TLRPC.TL_dataJSON().apply {
                this.data = receiptJson
            }
            this.purpose = purpose
        }
        var flags = ConnectionsManager.RequestFlagFailOnServerErrorsExceptFloodWait or ConnectionsManager.RequestFlagInvokeAfter
        if (purpose is TLRPC.TL_inputStorePaymentAuthCode) {
            flags = flags or ConnectionsManager.RequestFlagWithoutLogin
        }

        return when (val res = executeRequest<TLObject>(req, flags)) {
            is Result.Success -> {
                val data = res.data
                if (data is TLRPC.Updates) {
                    Result.Success(data)
                } else {
                    Result.Failure(AppError.Generic("Invalid updates response"))
                }
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    open suspend fun canPurchasePremium(purpose: TLRPC.InputStorePaymentPurpose): Result<Boolean> {
        return Result.Success(true)
    }
}
