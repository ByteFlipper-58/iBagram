package org.telegram.messenger.feature.messaging.mentions.domain.usecase

import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionsState
import org.telegram.messenger.feature.messaging.mentions.domain.repository.MentionsRepository

class GetMentionsStateUseCase(
    private val repository: MentionsRepository
) {
    operator fun invoke(): MentionsState = repository.getState()
}
