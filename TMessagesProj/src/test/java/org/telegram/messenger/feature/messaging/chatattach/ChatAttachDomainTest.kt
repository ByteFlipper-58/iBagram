package org.telegram.messenger.feature.messaging.chatattach

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.messaging.chatattach.data.mapper.ChatAttachMapper
import org.telegram.messenger.feature.messaging.chatattach.data.repository.LegacyChatAttachRepository
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachSendOptions
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.CalculateAttachCaptionLimitUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ClearAttachSelectionUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ObserveChatAttachStateUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.OpenChatAttachAlertUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ResolveAvailableAttachLayoutsUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.SelectAttachLayoutUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ToggleAttachItemSelectionUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.UpdateAttachSendOptionsUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ValidateSendOptionsUseCase
import org.telegram.messenger.feature.messaging.chatattach.presentation.ChatAttachEvent
import org.telegram.messenger.feature.messaging.chatattach.presentation.ChatAttachViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ChatAttachDomainTest {

    @Test
    fun testResolveAvailableLayouts() {
        val resolveLayouts = ResolveAvailableAttachLayoutsUseCase()

        // 1. All permissions enabled
        val allAllowed = resolveLayouts(ChatAttachPermissions())
        assertTrue(allAllowed.contains(ChatAttachLayoutType.PHOTO))
        assertTrue(allAllowed.contains(ChatAttachLayoutType.DOCUMENTS))
        assertTrue(allAllowed.contains(ChatAttachLayoutType.LOCATION))
        assertTrue(allAllowed.contains(ChatAttachLayoutType.CONTACTS))
        assertTrue(allAllowed.contains(ChatAttachLayoutType.MUSIC))
        assertTrue(allAllowed.contains(ChatAttachLayoutType.POLL))

        // 2. Chat restricted by admin
        val restricted = resolveLayouts(ChatAttachPermissions(isRestricted = true))
        assertTrue(restricted.isEmpty())

        // 3. Polls disabled
        val noPolls = resolveLayouts(ChatAttachPermissions(canSendPolls = false))
        assertFalse(noPolls.contains(ChatAttachLayoutType.POLL))
        assertTrue(noPolls.contains(ChatAttachLayoutType.PHOTO))

        // 4. Photos and videos disabled
        val noMedia = resolveLayouts(ChatAttachPermissions(canSendPhotos = false, canSendVideos = false))
        assertFalse(noMedia.contains(ChatAttachLayoutType.PHOTO))
        assertTrue(noMedia.contains(ChatAttachLayoutType.DOCUMENTS))

        // 5. Fallback when everything is turned off but not restricted
        val nothing = resolveLayouts(ChatAttachPermissions(
            canSendPhotos = false,
            canSendVideos = false,
            canSendMusic = false,
            canSendDocuments = false,
            canSendLocation = false,
            canSendContacts = false,
            canSendPolls = false
        ))
        assertEquals(listOf(ChatAttachLayoutType.DOCUMENTS), nothing)
    }

    @Test
    fun testCaptionLimitAndValidation() {
        val calculateLimit = CalculateAttachCaptionLimitUseCase()
        val validator = ValidateSendOptionsUseCase()

        // Standard user test
        val shortCaption = "Hello world"
        val standardInfo = calculateLimit(shortCaption, isPremium = false)
        assertEquals(1024, standardInfo.maxLimit)
        assertEquals(11, standardInfo.currentLength)
        assertEquals(1013, standardInfo.remainingCharacters)
        assertFalse(standardInfo.isLimitExceeded)

        // Standard user overflow
        val longCaption = "A".repeat(1025)
        val overflowInfo = calculateLimit(longCaption, isPremium = false)
        assertTrue(overflowInfo.isLimitExceeded)
        assertEquals(-1, overflowInfo.remainingCharacters)

        // Premium user handles 1025 characters fine
        val premiumInfo = calculateLimit(longCaption, isPremium = true)
        assertEquals(2048, premiumInfo.maxLimit)
        assertFalse(premiumInfo.isLimitExceeded)
        assertEquals(1023, premiumInfo.remainingCharacters)

        // Validation logic
        assertTrue(validator.isValid(ChatAttachSendOptions(caption = "Valid", starsPrice = 50L), isPremium = false))
        assertFalse(validator.isValid(ChatAttachSendOptions(caption = longCaption), isPremium = false))
        assertFalse(validator.isValid(ChatAttachSendOptions(starsPrice = -1L), isPremium = false))
        assertFalse(validator.isValid(ChatAttachSendOptions(ttlSeconds = -10), isPremium = false))
    }

    @Test
    fun testItemSelectionAndOrdering() {
        val repo = LegacyChatAttachRepository()
        val toggleSelection = ToggleAttachItemSelectionUseCase(repo)

        val item1 = ChatAttachItem(id = "item_1", type = ChatAttachLayoutType.PHOTO)
        val item2 = ChatAttachItem(id = "item_2", type = ChatAttachLayoutType.PHOTO)
        val item3 = ChatAttachItem(id = "item_3", type = ChatAttachLayoutType.PHOTO)

        // Select item 1, 2, 3
        assertTrue(toggleSelection(item1))
        assertTrue(toggleSelection(item2))
        assertTrue(toggleSelection(item3))

        assertEquals(3, repo.getState().selectedItems.size)
        assertEquals(1, repo.getState().selectedItems[0].order)
        assertEquals(2, repo.getState().selectedItems[1].order)
        assertEquals(3, repo.getState().selectedItems[2].order)

        // Remove item 2 -> item 3 should now have order 2
        assertTrue(toggleSelection(item2))
        assertEquals(2, repo.getState().selectedItems.size)
        assertEquals("item_1", repo.getState().selectedItems[0].id)
        assertEquals(1, repo.getState().selectedItems[0].order)
        assertEquals("item_3", repo.getState().selectedItems[1].id)
        assertEquals(2, repo.getState().selectedItems[1].order)

        // Clear selection
        repo.clearSelection()
        assertTrue(repo.getState().selectedItems.isEmpty())
    }

    @Test
    fun testMapperLayoutConversions() {
        assertEquals(ChatAttachLayoutType.PHOTO, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_PHOTO))
        assertEquals(ChatAttachLayoutType.MUSIC, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_MUSIC))
        assertEquals(ChatAttachLayoutType.DOCUMENTS, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_DOCUMENTS))
        assertEquals(ChatAttachLayoutType.CONTACTS, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_CONTACTS))
        assertEquals(ChatAttachLayoutType.LOCATION, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_LOCATION))
        assertEquals(ChatAttachLayoutType.POLL, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_POLL))
        assertEquals(ChatAttachLayoutType.REPLIES, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_REPLIES))
        assertEquals(ChatAttachLayoutType.TODO, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_TODO))
        assertEquals(ChatAttachLayoutType.STICKERS, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_STICKERS))
        assertEquals(ChatAttachLayoutType.EMOJI, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_EMOJI))
        assertEquals(ChatAttachLayoutType.LINK, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_LINK))
        assertEquals(ChatAttachLayoutType.RICH, ChatAttachMapper.mapIntToLayoutType(ChatAttachMapper.LAYOUT_TYPE_RICH))

        assertEquals(ChatAttachMapper.LAYOUT_TYPE_PHOTO, ChatAttachMapper.mapLayoutTypeToInt(ChatAttachLayoutType.PHOTO))
        assertEquals(ChatAttachMapper.LAYOUT_TYPE_DOCUMENTS, ChatAttachMapper.mapLayoutTypeToInt(ChatAttachLayoutType.DOCUMENTS))
        assertEquals(ChatAttachMapper.LAYOUT_TYPE_LOCATION, ChatAttachMapper.mapLayoutTypeToInt(ChatAttachLayoutType.LOCATION))
        assertEquals(ChatAttachMapper.LAYOUT_TYPE_POLL, ChatAttachMapper.mapLayoutTypeToInt(ChatAttachLayoutType.POLL))
    }

    @Test
    fun testChatAttachViewModelMviFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val repo = LegacyChatAttachRepository()
        val observeState = ObserveChatAttachStateUseCase(repo)
        val resolveLayouts = ResolveAvailableAttachLayoutsUseCase()
        val openAlert = OpenChatAttachAlertUseCase(repo, resolveLayouts)
        val selectLayout = SelectAttachLayoutUseCase(repo)
        val toggleSelection = ToggleAttachItemSelectionUseCase(repo)
        val updateOptions = UpdateAttachSendOptionsUseCase(repo)
        val clearSelection = ClearAttachSelectionUseCase(repo)

        val vm = ChatAttachViewModel(
            observeStateUseCase = observeState,
            openAlertUseCase = openAlert,
            selectLayoutUseCase = selectLayout,
            toggleSelectionUseCase = toggleSelection,
            updateSendOptionsUseCase = updateOptions,
            clearSelectionUseCase = clearSelection,
            repository = repo,
            scope = testScope
        )

        advanceUntilIdle()
        assertFalse(vm.uiState.value.isAlertVisible)
        assertFalse(vm.uiState.value.isSendEnabled)

        // 1. Open Alert
        vm.onEvent(ChatAttachEvent.OnOpenAlert(
            permissions = ChatAttachPermissions(),
            initialLayout = ChatAttachLayoutType.PHOTO
        ))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isAlertVisible)
        assertEquals(ChatAttachLayoutType.PHOTO, vm.uiState.value.currentLayout)
        assertTrue(vm.uiState.value.isPhotoLayout)

        // 2. Switch tab to DOCUMENTS
        vm.onEvent(ChatAttachEvent.OnLayoutSelected(ChatAttachLayoutType.DOCUMENTS))
        advanceUntilIdle()
        assertEquals(ChatAttachLayoutType.DOCUMENTS, vm.uiState.value.currentLayout)
        assertTrue(vm.uiState.value.isDocumentLayout)

        // 3. Toggle item selection
        val doc = ChatAttachItem(id = "doc_1", type = ChatAttachLayoutType.DOCUMENTS, displayName = "notes.pdf")
        vm.onEvent(ChatAttachEvent.OnItemSelectionToggled(doc))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.selectedCount)
        assertTrue(vm.uiState.value.isSendEnabled)

        // 4. Change caption & options
        vm.onEvent(ChatAttachEvent.OnCaptionChanged("Important document"))
        advanceUntilIdle()
        assertEquals("Important document", vm.uiState.value.sendOptions.caption)
        assertEquals(18, vm.uiState.value.captionInfo.currentLength)

        vm.onEvent(ChatAttachEvent.OnSpoilerToggled(true))
        vm.onEvent(ChatAttachEvent.OnSendAsFileToggled(true))
        vm.onEvent(ChatAttachEvent.OnCaptionAboveToggled(true))
        vm.onEvent(ChatAttachEvent.OnStarsPriceChanged(100L))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.sendOptions.hasSpoiler)
        assertTrue(vm.uiState.value.sendOptions.sendAsFile)
        assertTrue(vm.uiState.value.sendOptions.isCaptionAbove)
        assertEquals(100L, vm.uiState.value.sendOptions.starsPrice)

        // 5. Clear selection
        vm.onEvent(ChatAttachEvent.OnClearSelectionRequested)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.selectedCount)
        assertFalse(vm.uiState.value.isSendEnabled)

        // 6. Dismiss alert
        vm.onEvent(ChatAttachEvent.OnDismissRequested)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isAlertVisible)
    }
}
