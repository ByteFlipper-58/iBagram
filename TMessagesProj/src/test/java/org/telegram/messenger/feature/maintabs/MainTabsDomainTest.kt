package org.telegram.messenger.feature.maintabs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.maintabs.data.mapper.MainTabsMapper
import org.telegram.messenger.feature.maintabs.data.repository.LegacyMainTabsRepository
import org.telegram.messenger.feature.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.maintabs.domain.model.MainTabsConfigModel
import org.telegram.messenger.feature.maintabs.domain.usecase.GetMainTabsConfigUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.ObserveMainTabsConfigUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SelectMainTabUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetContactsPermissionWarningUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetMainTabsVisibleUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetShowCallsTabUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.UpdateChatsUnreadCountUseCase
import org.telegram.messenger.feature.maintabs.presentation.MainTabsEvent
import org.telegram.messenger.feature.maintabs.presentation.MainTabsViewModel
import org.telegram.ui.MainTabsActivityController

@OptIn(ExperimentalCoroutinesApi::class)
class MainTabsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        try {
            org.telegram.messenger.UserConfig.getInstance(0)?.setShowCallsTab(false)
        } catch (_: Throwable) {}
    }

    @After
    fun tearDown() {
        try {
            org.telegram.messenger.UserConfig.getInstance(0)?.setShowCallsTab(false)
        } catch (_: Throwable) {}
        Dispatchers.resetMain()
    }

    @Test
    fun testMainTabsMapperAndPositions() {
        assertEquals(MainTabType.CHATS, MainTabsMapper.positionToTab(0, false))
        assertEquals(MainTabType.CONTACTS, MainTabsMapper.positionToTab(1, false))
        assertEquals(MainTabType.SETTINGS, MainTabsMapper.positionToTab(2, false))
        assertEquals(MainTabType.CALLS, MainTabsMapper.positionToTab(2, true))
        assertEquals(MainTabType.PROFILE, MainTabsMapper.positionToTab(3, false))

        assertEquals(MainTabType.CHATS, MainTabsMapper.indexToTab(0))
        assertEquals(MainTabType.CONTACTS, MainTabsMapper.indexToTab(1))
        assertEquals(MainTabType.SETTINGS, MainTabsMapper.indexToTab(2))
        assertEquals(MainTabType.CALLS, MainTabsMapper.indexToTab(3))
        assertEquals(MainTabType.PROFILE, MainTabsMapper.indexToTab(4))

        assertEquals(0, MainTabsMapper.tabToPosition(MainTabType.CHATS))
        assertEquals(1, MainTabsMapper.tabToPosition(MainTabType.CONTACTS))
        assertEquals(2, MainTabsMapper.tabToPosition(MainTabType.SETTINGS))
        assertEquals(2, MainTabsMapper.tabToPosition(MainTabType.CALLS))
        assertEquals(3, MainTabsMapper.tabToPosition(MainTabType.PROFILE))

        assertEquals("5", MainTabsMapper.formatBadgeCount(5))
        assertNull(MainTabsMapper.formatBadgeCount(0))
        assertNull(MainTabsMapper.formatBadgeCount(-1))
    }

    @Test
    fun testMainTabsConfigModel() {
        val configDefault = MainTabsConfigModel()
        assertTrue(configDefault.isTabsVisible)
        assertEquals(MainTabType.CHATS, configDefault.selectedTab)
        assertFalse(configDefault.showCallsTab)
        assertEquals(MainTabType.SETTINGS, configDefault.activeTabForPosition2)
        assertEquals(listOf(MainTabType.CHATS, MainTabType.CONTACTS, MainTabType.SETTINGS, MainTabType.PROFILE), configDefault.visibleTabs)
        assertFalse(configDefault.chatsBadge.isVisible)
        assertFalse(configDefault.contactsBadge.isVisible)

        val configWithCallsAndBadges = MainTabsConfigModel(
            isTabsVisible = false,
            selectedTab = MainTabType.PROFILE,
            showCallsTab = true,
            chatsUnreadCount = 42,
            hasContactsPermissionWarning = true
        )
        assertFalse(configWithCallsAndBadges.isTabsVisible)
        assertEquals(MainTabType.PROFILE, configWithCallsAndBadges.selectedTab)
        assertTrue(configWithCallsAndBadges.showCallsTab)
        assertEquals(MainTabType.CALLS, configWithCallsAndBadges.activeTabForPosition2)
        assertEquals(listOf(MainTabType.CHATS, MainTabType.CONTACTS, MainTabType.CALLS, MainTabType.PROFILE), configWithCallsAndBadges.visibleTabs)

        assertTrue(configWithCallsAndBadges.chatsBadge.isVisible)
        assertEquals("42", configWithCallsAndBadges.chatsBadge.text)
        assertFalse(configWithCallsAndBadges.chatsBadge.isWarning)

        assertTrue(configWithCallsAndBadges.contactsBadge.isVisible)
        assertEquals("!", configWithCallsAndBadges.contactsBadge.text)
        assertTrue(configWithCallsAndBadges.contactsBadge.isWarning)
    }

    @Test
    fun testRepositoryTabsVisibilityAndControllerDelegate() {
        var delegateInvokedWith: Boolean? = null
        val fakeController = MainTabsActivityController { visible ->
            delegateInvokedWith = visible
        }

        val repository = LegacyMainTabsRepository(
            currentAccount = 0,
            initialController = fakeController
        )

        assertTrue(repository.getConfig().isTabsVisible)

        repository.setTabsVisible(false)
        assertFalse(repository.getConfig().isTabsVisible)
        assertEquals(false, delegateInvokedWith)

        repository.setTabsVisible(true)
        assertTrue(repository.getConfig().isTabsVisible)
        assertEquals(true, delegateInvokedWith)
    }

    @Test
    fun testRepositoryTabSelectionAndCallsToggle() {
        val repository = LegacyMainTabsRepository(currentAccount = 0, initialShowCallsTab = false)

        // Выбор через позицию
        repository.selectPosition(1)
        assertEquals(MainTabType.CONTACTS, repository.getConfig().selectedTab)

        // Позиция 2 при showCallsTab = false -> SETTINGS
        repository.selectPosition(2)
        assertEquals(MainTabType.SETTINGS, repository.getConfig().selectedTab)

        // Включение showCallsTab должно обновить текущий выбранный таб позиции 2 на CALLS
        repository.setShowCallsTab(true)
        assertTrue(repository.getConfig().showCallsTab)
        assertEquals(MainTabType.CALLS, repository.getConfig().selectedTab)

        // Выбор профиля
        repository.selectTab(MainTabType.PROFILE)
        assertEquals(MainTabType.PROFILE, repository.getConfig().selectedTab)

        // Выключение showCallsTab не меняет PROFILE
        repository.setShowCallsTab(false)
        assertFalse(repository.getConfig().showCallsTab)
        assertEquals(MainTabType.PROFILE, repository.getConfig().selectedTab)
    }

    @Test
    fun testRepositoryBadges() {
        val repository = LegacyMainTabsRepository(currentAccount = 0)

        assertEquals(0, repository.getConfig().chatsUnreadCount)
        assertFalse(repository.getConfig().hasContactsPermissionWarning)

        repository.updateChatsUnreadCount(15)
        assertEquals(15, repository.getConfig().chatsUnreadCount)
        assertEquals("15", repository.getConfig().chatsBadge.text)

        repository.updateChatsUnreadCount(-5)
        assertEquals(0, repository.getConfig().chatsUnreadCount)
        assertNull(repository.getConfig().chatsBadge.text)

        repository.setContactsPermissionWarning(true)
        assertTrue(repository.getConfig().hasContactsPermissionWarning)
        assertEquals("!", repository.getConfig().contactsBadge.text)
        assertTrue(repository.getConfig().contactsBadge.isWarning)
    }

    @Test
    fun testViewModelMviEventsAndReactiveFlow() {
        val repository = LegacyMainTabsRepository(currentAccount = 0, initialShowCallsTab = false)

        val viewModel = MainTabsViewModel(
            observeConfigUseCase = ObserveMainTabsConfigUseCase(repository),
            getConfigUseCase = GetMainTabsConfigUseCase(repository),
            setTabsVisibleUseCase = SetMainTabsVisibleUseCase(repository),
            selectMainTabUseCase = SelectMainTabUseCase(repository),
            setShowCallsTabUseCase = SetShowCallsTabUseCase(repository),
            updateChatsUnreadCountUseCase = UpdateChatsUnreadCountUseCase(repository),
            setContactsPermissionWarningUseCase = SetContactsPermissionWarningUseCase(repository)
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isTabsVisible)
        assertEquals(MainTabType.CHATS, viewModel.uiState.value.selectedTab)

        viewModel.onEvent(MainTabsEvent.SetTabsVisible(false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isTabsVisible)

        viewModel.onEvent(MainTabsEvent.SelectPosition(3))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(MainTabType.PROFILE, viewModel.uiState.value.selectedTab)

        viewModel.onEvent(MainTabsEvent.SetShowCallsTab(true))
        viewModel.onEvent(MainTabsEvent.SelectPosition(2))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(MainTabType.CALLS, viewModel.uiState.value.selectedTab)

        viewModel.onEvent(MainTabsEvent.UpdateUnreadCount(7))
        viewModel.onEvent(MainTabsEvent.SetContactsWarning(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.chatsUnreadCount)
        assertTrue(viewModel.uiState.value.hasContactsPermissionWarning)
    }
}
