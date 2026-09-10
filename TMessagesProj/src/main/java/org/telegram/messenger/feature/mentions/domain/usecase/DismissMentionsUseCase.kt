package org.telegram.messenger.feature.mentions.domain.usecase

import org.telegram.messenger.feature.mentions.domain.repository.MentionsRepository

class DismissMentionsUseCase(
    private val repository: MentionsRepository
) {
    operator fun invoke() {
        repository.setPanelVisible(false)
    }
}
