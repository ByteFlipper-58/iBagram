package org.telegram.messenger.feature.browser.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.browser.domain.model.BrowserState
import org.telegram.messenger.feature.browser.domain.model.BrowserType
import org.telegram.messenger.feature.browser.domain.model.UrlSafetyCheckResult

interface BrowserRepository {
    fun observeBrowserState(): Flow<BrowserState>
    fun getBrowserState(): BrowserState
    suspend fun updateBrowserType(type: BrowserType)
    suspend fun updateSettings(settings: BrowserSettingsModel)
    fun classifyUrl(url: String): UrlSafetyCheckResult
    suspend fun openUrl(url: String, forceExternal: Boolean = false): Boolean
    suspend fun addHistoryEntry(url: String, title: String? = null, domain: String? = null, siteName: String? = null, hasFavicon: Boolean = false)
    suspend fun getHistory(): List<BrowserHistoryEntryModel>
    suspend fun clearHistory()
    suspend fun clearCacheAndCookies()
}
