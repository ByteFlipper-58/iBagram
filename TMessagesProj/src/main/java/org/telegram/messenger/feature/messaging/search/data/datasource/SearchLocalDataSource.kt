package org.telegram.messenger.feature.messaging.search.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ContactsController
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.search.data.mapper.SearchMapper
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel

/**
 * Локальный источник данных для поиска по контактам, диалогам, кэшу недавних поисков и хэштегов.
 */
class SearchLocalDataSource(
    private val currentAccount: Int
) {

    private val lock = Any()
    private val _recentSearches = MutableStateFlow<List<SearchResultModel>>(emptyList())
    val recentSearches: StateFlow<List<SearchResultModel>> = _recentSearches.asStateFlow()

    private val _recentHashtags = MutableStateFlow<List<String>>(emptyList())
    val recentHashtags: StateFlow<List<String>> = _recentHashtags.asStateFlow()

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    fun searchLocal(query: String): Result<List<SearchResultModel>> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return Result.Success(emptyList())

        if (!isLegacyAvailable) {
            // Headless / unit test mode: search through in-memory recent searches
            val matched = _recentSearches.value.filter {
                it.title.lowercase().contains(q) || it.username?.lowercase()?.contains(q) == true
            }
            return Result.Success(matched)
        }

        return try {
            val messagesController = MessagesController.getInstance(currentAccount)
            val contactsController = ContactsController.getInstance(currentAccount)
            val results = mutableListOf<SearchResultModel>()
            val addedIds = mutableSetOf<Long>()

            val contacts = contactsController?.contacts
            if (contacts != null) {
                for (contact in contacts) {
                    val user = messagesController?.getUser(contact.user_id) ?: continue
                    val fullName = ContactsController.formatName(user.first_name, user.last_name).lowercase()
                    val username = (user.username ?: "").lowercase()
                    if (fullName.contains(q) || username.contains(q)) {
                        if (addedIds.add(user.id)) {
                            results.add(SearchMapper.mapUser(user))
                        }
                    }
                }
            }

            val dialogs = messagesController?.allDialogs
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
        } catch (e: Throwable) {
            Result.Failure(org.telegram.messenger.core.result.AppError.Generic(e.message ?: "Failed to search local dialogs", e))
        }
    }

    fun getRecentSearches(): Result<List<SearchResultModel>> = synchronized(lock) {
        Result.Success(_recentSearches.value)
    }

    fun setRecentSearches(searches: List<SearchResultModel>) = synchronized(lock) {
        _recentSearches.value = searches
    }

    fun addRecentSearch(item: SearchResultModel) = synchronized(lock) {
        val filtered = _recentSearches.value.filter { it.id != item.id }
        _recentSearches.value = listOf(item) + filtered
    }

    fun clearRecentSearches(): Result<Unit> = synchronized(lock) {
        _recentSearches.value = emptyList()
        Result.Success(Unit)
    }

    fun removeRecentSearch(id: Long): Result<Unit> = synchronized(lock) {
        _recentSearches.value = _recentSearches.value.filter { it.id != id }
        Result.Success(Unit)
    }

    fun getRecentHashtags(): Result<List<String>> = synchronized(lock) {
        Result.Success(_recentHashtags.value)
    }

    fun setRecentHashtags(hashtags: List<String>) = synchronized(lock) {
        _recentHashtags.value = hashtags
    }

    fun putRecentHashtag(hashtag: String): Result<Unit> = synchronized(lock) {
        val clean = if (hashtag.startsWith("#") || hashtag.startsWith("$")) hashtag else "#$hashtag"
        val filtered = _recentHashtags.value.filter { !it.equals(clean, ignoreCase = true) }
        _recentHashtags.value = listOf(clean) + filtered
        Result.Success(Unit)
    }

    fun clearRecentHashtags(): Result<Unit> = synchronized(lock) {
        _recentHashtags.value = emptyList()
        Result.Success(Unit)
    }
}
