package org.telegram.messenger.feature.system.browser.domain.model

enum class BrowserType(val id: Int) {
    IN_APP(0),
    CUSTOM_TABS(1),
    EXTERNAL_BROWSER(2);

    companion object {
        fun fromId(id: Int): BrowserType = values().firstOrNull { it.id == id } ?: IN_APP
    }
}

enum class UrlTargetType {
    TELEGRAM_INTERNAL,
    INSTANT_VIEW,
    TON_SITE,
    EXTERNAL_SAFE,
    EXTERNAL_UNTRUSTED
}

data class UrlSafetyCheckResult(
    val url: String,
    val targetType: UrlTargetType,
    val requiresConfirmation: Boolean,
    val isPunycodeSpoof: Boolean,
    val isSafe: Boolean,
    val extractedUsername: String? = null,
    val host: String? = null
)

data class BrowserHistoryEntryModel(
    val id: Long,
    val timeMs: Long,
    val url: String,
    val title: String? = null,
    val domain: String? = null,
    val siteName: String? = null,
    val hasFavicon: Boolean = false
)

data class BrowserSettingsModel(
    val browserType: BrowserType = BrowserType.IN_APP,
    val allowCustomTabs: Boolean = true,
    val clearCookiesOnExit: Boolean = false,
    val warnOnExternalLinks: Boolean = true
)

data class BrowserState(
    val settings: BrowserSettingsModel = BrowserSettingsModel(),
    val recentHistory: List<BrowserHistoryEntryModel> = emptyList(),
    val lastOpenedUrl: String? = null,
    val isCustomTabsAvailable: Boolean = true
)
