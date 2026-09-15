package org.telegram.messenger.feature.messaging.mentions.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.messaging.mentions.data.mapper.MentionsMapper
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionsState

/**
 * Потокобезопасный локальный источник данных для управления состоянием подсказок и кандидатов в чате.
 */
class MentionsLocalDataSource(
    private val currentAccount: Int,
    private val mapper: MentionsMapper = MentionsMapper()
) {

    private val lock = Any()
    private val _state = MutableStateFlow(MentionsState())

    fun observeState(): StateFlow<MentionsState> = _state.asStateFlow()

    fun getState(): MentionsState = _state.value

    fun updateQuery(query: MentionQuery) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(
            query = query,
            isSearching = query.isActive,
            isPanelVisible = query.isActive && current.candidates.isNotEmpty()
        )
    }

    fun setCandidates(candidates: List<MentionCandidate>) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(
            candidates = candidates,
            isSearching = false,
            isPanelVisible = candidates.isNotEmpty() && current.query.isActive
        )
    }

    fun setSearching(isSearching: Boolean) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(isSearching = isSearching)
    }

    fun setPanelVisible(isVisible: Boolean) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(isPanelVisible = isVisible)
    }

    fun clear() = synchronized(lock) {
        _state.value = MentionsState()
    }

    fun getLocalUserCandidates(query: String): List<MentionCandidate.UserCandidate> {
        val cleanQuery = query.lowercase().trim().removePrefix("@")
        if (cleanQuery.isEmpty()) return emptyList()

        return try {
            val controller = MessagesController.getInstance(currentAccount) ?: return emptyList()
            val result = mutableListOf<MentionCandidate.UserCandidate>()
            val users = controller.users?.values ?: return emptyList()
            for (user in users) {
                if (user == null) continue
                val username = user.username?.lowercase() ?: ""
                val first = user.first_name?.lowercase() ?: ""
                val last = user.last_name?.lowercase() ?: ""
                if (username.contains(cleanQuery) || first.contains(cleanQuery) || last.contains(cleanQuery)) {
                    val candidate = mapper.toUserCandidate(user)
                    if (candidate != null) {
                        result.add(candidate)
                        if (result.size >= 20) break
                    }
                }
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
