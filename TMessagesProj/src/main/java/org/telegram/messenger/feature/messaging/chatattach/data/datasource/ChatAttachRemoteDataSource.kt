package org.telegram.messenger.feature.messaging.chatattach.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType

class ChatAttachRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun getRemoteAttachmentLimit(layoutType: ChatAttachLayoutType): Result<Int> {
        return Result.Success(100)
    }
}
