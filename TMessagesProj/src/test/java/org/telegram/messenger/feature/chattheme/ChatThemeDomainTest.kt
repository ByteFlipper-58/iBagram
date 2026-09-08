package org.telegram.messenger.feature.chattheme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.chattheme.domain.model.DialogThemeStateModel
import org.telegram.messenger.feature.chattheme.domain.repository.ChatThemeRepository
import org.telegram.messenger.feature.chattheme.domain.usecase.GetAvailableChatThemesUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.GetDialogThemeStateUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.ObserveDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.ResetDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.SaveChatWallpaperUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.SetDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.presentation.ChatThemeEvent
import org.telegram.messenger.feature.chattheme.presentation.ChatThemeUiState
import org.telegram.messenger.feature.chattheme.presentation.ChatThemeViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ChatThemeDomainTest {

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
    fun testChatThemeModelKeyAndGiftProperties() {
        val emoticonTheme = ChatThemeModel(
            emoticon = "🌙",
            giftSlug = null,
            isDefault = false,
            themeId = 12345L
        )
        assertEquals("emoticon_🌙", emoticonTheme.key)
        assertFalse(emoticonTheme.isGiftTheme)

        val giftTheme = ChatThemeModel(
            emoticon = null,
            giftSlug = "star_gift_42",
            isDefault = false,
            themeId = 99999L
        )
        assertEquals("gift_star_gift_42", giftTheme.key)
        assertTrue(giftTheme.isGiftTheme)

        val defaultTheme = ChatThemeModel(
            emoticon = null,
            giftSlug = null,
            isDefault = true
        )
        assertEquals("default", defaultTheme.key)
        assertFalse(defaultTheme.isGiftTheme)
    }

    @Test
    fun testDialogThemeStateModelFlags() {
        val emptyState = DialogThemeStateModel(dialogId = 100L)
        assertFalse(emptyState.hasCustomTheme)
        assertFalse(emptyState.hasCustomWallpaper)

        val defaultThemeState = DialogThemeStateModel(
            dialogId = 101L,
            currentTheme = ChatThemeModel(isDefault = true),
            wallpaperId = 0L
        )
        assertFalse(defaultThemeState.hasCustomTheme)
        assertFalse(defaultThemeState.hasCustomWallpaper)

        val customState = DialogThemeStateModel(
            dialogId = 102L,
            currentTheme = ChatThemeModel(emoticon = "🔥", isDefault = false),
            wallpaperId = 777L
        )
        assertTrue(customState.hasCustomTheme)
        assertTrue(customState.hasCustomWallpaper)
    }

    @Test
    fun testUseCasesWithFakeRepository() = runTest {
        val fakeRepo = FakeChatThemeRepository()

        val observeUseCase = ObserveDialogThemeUseCase(fakeRepo)
        val getStateUseCase = GetDialogThemeStateUseCase(fakeRepo)
        val getThemesUseCase = GetAvailableChatThemesUseCase(fakeRepo)
        val setUseCase = SetDialogThemeUseCase(fakeRepo)
        val resetUseCase = ResetDialogThemeUseCase(fakeRepo)
        val saveWallpaperUseCase = SaveChatWallpaperUseCase(fakeRepo)

        // 1. Get available themes
        val themesResult = getThemesUseCase()
        assertTrue(themesResult is Result.Success)
        assertEquals(3, (themesResult as Result.Success).data.size)

        // 2. Initial state
        val initialState = getStateUseCase(100L)
        assertTrue(initialState is Result.Success)
        val stateData = (initialState as Result.Success).data
        assertEquals(100L, stateData.dialogId)
        assertNull(stateData.currentTheme)

        // 3. Set custom theme
        val setResult = setUseCase(100L, "🎄", null)
        assertTrue(setResult is Result.Success)
        val updatedState = getStateUseCase(100L)
        assertEquals("🎄", (updatedState as Result.Success).data.currentTheme?.emoticon)

        // 4. Save chat wallpaper
        val saveWallpaperResult = saveWallpaperUseCase(100L, 555L)
        assertTrue(saveWallpaperResult is Result.Success)
        val withWallpaperState = getStateUseCase(100L)
        assertEquals(555L, (withWallpaperState as Result.Success).data.wallpaperId)

        // 5. Reset theme
        val resetResult = resetUseCase(100L)
        assertTrue(resetResult is Result.Success)
        val resetState = getStateUseCase(100L)
        assertNull((resetState as Result.Success).data.currentTheme)
    }

    @Test
    fun testChatThemeViewModelWorkflow() = runTest {
        val fakeRepo = FakeChatThemeRepository()

        val viewModel = ChatThemeViewModel(
            observeDialogThemeUseCase = ObserveDialogThemeUseCase(fakeRepo),
            getDialogThemeStateUseCase = GetDialogThemeStateUseCase(fakeRepo),
            getAvailableThemesUseCase = GetAvailableChatThemesUseCase(fakeRepo),
            setDialogThemeUseCase = SetDialogThemeUseCase(fakeRepo),
            resetDialogThemeUseCase = ResetDialogThemeUseCase(fakeRepo),
            saveChatWallpaperUseCase = SaveChatWallpaperUseCase(fakeRepo)
        )

        assertEquals(ChatThemeUiState.Initial, viewModel.uiState.value)

        // Load themes
        viewModel.onEvent(ChatThemeEvent.LoadThemes(100L))
        advanceUntilIdle()

        val loadedState = viewModel.uiState.value
        assertTrue(loadedState is ChatThemeUiState.Success)
        val successState = loadedState as ChatThemeUiState.Success
        assertEquals(3, successState.availableThemes.size)
        assertEquals(100L, successState.dialogThemeState.dialogId)

        // Select theme
        val themeToSelect = successState.availableThemes[1]
        viewModel.onEvent(ChatThemeEvent.SelectTheme(themeToSelect))
        val selectedState = viewModel.uiState.value as ChatThemeUiState.Success
        assertEquals(themeToSelect, selectedState.selectedTheme)

        // Apply theme
        viewModel.onEvent(ChatThemeEvent.ApplyTheme(100L, themeToSelect))
        advanceUntilIdle()

        val appliedState = viewModel.uiState.value as ChatThemeUiState.Success
        assertFalse(appliedState.isSaving)
        assertNull(appliedState.error)
        assertEquals("⭐", appliedState.selectedTheme?.emoticon)

        // Reset theme
        viewModel.onEvent(ChatThemeEvent.ResetTheme(100L))
        advanceUntilIdle()

        val resetState = viewModel.uiState.value as ChatThemeUiState.Success
        assertFalse(resetState.isSaving)
        assertNull(resetState.selectedTheme)
    }

    private class FakeChatThemeRepository : ChatThemeRepository {
        private val states = mutableMapOf<Long, DialogThemeStateModel>()
        private val flowMap = mutableMapOf<Long, MutableSharedFlow<DialogThemeStateModel>>()

        private val availableThemes = listOf(
            ChatThemeModel(isDefault = true),
            ChatThemeModel(emoticon = "⭐", themeId = 1L),
            ChatThemeModel(giftSlug = "vip_stars", themeId = 2L)
        )

        private fun getOrCreateFlow(dialogId: Long): MutableSharedFlow<DialogThemeStateModel> {
            return flowMap.getOrPut(dialogId) { MutableSharedFlow(replay = 1) }
        }

        override fun observeDialogTheme(dialogId: Long): Flow<DialogThemeStateModel> {
            val flow = getOrCreateFlow(dialogId)
            val currentState = states.getOrPut(dialogId) { DialogThemeStateModel(dialogId) }
            flow.tryEmit(currentState)
            return flow
        }

        override suspend fun getDialogThemeState(dialogId: Long): Result<DialogThemeStateModel> {
            val state = states.getOrPut(dialogId) { DialogThemeStateModel(dialogId) }
            return Result.Success(state)
        }

        override suspend fun getAvailableThemes(withDefault: Boolean): Result<List<ChatThemeModel>> {
            val list = if (withDefault) availableThemes else availableThemes.filter { !it.isDefault }
            return Result.Success(list)
        }

        override suspend fun setDialogTheme(
            dialogId: Long,
            emoticon: String?,
            giftSlug: String?
        ): Result<Unit> {
            val theme = ChatThemeModel(
                emoticon = emoticon,
                giftSlug = giftSlug,
                isDefault = emoticon == null && giftSlug == null
            )
            val current = states.getOrPut(dialogId) { DialogThemeStateModel(dialogId) }
            val updated = current.copy(currentTheme = theme)
            states[dialogId] = updated
            getOrCreateFlow(dialogId).tryEmit(updated)
            return Result.Success(Unit)
        }

        override suspend fun resetDialogTheme(dialogId: Long): Result<Unit> {
            val current = states.getOrPut(dialogId) { DialogThemeStateModel(dialogId) }
            val updated = current.copy(currentTheme = null)
            states[dialogId] = updated
            getOrCreateFlow(dialogId).tryEmit(updated)
            return Result.Success(Unit)
        }

        override suspend fun saveChatWallpaper(dialogId: Long, wallpaperId: Long?): Result<Unit> {
            val current = states.getOrPut(dialogId) { DialogThemeStateModel(dialogId) }
            val updated = current.copy(wallpaperId = wallpaperId)
            states[dialogId] = updated
            getOrCreateFlow(dialogId).tryEmit(updated)
            return Result.Success(Unit)
        }
    }
}
