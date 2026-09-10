package org.telegram.messenger.feature.mentions.domain.usecase

import org.telegram.messenger.feature.mentions.domain.repository.MentionsRepository

class ClearMentionsUseCase(
    private val repository: MentionsRepository
) {
    operator fun invoke() {
        repository.clear()
    }
}
