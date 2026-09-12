package org.telegram.messenger.feature.system.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.data.datasource.NotificationsLocalDataSource
import org.telegram.messenger.feature.system.notifications.data.datasource.NotificationsRemoteDataSource
import org.telegram.messenger.feature.system.notifications.data.repository.NotificationsRepositoryImpl
import org.telegram.messenger.feature.system.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
        Dispatchers.resetMain()
    }

    // Fake local data source with controllable in-memory data
    private class FakeNotificationsLocalDataSource(account: Int) : NotificationsLocalDataSource(account) {
        val intPrefs = mutableMapOf<String, Int>()
        val boolPrefs = mutableMapOf<String, Boolean>()
        var currentTimeValue: Int = 1000
        var totalUnread: Int = 15
        var badgeNumber: Boolean = true
        var badgeMuted: Boolean = false
        var badgeMessages: Boolean = true
        var badgeUpdated = false
        var inChatSound = true
        val globalSettings = mutableMapOf<Int, Int>()
        val mutedDialogs = mutableMapOf<Pair<Long, Long>, Int>()

        override fun getCurrentTime(): Int = currentTimeValue

        override fun getInt(key: String, defaultValue: Int): Int = intPrefs[key] ?: defaultValue

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = boolPrefs[key] ?: defaultValue

        override fun putBoolean(key: String, value: Boolean): Boolean {
            boolPrefs[key] = value
            return true
        }

        override fun putInt(key: String, value: Int): Boolean {
            intPrefs[key] = value
            return true
        }

        override fun removeKey(key: String): Boolean {
            boolPrefs.remove(key)
            intPrefs.remove(key)
            return true
        }

        override fun getTotalUnreadCount(): Int = totalUnread

        override fun getShowBadgeNumber(): Boolean = boolPrefs["badgeNumber"] ?: badgeNumber

        override fun getShowBadgeMuted(): Boolean = boolPrefs["badgeNumberMuted"] ?: badgeMuted

        override fun getShowBadgeMessages(): Boolean = boolPrefs["badgeNumberMessages"] ?: badgeMessages

        override fun setBadgeSettings(showNumber: Boolean, showMuted: Boolean, showMessages: Boolean) {
            badgeNumber = showNumber
            badgeMuted = showMuted
            badgeMessages = showMessages
            boolPrefs["badgeNumber"] = showNumber
            boolPrefs["badgeNumberMuted"] = showMuted
            boolPrefs["badgeNumberMessages"] = showMessages
        }

        override fun updateBadge() {
            badgeUpdated = true
        }

        override fun setInChatSoundEnabled(enabled: Boolean) {
            inChatSound = enabled
            boolPrefs["EnableInChatSound"] = enabled
        }

        override fun setGlobalNotificationsEnabled(type: Int, time: Int) {
            globalSettings[type] = time
        }

        override fun muteDialog(dialogId: Long, topicId: Long, mute: Boolean) {
            if (mute) {
                mutedDialogs[Pair(dialogId, topicId)] = Int.MAX_VALUE
            } else {
                mutedDialogs.remove(Pair(dialogId, topicId))
            }
        }

        override fun muteDialogUntil(dialogId: Long, topicId: Long, untilDate: Int) {
            mutedDialogs[Pair(dialogId, topicId)] = untilDate
        }

        override fun isDialogMuted(dialogId: Long, topicId: Long): Boolean {
            val muteUntil = mutedDialogs[Pair(dialogId, topicId)] ?: 0
            return muteUntil > currentTimeValue
        }
    }

    // Fake remote data source recording calls
    private class FakeNotificationsRemoteDataSource(account: Int) : NotificationsRemoteDataSource(account) {
        var silentSignUpNotification: Boolean? = null

        override suspend fun setContactSignUpNotification(silent: Boolean): Result<Boolean> {
            silentSignUpNotification = silent
            return Result.Success(true)
        }
    }

    @Test
    fun testGetSettingsReturnsMappedSettings() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        local.currentTimeValue = 1000
        local.intPrefs["EnableAll2"] = 0 // < 1000 -> true
        local.intPrefs["EnableGroup2"] = 2000 // > 1000 -> false
        local.intPrefs["EnableChannel2"] = 0 // < 1000 -> true
        local.boolPrefs["EnableAllStories"] = true
        local.boolPrefs["EnableReactionsMessages"] = false
        local.boolPrefs["EnableReactionsStories"] = true
        local.boolPrefs["EnableInChatSound"] = true
        local.boolPrefs["EnableInAppSounds"] = false
        local.boolPrefs["EnableInAppVibrate"] = true
        local.boolPrefs["EnableInAppPreview"] = false
        local.boolPrefs["EnableContactJoined"] = true
        local.boolPrefs["PinnedMessages"] = true

        val settings = repo.getSettings()
        assertTrue(settings.privateChatsEnabled)
        assertFalse(settings.groupsEnabled)
        assertTrue(settings.channelsEnabled)
        assertTrue(settings.storiesEnabled)
        assertFalse(settings.reactionsMessagesEnabled)
        assertTrue(settings.reactionsStoriesEnabled)
        assertTrue(settings.inChatSoundEnabled)
        assertFalse(settings.inAppSoundsEnabled)
        assertTrue(settings.inAppVibrateEnabled)
        assertFalse(settings.inAppPreviewEnabled)
        assertTrue(settings.contactJoinedNotificationsEnabled)
        assertTrue(settings.pinnedMessagesNotificationsEnabled)
    }

    @Test
    fun testGetBadgeReturnsUnreadCount() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        local.totalUnread = 25
        val badge = repo.getBadge()
        assertEquals(25, badge.totalUnreadCount)
        assertEquals(25, badge.badgeCount)
    }

    @Test
    fun testGetBadgeSettingsReturnsMappedBadgeSettings() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        local.setBadgeSettings(showNumber = false, showMuted = true, showMessages = false)
        val settings = repo.getBadgeSettings()
        assertFalse(settings.showBadgeNumber)
        assertTrue(settings.showBadgeMuted)
        assertFalse(settings.showBadgeMessages)
    }

    @Test
    fun testSetPeerTypeNotificationsEnabled() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        val resPrivate = repo.setPeerTypeNotificationsEnabled(NotificationPeerType.PRIVATE_CHATS, false)
        assertTrue(resPrivate.isSuccess)
        assertEquals(Int.MAX_VALUE, local.globalSettings[1]) // TYPE_PRIVATE = 1

        val resGroup = repo.setPeerTypeNotificationsEnabled(NotificationPeerType.GROUPS, true)
        assertTrue(resGroup.isSuccess)
        assertEquals(0, local.globalSettings[0]) // TYPE_GROUP = 0

        val resStories = repo.setPeerTypeNotificationsEnabled(NotificationPeerType.STORIES, false)
        assertTrue(resStories.isSuccess)
        assertFalse(local.boolPrefs["EnableAllStories"] ?: true)

        val resReactions = repo.setPeerTypeNotificationsEnabled(NotificationPeerType.REACTIONS_MESSAGES, false)
        assertTrue(resReactions.isSuccess)
        assertFalse(local.boolPrefs["EnableReactionsMessages"] ?: true)
    }

    @Test
    fun testInAppAndContactToggles() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        assertTrue(repo.setInChatSoundEnabled(false).isSuccess)
        assertFalse(local.inChatSound)

        assertTrue(repo.setInAppSoundsEnabled(false).isSuccess)
        assertFalse(local.boolPrefs["EnableInAppSounds"] ?: true)

        assertTrue(repo.setInAppVibrateEnabled(false).isSuccess)
        assertFalse(local.boolPrefs["EnableInAppVibrate"] ?: true)

        assertTrue(repo.setInAppPreviewEnabled(false).isSuccess)
        assertFalse(local.boolPrefs["EnableInAppPreview"] ?: true)

        assertTrue(repo.setPinnedMessagesNotificationsEnabled(false).isSuccess)
        assertFalse(local.boolPrefs["PinnedMessages"] ?: true)

        assertTrue(repo.setContactJoinedNotificationsEnabled(false).isSuccess)
        assertFalse(local.boolPrefs["EnableContactJoined"] ?: true)
        assertEquals(true, remote.silentSignUpNotification) // silent = !enabled
    }

    @Test
    fun testUpdateBadgeSettingsAndRefresh() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        val updatedSettings = BadgeSettingsModel(showBadgeNumber = false, showBadgeMuted = true, showBadgeMessages = false)
        assertTrue(repo.updateBadgeSettings(updatedSettings).isSuccess)
        assertFalse(local.badgeNumber)
        assertTrue(local.badgeMuted)
        assertFalse(local.badgeMessages)
        assertTrue(local.badgeUpdated)

        local.badgeUpdated = false
        assertTrue(repo.refreshBadge().isSuccess)
        assertTrue(local.badgeUpdated)
    }

    @Test
    fun testMuteDialogAndUntil() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        local.currentTimeValue = 1000
        assertFalse(repo.isDialogMuted(12345L, 0L))

        assertTrue(repo.muteDialog(12345L, 0L, true).isSuccess)
        assertTrue(repo.isDialogMuted(12345L, 0L))

        assertTrue(repo.muteDialog(12345L, 0L, false).isSuccess)
        assertFalse(repo.isDialogMuted(12345L, 0L))

        assertTrue(repo.muteDialogUntil(12345L, 10L, 5000).isSuccess)
        assertTrue(repo.isDialogMuted(12345L, 10L))
    }

    @Test
    fun testObserveFlowsEmitInitialValues() = runTest {
        val local = FakeNotificationsLocalDataSource(0)
        val remote = FakeNotificationsRemoteDataSource(0)
        val repo = NotificationsRepositoryImpl(0, local, remote, testDispatcher)

        local.totalUnread = 7
        val badge = repo.observeBadge().first()
        assertEquals(7, badge.totalUnreadCount)

        val settings = repo.observeSettings().first()
        assertNotNull(settings)

        val badgeSettings = repo.observeBadgeSettings().first()
        assertNotNull(badgeSettings)
    }

    @Test
    fun testContainerWiringDefaultsToImpl() {
        val container = AccountFeatureContainer.get(0)
        val repo = container.notificationsRepository
        assertTrue(repo is NotificationsRepositoryImpl)

        val created = container.createNotificationsRepository()
        assertTrue(created is NotificationsRepositoryImpl)
    }
}
