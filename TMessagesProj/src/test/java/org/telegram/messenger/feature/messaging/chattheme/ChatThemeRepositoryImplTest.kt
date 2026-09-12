package org.telegram.messenger.feature.messaging.chattheme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.ChatThemeController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chattheme.data.datasource.ChatThemeLocalDataSource
import org.telegram.messenger.feature.messaging.chattheme.data.datasource.ChatThemeRemoteDataSource
import org.telegram.messenger.feature.messaging.chattheme.data.repository.ChatThemeRepositoryImpl
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.ActionBar.EmojiThemes
import org.telegram.ui.ActionBar.theme.ThemeKey

@OptIn(ExperimentalCoroutinesApi::class)
class ChatThemeRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeChatThemeLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeChatThemeRemoteDataSource
    private lateinit var repository: ChatThemeRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeChatThemeLocalDataSource()
        fakeRemoteDataSource = FakeChatThemeRemoteDataSource()
        repository = ChatThemeRepositoryImpl(
            account = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetDialogThemeState() = runTest {
        val wallpaper = TLRPC.TL_wallPaper().apply {
            id = 555L
            slug = "custom_slug"
        }
        fakeLocalDataSource.wallpaperToReturn = wallpaper

        val result = repository.getDialogThemeState(100L)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(100L, data.dialogId)
        assertEquals(555L, data.wallpaperId)
        assertEquals("custom_slug", data.wallpaperSlug)
    }

    @Test
    fun testObserveDialogTheme() = runTest {
        val wallpaper = TLRPC.TL_wallPaper().apply {
            id = 777L
            slug = "observed_slug"
        }
        fakeLocalDataSource.wallpaperToReturn = wallpaper

        val firstState = repository.observeDialogTheme(200L).first()
        assertEquals(200L, firstState.dialogId)
        assertEquals(777L, firstState.wallpaperId)
        assertEquals("observed_slug", firstState.wallpaperSlug)
    }

    @Test
    fun testGetAvailableThemesSuccess() = runTest {
        val tlTheme = TLRPC.TL_theme().apply {
            id = 123L
            emoticon = "🎉"
        }
        val tlThemes = TL_account.TL_themes().apply {
            hash = 999L
            themes.add(tlTheme)
        }
        fakeRemoteDataSource.themesResponse = tlThemes

        val result = repository.getAvailableThemes(withDefault = false)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertTrue(fakeLocalDataSource.saveThemesCalled)
        assertEquals(999L, fakeLocalDataSource.lastSavedHash)
    }

    @Test
    fun testGetAvailableThemesFailureWithFallback() = runTest {
        val cachedTheme = EmojiThemes(0).apply {
            emoji = "🎉"
            key = ThemeKey.ofEmoticon("🎉")
        }
        fakeLocalDataSource.themeToReturn = cachedTheme
        fakeRemoteDataSource.shouldFail = true
        fakeLocalDataSource.themesHashToReturn = 100L
        fakeLocalDataSource.lastReloadToReturn = 0L // Outdated reload triggers remote, which fails, but cached fallback is used

        val result = repository.getAvailableThemes(withDefault = false)
        assertTrue(result is Result.Success)
        val themes = (result as Result.Success).data
        assertEquals(1, themes.size)
        assertEquals("🎉", themes[0].emoticon)
    }

    @Test
    fun testGetAvailableThemesFailureWithoutCache() = runTest {
        fakeRemoteDataSource.shouldFail = true
        fakeLocalDataSource.themeToReturn = null
        fakeLocalDataSource.themesHashToReturn = 0L

        val result = repository.getAvailableThemes(withDefault = false)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun testSetDialogThemeWithEmoticon() = runTest {
        val updates = TLRPC.TL_updates()
        fakeRemoteDataSource.setThemeUpdatesResponse = updates

        val result = repository.setDialogTheme(100L, "🔥", null)
        assertTrue(result is Result.Success)
        assertEquals("🔥", fakeLocalDataSource.lastSetThemeKey?.emoticon)
        assertEquals(1, fakeLocalDataSource.processUpdatesCount)
    }

    @Test
    fun testSetDialogThemeWithGiftSlug() = runTest {
        val updates = TLRPC.TL_updates()
        fakeRemoteDataSource.setThemeUpdatesResponse = updates

        val result = repository.setDialogTheme(100L, null, "super_gift")
        assertTrue(result is Result.Success)
        assertEquals("super_gift", fakeLocalDataSource.lastSetThemeKey?.giftSlug)
        assertEquals(1, fakeLocalDataSource.processUpdatesCount)
    }

    @Test
    fun testResetDialogTheme() = runTest {
        val result = repository.resetDialogTheme(100L)
        assertTrue(result is Result.Success)
        assertNull(fakeLocalDataSource.lastSetThemeKey)
    }

    @Test
    fun testSaveChatWallpaper() = runTest {
        val saveResult = repository.saveChatWallpaper(100L, 888L)
        assertTrue(saveResult is Result.Success)
        assertEquals(888L, fakeLocalDataSource.lastSavedWallpaper?.id)

        val resetResult = repository.saveChatWallpaper(100L, null)
        assertTrue(resetResult is Result.Success)
        assertNull(fakeLocalDataSource.lastSavedWallpaper)
    }

    @Test
    fun testStranglerHook() {
        val repo = ChatThemeController.getChatThemeRepository(0)
        assertNotNull(repo)
    }

    private class FakeChatThemeLocalDataSource : ChatThemeLocalDataSource(0) {
        var wallpaperToReturn: TLRPC.WallPaper? = null
        var themeToReturn: EmojiThemes? = null
        var themesHashToReturn: Long = 0L
        var lastReloadToReturn: Long = 0L
        var saveThemesCalled = false
        var lastSavedHash: Long = 0L
        var lastSetThemeKey: ThemeKey? = null
        var lastSavedWallpaper: TLRPC.WallPaper? = null
        var processUpdatesCount = 0
        var peerToReturn: TLRPC.InputPeer? = TLRPC.TL_inputPeerChat().apply { chat_id = 100L }

        override fun getThemesHash(): Long = themesHashToReturn

        override fun getLastReloadTimeMs(): Long = lastReloadToReturn

        override fun saveThemesToPrefs(themes: List<TLRPC.TL_theme>, hash: Long, lastReloadTimeMs: Long) {
            saveThemesCalled = true
            lastSavedHash = hash
            themesHashToReturn = hash
            lastReloadToReturn = lastReloadTimeMs
        }

        override fun getDialogTheme(dialogId: Long): EmojiThemes? = themeToReturn

        override fun getDialogWallpaper(dialogId: Long): TLRPC.WallPaper? = wallpaperToReturn

        override fun getEmojiThemes(withDefault: Boolean): List<EmojiThemes> {
            return if (themeToReturn != null) listOf(themeToReturn!!) else emptyList()
        }

        override fun getInputPeer(dialogId: Long): TLRPC.InputPeer? = peerToReturn

        override fun setDialogTheme(dialogId: Long, themeKey: ThemeKey?) {
            lastSetThemeKey = themeKey
        }

        override fun saveChatWallpaper(dialogId: Long, wallPaper: TLRPC.WallPaper?) {
            lastSavedWallpaper = wallPaper
            wallpaperToReturn = wallPaper
        }

        override suspend fun processUpdates(updates: TLRPC.Updates) {
            processUpdatesCount++
        }
    }

    private class FakeChatThemeRemoteDataSource : ChatThemeRemoteDataSource(0) {
        var shouldFail = false
        var themesResponse: TLObject = TL_account.TL_themes()
        var setThemeUpdatesResponse: TLRPC.Updates = TLRPC.TL_updates()

        override suspend fun getChatThemes(hash: Long): Result<TLObject> {
            if (shouldFail) return Result.failure("Network error")
            return Result.Success(themesResponse)
        }

        override suspend fun setChatTheme(peer: TLRPC.InputPeer, theme: TLRPC.InputChatTheme): Result<TLRPC.Updates> {
            if (shouldFail) return Result.failure("Network error")
            return Result.Success(setThemeUpdatesResponse)
        }
    }
}
