package org.telegram.messenger.feature.system.browser.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.browser.data.datasource.BrowserLocalDataSource
import org.telegram.messenger.feature.system.browser.data.datasource.BrowserRemoteDataSource
import org.telegram.messenger.feature.system.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserState
import org.telegram.messenger.feature.system.browser.domain.model.BrowserType
import org.telegram.messenger.feature.system.browser.domain.model.UrlSafetyCheckResult
import org.telegram.messenger.feature.system.browser.domain.repository.BrowserRepository

/**
 * Implementation of [BrowserRepository] coordinating local in-app browser features
 * and remote browsing safety checks.
 */
class BrowserRepositoryImpl(
    private val localDataSource: BrowserLocalDataSource,
    private val remoteDataSource: BrowserRemoteDataSource
) : BrowserRepository {

    override fun observeBrowserState(): Flow<BrowserState> {
        return localDataSource.observeBrowserState()
    }

    override fun getBrowserState(): BrowserState {
        return localDataSource.getBrowserState()
    }

    override suspend fun updateBrowserType(type: BrowserType) {
        localDataSource.updateBrowserType(type)
    }

    override suspend fun updateSettings(settings: BrowserSettingsModel) {
        localDataSource.updateSettings(settings)
    }

    override fun classifyUrl(url: String): UrlSafetyCheckResult {
        return localDataSource.classifyUrl(url)
    }

    override suspend fun openUrl(url: String, forceExternal: Boolean): Boolean {
        return localDataSource.openUrl(url, forceExternal)
    }

    override suspend fun addHistoryEntry(
        url: String,
        title: String?,
        domain: String?,
        siteName: String?,
        hasFavicon: Boolean
    ) {
        localDataSource.addHistoryEntry(url, title, domain, siteName, hasFavicon)
    }

    override suspend fun getHistory(): List<BrowserHistoryEntryModel> {
        return localDataSource.getHistory()
    }

    override suspend fun clearHistory() {
        localDataSource.clearHistory()
    }

    override suspend fun clearCacheAndCookies() {
        localDataSource.clearCacheAndCookies()
    }
}
