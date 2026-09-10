package org.telegram.messenger.feature.mentions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.mentions.domain.model.MentionsState
import org.telegram.messenger.feature.mentions.domain.repository.MentionsRepository

class ObserveMentionsStateUseCase(
    private val repository: MentionsRepository
) {
    operator fun invoke(): Flow<MentionsState> = repository.observeState()
}
