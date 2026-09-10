package org.telegram.messenger.feature.messaging.mentions.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionsState

/**
 * Контракт репозитория автодополнения упоминаний, хэштегов и команд ботов.
 */
interface MentionsRepository {
    fun observeState(): Flow<MentionsState>
    fun getState(): MentionsState
    fun updateQuery(query: MentionQuery)
    fun setCandidates(candidates: List<MentionCandidate>)
    fun setSearching(isSearching: Boolean)
    fun setPanelVisible(isVisible: Boolean)
    fun clear()
}
