package org.telegram.messenger.feature.messaging.topics.presentation

import org.telegram.messenger.feature.messaging.topics.domain.model.TopicFilterType

/**
 * MVI Events for the forum topics screen.
 */
sealed class TopicsEvent {
    data class LoadTopics(val chatId: Long, val fromCache: Boolean = false) : TopicsEvent()
    data class SetFilter(val filter: TopicFilterType) : TopicsEvent()
    data class Search(val query: String) : TopicsEvent()
    data class ToggleClose(val topicId: Long, val close: Boolean) : TopicsEvent()
    data class TogglePin(val topicId: Long, val pin: Boolean) : TopicsEvent()
    data class ToggleShow(val topicId: Long, val show: Boolean) : TopicsEvent()
    data class Delete(val topicIds: List<Long>) : TopicsEvent()
    data class ReorderPinned(val topicIds: List<Long>) : TopicsEvent()
    data class MarkReactionsRead(val topicId: Long) : TopicsEvent()
    object Refresh : TopicsEvent()
}
