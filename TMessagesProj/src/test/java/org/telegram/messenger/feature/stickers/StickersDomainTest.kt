package org.telegram.messenger.feature.stickers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stickers.data.mapper.StickerMapper
import org.telegram.messenger.feature.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.stickers.domain.model.StickerSetModel
import org.telegram.messenger.feature.stickers.domain.model.StickerType
import org.telegram.messenger.feature.stickers.domain.repository.StickersRepository
import org.telegram.messenger.feature.stickers.domain.usecase.GetRecentStickersUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickerSetUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickerSetsUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickersForEmojiUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ObserveStickerSetsUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ToggleStickerSetArchivedUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ToggleStickerSetInstalledUseCase
import org.telegram.messenger.feature.stickers.presentation.StickersEvent
import org.telegram.messenger.feature.stickers.presentation.StickersUiState
import org.telegram.messenger.feature.stickers.presentation.StickersViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class StickersDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeStickersRepository : StickersRepository {
        val stickerSetsMap = mutableMapOf<Long, StickerSetModel>()
        val stickerSetsFlow = MutableSharedFlow<List<StickerSetModel>>(replay = 1)
        val recentStickersList = mutableListOf<StickerModel>()
        val emojiMap = mutableMapOf<String, MutableList<StickerModel>>()
        var shouldSucceed = true

        fun putStickerSet(set: StickerSetModel) {
            stickerSetsMap[set.id] = set
            stickerSetsFlow.tryEmit(stickerSetsMap.values.toList())
        }

        override fun observeStickerSets(type: Int): Flow<List<StickerSetModel>> = stickerSetsFlow.asSharedFlow()

        override suspend fun getStickerSets(type: Int): List<StickerSetModel> = stickerSetsMap.values.toList()

        override suspend fun getStickerSet(id: Long): StickerSetModel? = stickerSetsMap[id]

        override suspend fun getRecentStickers(type: Int): List<StickerModel> = recentStickersList

        override suspend fun getStickersForEmoji(emoji: String): List<StickerModel> = emojiMap[emoji] ?: emptyList()

        override suspend fun toggleStickerSetInstalled(id: Long, install: Boolean): Result<Unit> {
            return if (shouldSucceed) {
                val set = stickerSetsMap[id]
                if (set != null) {
                    putStickerSet(set.copy(isInstalled = install))
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Sticker set not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to toggle installed"))
            }
        }

        override suspend fun toggleStickerSetArchived(id: Long, archive: Boolean): Result<Unit> {
            return if (shouldSucceed) {
                val set = stickerSetsMap[id]
                if (set != null) {
                    putStickerSet(set.copy(isArchived = archive))
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Sticker set not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to toggle archived"))
            }
        }
    }

    @Test
    fun testStickerMapperFromDocument() {
        val document = TLRPC.TL_document().apply {
            id = 123456L
            access_hash = 654321L
            mime_type = "image/webp"
            size = 20480
            attributes = ArrayList(listOf(
                TLRPC.TL_documentAttributeSticker().apply {
                    alt = "👍"
                    stickerset = TLRPC.TL_inputStickerSetID().apply { id = 999L }
                },
                TLRPC.TL_documentAttributeImageSize().apply {
                    w = 512
                    h = 512
                }
            ))
        }

        val domainModel = StickerMapper.toDomain(document)

        assertEquals(123456L, domainModel.id)
        assertEquals(654321L, domainModel.accessHash)
        assertEquals(999L, domainModel.setId)
        assertEquals("image/webp", domainModel.mimeType)
        assertEquals("👍", domainModel.emoji)
        assertEquals(StickerType.IMAGE, domainModel.type)
        assertEquals(512, domainModel.width)
        assertEquals(512, domainModel.height)
        assertEquals(20480L, domainModel.size)
    }

    @Test
    fun testStickerMapperAnimatedAndVideoTypes() {
        val animatedDoc = TLRPC.TL_document().apply {
            id = 1L
            mime_type = "application/x-tgsticker"
        }
        val animatedModel = StickerMapper.toDomain(animatedDoc)
        assertEquals(StickerType.ANIMATED, animatedModel.type)

        val videoDoc = TLRPC.TL_document().apply {
            id = 2L
            mime_type = "video/webm"
        }
        val videoModel = StickerMapper.toDomain(videoDoc)
        assertEquals(StickerType.VIDEO, videoModel.type)

        val emojiDoc = TLRPC.TL_document().apply {
            id = 3L
            mime_type = "image/webp"
            attributes = ArrayList(listOf(
                TLRPC.TL_documentAttributeCustomEmoji().apply {
                    alt = "🔥"
                    stickerset = TLRPC.TL_inputStickerSetID().apply { id = 888L }
                }
            ))
        }
        val emojiModel = StickerMapper.toDomain(emojiDoc)
        assertEquals(StickerType.EMOJI, emojiModel.type)
        assertEquals(888L, emojiModel.setId)
    }

    @Test
    fun testStickerMapperNullHandling() {
        val nullSticker = StickerMapper.toDomain(null as TLRPC.Document?)
        assertEquals(0L, nullSticker.id)
        assertEquals("", nullSticker.mimeType)

        val nullSet = StickerMapper.toDomain(null as TLRPC.TL_messages_stickerSet?)
        assertEquals(0L, nullSet.id)
        assertEquals("", nullSet.title)
    }

    @Test
    fun testStickerMapperFromStickerSet() {
        val set = TLRPC.TL_messages_stickerSet().apply {
            this.set = TLRPC.TL_stickerSet().apply {
                id = 100L
                access_hash = 200L
                title = "Pepe the Frog"
                short_name = "pepe"
                count = 1
                installed = true
                archived = false
                official = true
                emojis = false
            }
            documents = ArrayList(listOf(
                TLRPC.TL_document().apply {
                    id = 1001L
                    mime_type = "image/webp"
                }
            ))
        }

        val domainSet = StickerMapper.toDomain(set)

        assertEquals(100L, domainSet.id)
        assertEquals("Pepe the Frog", domainSet.title)
        assertEquals("pepe", domainSet.shortName)
        assertEquals(1, domainSet.count)
        assertTrue(domainSet.isInstalled)
        assertFalse(domainSet.isArchived)
        assertTrue(domainSet.isOfficial)
        assertEquals(1, domainSet.stickers.size)
        assertEquals(1001L, domainSet.stickers.first().id)
    }

    @Test
    fun testObserveAndGetStickerSetsUseCases() = runTest {
        val repo = FakeStickersRepository()
        val s1 = StickerSetModel(id = 1L, title = "Set 1", shortName = "set1", isInstalled = true)
        val s2 = StickerSetModel(id = 2L, title = "Set 2", shortName = "set2", isInstalled = false)
        repo.putStickerSet(s1)
        repo.putStickerSet(s2)

        val observeUseCase = ObserveStickerSetsUseCase(repo)
        val getSetsUseCase = GetStickerSetsUseCase(repo)
        val getSetUseCase = GetStickerSetUseCase(repo)

        val list = getSetsUseCase(0)
        assertEquals(2, list.size)

        val single = getSetUseCase(1L)
        assertNotNull(single)
        assertEquals("Set 1", single?.title)

        val notFound = getSetUseCase(999L)
        assertNull(notFound)

        var emitted: List<StickerSetModel>? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            observeUseCase(0).collect { emitted = it }
        }

        advanceUntilIdle()
        assertNotNull(emitted)
        assertEquals(2, emitted?.size)

        job.cancel()
    }

    @Test
    fun testGetRecentStickersAndEmojiUseCases() = runTest {
        val repo = FakeStickersRepository()
        val sticker = StickerModel(id = 10L, emoji = "❤️")
        repo.recentStickersList.add(sticker)
        repo.emojiMap["❤️"] = mutableListOf(sticker)

        val recentUseCase = GetRecentStickersUseCase(repo)
        val emojiUseCase = GetStickersForEmojiUseCase(repo)

        val recent = recentUseCase(0)
        assertEquals(1, recent.size)
        assertEquals(10L, recent.first().id)

        val emojiStickers = emojiUseCase("❤️")
        assertEquals(1, emojiStickers.size)
        assertEquals("❤️", emojiStickers.first().emoji)

        val empty = emojiUseCase("nonexistent")
        assertTrue(empty.isEmpty())
    }

    @Test
    fun testToggleStickerSetInstalledAndArchivedUseCases() = runTest {
        val repo = FakeStickersRepository()
        val s1 = StickerSetModel(id = 1L, title = "Set 1", shortName = "set1", isInstalled = false, isArchived = false)
        repo.putStickerSet(s1)

        val installUseCase = ToggleStickerSetInstalledUseCase(repo)
        val archiveUseCase = ToggleStickerSetArchivedUseCase(repo)

        val installResult = installUseCase(1L, true)
        assertTrue(installResult.isSuccess)
        assertTrue(repo.getStickerSet(1L)?.isInstalled == true)

        val archiveResult = archiveUseCase(1L, true)
        assertTrue(archiveResult.isSuccess)
        assertTrue(repo.getStickerSet(1L)?.isArchived == true)

        // Error path
        repo.shouldSucceed = false
        val failInstall = installUseCase(1L, false)
        assertTrue(failInstall.isFailure)

        val failArchive = archiveUseCase(1L, false)
        assertTrue(failArchive.isFailure)
    }

    @Test
    fun testStickersViewModelFlowAndEvents() = runTest {
        val repo = FakeStickersRepository()
        val s1 = StickerSetModel(id = 1L, title = "Pack 1", shortName = "pack1", isInstalled = true)
        val sticker = StickerModel(id = 55L, emoji = "👍")
        repo.putStickerSet(s1)
        repo.recentStickersList.add(sticker)
        repo.emojiMap["👍"] = mutableListOf(sticker)

        val viewModel = StickersViewModel(
            observeStickerSetsUseCase = ObserveStickerSetsUseCase(repo),
            getStickerSetsUseCase = GetStickerSetsUseCase(repo),
            getStickerSetUseCase = GetStickerSetUseCase(repo),
            getRecentStickersUseCase = GetRecentStickersUseCase(repo),
            getStickersForEmojiUseCase = GetStickersForEmojiUseCase(repo),
            toggleStickerSetInstalledUseCase = ToggleStickerSetInstalledUseCase(repo),
            toggleStickerSetArchivedUseCase = ToggleStickerSetArchivedUseCase(repo)
        )

        val events = mutableListOf<StickersEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        advanceUntilIdle()

        // Verify initial state
        assertTrue(viewModel.uiState.value is StickersUiState.Success)
        val state = viewModel.uiState.value as StickersUiState.Success
        assertEquals(1, state.stickerSets.size)
        assertEquals(1, state.recentStickers.size)
        assertEquals("", state.selectedEmoji)

        // Search emoji
        viewModel.searchEmoji("👍")
        advanceUntilIdle()
        val emojiState = viewModel.uiState.value as StickersUiState.Success
        assertEquals("👍", emojiState.selectedEmoji)
        assertEquals(1, emojiState.emojiStickers.size)
        assertEquals(55L, emojiState.emojiStickers.first().id)

        // Clear emoji search
        viewModel.searchEmoji("")
        advanceUntilIdle()
        val clearedState = viewModel.uiState.value as StickersUiState.Success
        assertEquals("", clearedState.selectedEmoji)
        assertEquals(0, clearedState.emojiStickers.size)

        // Install set
        viewModel.installStickerSet(1L)
        advanceUntilIdle()
        assertTrue(events.contains(StickersEvent.StickerSetInstalled(1L)))

        // Uninstall set
        viewModel.uninstallStickerSet(1L)
        advanceUntilIdle()
        assertTrue(events.contains(StickersEvent.StickerSetUninstalled(1L)))

        // Archive set
        viewModel.archiveStickerSet(1L)
        advanceUntilIdle()
        assertTrue(events.contains(StickersEvent.StickerSetArchived(1L)))

        // Refresh
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is StickersUiState.Success)

        eventsJob.cancel()
    }

    @Test
    fun testStickersViewModelErrorHandling() = runTest {
        val repo = FakeStickersRepository().apply { shouldSucceed = false }

        val viewModel = StickersViewModel(
            observeStickerSetsUseCase = ObserveStickerSetsUseCase(repo),
            getStickerSetsUseCase = GetStickerSetsUseCase(repo),
            getStickerSetUseCase = GetStickerSetUseCase(repo),
            getRecentStickersUseCase = GetRecentStickersUseCase(repo),
            getStickersForEmojiUseCase = GetStickersForEmojiUseCase(repo),
            toggleStickerSetInstalledUseCase = ToggleStickerSetInstalledUseCase(repo),
            toggleStickerSetArchivedUseCase = ToggleStickerSetArchivedUseCase(repo)
        )

        val events = mutableListOf<StickersEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.installStickerSet(999L)
        advanceUntilIdle()
        assertTrue(events.any { it is StickersEvent.ShowError && it.message == "Failed to toggle installed" })

        viewModel.uninstallStickerSet(999L)
        advanceUntilIdle()
        assertTrue(events.any { it is StickersEvent.ShowError && it.message == "Failed to toggle installed" })

        viewModel.archiveStickerSet(999L)
        advanceUntilIdle()
        assertTrue(events.any { it is StickersEvent.ShowError && it.message == "Failed to toggle archived" })

        eventsJob.cancel()
    }

    @Test
    fun testAccountFeatureContainerWiring() {
        val container = AccountFeatureContainer.get(0)
        val customRepo = FakeStickersRepository()
        container.stickersRepository = customRepo

        assertEquals(customRepo, container.stickersRepository)
        assertNotNull(container.observeStickerSetsUseCase)
        assertNotNull(container.getStickerSetsUseCase)
        assertNotNull(container.getStickerSetUseCase)
        assertNotNull(container.getRecentStickersUseCase)
        assertNotNull(container.getStickersForEmojiUseCase)
        assertNotNull(container.toggleStickerSetInstalledUseCase)
        assertNotNull(container.toggleStickerSetArchivedUseCase)
        assertNotNull(container.stickersViewModel)
        assertNotNull(container.getStickersViewModel(0))

        AccountFeatureContainer.reset(0)
    }
}
