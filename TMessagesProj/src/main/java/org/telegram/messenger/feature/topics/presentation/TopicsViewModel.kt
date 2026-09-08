package org.telegram.messenger.feature.topics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.topics.domain.model.TopicFilterType
import org.telegram.messenger.feature.topics.domain.model.TopicModel
import org.telegram.messenger.feature.topics.domain.usecase.DeleteTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetForumUnreadCountUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.LoadTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.MarkTopicReactionsAsReadUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ObserveForumUnreadCountUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ObserveTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ReloadTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ReorderPinnedTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ToggleCloseTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.TogglePinTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ToggleShowTopicUseCase

class TopicsViewModel(
    private val observeTopicsUseCase: ObserveTopicsUseCase,
    private val observeForumUnreadCountUseCase: ObserveForumUnreadCountUseCase,
    private val getTopicsUseCase: GetTopicsUseCase,
    private val getTopicUseCase: GetTopicUseCase,
    private val loadTopicsUseCase: LoadTopicsUseCase,
    private val reloadTopicsUseCase: ReloadTopicsUseCase,
    private val toggleCloseTopicUseCase: ToggleCloseTopicUseCase,
    private val togglePinTopicUseCase: TogglePinTopicUseCase,
    private val toggleShowTopicUseCase: ToggleShowTopicUseCase,
    private val deleteTopicsUseCase: DeleteTopicsUseCase,
    private val reorderPinnedTopicsUseCase: ReorderPinnedTopicsUseCase,
    private val markTopicReactionsAsReadUseCase: MarkTopicReactionsAsReadUseCase,
    private val getForumUnreadCountUseCase: GetForumUnreadCountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    private var observeTopicsJob: Job? = null
    private var observeUnreadJob: Job? = null

    fun onEvent(event: TopicsEvent) {
        when (event) {
            is TopicsEvent.LoadTopics -> loadTopics(event.chatId, event.fromCache)
            is TopicsEvent.SetFilter -> setFilter(event.filter)
            is TopicsEvent.Search -> search(event.query)
            is TopicsEvent.ToggleClose -> toggleClose(event.topicId, event.close)
            is TopicsEvent.TogglePin -> togglePin(event.topicId, event.pin)
            is TopicsEvent.ToggleShow -> toggleShow(event.topicId, event.show)
            is TopicsEvent.Delete -> deleteTopics(event.topicIds)
            is TopicsEvent.ReorderPinned -> reorderPinned(event.topicIds)
            is TopicsEvent.MarkReactionsRead -> markReactionsRead(event.topicId)
            is TopicsEvent.Refresh -> refresh()
        }
    }

    private fun loadTopics(chatId: Long, fromCache: Boolean) {
        _uiState.update { it.copy(chatId = chatId, isLoading = true, errorMessage = null) }

        observeTopicsJob?.cancel()
        observeTopicsJob = observeTopicsUseCase(chatId)
            .onEach { topics ->
                _uiState.update { current ->
                    current.copy(
                        topics = topics,
                        filteredTopics = filterTopics(topics, current.filter, current.searchQuery),
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)

        observeUnreadJob?.cancel()
        observeUnreadJob = observeForumUnreadCountUseCase(chatId)
            .onEach { unread ->
                _uiState.update { it.copy(unreadCount = unread) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            loadTopicsUseCase(chatId, fromCache)
        }
    }

    private fun setFilter(filter: TopicFilterType) {
        _uiState.update { current ->
            current.copy(
                filter = filter,
                filteredTopics = filterTopics(current.topics, filter, current.searchQuery)
            )
        }
    }

    private fun search(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredTopics = filterTopics(current.topics, current.filter, query)
            )
        }
    }

    private fun toggleClose(topicId: Long, close: Boolean) {
        val chatId = _uiState.value.chatId
        if (chatId == 0L) return
        viewModelScope.launch {
            toggleCloseTopicUseCase(chatId, topicId, close)
        }
    }

    private fun togglePin(topicId: Long, pin: Boolean) {
        val chatId = _uiState.value.chatId
        if (chatId == 0L) return
        viewModelScope.launch {
            togglePinTopicUseCase(chatId, topicId, pin)
        }
    }

    private fun toggleShow(topicId: Long, show: Boolean) {
        val chatId = _uiState.value.chatId
        if (chatId == 0L) return
        viewModelScope.launch {
            toggleShowTopicUseCase(chatId, topicId, show)
        }
    }

    private fun deleteTopics(topicIds: List<Long>) {
        val chatId = _uiState.value.chatId
        if (chatId == 0L) return
        viewModelScope.launch {
            deleteTopicsUseCase(chatId, topicIds)
        }
    }

    private fun reorderPinned(topicIds: List<Long>) {
        val chatId = _uiState.value.chatId
        if (chatId == 0L) return
        viewModelScope.launch {
            reorderPinnedTopicsUseCase(chatId, topicIds)
        }
    }

    private fun markReactionsRead(topicId: Long) {
        val chatId = _uiState.value.chatId
        if (chatId == 0L) return
        viewModelScope.launch {
            markTopicReactionsAsReadUseCase(chatId, topicId)
        }
    }

    private fun refresh() {
        val chatId = _uiState.value.chatId
        if (chatId == 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            reloadTopicsUseCase(chatId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun filterTopics(
        topics: List<TopicModel>,
        filter: TopicFilterType,
        query: String
    ): List<TopicModel> {
        val trimmedQuery = query.trim()
        return topics.filter { topic ->
            val matchesFilter = when (filter) {
                TopicFilterType.ALL -> true
                TopicFilterType.OPEN -> !topic.isClosed
                TopicFilterType.CLOSED -> topic.isClosed
                TopicFilterType.PINNED -> topic.isPinned
                TopicFilterType.HIDDEN -> topic.isHidden
            }
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                topic.title.contains(trimmedQuery, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }
}
