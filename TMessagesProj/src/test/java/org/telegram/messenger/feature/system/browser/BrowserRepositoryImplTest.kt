package org.telegram.messenger.feature.system.browser

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.browser.data.datasource.BrowserLocalDataSource
import org.telegram.messenger.feature.system.browser.data.datasource.BrowserRemoteDataSource
import org.telegram.messenger.feature.system.browser.data.repository.BrowserRepositoryImpl
import org.telegram.messenger.feature.system.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserType
import org.telegram.messenger.feature.system.browser.domain.model.UrlTargetType

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var localDataSource: BrowserLocalDataSource
    private lateinit var remoteDataSource: BrowserRemoteDataSource
    private lateinit var repository: BrowserRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = BrowserLocalDataSource(
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
            testMode = true
        )
        remoteDataSource = BrowserRemoteDataSource(currentAccount = 0)
        repository = BrowserRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testGetBrowserStateReturnsDefaults() {
        val state = repository.getBrowserState()
        assertNotNull(state)
        assertEquals(BrowserType.IN_APP, state.settings.browserType)
        assertTrue(state.settings.allowCustomTabs)
        assertTrue(state.recentHistory.isEmpty())
    }

    @Test
    fun testObserveBrowserStateEmitsCurrentState() = runTest(testDispatcher) {
        val state = repository.observeBrowserState().first()
        assertNotNull(state)
        assertEquals(BrowserType.IN_APP, state.settings.browserType)
    }

    @Test
    fun testUpdateBrowserType() = runTest(testDispatcher) {
        repository.updateBrowserType(BrowserType.CUSTOM_TABS)
        assertEquals(BrowserType.CUSTOM_TABS, repository.getBrowserState().settings.browserType)

        repository.updateBrowserType(BrowserType.EXTERNAL_BROWSER)
        assertEquals(BrowserType.EXTERNAL_BROWSER, repository.getBrowserState().settings.browserType)
    }

    @Test
    fun testUpdateSettings() = runTest(testDispatcher) {
        val newSettings = BrowserSettingsModel(
            browserType = BrowserType.EXTERNAL_BROWSER,
            allowCustomTabs = false,
            clearCookiesOnExit = true,
            warnOnExternalLinks = false
        )
        repository.updateSettings(newSettings)

        val updated = repository.getBrowserState().settings
        assertEquals(BrowserType.EXTERNAL_BROWSER, updated.browserType)
        assertFalse(updated.allowCustomTabs)
        assertTrue(updated.clearCookiesOnExit)
        assertFalse(updated.warnOnExternalLinks)
    }

    @Test
    fun testClassifyUrl() {
        val tgResult = repository.classifyUrl("https://t.me/durov")
        assertNotNull(tgResult)
        assertEquals("durov", tgResult.extractedUsername)
        assertEquals(UrlTargetType.TELEGRAM_INTERNAL, tgResult.targetType)

        val webResult = repository.classifyUrl("https://google.com/search")
        assertNotNull(webResult)
        assertEquals("google.com", webResult.host)
        assertTrue(webResult.isSafe)
    }

    @Test
    fun testHistoryTrackingAndClear() = runTest(testDispatcher) {
        repository.addHistoryEntry(
            url = "https://telegram.org",
            title = "Telegram Messenger",
            domain = "telegram.org",
            siteName = "Telegram",
            hasFavicon = true
        )

        val history = repository.getHistory()
        assertEquals(1, history.size)
        assertEquals("https://telegram.org", history[0].url)
        assertEquals("Telegram Messenger", history[0].title)
        assertTrue(history[0].hasFavicon)

        repository.clearHistory()
        assertTrue(repository.getHistory().isEmpty())
        assertTrue(repository.getBrowserState().recentHistory.isEmpty())
    }

    @Test
    fun testOpenUrlInTestMode() = runTest(testDispatcher) {
        val opened = repository.openUrl("https://example.com")
        assertTrue(opened)
        assertEquals("https://example.com", repository.getBrowserState().lastOpenedUrl)
    }
}
