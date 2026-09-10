package org.telegram.messenger.feature.messaging.mentions.domain.usecase

import org.telegram.messenger.feature.messaging.mentions.domain.repository.MentionsRepository

class ClearMentionsUseCase(
    private val repository: MentionsRepository
) {
    operator fun invoke() {
        repository.clear()
    }
}
