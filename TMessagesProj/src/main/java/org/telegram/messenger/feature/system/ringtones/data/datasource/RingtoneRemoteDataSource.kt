package org.telegram.messenger.feature.system.ringtones.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source managing cloud-synced custom notification ringtones via MTProto.
 */
open class RingtoneRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchRemoteRingtones(hash: Long = 0L): Result<Boolean> {
        return try {
            val req = TL_account.getSavedRingtones()
            req.hash = hash
            val result = executeRequest<TL_account.SavedRingtones>(req)
            if (result.isSuccess) {
                Result.Success(true)
            } else {
                Result.Success(false)
            }
        } catch (_: Throwable) {
            Result.Success(false)
        }
    }

    open suspend fun saveRemoteRingtone(
        documentId: Long,
        accessHash: Long,
        fileReference: ByteArray? = null,
        unsave: Boolean = false
    ): Result<Boolean> {
        return try {
            val req = TL_account.saveRingtone()
            req.unsave = unsave
            val inputDoc = TLRPC.TL_inputDocument()
            inputDoc.id = documentId
            inputDoc.access_hash = accessHash
            inputDoc.file_reference = fileReference ?: ByteArray(0)
            req.id = inputDoc
            val result = executeRequest<TL_account.SavedRingtone>(req)
            if (result.isSuccess) {
                Result.Success(true)
            } else {
                Result.Success(false)
            }
        } catch (_: Throwable) {
            Result.Success(false)
        }
    }
}
