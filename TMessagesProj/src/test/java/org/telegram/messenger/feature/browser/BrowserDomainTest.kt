package org.telegram.messenger.feature.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.browser.data.mapper.BrowserMapper
import org.telegram.messenger.feature.browser.data.repository.LegacyBrowserRepository
import org.telegram.messenger.feature.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.browser.domain.model.BrowserType
import org.telegram.messenger.feature.browser.domain.model.UrlTargetType
import org.telegram.messenger.feature.browser.domain.usecase.CheckUrlSafetyUseCase
import org.telegram.messenger.feature.browser.domain.usecase.ClassifyUrlTargetUseCase
import org.telegram.messenger.feature.browser.domain.usecase.ExtractUsernameFromUrlUseCase
import org.telegram.messenger.feature.browser.domain.usecase.GetBrowserStateUseCase
import org.telegram.messenger.feature.browser.domain.usecase.ManageBrowserHistoryUseCase
import org.telegram.messenger.feature.browser.domain.usecase.ObserveBrowserStateUseCase
import org.telegram.messenger.feature.browser.domain.usecase.OpenBrowserUrlUseCase
import org.telegram.messenger.feature.browser.domain.usecase.UpdateBrowserSettingsUseCase
import org.telegram.messenger.feature.browser.presentation.BrowserEvent
import org.telegram.messenger.feature.browser.presentation.BrowserViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testClassifyUrlTarget() {
        val classify = ClassifyUrlTargetUseCase()

        assertEquals(UrlTargetType.TELEGRAM_INTERNAL, classify("tg://resolve?domain=telegram"))
        assertEquals(UrlTargetType.TELEGRAM_INTERNAL, classify("tg:passport"))
        assertEquals(UrlTargetType.TELEGRAM_INTERNAL, classify("https://t.me/durov"))
        assertEquals(UrlTargetType.TELEGRAM_INTERNAL, classify("http://telegram.me/channel"))
        assertEquals(UrlTargetType.TELEGRAM_INTERNAL, classify("https://telegram.dog/group"))

        assertEquals(UrlTargetType.INSTANT_VIEW, classify("https://telegra.ph/my-article-01"))
        assertEquals(UrlTargetType.INSTANT_VIEW, classify("https://graph.org/sample-text"))
        assertEquals(UrlTargetType.INSTANT_VIEW, classify("https://t.me/iv?url=https://example.com"))

        assertEquals(UrlTargetType.TON_SITE, classify("ton://wallet/transfer"))
        assertEquals(UrlTargetType.TON_SITE, classify("https://foundation.ton"))

        assertEquals(UrlTargetType.EXTERNAL_SAFE, classify("https://fragment.com/username/vip"))
        assertEquals(UrlTargetType.EXTERNAL_SAFE, classify("https://telegram.org/blog/updates"))

        assertEquals(UrlTargetType.EXTERNAL_UNTRUSTED, classify("https://www.google.com/search"))
        assertEquals(UrlTargetType.EXTERNAL_UNTRUSTED, classify("http://insecure-site.org"))
    }

    @Test
    fun testExtractUsernameFromUrl() {
        val extract = ExtractUsernameFromUrlUseCase()

        assertEquals("durov", extract("@durov"))
        assertEquals("telegram", extract("https://t.me/telegram"))
        assertEquals("news_channel", extract("http://telegram.me/news_channel"))
        assertEquals("super_bot", extract("https://telegram.dog/super_bot?start=123"))

        // System keywords should not be considered usernames
        assertNull(extract("https://t.me/iv"))
        assertNull(extract("https://t.me/joinchat"))
        assertNull(extract("https://t.me/share"))
        assertNull(extract("invalid-link"))
    }

    @Test
    fun testUrlSafetyCheckAndPunycodeSpoofing() {
        val check = CheckUrlSafetyUseCase()

        // 1. Safe internal Telegram URL
        val internalResult = check("tg://resolve?domain=test")
        assertEquals(UrlTargetType.TELEGRAM_INTERNAL, internalResult.targetType)
        assertFalse(internalResult.requiresConfirmation)
        assertTrue(internalResult.isSafe)
        assertFalse(internalResult.isPunycodeSpoof)

        // 2. Safe external domain
        val safeResult = check("https://fragment.com/stars")
        assertEquals(UrlTargetType.EXTERNAL_SAFE, safeResult.targetType)
        assertFalse(safeResult.requiresConfirmation)
        assertTrue(safeResult.isSafe)

        // 3. Untrusted external link
        val untrustedResult = check("https://random-blog.net/post/1")
        assertEquals(UrlTargetType.EXTERNAL_UNTRUSTED, untrustedResult.targetType)
        assertTrue(untrustedResult.requiresConfirmation)

        // 4. Punycode IDN domain
        val punycodeResult = check("https://xn--e1afmkfd.xn--p1ai/login")
        assertTrue(punycodeResult.isPunycodeSpoof)
        assertTrue(punycodeResult.requiresConfirmation)
        assertFalse(punycodeResult.isSafe)

        // 5. Homoglyph spoofing attack (mixing Latin 'g' with Cyrillic 'о')
        val homoglyphResult = check("https://g\u043E\u043Egle.com/secure")
        assertTrue(homoglyphResult.isPunycodeSpoof)
        assertTrue(homoglyphResult.requiresConfirmation)
    }

    @Test
    fun testBrowserHistoryAndStateFlow() = runTest(testDispatcher) {
        val repository = LegacyBrowserRepository(
            currentAccount = 0,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )

        // Initial state
        val initialState = repository.getBrowserState()
        assertEquals(BrowserType.IN_APP, initialState.settings.browserType)
        assertTrue(initialState.recentHistory.isEmpty())

        // Add history entry
        repository.addHistoryEntry("https://telegra.ph/first", "First Article", domain = "telegra.ph", siteName = "Telegraph", hasFavicon = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val history = repository.getHistory()
        assertEquals(1, history.size)
        assertEquals("https://telegra.ph/first", history[0].url)
        assertEquals("First Article", history[0].title)
        assertEquals("telegra.ph", history[0].domain)
        assertTrue(history[0].hasFavicon)

        // Add second entry
        repository.addHistoryEntry("https://example.com/second", "Second Site", domain = "example.com", siteName = null, hasFavicon = false)
        testDispatcher.scheduler.advanceUntilIdle()

        val history2 = repository.getHistory()
        assertEquals(2, history2.size)
        assertEquals("https://example.com/second", history2[0].url)

        // Clear history
        repository.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.getHistory().isEmpty())
        assertTrue(repository.getBrowserState().recentHistory.isEmpty())
    }

    @Test
    fun testBrowserViewModelMviLifecycle() = runTest(testDispatcher) {
        val repository = LegacyBrowserRepository(
            currentAccount = 0,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
        val checkSafety = CheckUrlSafetyUseCase()
        val observeState = ObserveBrowserStateUseCase(repository)
        val updateSettings = UpdateBrowserSettingsUseCase(repository)
        val openUrl = OpenBrowserUrlUseCase(repository, checkSafety)
        val manageHistory = ManageBrowserHistoryUseCase(repository)

        val viewModel = BrowserViewModel(
            observeBrowserState = observeState,
            updateBrowserSettings = updateSettings,
            checkUrlSafety = checkSafety,
            openBrowserUrl = openUrl,
            manageBrowserHistory = manageHistory
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(BrowserType.IN_APP, viewModel.uiState.value.currentBrowserType)

        // 1. Change browser type to CUSTOM_TABS
        viewModel.onEvent(BrowserEvent.SetBrowserType(BrowserType.CUSTOM_TABS))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(BrowserType.CUSTOM_TABS, viewModel.uiState.value.currentBrowserType)

        // 2. Toggle warning
        viewModel.onEvent(BrowserEvent.ToggleWarnExternalLinks(false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isWarnExternalLinks)

        // 3. Check URL
        viewModel.onEvent(BrowserEvent.CheckUrl("https://t.me/durov"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentUrlCheck)
        assertEquals("durov", viewModel.uiState.value.currentUrlCheck?.extractedUsername)

        // 4. Open URL
        viewModel.onEvent(BrowserEvent.OpenUrl("https://telegra.ph/post"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("https://telegra.ph/post", viewModel.uiState.value.lastOpenedUrl)
        assertTrue(viewModel.uiState.value.hasHistory)

        // 5. Clear history
        viewModel.onEvent(BrowserEvent.ClearHistory)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasHistory)
    }
}
