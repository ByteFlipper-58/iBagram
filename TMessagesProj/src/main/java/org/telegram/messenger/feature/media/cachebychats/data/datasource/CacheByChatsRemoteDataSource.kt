package org.telegram.messenger.feature.media.cachebychats.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject

/**
 * Remote data source for cache retention settings synchronization over MTProto.
 */
open class CacheByChatsRemoteDataSource(currentAccount: Int) : BaseRemoteDataSource(currentAccount) {

    open suspend fun syncCacheSettings(request: TLObject): Result<TLObject> {
        return executeRequest(request)
    }
}
