package org.telegram.messenger.feature.business.timezones.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Remote data source for fetching available business timezones via MTProto RPC.
 */
open class TimezonesRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun loadTimezones(hash: Int = 0): Result<List<TLRPC.TL_timezone>> {
        val req = TLRPC.TL_help_getTimezonesList().apply {
            this.hash = hash
        }
        val result = executeRequest<TLRPC.TL_help_timezonesList>(req)
        return result.map { it.timezones ?: emptyList() }
    }
}
