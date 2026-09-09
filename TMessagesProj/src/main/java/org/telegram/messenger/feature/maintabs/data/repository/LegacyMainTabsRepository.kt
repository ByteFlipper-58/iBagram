package org.telegram.messenger.feature.maintabs.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.maintabs.domain.model.MainTabsConfigModel
import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository
import org.telegram.ui.MainTabsActivityController

class LegacyMainTabsRepository(
    private val currentAccount: Int,
    initialController: MainTabsActivityController? = null,
    initialShowCallsTab: Boolean? = null
) : MainTabsRepository {

    private val lock = Any()
    @Volatile
    private var controllerDelegate: MainTabsActivityController? = initialController

    private val _configState: MutableStateFlow<MainTabsConfigModel>

    init {
        val callsTab = initialShowCallsTab ?: try {
            UserConfig.getInstance(currentAccount)?.showCallsTab ?: false
        } catch (_: Throwable) {
            false
        }

        _configState = MutableStateFlow(
            MainTabsConfigModel(
                isTabsVisible = true,
                selectedTab = MainTabType.CHATS,
                showCallsTab = callsTab,
                chatsUnreadCount = 0,
                hasContactsPermissionWarning = false
            )
        )
    }

    fun setControllerDelegate(controller: MainTabsActivityController?) {
        synchronized(lock) {
            controllerDelegate = controller
        }
    }

    override fun observeConfig(): Flow<MainTabsConfigModel> = _configState.asStateFlow()

    override fun getConfig(): MainTabsConfigModel = _configState.value

    override fun setTabsVisible(visible: Boolean) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(isTabsVisible = visible)
            try {
                controllerDelegate?.setTabsVisible(visible)
            } catch (_: Throwable) {
                // Ignore failure in headless or detached environment
            }
        }
    }

    override fun selectTab(tab: MainTabType) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(selectedTab = tab)
        }
    }

    override fun selectPosition(position: Int) {
        synchronized(lock) {
            val tab = MainTabType.fromPosition(position, _configState.value.showCallsTab)
            _configState.value = _configState.value.copy(selectedTab = tab)
        }
    }

    override fun setShowCallsTab(show: Boolean) {
        synchronized(lock) {
            try {
                UserConfig.getInstance(currentAccount)?.setShowCallsTab(show)
            } catch (_: Throwable) {
                // Ignore in headless test environment
            }

            val currentSelected = _configState.value.selectedTab
            val newSelected = if (currentSelected.position == 2) {
                if (show) MainTabType.CALLS else MainTabType.SETTINGS
            } else {
                currentSelected
            }

            _configState.value = _configState.value.copy(
                showCallsTab = show,
                selectedTab = newSelected
            )
        }
    }

    override fun updateChatsUnreadCount(unreadCount: Int) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(
                chatsUnreadCount = maxOf(0, unreadCount)
            )
        }
    }

    override fun setContactsPermissionWarning(hasWarning: Boolean) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(
                hasContactsPermissionWarning = hasWarning
            )
        }
    }
}
