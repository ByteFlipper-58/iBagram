package org.telegram.messenger.feature.messaging.mentions.presentation

import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate

/**
 * UI-события ввода и выбора подсказок автодополнения.
 */
sealed interface MentionsEvent {
    data class OnInputTextChanged(
        val text: CharSequence?,
        val cursorPosition: Int,
        val allowContextBots: Boolean = true
    ) : MentionsEvent

    data class OnCandidatesLoaded(
        val candidates: List<MentionCandidate>,
        val query: String = ""
    ) : MentionsEvent

    data class OnCandidateSelected(
        val candidate: MentionCandidate,
        val currentText: String
    ) : MentionsEvent

    object OnDismissRequested : MentionsEvent
    object OnClearRequested : MentionsEvent
}
