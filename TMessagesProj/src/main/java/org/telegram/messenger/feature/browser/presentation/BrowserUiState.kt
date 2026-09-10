package org.telegram.messenger.feature.browser.presentation

import org.telegram.messenger.feature.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.browser.domain.model.BrowserType
import org.telegram.messenger.feature.browser.domain.model.UrlSafetyCheckResult

data class BrowserUiState(
    val settings: BrowserSettingsModel = BrowserSettingsModel(),
    val recentHistory: List<BrowserHistoryEntryModel> = emptyList(),
    val currentUrlCheck: UrlSafetyCheckResult? = null,
    val lastOpenedUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val currentBrowserType: BrowserType get() = settings.browserType
    val isWarnExternalLinks: Boolean get() = settings.warnOnExternalLinks
    val hasHistory: Boolean get() = recentHistory.isNotEmpty()
}
