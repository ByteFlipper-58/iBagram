package org.telegram.messenger.feature.messaging.chatattach.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.chatattach.data.datasource.ChatAttachLocalDataSource
import org.telegram.messenger.feature.messaging.chatattach.data.datasource.ChatAttachRemoteDataSource
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachSendOptions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachState
import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository

class ChatAttachRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: ChatAttachLocalDataSource,
    private val remoteDataSource: ChatAttachRemoteDataSource
) : ChatAttachRepository {

    override fun observeState(): StateFlow<ChatAttachState> = localDataSource.state

    override fun getState(): ChatAttachState = localDataSource.getState()

    override fun openAlert(permissions: ChatAttachPermissions, initialLayout: ChatAttachLayoutType) {
        localDataSource.openAlert(permissions, initialLayout)
    }

    override fun selectLayout(layout: ChatAttachLayoutType) {
        localDataSource.selectLayout(layout)
    }

    override fun setAvailableLayouts(layouts: List<ChatAttachLayoutType>) {
        localDataSource.setAvailableLayouts(layouts)
    }

    override fun toggleItemSelection(item: ChatAttachItem) {
        localDataSource.toggleItemSelection(item)
    }

    override fun setSelectedItems(items: List<ChatAttachItem>) {
        localDataSource.setSelectedItems(items)
    }

    override fun clearSelection() {
        localDataSource.clearSelection()
    }

    override fun updateSendOptions(options: ChatAttachSendOptions) {
        localDataSource.updateSendOptions(options)
    }

    override fun setPermissions(permissions: ChatAttachPermissions) {
        localDataSource.setPermissions(permissions)
    }

    override fun dismissAlert() {
        localDataSource.dismissAlert()
    }

    override fun clear() {
        localDataSource.clear()
    }
}
