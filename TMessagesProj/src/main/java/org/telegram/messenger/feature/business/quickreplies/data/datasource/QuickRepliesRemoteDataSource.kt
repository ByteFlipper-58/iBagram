package org.telegram.messenger.feature.business.quickreplies.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC

class QuickRepliesRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun loadQuickReplies(): Result<Unit> {
        val req = TLRPC.TL_messages_getQuickReplies().apply {
            hash = 0
        }
        return try {
            when (val res = executeRequest<TLObject>(req)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Failure -> Result.Failure(res.error)
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Network("Failed to load quick replies: ${e.message}"))
        }
    }

    suspend fun renameReply(id: Int, newName: String): Result<Unit> {
        val req = TLRPC.TL_messages_editQuickReplyShortcut().apply {
            shortcut_id = id
            shortcut = newName
        }
        return try {
            when (val res = executeRequest<TLObject>(req)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Failure -> Result.Failure(res.error)
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Network("Failed to rename quick reply: ${e.message}"))
        }
    }

    suspend fun reorderReplies(ids: List<Int>): Result<Unit> {
        val req = TLRPC.TL_messages_reorderQuickReplies().apply {
            order.addAll(ids)
        }
        return try {
            when (val res = executeRequest<TLObject>(req)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Failure -> Result.Failure(res.error)
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Network("Failed to reorder quick replies: ${e.message}"))
        }
    }

    suspend fun deleteReply(id: Int): Result<Unit> {
        val req = TLRPC.TL_messages_deleteQuickReplyShortcut().apply {
            shortcut_id = id
        }
        return try {
            when (val res = executeRequest<TLObject>(req)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Failure -> Result.Failure(res.error)
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Network("Failed to delete quick reply: ${e.message}"))
        }
    }
}
