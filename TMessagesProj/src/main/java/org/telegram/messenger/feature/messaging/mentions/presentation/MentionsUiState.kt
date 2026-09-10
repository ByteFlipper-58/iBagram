package org.telegram.messenger.feature.messaging.mentions.presentation

import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery

/**
 * UI-состояние панели подсказок автодополнения.
 */
data class MentionsUiState(
    val query: MentionQuery = MentionQuery(),
    val candidates: List<MentionCandidate> = emptyList(),
    val isSearching: Boolean = false,
    val isPanelVisible: Boolean = false
) {
    val count: Int
        get() = candidates.size
}
