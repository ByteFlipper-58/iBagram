package org.telegram.messenger.feature.system.keyboardhide.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

open class KeyboardHideRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun syncKeyboardHideConfig(): Result<Unit> {
        return Result.success(Unit)
    }
}
