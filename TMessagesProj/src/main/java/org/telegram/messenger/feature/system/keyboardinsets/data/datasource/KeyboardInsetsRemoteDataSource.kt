package org.telegram.messenger.feature.system.keyboardinsets.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

open class KeyboardInsetsRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun syncKeyboardInsetsConfig(): Result<Unit> {
        return Result.success(Unit)
    }
}
