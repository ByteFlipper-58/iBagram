package org.telegram.messenger.feature.messaging.mentions.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

/**
 * Удаленный источник данных для автодополнения упоминаний и inline-ботов.
 */
class MentionsRemoteDataSource(
    private val currentAccount: Int
) {

    private val connectionsManager: ConnectionsManager?
        get() = try {
            ConnectionsManager.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val messagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    suspend fun searchRemoteUsers(query: String, limit: Int = 20): Result<List<TLRPC.User>> = withContext(Dispatchers.Main) {
        val cm = connectionsManager ?: return@withContext Result.failure(AppError.Network("ConnectionsManager not available"))

        val req = TLRPC.TL_contacts_search().apply {
            q = query
            this.limit = limit
        }

        suspendCancellableCoroutine<Result<List<TLRPC.User>>> { continuation ->
            val reqId = cm.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(Result.failure(AppError.Network("Failed to search contacts: ${error.text}")))
                } else if (response is TLRPC.TL_contacts_found) {
                    continuation.resume(Result.success(response.users ?: emptyList()))
                } else {
                    continuation.resume(Result.success(emptyList()))
                }
            })

            continuation.invokeOnCancellation {
                cm.cancelRequest(reqId, true)
            }
        }
    }

    suspend fun queryInlineBot(botUsername: String, query: String, offset: String = ""): Result<TLRPC.messages_BotResults> = withContext(Dispatchers.Main) {
        val cm = connectionsManager ?: return@withContext Result.failure(AppError.Network("ConnectionsManager not available"))
        val mc = messagesController ?: return@withContext Result.failure(AppError.NotFound("MessagesController not available"))

        val user = mc.getUserOrChat(botUsername) as? TLRPC.User
        val inputUser = if (user != null) {
            mc.getInputUser(user)
        } else {
            TLRPC.TL_inputUser().apply {
                user_id = 0L
                access_hash = 0L
            }
        }

        val req = TLRPC.TL_messages_getInlineBotResults().apply {
            bot = inputUser
            this.query = query
            this.offset = offset
            peer = TLRPC.TL_inputPeerEmpty()
        }

        suspendCancellableCoroutine<Result<TLRPC.messages_BotResults>> { continuation ->
            val reqId = cm.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(Result.failure(AppError.Network("Failed to query inline bot: ${error.text}")))
                } else if (response is TLRPC.messages_BotResults) {
                    continuation.resume(Result.success(response))
                } else {
                    continuation.resume(Result.failure(AppError.Network("Unknown response for inline bot query")))
                }
            })

            continuation.invokeOnCancellation {
                cm.cancelRequest(reqId, true)
            }
        }
    }
}
