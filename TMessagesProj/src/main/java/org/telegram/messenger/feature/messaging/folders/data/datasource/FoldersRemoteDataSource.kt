package org.telegram.messenger.feature.messaging.folders.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector

/**
 * Remote data source for Chat Folders (Dialog Filters) operations via MTProto RPC requests.
 */
open class FoldersRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches dialog filters from MTProto server.
     */
    open suspend fun getDialogFilters(): Result<TLObject> {
        val req = TLRPC.TL_messages_getDialogFilters()
        return executeRequest(req)
    }

    /**
     * Updates or deletes a dialog filter on MTProto server.
     * When [filter] is null, the filter with the specified [id] is deleted on the server.
     */
    open suspend fun updateDialogFilter(
        id: Int,
        filter: TLRPC.TL_dialogFilter?
    ): Result<Boolean> {
        val req = TLRPC.TL_messages_updateDialogFilter().apply {
            this.id = id
            if (filter != null) {
                this.flags = this.flags or 1
                this.filter = filter
            }
        }
        return executeRequest<TLRPC.Bool>(req).map { it is TLRPC.TL_boolTrue }
    }

    /**
     * Updates the ordering of dialog filters on MTProto server.
     */
    open suspend fun updateDialogFiltersOrder(order: List<Int>): Result<Boolean> {
        val req = TLRPC.TL_messages_updateDialogFiltersOrder().apply {
            this.order.addAll(order)
        }
        return executeRequest<TLRPC.Bool>(req).map { it is TLRPC.TL_boolTrue }
    }

    /**
     * Fetches suggested dialog filters from MTProto server.
     */
    open suspend fun getSuggestedDialogFilters(): Result<Vector<TLRPC.TL_dialogFilterSuggested>> {
        val req = TLRPC.TL_messages_getSuggestedDialogFilters()
        return executeRequest(req)
    }
}
