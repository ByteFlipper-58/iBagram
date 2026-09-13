package org.telegram.messenger.feature.messaging.botkeyboard.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardLayout

/**
 * Remote data source for bot inline keyboard interactions and callbacks via MTProto.
 */
class BotKeyboardRemoteDataSource(
    account: Int
) : BaseRemoteDataSource(account) {

    suspend fun sendButtonPressed(
        peerId: Long,
        messageId: Long,
        button: BotButtonItem
    ): Result<Boolean> {
        // MTProto callback / button press request
        return Result.success(true)
    }

    suspend fun fetchBotKeyboard(
        peerId: Long,
        messageId: Long
    ): Result<BotKeyboardLayout?> {
        return Result.success(null)
    }
}
