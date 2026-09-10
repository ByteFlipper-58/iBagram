package org.telegram.messenger.feature.media.contentpreview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.telegram.messenger.feature.media.contentpreview.data.mapper.ContentPreviewMapper
import org.telegram.messenger.feature.media.contentpreview.data.repository.LegacyContentPreviewRepository
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionType
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewContentType
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.CalculatePreviewDragUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ClearContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.DismissContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.EvaluatePreviewEligibilityUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.GetContentPreviewStateUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ObserveContentPreviewStateUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.OpenContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ResolvePreviewActionsUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.TriggerPreviewActionUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.UpdatePreviewDragUseCase
import org.telegram.messenger.feature.media.contentpreview.presentation.ContentPreviewEvent
import org.telegram.messenger.feature.media.contentpreview.presentation.ContentPreviewViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ContentPreviewDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private val evaluatePreviewEligibilityUseCase = EvaluatePreviewEligibilityUseCase()
    private val calculatePreviewDragUseCase = CalculatePreviewDragUseCase()
    private val resolvePreviewActionsUseCase = ResolvePreviewActionsUseCase()

    private lateinit var repository: LegacyContentPreviewRepository
    private lateinit var observeContentPreviewStateUseCase: ObserveContentPreviewStateUseCase
    private lateinit var getContentPreviewStateUseCase: GetContentPreviewStateUseCase
    private lateinit var openContentPreviewUseCase: OpenContentPreviewUseCase
    private lateinit var updatePreviewDragUseCase: UpdatePreviewDragUseCase
    private lateinit var triggerPreviewActionUseCase: TriggerPreviewActionUseCase
    private lateinit var dismissContentPreviewUseCase: DismissContentPreviewUseCase
    private lateinit var clearContentPreviewUseCase: ClearContentPreviewUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyContentPreviewRepository()
        observeContentPreviewStateUseCase = ObserveContentPreviewStateUseCase(repository)
        getContentPreviewStateUseCase = GetContentPreviewStateUseCase(repository)
        openContentPreviewUseCase = OpenContentPreviewUseCase(
            repository,
            evaluatePreviewEligibilityUseCase,
            resolvePreviewActionsUseCase
        )
        updatePreviewDragUseCase = UpdatePreviewDragUseCase(repository, calculatePreviewDragUseCase)
        triggerPreviewActionUseCase = TriggerPreviewActionUseCase(repository)
        dismissContentPreviewUseCase = DismissContentPreviewUseCase(repository)
        clearContentPreviewUseCase = ClearContentPreviewUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEvaluatePreviewEligibility() {
        // null item
        assertFalse(evaluatePreviewEligibilityUseCase(null))

        // NONE type
        val noneItem = ContentPreviewItem(contentType = PreviewContentType.NONE)
        assertFalse(evaluatePreviewEligibilityUseCase(noneItem))

        // STICKER with valid documentId
        val validSticker = ContentPreviewItem(contentType = PreviewContentType.STICKER, documentId = 12345L)
        assertTrue(evaluatePreviewEligibilityUseCase(validSticker))

        // EMOJI with emoticon
        val validEmoji = ContentPreviewItem(contentType = PreviewContentType.EMOJI, emoticon = "🔥")
        assertTrue(evaluatePreviewEligibilityUseCase(validEmoji))

        // Empty sticker
        val emptySticker = ContentPreviewItem(contentType = PreviewContentType.STICKER, documentId = 0L, emoticon = null)
        assertFalse(evaluatePreviewEligibilityUseCase(emptySticker))
    }

    @Test
    fun testCalculatePreviewDragAndGesture() {
        // 1. Начальная позиция (без сдвига)
        val g0 = calculatePreviewDragUseCase(startY = 500f, currentY = 500f, maxDragDistance = 200f)
        assertEquals(0f, g0.deltaY, 0.001f)
        assertEquals(0f, g0.progress, 0.001f)
        assertFalse(g0.shouldShowMenu)

        // 2. Сдвиг вверх на 100px из 200px (прогресс 0.5)
        val g1 = calculatePreviewDragUseCase(startY = 500f, currentY = 400f, maxDragDistance = 200f)
        assertEquals(100f, g1.deltaY, 0.001f)
        assertEquals(0.5f, g1.progress, 0.001f)
        assertFalse(g1.shouldShowMenu)

        // 3. Сдвиг вверх на 160px из 200px (прогресс 0.8 >= 0.75 -> меню должно открыться)
        val g2 = calculatePreviewDragUseCase(startY = 500f, currentY = 340f, maxDragDistance = 200f)
        assertEquals(160f, g2.deltaY, 0.001f)
        assertEquals(0.8f, g2.progress, 0.001f)
        assertTrue(g2.shouldShowMenu)

        // 4. Сдвиг вниз (startY < currentY -> deltaY = 0)
        val g3 = calculatePreviewDragUseCase(startY = 500f, currentY = 600f, maxDragDistance = 200f)
        assertEquals(0f, g3.deltaY, 0.001f)
        assertEquals(0f, g3.progress, 0.001f)
        assertFalse(g3.shouldShowMenu)
    }

    @Test
    fun testResolvePreviewActions() {
        // 1. Стикер со всеми включёнными возможностями
        val sticker = ContentPreviewItem(
            contentType = PreviewContentType.STICKER,
            documentId = 1L,
            canSend = true,
            canSchedule = true,
            isFavorite = false,
            isRecent = true,
            canEdit = true,
            canDelete = true
        )
        val stickerActions = resolvePreviewActionsUseCase(sticker)
        assertTrue(stickerActions.any { it.action == PreviewActionType.SEND })
        assertTrue(stickerActions.any { it.action == PreviewActionType.SEND_WITHOUT_SOUND })
        assertTrue(stickerActions.any { it.action == PreviewActionType.SCHEDULE })
        assertTrue(stickerActions.any { it.action == PreviewActionType.TOGGLE_FAVORITE && it.title == "Add to Favorites" })
        assertTrue(stickerActions.any { it.action == PreviewActionType.REMOVE_FROM_RECENT && it.isDestructive })
        assertTrue(stickerActions.any { it.action == PreviewActionType.VIEW_PACK })
        assertTrue(stickerActions.any { it.action == PreviewActionType.EDIT_STICKER })
        assertTrue(stickerActions.any { it.action == PreviewActionType.DELETE_STICKER && it.isDestructive })

        // 2. GIF
        val gif = ContentPreviewItem(
            contentType = PreviewContentType.GIF,
            documentId = 2L,
            canSend = true,
            canSchedule = false,
            canAddCaption = true,
            isFavorite = true
        )
        val gifActions = resolvePreviewActionsUseCase(gif)
        assertTrue(gifActions.any { it.action == PreviewActionType.SEND })
        assertFalse(gifActions.any { it.action == PreviewActionType.SCHEDULE })
        assertTrue(gifActions.any { it.action == PreviewActionType.ADD_CAPTION })
        assertTrue(gifActions.any { it.action == PreviewActionType.TOGGLE_FAVORITE && it.title == "Remove from Saved" })

        // 3. EMOJI
        val emoji = ContentPreviewItem(
            contentType = PreviewContentType.EMOJI,
            emoticon = "🎉",
            canSend = true,
            canSetStatus = true,
            canCopy = true
        )
        val emojiActions = resolvePreviewActionsUseCase(emoji)
        assertTrue(emojiActions.any { it.action == PreviewActionType.SEND })
        assertTrue(emojiActions.any { it.action == PreviewActionType.SET_EMOJI_STATUS })
        assertTrue(emojiActions.any { it.action == PreviewActionType.COPY_EMOJI })
    }

    @Test
    fun testContentPreviewMapper() {
        assertEquals(PreviewContentType.STICKER, ContentPreviewMapper.mapContentTypeId(0))
        assertEquals(PreviewContentType.GIF, ContentPreviewMapper.mapContentTypeId(1))
        assertEquals(PreviewContentType.EMOJI, ContentPreviewMapper.mapContentTypeId(2))
        assertEquals(PreviewContentType.CUSTOM_STICKER, ContentPreviewMapper.mapContentTypeId(3))
        assertEquals(PreviewContentType.NONE, ContentPreviewMapper.mapContentTypeId(-1))

        assertEquals(0, ContentPreviewMapper.mapContentTypeToId(PreviewContentType.STICKER))

        val progress = ContentPreviewMapper.calculateNormalizedDragProgress(
            startY = 300f,
            currentY = 150f,
            maxDistance = 150f
        )
        assertEquals(1.0f, progress, 0.001f)

        // Haptic feedback trigger
        assertTrue(ContentPreviewMapper.shouldTriggerHaptic(0.70f, 0.76f, 0.75f))
        assertFalse(ContentPreviewMapper.shouldTriggerHaptic(0.76f, 0.80f, 0.75f))
        assertFalse(ContentPreviewMapper.shouldTriggerHaptic(0.50f, 0.60f, 0.75f))
    }

    @Test
    fun testRepositoryAndViewModelMvi() = runTest {
        val vm = ContentPreviewViewModel(
            observeContentPreviewStateUseCase = observeContentPreviewStateUseCase,
            openContentPreviewUseCase = openContentPreviewUseCase,
            updatePreviewDragUseCase = updatePreviewDragUseCase,
            triggerPreviewActionUseCase = triggerPreviewActionUseCase,
            dismissContentPreviewUseCase = dismissContentPreviewUseCase,
            clearContentPreviewUseCase = clearContentPreviewUseCase
        )

        advanceUntilIdle()
        val s0 = vm.uiState.value
        assertFalse(s0.isVisible)
        assertFalse(s0.isMenuVisible)
        assertNull(s0.currentItem)

        // 1. Открытие превью
        val item = ContentPreviewItem(
            contentType = PreviewContentType.STICKER,
            documentId = 999L,
            canSend = true
        )
        vm.onEvent(ContentPreviewEvent.OnOpenPreviewRequested(item))
        advanceUntilIdle()

        val s1 = vm.uiState.value
        assertTrue(s1.isVisible)
        assertFalse(s1.isMenuVisible)
        assertNotNull(s1.currentItem)
        assertEquals(PreviewContentType.STICKER, s1.contentType)
        assertTrue(s1.hasActions)
        assertTrue(s1.canSend)

        // 2. Движение пальцем вверх для вызова меню
        vm.onEvent(ContentPreviewEvent.OnDragUpdated(startY = 400f, currentY = 200f, maxDragDistance = 250f))
        advanceUntilIdle()

        val s2 = vm.uiState.value
        assertTrue(s2.isVisible)
        assertTrue(s2.isMenuVisible) // 200 / 250 = 0.8 >= 0.75
        assertEquals(0.8f, s2.dragProgress, 0.001f)

        // 3. Выбор действия (Send)
        vm.onEvent(ContentPreviewEvent.OnActionSelected(PreviewActionType.SEND))
        advanceUntilIdle()

        val s3 = vm.uiState.value
        assertFalse(s3.isVisible)
        assertNull(s3.currentItem)

        // 4. Повторное открытие и ручное закрытие
        vm.onEvent(ContentPreviewEvent.OnOpenPreviewRequested(item))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isVisible)

        vm.onEvent(ContentPreviewEvent.OnDismissRequested)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isVisible)
    }
}
