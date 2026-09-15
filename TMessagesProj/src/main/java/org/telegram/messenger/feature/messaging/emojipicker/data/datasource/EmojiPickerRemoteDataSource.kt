package org.telegram.messenger.feature.messaging.emojipicker.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType

class EmojiPickerRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun fetchTrending(tab: EmojiPickerTabType): Result<List<Any>> {
        return Result.Success(emptyList())
    }
}
