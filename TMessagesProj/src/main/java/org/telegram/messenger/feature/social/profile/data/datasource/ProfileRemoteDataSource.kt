package org.telegram.messenger.feature.social.profile.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Remote data source managing remote fetch and peer blocklist updates for profiles.
 */
open class ProfileRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun loadFullUser(user: TLRPC.User): Result<Unit> {
        return try {
            MessagesController.getInstance(currentAccount).loadFullUser(user, 0, true)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to load full user ${user.id}", e))
        }
    }

    open suspend fun loadFullChat(chatId: Long): Result<Unit> {
        return try {
            MessagesController.getInstance(currentAccount).loadFullChat(chatId, 0, true)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to load full chat $chatId", e))
        }
    }

    open suspend fun blockPeer(peerId: Long): Result<Unit> {
        return try {
            MessagesController.getInstance(currentAccount).blockPeer(peerId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to block peer $peerId", e))
        }
    }

    open suspend fun unblockPeer(peerId: Long): Result<Unit> {
        return try {
            MessagesController.getInstance(currentAccount).unblockPeer(peerId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to unblock peer $peerId", e))
        }
    }
}
