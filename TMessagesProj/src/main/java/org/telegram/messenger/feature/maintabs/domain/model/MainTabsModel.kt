package org.telegram.messenger.feature.maintabs.domain.model

enum class MainTabType(val position: Int, val legacyIndex: Int) {
    CHATS(position = 0, legacyIndex = 0),
    CONTACTS(position = 1, legacyIndex = 1),
    SETTINGS(position = 2, legacyIndex = 2),
    CALLS(position = 2, legacyIndex = 3),
    PROFILE(position = 3, legacyIndex = 4);

    companion object {
        fun fromPosition(position: Int, showCallsTab: Boolean): MainTabType {
            return when (position) {
                0 -> CHATS
                1 -> CONTACTS
                2 -> if (showCallsTab) CALLS else SETTINGS
                3 -> PROFILE
                else -> CHATS
            }
        }

        fun fromLegacyIndex(index: Int): MainTabType {
            return when (index) {
                0 -> CHATS
                1 -> CONTACTS
                2 -> SETTINGS
                3 -> CALLS
                4 -> PROFILE
                else -> CHATS
            }
        }
    }
}

data class MainTabBadgeModel(
    val text: String? = null,
    val isWarning: Boolean = false
) {
    val isVisible: Boolean
        get() = !text.isNullOrEmpty()
}

data class MainTabsConfigModel(
    val isTabsVisible: Boolean = true,
    val selectedTab: MainTabType = MainTabType.CHATS,
    val showCallsTab: Boolean = false,
    val chatsUnreadCount: Int = 0,
    val hasContactsPermissionWarning: Boolean = false
) {
    val activeTabForPosition2: MainTabType
        get() = if (showCallsTab) MainTabType.CALLS else MainTabType.SETTINGS

    val visibleTabs: List<MainTabType>
        get() = listOf(MainTabType.CHATS, MainTabType.CONTACTS, activeTabForPosition2, MainTabType.PROFILE)

    val chatsBadge: MainTabBadgeModel
        get() = if (chatsUnreadCount > 0) {
            MainTabBadgeModel(text = chatsUnreadCount.toString(), isWarning = false)
        } else {
            MainTabBadgeModel()
        }

    val contactsBadge: MainTabBadgeModel
        get() = if (hasContactsPermissionWarning) {
            MainTabBadgeModel(text = "!", isWarning = true)
        } else {
            MainTabBadgeModel()
        }
}
