package org.telegram.messenger.feature.messaging.chatattach

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.chatattach.data.datasource.ChatAttachLocalDataSource
import org.telegram.messenger.feature.messaging.chatattach.data.datasource.ChatAttachRemoteDataSource
import org.telegram.messenger.feature.messaging.chatattach.data.repository.ChatAttachRepositoryImpl
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachSendOptions

class ChatAttachRepositoryImplTest {

    private lateinit var localDataSource: ChatAttachLocalDataSource
    private lateinit var remoteDataSource: ChatAttachRemoteDataSource
    private lateinit var repository: ChatAttachRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = ChatAttachLocalDataSource(0)
        remoteDataSource = ChatAttachRemoteDataSource(0)
        repository = ChatAttachRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun initialState_hasDefaultValues() {
        val state = repository.getState()
        assertFalse(state.isAlertVisible)
        assertEquals(ChatAttachLayoutType.PHOTO, state.currentLayout)
        assertTrue(state.selectedItems.isEmpty())
    }

    @Test
    fun openAlert_updatesVisibilityAndLayout() = runBlocking {
        val permissions = ChatAttachPermissions(canSendPhotos = true, canSendMusic = false)
        repository.openAlert(permissions, ChatAttachLayoutType.MUSIC)

        val state = repository.observeState().first()
        assertTrue(state.isAlertVisible)
        assertEquals(ChatAttachLayoutType.MUSIC, state.currentLayout)
        assertFalse(state.permissions.canSendMusic)
    }

    @Test
    fun selectLayout_updatesCurrentLayout() {
        repository.selectLayout(ChatAttachLayoutType.DOCUMENTS)
        assertEquals(ChatAttachLayoutType.DOCUMENTS, repository.getState().currentLayout)
    }

    @Test
    fun toggleItemSelection_addsAndRemovesItem() {
        val item1 = ChatAttachItem(id = "1", type = ChatAttachLayoutType.PHOTO)
        val item2 = ChatAttachItem(id = "2", type = ChatAttachLayoutType.PHOTO)

        repository.toggleItemSelection(item1)
        var state = repository.getState()
        assertEquals(1, state.selectedItems.size)
        assertEquals("1", state.selectedItems[0].id)
        assertEquals(1, state.selectedItems[0].order)

        repository.toggleItemSelection(item2)
        state = repository.getState()
        assertEquals(2, state.selectedItems.size)
        assertEquals("2", state.selectedItems[1].id)
        assertEquals(2, state.selectedItems[1].order)

        // Toggle item1 off
        repository.toggleItemSelection(item1)
        state = repository.getState()
        assertEquals(1, state.selectedItems.size)
        assertEquals("2", state.selectedItems[0].id)
        assertEquals(1, state.selectedItems[0].order) // Reordered to 1
    }

    @Test
    fun clearSelection_emptiesList() {
        val item = ChatAttachItem(id = "10", type = ChatAttachLayoutType.PHOTO)
        repository.toggleItemSelection(item)
        assertEquals(1, repository.getState().selectedItems.size)

        repository.clearSelection()
        assertTrue(repository.getState().selectedItems.isEmpty())
    }

    @Test
    fun updateSendOptions_updatesOptions() {
        val options = ChatAttachSendOptions(caption = "Hello test", sendAsFile = true)
        repository.updateSendOptions(options)

        val state = repository.getState()
        assertEquals("Hello test", state.sendOptions.caption)
        assertTrue(state.sendOptions.sendAsFile)
    }

    @Test
    fun dismissAlert_setsAlertInvisible() {
        repository.openAlert(ChatAttachPermissions(), ChatAttachLayoutType.PHOTO)
        assertTrue(repository.getState().isAlertVisible)

        repository.dismissAlert()
        assertFalse(repository.getState().isAlertVisible)
    }

    @Test
    fun clear_resetsState() {
        repository.openAlert(ChatAttachPermissions(), ChatAttachLayoutType.DOCUMENTS)
        val item = ChatAttachItem(id = "5", type = ChatAttachLayoutType.DOCUMENTS)
        repository.toggleItemSelection(item)

        repository.clear()
        val state = repository.getState()
        assertFalse(state.isAlertVisible)
        assertTrue(state.selectedItems.isEmpty())
    }
}
