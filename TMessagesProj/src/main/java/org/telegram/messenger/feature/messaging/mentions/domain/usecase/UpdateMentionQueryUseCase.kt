package org.telegram.messenger.feature.messaging.mentions.domain.usecase

import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.messaging.mentions.domain.repository.MentionsRepository

class UpdateMentionQueryUseCase(
    private val repository: MentionsRepository,
    private val parseMentionQueryUseCase: ParseMentionQueryUseCase = ParseMentionQueryUseCase()
) {
    operator fun invoke(text: CharSequence?, cursorPosition: Int, allowContextBots: Boolean = true): MentionQuery {
        val query = parseMentionQueryUseCase(text, cursorPosition, allowContextBots)
        repository.updateQuery(query)
        return query
    }
}
