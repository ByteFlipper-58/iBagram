package org.telegram.messenger.feature.system.maintabs

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.maintabs.data.datasource.MainTabsLocalDataSource
import org.telegram.messenger.feature.system.maintabs.data.datasource.MainTabsRemoteDataSource
import org.telegram.messenger.feature.system.maintabs.data.repository.MainTabsRepositoryImpl
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabType
import org.telegram.ui.MainTabsActivityController

class MainTabsRepositoryImplTest {

    private lateinit var localDataSource: MainTabsLocalDataSource
    private lateinit var remoteDataSource: MainTabsRemoteDataSource
    private lateinit var repository: MainTabsRepositoryImpl
    private var controllerTabsVisible: Boolean? = null

    private val fakeController = object : MainTabsActivityController {
        override fun setTabsVisible(visible: Boolean) {
            controllerTabsVisible = visible
        }
    }

    @Before
    fun setUp() {
        controllerTabsVisible = null
        localDataSource = MainTabsLocalDataSource(
            currentAccount = 0,
            initialController = fakeController,
            initialShowCallsTab = false,
            testMode = true
        )
        remoteDataSource = MainTabsRemoteDataSource(0)
        repository = MainTabsRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialConfig() {
        val config = repository.getConfig()
        assertTrue(config.isTabsVisible)
        assertEquals(MainTabType.CHATS, config.selectedTab)
        assertFalse(config.showCallsTab)
        assertEquals(0, config.chatsUnreadCount)
    }

    @Test
    fun testSetTabsVisible() {
        repository.setTabsVisible(false)
        assertFalse(repository.getConfig().isTabsVisible)
        assertEquals(false, controllerTabsVisible)

        repository.setTabsVisible(true)
        assertTrue(repository.getConfig().isTabsVisible)
        assertEquals(true, controllerTabsVisible)
    }

    @Test
    fun testSelectTabAndPosition() {
        repository.selectTab(MainTabType.SETTINGS)
        assertEquals(MainTabType.SETTINGS, repository.getConfig().selectedTab)

        repository.selectPosition(1)
        assertEquals(MainTabType.CONTACTS, repository.getConfig().selectedTab)
    }

    @Test
    fun testShowCallsTab() {
        repository.setShowCallsTab(true)
        assertTrue(repository.getConfig().showCallsTab)

        repository.selectPosition(2)
        assertEquals(MainTabType.CALLS, repository.getConfig().selectedTab)

        repository.setShowCallsTab(false)
        assertFalse(repository.getConfig().showCallsTab)
        assertEquals(MainTabType.SETTINGS, repository.getConfig().selectedTab)
    }

    @Test
    fun testUnreadCountAndPermissionWarning() {
        repository.updateChatsUnreadCount(5)
        assertEquals(5, repository.getConfig().chatsUnreadCount)

        repository.updateChatsUnreadCount(-10)
        assertEquals(0, repository.getConfig().chatsUnreadCount)

        repository.setContactsPermissionWarning(true)
        assertTrue(repository.getConfig().hasContactsPermissionWarning)
    }

    @Test
    fun testRemoteDataSource() = runBlocking {
        val result = remoteDataSource.fetchRemoteTabsConfig()
        assertTrue(result.isSuccess)
    }
}
