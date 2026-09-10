package org.telegram.messenger.feature.chatattach.domain.usecase

import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachSendOptions
import org.telegram.messenger.feature.chatattach.domain.repository.ChatAttachRepository

class UpdateAttachSendOptionsUseCase(
    private val repository: ChatAttachRepository
) {
    operator fun invoke(options: ChatAttachSendOptions) {
        repository.updateSendOptions(options)
    }
}
