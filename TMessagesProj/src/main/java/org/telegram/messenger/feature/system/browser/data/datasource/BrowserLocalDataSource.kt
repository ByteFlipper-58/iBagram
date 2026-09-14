package org.telegram.messenger.feature.system.browser.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.browser.Browser
import org.telegram.messenger.feature.system.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserState
import org.telegram.messenger.feature.system.browser.domain.model.BrowserType
import org.telegram.messenger.feature.system.browser.domain.model.UrlSafetyCheckResult
import org.telegram.messenger.feature.system.browser.domain.usecase.CheckUrlSafetyUseCase
import java.util.concurrent.atomic.AtomicLong

/**
 * Local data source managing browser preferences, in-app browser state,
 * URL history, and external/custom tabs dispatching.
 */
class BrowserLocalDataSource(
    private val checkUrlSafetyUseCase: CheckUrlSafetyUseCase = CheckUrlSafetyUseCase(),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    var testMode: Boolean = false
) {

    private val idGenerator = AtomicLong(1000)
    private val inMemoryHistory = mutableListOf<BrowserHistoryEntryModel>()

    private val _browserState = MutableStateFlow(
        BrowserState(
            settings = BrowserSettingsModel(
                browserType = BrowserType.IN_APP,
                allowCustomTabs = true,
                clearCookiesOnExit = false,
                warnOnExternalLinks = true
            ),
            recentHistory = emptyList(),
            lastOpenedUrl = null,
            isCustomTabsAvailable = true
        )
    )

    fun observeBrowserState(): Flow<BrowserState> = _browserState.asStateFlow()

    fun getBrowserState(): BrowserState = _browserState.value

    fun updateBrowserType(type: BrowserType) {
        val current = _browserState.value
        val newSettings = current.settings.copy(browserType = type)
        _browserState.value = current.copy(settings = newSettings)
    }

    fun updateSettings(settings: BrowserSettingsModel) {
        _browserState.value = _browserState.value.copy(settings = settings)
    }

    fun classifyUrl(url: String): UrlSafetyCheckResult {
        return checkUrlSafetyUseCase(url)
    }

    suspend fun openUrl(url: String, forceExternal: Boolean = false): Boolean = withContext(mainDispatcher) {
        val current = _browserState.value
        _browserState.value = current.copy(lastOpenedUrl = url)

        if (testMode) {
            return@withContext true
        }

        val context = try {
            ApplicationLoader.applicationContext
        } catch (_: Throwable) {
            null
        } ?: return@withContext true

        try {
            if (forceExternal || current.settings.browserType == BrowserType.EXTERNAL_BROWSER) {
                Browser.openUrlInSystemBrowser(context, url)
            } else {
                val allowCustom = current.settings.browserType == BrowserType.CUSTOM_TABS
                Browser.openUrl(context, url, allowCustom)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun addHistoryEntry(
        url: String,
        title: String? = null,
        domain: String? = null,
        siteName: String? = null,
        hasFavicon: Boolean = false
    ) {
        val entry = BrowserHistoryEntryModel(
            id = idGenerator.incrementAndGet(),
            timeMs = System.currentTimeMillis(),
            url = url,
            title = title,
            domain = domain,
            siteName = siteName,
            hasFavicon = hasFavicon
        )
        synchronized(inMemoryHistory) {
            inMemoryHistory.add(0, entry)
            if (inMemoryHistory.size > 200) {
                inMemoryHistory.removeAt(inMemoryHistory.size - 1)
            }
        }
        val current = _browserState.value
        _browserState.value = current.copy(recentHistory = inMemoryHistory.toList())
    }

    fun getHistory(): List<BrowserHistoryEntryModel> {
        return synchronized(inMemoryHistory) { inMemoryHistory.toList() }
    }

    fun clearHistory() {
        synchronized(inMemoryHistory) {
            inMemoryHistory.clear()
        }
        _browserState.value = _browserState.value.copy(recentHistory = emptyList())
    }

    suspend fun clearCacheAndCookies() = withContext(ioDispatcher) {
        clearHistory()
    }
}
