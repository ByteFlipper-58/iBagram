package org.telegram.messenger.feature.media.fileref.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC

/**
 * Remote data source for MTProto file reference requests.
 */
open class FileRefRemoteDataSource(currentAccount: Int) : BaseRemoteDataSource(currentAccount) {

    open suspend fun sendFileRefRequest(request: TLObject): Result<TLObject> {
        return executeRequest(request)
    }

    open fun cancelRequest(reqId: Int, notifyServer: Boolean = true) {
        try {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, notifyServer)
        } catch (_: Throwable) {
        }
    }
}
