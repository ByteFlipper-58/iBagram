package org.telegram.messenger.feature.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.notifications.data.mapper.NotificationMapper
import org.telegram.messenger.feature.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.notifications.domain.model.DialogMuteState
import org.telegram.messenger.feature.notifications.domain.model.NotificationPeerType
import org.telegram.messenger.feature.notifications.domain.model.NotificationSettingsModel
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository
import org.telegram.messenger.feature.notifications.domain.usecase.GetBadgeSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.GetBadgeUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.GetNotificationSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.IsDialogMutedUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.MuteDialogUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ObserveBadgeSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ObserveBadgeUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ObserveNotificationSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.RefreshBadgeUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleContactJoinedNotificationsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInAppPreviewUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInAppSoundsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInAppVibrateUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInChatSoundUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.TogglePeerNotificationsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.TogglePinnedMessagesNotificationsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.UpdateBadgeSettingsUseCase
import org.telegram.messenger.feature.notifications.presentation.NotificationsEvent
import org.telegram.messenger.feature.notifications.presentation.NotificationsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeNotificationsRepository : NotificationsRepository {
        var shouldSucceed = true
        var errorMessage = "Repository failure"

        var settings = NotificationSettingsModel()
        var badgeSettings = BadgeSettingsModel()
        var badgeCount = BadgeCountModel()
        val mutedDialogs = mutableMapOf<Pair<Long, Long>, Int>() // (dialogId, topicId) -> untilDate

        val settingsFlow = MutableSharedFlow<NotificationSettingsModel>(replay = 1)
        val badgeSettingsFlow = MutableSharedFlow<BadgeSettingsModel>(replay = 1)
        val badgeFlow = MutableSharedFlow<BadgeCountModel>(replay = 1)

        init {
            settingsFlow.tryEmit(settings)
            badgeSettingsFlow.tryEmit(badgeSettings)
            badgeFlow.tryEmit(badgeCount)
        }

        override fun observeSettings(): Flow<NotificationSettingsModel> = settingsFlow.asSharedFlow()
        override suspend fun getSettings(): NotificationSettingsModel = settings

        override fun observeBadge(): Flow<BadgeCountModel> = badgeFlow.asSharedFlow()
        override suspend fun getBadge(): BadgeCountModel = badgeCount

        override fun observeBadgeSettings(): Flow<BadgeSettingsModel> = badgeSettingsFlow.asSharedFlow()
        override suspend fun getBadgeSettings(): BadgeSettingsModel = badgeSettings

        override suspend fun setPeerTypeNotificationsEnabled(
            type: NotificationPeerType,
            enabled: Boolean
        ): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            settings = when (type) {
                NotificationPeerType.PRIVATE_CHATS -> settings.copy(privateChatsEnabled = enabled)
                NotificationPeerType.GROUPS -> settings.copy(groupsEnabled = enabled)
                NotificationPeerType.CHANNELS -> settings.copy(channelsEnabled = enabled)
                NotificationPeerType.STORIES -> settings.copy(storiesEnabled = enabled)
                NotificationPeerType.REACTIONS_MESSAGES -> settings.copy(reactionsMessagesEnabled = enabled)
                NotificationPeerType.REACTIONS_STORIES -> settings.copy(reactionsStoriesEnabled = enabled)
            }
            settingsFlow.tryEmit(settings)
            return Result.Success(Unit)
        }

        override suspend fun setInChatSoundEnabled(enabled: Boolean): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            settings = settings.copy(inChatSoundEnabled = enabled)
            settingsFlow.tryEmit(settings)
            return Result.Success(Unit)
        }

        override suspend fun setInAppSoundsEnabled(enabled: Boolean): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            settings = settings.copy(inAppSoundsEnabled = enabled)
            settingsFlow.tryEmit(settings)
            return Result.Success(Unit)
        }

        override suspend fun setInAppVibrateEnabled(enabled: Boolean): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            settings = settings.copy(inAppVibrateEnabled = enabled)
            settingsFlow.tryEmit(settings)
            return Result.Success(Unit)
        }

        override suspend fun setInAppPreviewEnabled(enabled: Boolean): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            settings = settings.copy(inAppPreviewEnabled = enabled)
            settingsFlow.tryEmit(settings)
            return Result.Success(Unit)
        }

        override suspend fun setContactJoinedNotificationsEnabled(enabled: Boolean): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            settings = settings.copy(contactJoinedNotificationsEnabled = enabled)
            settingsFlow.tryEmit(settings)
            return Result.Success(Unit)
        }

        override suspend fun setPinnedMessagesNotificationsEnabled(enabled: Boolean): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            settings = settings.copy(pinnedMessagesNotificationsEnabled = enabled)
            settingsFlow.tryEmit(settings)
            return Result.Success(Unit)
        }

        override suspend fun updateBadgeSettings(newSettings: BadgeSettingsModel): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            badgeSettings = newSettings
            badgeSettingsFlow.tryEmit(badgeSettings)
            return Result.Success(Unit)
        }

        override suspend fun muteDialog(dialogId: Long, topicId: Long, mute: Boolean): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            if (mute) {
                mutedDialogs[Pair(dialogId, topicId)] = Int.MAX_VALUE
            } else {
                mutedDialogs.remove(Pair(dialogId, topicId))
            }
            return Result.Success(Unit)
        }

        override suspend fun muteDialogUntil(dialogId: Long, topicId: Long, untilDate: Int): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            mutedDialogs[Pair(dialogId, topicId)] = untilDate
            return Result.Success(Unit)
        }

        override suspend fun isDialogMuted(dialogId: Long, topicId: Long): Boolean {
            val muteUntil = mutedDialogs[Pair(dialogId, topicId)] ?: 0
            return muteUntil > 0
        }

        override suspend fun refreshBadge(): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            badgeCount = BadgeCountModel(totalUnreadCount = 42, badgeCount = 42)
            badgeFlow.tryEmit(badgeCount)
            return Result.Success(Unit)
        }
    }

    @Test
    fun testDomainModelsDefaults() {
        val settings = NotificationSettingsModel()
        assertTrue(settings.privateChatsEnabled)
        assertTrue(settings.groupsEnabled)
        assertTrue(settings.channelsEnabled)
        assertTrue(settings.storiesEnabled)
        assertTrue(settings.reactionsMessagesEnabled)
        assertTrue(settings.reactionsStoriesEnabled)
        assertTrue(settings.inChatSoundEnabled)
        assertTrue(settings.inAppSoundsEnabled)
        assertTrue(settings.inAppVibrateEnabled)
        assertTrue(settings.inAppPreviewEnabled)
        assertTrue(settings.contactJoinedNotificationsEnabled)
        assertTrue(settings.pinnedMessagesNotificationsEnabled)

        val badgeSettings = BadgeSettingsModel()
        assertTrue(badgeSettings.showBadgeNumber)
        assertFalse(badgeSettings.showBadgeMuted)
        assertTrue(badgeSettings.showBadgeMessages)

        val badgeCount = BadgeCountModel(totalUnreadCount = 5, badgeCount = 5)
        assertEquals(5, badgeCount.totalUnreadCount)
        assertEquals(5, badgeCount.badgeCount)

        val muteState = DialogMuteState(dialogId = 123L, topicId = 456L, isMuted = true, muteUntil = 9999)
        assertEquals(123L, muteState.dialogId)
        assertEquals(456L, muteState.topicId)
        assertTrue(muteState.isMuted)
        assertEquals(9999, muteState.muteUntil)
    }

    @Test
    fun testNotificationMapperBadgeCount() {
        val mapped = NotificationMapper.mapBadgeCount(15, 10)
        assertEquals(15, mapped.totalUnreadCount)
        assertEquals(10, mapped.badgeCount)
    }

    @Test
    fun testUseCasesAndRepository() = runTest(testDispatcher) {
        val repository = FakeNotificationsRepository()

        val getSettings = GetNotificationSettingsUseCase(repository)
        val observeSettings = ObserveNotificationSettingsUseCase(repository)
        val getBadge = GetBadgeUseCase(repository)
        val observeBadge = ObserveBadgeUseCase(repository)
        val getBadgeSettings = GetBadgeSettingsUseCase(repository)
        val observeBadgeSettings = ObserveBadgeSettingsUseCase(repository)
        val togglePeer = TogglePeerNotificationsUseCase(repository)
        val toggleInChatSound = ToggleInChatSoundUseCase(repository)
        val toggleInAppSounds = ToggleInAppSoundsUseCase(repository)
        val toggleInAppVibrate = ToggleInAppVibrateUseCase(repository)
        val toggleInAppPreview = ToggleInAppPreviewUseCase(repository)
        val toggleContactJoined = ToggleContactJoinedNotificationsUseCase(repository)
        val togglePinned = TogglePinnedMessagesNotificationsUseCase(repository)
        val updateBadgeSettings = UpdateBadgeSettingsUseCase(repository)
        val muteDialog = MuteDialogUseCase(repository)
        val isDialogMuted = IsDialogMutedUseCase(repository)
        val refreshBadge = RefreshBadgeUseCase(repository)

        // Settings
        val initialSettings = getSettings()
        assertTrue(initialSettings.privateChatsEnabled)

        togglePeer(NotificationPeerType.PRIVATE_CHATS, false)
        assertFalse(getSettings().privateChatsEnabled)
        assertFalse(observeSettings().first().privateChatsEnabled)

        togglePeer(NotificationPeerType.GROUPS, false)
        assertFalse(getSettings().groupsEnabled)

        togglePeer(NotificationPeerType.CHANNELS, false)
        assertFalse(getSettings().channelsEnabled)

        togglePeer(NotificationPeerType.STORIES, false)
        assertFalse(getSettings().storiesEnabled)

        togglePeer(NotificationPeerType.REACTIONS_MESSAGES, false)
        assertFalse(getSettings().reactionsMessagesEnabled)

        togglePeer(NotificationPeerType.REACTIONS_STORIES, false)
        assertFalse(getSettings().reactionsStoriesEnabled)

        // In-app toggles
        toggleInChatSound(false)
        assertFalse(getSettings().inChatSoundEnabled)

        toggleInAppSounds(false)
        assertFalse(getSettings().inAppSoundsEnabled)

        toggleInAppVibrate(false)
        assertFalse(getSettings().inAppVibrateEnabled)

        toggleInAppPreview(false)
        assertFalse(getSettings().inAppPreviewEnabled)

        toggleContactJoined(false)
        assertFalse(getSettings().contactJoinedNotificationsEnabled)

        togglePinned(false)
        assertFalse(getSettings().pinnedMessagesNotificationsEnabled)

        // Badge settings
        val customBadge = BadgeSettingsModel(showBadgeNumber = false, showBadgeMuted = true, showBadgeMessages = false)
        updateBadgeSettings(customBadge)
        assertEquals(customBadge, getBadgeSettings())
        assertEquals(customBadge, observeBadgeSettings().first())

        // Mute dialog
        assertFalse(isDialogMuted(100L))
        muteDialog(100L, mute = true)
        assertTrue(isDialogMuted(100L))
        muteDialog(100L, mute = false)
        assertFalse(isDialogMuted(100L))

        muteDialog.until(200L, topicId = 1L, untilDate = 5000)
        assertTrue(isDialogMuted(200L, 1L))

        // Refresh badge
        refreshBadge()
        val badge = getBadge()
        assertEquals(42, badge.totalUnreadCount)
        assertEquals(42, badge.badgeCount)
        assertEquals(42, observeBadge().first().totalUnreadCount)
    }

    @Test
    fun testViewModelStateAndEvents() = runTest(testDispatcher) {
        val repository = FakeNotificationsRepository()

        val viewModel = NotificationsViewModel(
            observeNotificationSettingsUseCase = ObserveNotificationSettingsUseCase(repository),
            getNotificationSettingsUseCase = GetNotificationSettingsUseCase(repository),
            observeBadgeUseCase = ObserveBadgeUseCase(repository),
            getBadgeUseCase = GetBadgeUseCase(repository),
            observeBadgeSettingsUseCase = ObserveBadgeSettingsUseCase(repository),
            getBadgeSettingsUseCase = GetBadgeSettingsUseCase(repository),
            togglePeerNotificationsUseCase = TogglePeerNotificationsUseCase(repository),
            toggleInChatSoundUseCase = ToggleInChatSoundUseCase(repository),
            toggleInAppSoundsUseCase = ToggleInAppSoundsUseCase(repository),
            toggleInAppVibrateUseCase = ToggleInAppVibrateUseCase(repository),
            toggleInAppPreviewUseCase = ToggleInAppPreviewUseCase(repository),
            toggleContactJoinedNotificationsUseCase = ToggleContactJoinedNotificationsUseCase(repository),
            togglePinnedMessagesNotificationsUseCase = TogglePinnedMessagesNotificationsUseCase(repository),
            updateBadgeSettingsUseCase = UpdateBadgeSettingsUseCase(repository),
            muteDialogUseCase = MuteDialogUseCase(repository),
            refreshBadgeUseCase = RefreshBadgeUseCase(repository)
        )

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.settings.privateChatsEnabled)

        // Event: TogglePeerType
        viewModel.onEvent(NotificationsEvent.TogglePeerType(NotificationPeerType.PRIVATE_CHATS, false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.privateChatsEnabled)

        viewModel.onEvent(NotificationsEvent.TogglePeerType(NotificationPeerType.GROUPS, false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.groupsEnabled)

        viewModel.onEvent(NotificationsEvent.TogglePeerType(NotificationPeerType.CHANNELS, false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.channelsEnabled)

        viewModel.onEvent(NotificationsEvent.TogglePeerType(NotificationPeerType.STORIES, false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.storiesEnabled)

        viewModel.onEvent(NotificationsEvent.TogglePeerType(NotificationPeerType.REACTIONS_MESSAGES, false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.reactionsMessagesEnabled)

        viewModel.onEvent(NotificationsEvent.TogglePeerType(NotificationPeerType.REACTIONS_STORIES, false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.reactionsStoriesEnabled)

        // Event: Toggles
        viewModel.onEvent(NotificationsEvent.ToggleInChatSound(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.inChatSoundEnabled)

        viewModel.onEvent(NotificationsEvent.ToggleInAppSounds(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.inAppSoundsEnabled)

        viewModel.onEvent(NotificationsEvent.ToggleInAppVibrate(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.inAppVibrateEnabled)

        viewModel.onEvent(NotificationsEvent.ToggleInAppPreview(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.inAppPreviewEnabled)

        viewModel.onEvent(NotificationsEvent.ToggleContactJoined(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.contactJoinedNotificationsEnabled)

        viewModel.onEvent(NotificationsEvent.TogglePinnedMessages(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.pinnedMessagesNotificationsEnabled)

        // Event: UpdateBadgeSettings
        val newBadgeSettings = BadgeSettingsModel(showBadgeNumber = false, showBadgeMuted = true, showBadgeMessages = false)
        viewModel.onEvent(NotificationsEvent.UpdateBadgeSettings(newBadgeSettings))
        advanceUntilIdle()
        assertEquals(newBadgeSettings, viewModel.uiState.value.badgeSettings)

        // Event: RefreshBadge
        viewModel.onEvent(NotificationsEvent.RefreshBadge)
        advanceUntilIdle()
        assertEquals(42, viewModel.uiState.value.badgeCount.totalUnreadCount)

        // Event: MuteDialog
        viewModel.onEvent(NotificationsEvent.MuteDialog(dialogId = 999L, mute = true))
        advanceUntilIdle()
        assertTrue(repository.isDialogMuted(999L))

        viewModel.onEvent(NotificationsEvent.MuteDialogUntil(dialogId = 888L, topicId = 0, untilDate = 5000))
        advanceUntilIdle()
        assertTrue(repository.isDialogMuted(888L))

        // Error handling
        repository.shouldSucceed = false
        repository.errorMessage = "Mute error"
        viewModel.onEvent(NotificationsEvent.MuteDialog(dialogId = 777L, mute = true))
        advanceUntilIdle()
        assertEquals("Mute error", viewModel.uiState.value.errorMessage)

        viewModel.onEvent(NotificationsEvent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
