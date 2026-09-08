package org.telegram.messenger.feature.topics.presentation

import org.telegram.messenger.feature.topics.domain.model.ForumUnreadCountModel
import org.telegram.messenger.feature.topics.domain.model.TopicFilterType
import org.telegram.messenger.feature.topics.domain.model.TopicModel

/**
 * Immutable UI State for the forum topics screen.
 */
data class TopicsUiState(
    val chatId: Long = 0L,
    val topics: List<TopicModel> = emptyList(),
    val filteredTopics: List<TopicModel> = emptyList(),
    val filter: TopicFilterType = TopicFilterType.ALL,
    val unreadCount: ForumUnreadCountModel = ForumUnreadCountModel(0L, 0, 0),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
