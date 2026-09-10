package org.telegram.messenger.feature.mentions.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.mentions.domain.model.MentionsState
import org.telegram.messenger.feature.mentions.domain.repository.MentionsRepository

/**
 * Потокобезопасная реализация [MentionsRepository] для управления состоянием подсказок в чате.
 */
class LegacyMentionsRepository : MentionsRepository {

    private val lock = Any()
    private val _state = MutableStateFlow(MentionsState())

    override fun observeState(): Flow<MentionsState> = _state.asStateFlow()

    override fun getState(): MentionsState = _state.value

    override fun updateQuery(query: MentionQuery) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(
            query = query,
            isSearching = query.isActive,
            isPanelVisible = query.isActive && current.candidates.isNotEmpty()
        )
    }

    override fun setCandidates(candidates: List<MentionCandidate>) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(
            candidates = candidates,
            isSearching = false,
            isPanelVisible = candidates.isNotEmpty() && current.query.isActive
        )
    }

    override fun setSearching(isSearching: Boolean) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(isSearching = isSearching)
    }

    override fun setPanelVisible(isVisible: Boolean) = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(isPanelVisible = isVisible)
    }

    override fun clear() = synchronized(lock) {
        _state.value = MentionsState()
    }
}
