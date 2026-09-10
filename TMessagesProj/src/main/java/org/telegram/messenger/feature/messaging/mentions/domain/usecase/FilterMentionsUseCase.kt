package org.telegram.messenger.feature.messaging.mentions.domain.usecase

import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate

/**
 * Фильтрует список кандидатов автодополнения по переданной строке запроса.
 */
class FilterMentionsUseCase {

    operator fun invoke(
        candidates: List<MentionCandidate>,
        query: String,
        limit: Int = 20
    ): List<MentionCandidate> {
        if (candidates.isEmpty()) return emptyList()
        val normalizedQuery = query.trim().lowercase()

        val filtered = if (normalizedQuery.isEmpty()) {
            candidates
        } else {
            candidates.filter { candidate ->
                when (candidate) {
                    is MentionCandidate.UserCandidate -> {
                        (candidate.username?.lowercase()?.contains(normalizedQuery) == true) ||
                        (candidate.firstName?.lowercase()?.contains(normalizedQuery) == true) ||
                        (candidate.lastName?.lowercase()?.contains(normalizedQuery) == true)
                    }
                    is MentionCandidate.HashtagCandidate -> {
                        candidate.hashtag.lowercase().contains(normalizedQuery)
                    }
                    is MentionCandidate.BotCommandCandidate -> {
                        candidate.command.lowercase().contains(normalizedQuery) ||
                        (candidate.helpText?.lowercase()?.contains(normalizedQuery) == true)
                    }
                    is MentionCandidate.EmojiKeywordCandidate -> {
                        candidate.keyword.lowercase().contains(normalizedQuery)
                    }
                    is MentionCandidate.QuickReplyCandidate -> {
                        candidate.shortcut.lowercase().contains(normalizedQuery) ||
                        candidate.text.lowercase().contains(normalizedQuery)
                    }
                }
            }
        }

        return filtered.take(limit)
    }
}
