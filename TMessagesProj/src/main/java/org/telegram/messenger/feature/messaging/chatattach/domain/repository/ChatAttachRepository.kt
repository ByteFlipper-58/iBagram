package org.telegram.messenger.feature.messaging.chatattach.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachSendOptions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachState

interface ChatAttachRepository {
    fun observeState(): StateFlow<ChatAttachState>
    fun getState(): ChatAttachState
    fun openAlert(permissions: ChatAttachPermissions, initialLayout: ChatAttachLayoutType)
    fun selectLayout(layout: ChatAttachLayoutType)
    fun setAvailableLayouts(layouts: List<ChatAttachLayoutType>)
    fun toggleItemSelection(item: ChatAttachItem)
    fun setSelectedItems(items: List<ChatAttachItem>)
    fun clearSelection()
    fun updateSendOptions(options: ChatAttachSendOptions)
    fun setPermissions(permissions: ChatAttachPermissions)
    fun dismissAlert()
    fun clear()
}
