package org.telegram.messenger.feature.messaging.factcheck.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector

/**
 * Remote data source for executing MTProto RPCs related to message fact checking.
 */
open class FactCheckRemoteDataSource(currentAccount: Int) : BaseRemoteDataSource(currentAccount) {

    open suspend fun getFactChecks(peer: TLRPC.InputPeer, msgIds: List<Int>): Result<List<TLRPC.TL_factCheck>> {
        val req = TLRPC.TL_getFactCheck().apply {
            this.peer = peer
            this.msg_id.addAll(msgIds)
        }
        val result: Result<TLObject> = executeRequest(req)
        return when (result) {
            is Result.Success -> {
                val list = mutableListOf<TLRPC.TL_factCheck>()
                val res = result.data
                if (res is Vector<*>) {
                    for (obj in res.objects) {
                        if (obj is TLRPC.TL_factCheck) {
                            list.add(obj)
                        }
                    }
                } else if (res is TLRPC.TL_factCheck) {
                    list.add(res)
                }
                Result.Success(list)
            }
            is Result.Failure -> Result.failure(result.error)
        }
    }

    open suspend fun getFactCheck(peer: TLRPC.InputPeer, msgId: Int): Result<TLRPC.TL_factCheck?> {
        val result = getFactChecks(peer, listOf(msgId))
        return when (result) {
            is Result.Success -> Result.Success(result.data.firstOrNull())
            is Result.Failure -> Result.failure(result.error)
        }
    }

    open suspend fun editFactCheck(
        peer: TLRPC.InputPeer,
        msgId: Int,
        text: TLRPC.TL_textWithEntities
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_editFactCheck().apply {
            this.peer = peer
            this.msg_id = msgId
            this.text = text
        }
        return executeRequest(req)
    }

    open suspend fun deleteFactCheck(
        peer: TLRPC.InputPeer,
        msgId: Int
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_deleteFactCheck().apply {
            this.peer = peer
            this.msg_id = msgId
        }
        return executeRequest(req)
    }
}
