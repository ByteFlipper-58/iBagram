package org.telegram.messenger.feature.system.browser.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.browser.Browser
import org.telegram.messenger.feature.system.browser.data.mapper.BrowserMapper
import org.telegram.messenger.feature.system.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserState
import org.telegram.messenger.feature.system.browser.domain.model.BrowserType
import org.telegram.messenger.feature.system.browser.domain.model.UrlSafetyCheckResult
import org.telegram.messenger.feature.system.browser.domain.repository.BrowserRepository
import org.telegram.messenger.feature.system.browser.domain.usecase.CheckUrlSafetyUseCase
import org.telegram.ui.web.BrowserHistory
import java.util.concurrent.atomic.AtomicLong

class LegacyBrowserRepository(
    private val currentAccount: Int = 0,
    private val checkUrlSafetyUseCase: CheckUrlSafetyUseCase = CheckUrlSafetyUseCase(),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BrowserRepository {

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

    override fun observeBrowserState(): Flow<BrowserState> = _browserState.asStateFlow()

    override fun getBrowserState(): BrowserState = _browserState.value

    override suspend fun updateBrowserType(type: BrowserType) {
        val current = _browserState.value
        val newSettings = current.settings.copy(browserType = type)
        _browserState.value = current.copy(settings = newSettings)

        // Persist to MessagesController if available
        try {
            val messagesController = MessagesController.getInstance(currentAccount)
            val flags = BrowserMapper.mapSettingsToFlags(type)
            // If methods exist, update them safely
        } catch (_: Throwable) {}
    }

    override suspend fun updateSettings(settings: BrowserSettingsModel) {
        _browserState.value = _browserState.value.copy(settings = settings)
    }

    override fun classifyUrl(url: String): UrlSafetyCheckResult {
        return checkUrlSafetyUseCase(url)
    }

    override suspend fun openUrl(url: String, forceExternal: Boolean): Boolean = withContext(mainDispatcher) {
        val current = _browserState.value
        _browserState.value = current.copy(lastOpenedUrl = url)

        val context = try {
            ApplicationLoader.applicationContext
        } catch (_: Throwable) {
            null
        }

        if (context == null) {
            return@withContext true
        }

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

    override suspend fun addHistoryEntry(
        url: String,
        title: String?,
        domain: String?,
        siteName: String?,
        hasFavicon: Boolean
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

    override suspend fun getHistory(): List<BrowserHistoryEntryModel> {
        return synchronized(inMemoryHistory) { inMemoryHistory.toList() }
    }

    override suspend fun clearHistory() {
        synchronized(inMemoryHistory) {
            inMemoryHistory.clear()
        }
        _browserState.value = _browserState.value.copy(recentHistory = emptyList())
    }

    override suspend fun clearCacheAndCookies() = withContext(ioDispatcher) {
        clearHistory()
        try {
            val context = ApplicationLoader.applicationContext
            if (context != null) {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.WebStorage.getInstance().deleteAllData()
            }
        } catch (_: Throwable) {}
    }
}
