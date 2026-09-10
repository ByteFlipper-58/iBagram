package org.telegram.messenger.feature.chatattach.domain.usecase

import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachPermissions

class ResolveAvailableAttachLayoutsUseCase {
    operator fun invoke(permissions: ChatAttachPermissions): List<ChatAttachLayoutType> {
        if (permissions.isRestricted) {
            return emptyList()
        }

        val layouts = mutableListOf<ChatAttachLayoutType>()
        if (permissions.canSendPhotos || permissions.canSendVideos) {
            layouts.add(ChatAttachLayoutType.PHOTO)
        }
        if (permissions.canSendDocuments) {
            layouts.add(ChatAttachLayoutType.DOCUMENTS)
        }
        if (permissions.canSendLocation) {
            layouts.add(ChatAttachLayoutType.LOCATION)
        }
        if (permissions.canSendContacts) {
            layouts.add(ChatAttachLayoutType.CONTACTS)
        }
        if (permissions.canSendMusic) {
            layouts.add(ChatAttachLayoutType.MUSIC)
        }
        if (permissions.canSendPolls) {
            layouts.add(ChatAttachLayoutType.POLL)
        }
        return if (layouts.isEmpty()) {
            listOf(ChatAttachLayoutType.DOCUMENTS)
        } else {
            layouts
        }
    }
}
