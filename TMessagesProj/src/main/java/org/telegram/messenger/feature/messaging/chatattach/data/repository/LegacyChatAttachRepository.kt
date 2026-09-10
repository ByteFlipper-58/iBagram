package org.telegram.messenger.feature.messaging.chatattach.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachSendOptions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachState
import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository

class LegacyChatAttachRepository : ChatAttachRepository {

    private val _state = MutableStateFlow(ChatAttachState())
    private val lock = Any()

    override fun observeState(): StateFlow<ChatAttachState> = _state.asStateFlow()

    override fun getState(): ChatAttachState = _state.value

    override fun openAlert(permissions: ChatAttachPermissions, initialLayout: ChatAttachLayoutType) {
        synchronized(lock) {
            _state.update { current ->
                current.copy(
                    permissions = permissions,
                    currentLayout = initialLayout,
                    isAlertVisible = true
                )
            }
        }
    }

    override fun selectLayout(layout: ChatAttachLayoutType) {
        synchronized(lock) {
            _state.update { it.copy(currentLayout = layout) }
        }
    }

    override fun setAvailableLayouts(layouts: List<ChatAttachLayoutType>) {
        synchronized(lock) {
            _state.update { current ->
                val newLayout = if (layouts.contains(current.currentLayout)) {
                    current.currentLayout
                } else {
                    layouts.firstOrNull() ?: ChatAttachLayoutType.PHOTO
                }
                current.copy(
                    availableLayouts = layouts,
                    currentLayout = newLayout
                )
            }
        }
    }

    override fun toggleItemSelection(item: ChatAttachItem) {
        synchronized(lock) {
            _state.update { current ->
                val exists = current.selectedItems.any { it.id == item.id }
                val updatedItems = if (exists) {
                    current.selectedItems
                        .filterNot { it.id == item.id }
                        .mapIndexed { index, selected -> selected.copy(order = index + 1) }
                } else {
                    if (current.selectedItems.size >= current.maxSelectionLimit) {
                        current.selectedItems
                    } else {
                        current.selectedItems + item.copy(order = current.selectedItems.size + 1)
                    }
                }
                current.copy(selectedItems = updatedItems)
            }
        }
    }

    override fun setSelectedItems(items: List<ChatAttachItem>) {
        synchronized(lock) {
            _state.update { current ->
                val reordered = items.mapIndexed { index, item -> item.copy(order = index + 1) }
                current.copy(selectedItems = reordered)
            }
        }
    }

    override fun clearSelection() {
        synchronized(lock) {
            _state.update { it.copy(selectedItems = emptyList()) }
        }
    }

    override fun updateSendOptions(options: ChatAttachSendOptions) {
        synchronized(lock) {
            _state.update { it.copy(sendOptions = options) }
        }
    }

    override fun setPermissions(permissions: ChatAttachPermissions) {
        synchronized(lock) {
            _state.update { it.copy(permissions = permissions) }
        }
    }

    override fun dismissAlert() {
        synchronized(lock) {
            _state.update { it.copy(isAlertVisible = false) }
        }
    }

    override fun clear() {
        synchronized(lock) {
            _state.value = ChatAttachState()
        }
    }
}
