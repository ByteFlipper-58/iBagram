package org.telegram.messenger.feature.messaging.search.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ChatObject
import org.telegram.messenger.ContactsController
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLog
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.search.data.mapper.SearchMapper
import org.telegram.messenger.feature.messaging.search.domain.model.SearchFilter
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.messaging.search.domain.repository.SearchRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Adapters.DialogsSearchAdapter
import kotlin.coroutines.resume

class LegacySearchRepository(
    private val currentAccount: Int
) : SearchRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val messagesStorage: MessagesStorage
        get() = MessagesStorage.getInstance(currentAccount)

    private val contactsController: ContactsController
        get() = ContactsController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    override suspend fun searchGlobal(
        query: String,
        filter: SearchFilter
    ): Result<List<SearchResultModel>> = withContext(Dispatchers.Main) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext Result.Success(emptyList())
        }

        suspendCancellableCoroutine { cont ->
            val req = TLRPC.TL_contacts_search().apply {
                this.q = trimmedQuery
                this.limit = 30
            }

            val reqId = connectionsManager.sendRequest(req, { response, error ->
                if (error != null) {
                    cont.resume(Result.Failure(AppError.Generic("Search error: ${error.text}")))
                } else if (response is TLRPC.TL_contacts_found) {
                    AndroidUtilities.runOnUIThread {
                        messagesController.putChats(response.chats, false)
                        messagesController.putUsers(response.users, false)
                        messagesStorage.putUsersAndChats(response.users, response.chats, true, true)

                        val results = mutableListOf<SearchResultModel>()

                        // Users / Bots
                        for (user in response.users) {
                            when (filter) {
                                SearchFilter.ALL -> results.add(SearchMapper.mapUser(user))
                                SearchFilter.USERS -> if (!user.bot) results.add(SearchMapper.mapUser(user))
                                SearchFilter.BOTS -> if (user.bot) results.add(SearchMapper.mapUser(user))
                                else -> {}
                            }
                        }

                        // Channels / Groups
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
                    }
                } else {
                    cont.resume(Result.Success(emptyList()))
                }
            })

            cont.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }
    }

    override suspend fun searchLocal(query: String): Result<List<SearchResultModel>> = withContext(Dispatchers.Main) {
        try {
            val q = query.trim().lowercase()
            if (q.isEmpty()) {
                return@withContext Result.Success(emptyList())
            }

            val results = mutableListOf<SearchResultModel>()
            val addedIds = mutableSetOf<Long>()

            // 1. Search in cached contacts
            val contacts = contactsController.contacts
            if (contacts != null) {
                for (contact in contacts) {
                    val user = messagesController.getUser(contact.user_id) ?: continue
                    val fullName = ContactsController.formatName(user.first_name, user.last_name).lowercase()
                    val username = (user.username ?: "").lowercase()
                    if (fullName.contains(q) || username.contains(q)) {
                        if (addedIds.add(user.id)) {
                            results.add(SearchMapper.mapUser(user))
                        }
                    }
                }
            }

            // 2. Search in loaded dialogs
            val dialogs = messagesController.allDialogs
            if (dialogs != null) {
                for (dialog in dialogs) {
                    val did = dialog.id
                    if (DialogObject.isUserDialog(did)) {
                        val user = messagesController.getUser(did) ?: continue
                        val fullName = ContactsController.formatName(user.first_name, user.last_name).lowercase()
                        val username = (user.username ?: "").lowercase()
                        if (fullName.contains(q) || username.contains(q)) {
                            if (addedIds.add(user.id)) {
                                results.add(SearchMapper.mapUser(user))
                            }
                        }
                    } else if (DialogObject.isChatDialog(did)) {
                        val chat = messagesController.getChat(-did) ?: continue
                        val title = (chat.title ?: "").lowercase()
                        val username = (chat.username ?: "").lowercase()
                        if (title.contains(q) || username.contains(q)) {
                            if (addedIds.add(-chat.id)) {
                                results.add(SearchMapper.mapChat(chat))
                            }
                        }
                    }
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to search local dialogs", e))
        }
    }

    override suspend fun getRecentSearches(): Result<List<SearchResultModel>> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            DialogsSearchAdapter.loadRecentSearch(currentAccount, 0) { arrayList, _ ->
                val models = mutableListOf<SearchResultModel>()
                if (arrayList != null) {
                    for (obj in arrayList) {
                        when (val target = obj.`object`) {
                            is TLRPC.User -> models.add(SearchMapper.mapUser(target))
                            is TLRPC.Chat -> models.add(SearchMapper.mapChat(target))
                            is TLRPC.EncryptedChat -> {
                                val user = messagesController.getUser(target.user_id.toLong())
                                if (user != null) {
                                    models.add(SearchMapper.mapUser(user))
                                }
                            }
                        }
                    }
                }
                cont.resume(Result.Success(models))
            }
        }
    }

    override suspend fun clearRecentSearches(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messagesStorage.storageQueue.postRunnable {
                try {
                    messagesStorage.database.executeFast("DELETE FROM search_recent WHERE 1").stepThis().dispose()
                } catch (e: Exception) {
                    FileLog.e(e)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to clear recent searches", e))
        }
    }

    override suspend fun removeRecentSearch(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messagesStorage.storageQueue.postRunnable {
                try {
                    messagesStorage.database.executeFast("DELETE FROM search_recent WHERE did = $id").stepThis().dispose()
                } catch (e: Exception) {
                    FileLog.e(e)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to remove recent search", e))
        }
    }

    override suspend fun getRecentHashtags(): Result<List<String>> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            messagesStorage.storageQueue.postRunnable {
                try {
                    val cursor = messagesStorage.database.queryFinalized("SELECT id, date FROM hashtag_recent_v2 WHERE 1")
                    val list = mutableListOf<Pair<String, Int>>()
                    while (cursor.next()) {
                        val tag = cursor.stringValue(0)
                        val date = cursor.intValue(1)
                        if (!tag.isNullOrEmpty()) {
                            list.add(Pair(tag, date))
                        }
                    }
                    cursor.dispose()
                    list.sortByDescending { it.second }
                    val sortedTags = list.map { it.first }
                    AndroidUtilities.runOnUIThread {
                        cont.resume(Result.Success(sortedTags))
                    }
                } catch (e: Exception) {
                    AndroidUtilities.runOnUIThread {
                        cont.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to load recent hashtags", e)))
                    }
                }
            }
        }
    }

    override suspend fun putRecentHashtag(hashtag: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messagesStorage.storageQueue.postRunnable {
                try {
                    val state = messagesStorage.database.executeFast("REPLACE INTO hashtag_recent_v2 VALUES(?, ?)")
                    state.requery()
                    state.bindString(1, hashtag)
                    state.bindInteger(2, (System.currentTimeMillis() / 1000).toInt())
                    state.step()
                    state.dispose()
                } catch (e: Exception) {
                    FileLog.e(e)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to put recent hashtag", e))
        }
    }

    override suspend fun clearRecentHashtags(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messagesStorage.storageQueue.postRunnable {
                try {
                    messagesStorage.database.executeFast("DELETE FROM hashtag_recent_v2 WHERE 1").stepThis().dispose()
                } catch (e: Exception) {
                    FileLog.e(e)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to clear recent hashtags", e))
        }
    }
}
