package org.telegram.messenger.feature.system.maintabs.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabsConfigModel
import org.telegram.ui.MainTabsActivityController

open class MainTabsLocalDataSource(
    private val currentAccount: Int = 0,
    initialController: MainTabsActivityController? = null,
    initialShowCallsTab: Boolean? = null,
    private val testMode: Boolean = false
) {
    private val lock = Any()

    @Volatile
    private var controllerDelegate: MainTabsActivityController? = initialController

    private val _configState: MutableStateFlow<MainTabsConfigModel>

    init {
        val callsTab = initialShowCallsTab ?: if (testMode) {
            false
        } else {
            try {
                UserConfig.getInstance(currentAccount)?.showCallsTab ?: false
            } catch (_: Throwable) {
                false
            }
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

    open fun setControllerDelegate(controller: MainTabsActivityController?) {
        synchronized(lock) {
            controllerDelegate = controller
        }
    }

    open fun observeConfig(): Flow<MainTabsConfigModel> = _configState.asStateFlow()

    open fun getConfig(): MainTabsConfigModel = _configState.value

    open fun setTabsVisible(visible: Boolean) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(isTabsVisible = visible)
            try {
                controllerDelegate?.setTabsVisible(visible)
            } catch (_: Throwable) {
                // Ignore failure in headless or detached environment
            }
        }
    }

    open fun selectTab(tab: MainTabType) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(selectedTab = tab)
        }
    }

    open fun selectPosition(position: Int) {
        synchronized(lock) {
            val tab = MainTabType.fromPosition(position, _configState.value.showCallsTab)
            _configState.value = _configState.value.copy(selectedTab = tab)
        }
    }

    open fun setShowCallsTab(show: Boolean) {
        synchronized(lock) {
            if (!testMode) {
                try {
                    UserConfig.getInstance(currentAccount)?.setShowCallsTab(show)
                } catch (_: Throwable) {
                    // Ignore in headless test environment
                }
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

    open fun updateChatsUnreadCount(unreadCount: Int) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(
                chatsUnreadCount = maxOf(0, unreadCount)
            )
        }
    }

    open fun setContactsPermissionWarning(hasWarning: Boolean) {
        synchronized(lock) {
            _configState.value = _configState.value.copy(
                hasContactsPermissionWarning = hasWarning
            )
        }
    }
}
