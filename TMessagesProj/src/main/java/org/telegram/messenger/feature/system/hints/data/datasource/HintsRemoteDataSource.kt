package org.telegram.messenger.feature.system.hints.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.messenger.feature.system.hints.domain.model.HintsStateModel

/**
 * Remote data source for syncing dismissed feature hints and cloud discovery state via MTProto.
 */
class HintsRemoteDataSource(
    account: Int
) : BaseRemoteDataSource(account) {

    suspend fun syncDismissedHints(hints: List<HintType>): Result<Boolean> {
        // MTProto help.dismissSuggestion or similar cloud dismissal
        return Result.success(true)
    }

    suspend fun fetchHintsState(): Result<HintsStateModel?> {
        return Result.success(null)
    }
}
