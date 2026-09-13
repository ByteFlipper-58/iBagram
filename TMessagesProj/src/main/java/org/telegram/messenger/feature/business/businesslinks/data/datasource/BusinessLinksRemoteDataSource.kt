package org.telegram.messenger.feature.business.businesslinks.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source for managing business chat links via MTProto RPC requests.
 */
open class BusinessLinksRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun createLink(inputLink: TL_account.TL_inputBusinessChatLink): Result<TL_account.TL_businessChatLink> {
        val req = TL_account.createBusinessChatLink().apply {
            this.link = inputLink
        }
        return executeRequest<TL_account.TL_businessChatLink>(req)
    }

    open suspend fun editLink(slug: String, inputLink: TL_account.TL_inputBusinessChatLink): Result<TL_account.TL_businessChatLink> {
        val req = TL_account.editBusinessChatLink().apply {
            this.slug = slug
            this.link = inputLink
        }
        return executeRequest<TL_account.TL_businessChatLink>(req)
    }

    open suspend fun deleteLink(slug: String): Result<Unit> {
        val req = TL_account.deleteBusinessChatLink().apply {
            this.slug = slug
        }
        return executeRequest<TLObject>(req).map { }
    }
}
