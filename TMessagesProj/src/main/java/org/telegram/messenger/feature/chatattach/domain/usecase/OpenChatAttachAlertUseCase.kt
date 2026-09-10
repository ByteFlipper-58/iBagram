package org.telegram.messenger.feature.chatattach.domain.usecase

import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.chatattach.domain.repository.ChatAttachRepository

class OpenChatAttachAlertUseCase(
    private val repository: ChatAttachRepository,
    private val resolveAvailableLayoutsUseCase: ResolveAvailableAttachLayoutsUseCase
) {
    operator fun invoke(
        permissions: ChatAttachPermissions,
        preferredLayout: ChatAttachLayoutType = ChatAttachLayoutType.PHOTO
    ) {
        val available = resolveAvailableLayoutsUseCase(permissions)
        repository.setAvailableLayouts(available)
        val initialLayout = if (available.contains(preferredLayout)) {
            preferredLayout
        } else {
            available.firstOrNull() ?: ChatAttachLayoutType.PHOTO
        }
        repository.openAlert(permissions, initialLayout)
    }
}
