package org.telegram.messenger.feature.messaging.mentions.domain.usecase

import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionReplacement

/**
 * Подставляет выбранную подсказку автодополнения в исходный текст и рассчитывает новую позицию курсора.
 */
class FormatMentionReplacementUseCase {

    operator fun invoke(
        originalText: String,
        query: MentionQuery,
        candidate: MentionCandidate
    ): MentionReplacement {
        val insertText = when (candidate) {
            is MentionCandidate.UserCandidate -> {
                if (!candidate.username.isNullOrBlank()) {
                    "@${candidate.username} "
                } else {
                    "${candidate.displayName} "
                }
            }
            is MentionCandidate.HashtagCandidate -> {
                val cleanTag = candidate.hashtag.removePrefix("#")
                "#$cleanTag "
            }
            is MentionCandidate.BotCommandCandidate -> {
                val cleanCmd = candidate.command.removePrefix("/")
                "/$cleanCmd "
            }
            is MentionCandidate.EmojiKeywordCandidate -> {
                "${candidate.emoji} "
            }
            is MentionCandidate.QuickReplyCandidate -> {
                candidate.text
            }
        }

        val start = query.startPosition.coerceIn(0, originalText.length)
        val end = (start + query.length).coerceIn(start, originalText.length)

        val before = originalText.substring(0, start)
        val after = originalText.substring(end)

        val newText = before + insertText + after
        val newCursor = start + insertText.length

        return MentionReplacement(
            replacementText = newText,
            newCursorPosition = newCursor
        )
    }
}
