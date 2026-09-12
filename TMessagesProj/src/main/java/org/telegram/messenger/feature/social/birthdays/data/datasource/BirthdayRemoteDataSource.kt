package org.telegram.messenger.feature.social.birthdays.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source executing MTProto RPC requests for Telegram contact birthdays.
 */
open class BirthdayRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches contact birthdays from MTProto server.
     */
    open suspend fun getBirthdays(): Result<TL_account.contactBirthdays> {
        val req = TL_account.getBirthdays()
        return executeRequest(req)
    }
}
