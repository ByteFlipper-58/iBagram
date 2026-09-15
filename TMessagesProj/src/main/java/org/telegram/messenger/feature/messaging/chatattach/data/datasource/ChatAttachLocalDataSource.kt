package org.telegram.messenger.feature.messaging.chatattach.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachSendOptions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachState

class ChatAttachLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val _state = MutableStateFlow(ChatAttachState())
    val state: StateFlow<ChatAttachState> = _state.asStateFlow()
    private val lock = Any()

    fun getState(): ChatAttachState = _state.value

    fun openAlert(permissions: ChatAttachPermissions, initialLayout: ChatAttachLayoutType) {
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

    fun selectLayout(layout: ChatAttachLayoutType) {
        synchronized(lock) {
            _state.update { it.copy(currentLayout = layout) }
        }
    }

    fun setAvailableLayouts(layouts: List<ChatAttachLayoutType>) {
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

    fun toggleItemSelection(item: ChatAttachItem) {
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

    fun setSelectedItems(items: List<ChatAttachItem>) {
        synchronized(lock) {
            _state.update { current ->
                val reordered = items.mapIndexed { index, item -> item.copy(order = index + 1) }
                current.copy(selectedItems = reordered)
            }
        }
    }

    fun clearSelection() {
        synchronized(lock) {
            _state.update { it.copy(selectedItems = emptyList()) }
        }
    }

    fun updateSendOptions(options: ChatAttachSendOptions) {
        synchronized(lock) {
            _state.update { it.copy(sendOptions = options) }
        }
    }

    fun setPermissions(permissions: ChatAttachPermissions) {
        synchronized(lock) {
            _state.update { it.copy(permissions = permissions) }
        }
    }

    fun dismissAlert() {
        synchronized(lock) {
            _state.update { it.copy(isAlertVisible = false) }
        }
    }

    fun clear() {
        synchronized(lock) {
            _state.value = ChatAttachState()
        }
    }
}
