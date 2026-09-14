package org.telegram.messenger.feature.system.recyclerscroll.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

open class RecyclerScrollRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun syncRecyclerScrollConfig(): Result<Unit> {
        return Result.success(Unit)
    }
}
