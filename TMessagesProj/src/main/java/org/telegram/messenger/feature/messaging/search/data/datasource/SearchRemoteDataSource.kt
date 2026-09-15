package org.telegram.messenger.feature.messaging.search.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ChatObject
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.search.data.mapper.SearchMapper
import org.telegram.messenger.feature.messaging.search.domain.model.SearchFilter
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

/**
 * Удаленный источник данных для глобального поиска по Telegram через MTProto.
 */
class SearchRemoteDataSource(
    private val currentAccount: Int
) {

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    private val connectionsManager: ConnectionsManager?
        get() = try {
            if (isLegacyAvailable) ConnectionsManager.getInstance(currentAccount) else null
        } catch (_: Throwable) {
            null
        }

    suspend fun searchGlobal(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Result<List<SearchResultModel>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return Result.Success(emptyList())

        val cm = connectionsManager ?: return Result.Success(emptyList())

        return suspendCancellableCoroutine { cont ->
            val req = TLRPC.TL_contacts_search().apply {
                this.q = trimmedQuery
                this.limit = 30
            }

            val reqId = cm.sendRequest(req, { response, error ->
                if (error != null) {
                    cont.resume(Result.Failure(AppError.Generic("Search error: ${error.text}")))
                } else if (response is TLRPC.TL_contacts_found) {
                    val results = mutableListOf<SearchResultModel>()

                    for (user in response.users) {
                        when (filter) {
                            SearchFilter.ALL -> results.add(SearchMapper.mapUser(user))
                            SearchFilter.USERS -> if (!user.bot) results.add(SearchMapper.mapUser(user))
                            SearchFilter.BOTS -> if (user.bot) results.add(SearchMapper.mapUser(user))
                            else -> {}
                        }
                    }

                    for (chat in response.chats) {
                        val isChannel = ChatObject.isChannel(chat) && !chat.megagroup
                        when (filter) {
                            SearchFilter.ALL -> results.add(SearchMapper.mapChat(chat))
                            SearchFilter.CHANNELS -> if (isChannel) results.add(SearchMapper.mapChat(chat))
                            SearchFilter.GROUPS -> if (!isChannel) results.add(SearchMapper.mapChat(chat))
                            else -> {}
                        }
                    }

                    cont.resume(Result.Success(results))
                } else {
                    cont.resume(Result.Success(emptyList()))
                }
            })

            cont.invokeOnCancellation {
                cm.cancelRequest(reqId, true)
            }
        }
    }
}
