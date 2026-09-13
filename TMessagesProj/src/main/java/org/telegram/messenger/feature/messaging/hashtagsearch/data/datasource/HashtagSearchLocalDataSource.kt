package org.telegram.messenger.feature.messaging.hashtagsearch.data.datasource

import android.app.Activity
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.feature.messaging.hashtagsearch.data.mapper.HashtagMapper
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Local data source managing hashtag history preferences and in-memory search states.
 */
open class HashtagSearchLocalDataSource(
    private val currentAccount: Int
) {

    private val _history = CopyOnWriteArrayList<String>()
    val history: List<String> get() = _history.toList()

    private val _historyFlow = MutableStateFlow<List<String>>(emptyList())
    val historyFlow: StateFlow<List<String>> = _historyFlow.asStateFlow()

    private val _myMessagesResult = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.MY_MESSAGES))
    val myMessagesResult: StateFlow<HashtagSearchResultModel> = _myMessagesResult.asStateFlow()

    private val _publicPostsResult = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.PUBLIC_POSTS))
    val publicPostsResult: StateFlow<HashtagSearchResultModel> = _publicPostsResult.asStateFlow()

    private val _channelPostsResult = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.CHANNEL_POSTS))
    val channelPostsResult: StateFlow<HashtagSearchResultModel> = _channelPostsResult.asStateFlow()

    private val historyPreferences: SharedPreferences? by lazy {
        try {
            ApplicationLoader.applicationContext?.getSharedPreferences(
                "hashtag_search_history$currentAccount",
                Activity.MODE_PRIVATE
            )
        } catch (_: Throwable) {
            null
        }
    }

    init {
        loadHistoryFromPref()
    }

    fun loadHistoryFromPref() {
        val prefs = historyPreferences
        if (prefs != null) {
            try {
                val count = prefs.getInt("count", 0)
                val list = ArrayList<String>(count)
                for (i in 0 until count) {
                    var value = prefs.getString("e_$i", "") ?: ""
                    if (value.isNotEmpty()) {
                        if (!value.startsWith("#") && !value.startsWith("$")) {
                            value = "#$value"
                        }
                        list.add(value)
                    }
                }
                _history.clear()
                _history.addAll(list)
                _historyFlow.value = _history.toList()
                return
            } catch (_: Throwable) {
            }
        }
        _historyFlow.value = _history.toList()
    }

    private fun saveHistoryToPref() {
        val prefs = historyPreferences ?: return
        try {
            val editor = prefs.edit()
            editor.clear()
            editor.putInt("count", _history.size)
            for (i in _history.indices) {
                editor.putString("e_$i", _history[i])
            }
            editor.apply()
        } catch (_: Throwable) {
        }
    }

    open fun putToHistory(hashtag: String) {
        val normalized = HashtagMapper.normalizeHashtag(hashtag)
        if (normalized.isEmpty()) return

        val index = _history.indexOf(normalized)
        if (index != -1) {
            if (index == 0) return
            _history.removeAt(index)
        }
        _history.add(0, normalized)

        if (_history.size > 100) {
            while (_history.size > 100) {
                _history.removeAt(_history.size - 1)
            }
        }
        saveHistoryToPref()
        _historyFlow.value = _history.toList()
    }

    open fun removeFromHistory(hashtag: String) {
        val index = _history.indexOf(hashtag)
        if (index != -1) {
            _history.removeAt(index)
            saveHistoryToPref()
            _historyFlow.value = _history.toList()
        }
    }

    open fun clearHistory() {
        _history.clear()
        saveHistoryToPref()
        _historyFlow.value = emptyList()
    }

    open fun getSearchResult(searchType: HashtagSearchType): HashtagSearchResultModel {
        return when (searchType) {
            HashtagSearchType.MY_MESSAGES -> _myMessagesResult.value
            HashtagSearchType.PUBLIC_POSTS -> _publicPostsResult.value
            HashtagSearchType.CHANNEL_POSTS -> _channelPostsResult.value
        }
    }

    open fun updateSearchResult(searchType: HashtagSearchType, model: HashtagSearchResultModel) {
        when (searchType) {
            HashtagSearchType.MY_MESSAGES -> _myMessagesResult.value = model
            HashtagSearchType.PUBLIC_POSTS -> _publicPostsResult.value = model
            HashtagSearchType.CHANNEL_POSTS -> _channelPostsResult.value = model
        }
    }

    open fun clearSearchResults(searchType: HashtagSearchType? = null) {
        if (searchType != null) {
            updateSearchResult(searchType, HashtagSearchResultModel(query = "", searchType = searchType))
        } else {
            updateSearchResult(HashtagSearchType.MY_MESSAGES, HashtagSearchResultModel(query = "", searchType = HashtagSearchType.MY_MESSAGES))
            updateSearchResult(HashtagSearchType.PUBLIC_POSTS, HashtagSearchResultModel(query = "", searchType = HashtagSearchType.PUBLIC_POSTS))
            updateSearchResult(HashtagSearchType.CHANNEL_POSTS, HashtagSearchResultModel(query = "", searchType = HashtagSearchType.CHANNEL_POSTS))
        }
    }

    open fun notifyHashtagSearchUpdated(guid: Int, count: Int, endReached: Boolean) {
        try {
            NotificationCenter.getInstance(currentAccount).postNotificationName(
                NotificationCenter.hashtagSearchUpdated,
                guid,
                count,
                endReached,
                0,
                0,
                0
            )
        } catch (_: Throwable) {
            // Safe fallback for headless JVM tests
        }
    }
}
