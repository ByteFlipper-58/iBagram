package org.telegram.messenger.feature.richcaption

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.richcaption.data.mapper.RichCaptionMapper
import org.telegram.messenger.feature.richcaption.data.repository.LegacyRichCaptionRepository
import org.telegram.messenger.feature.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.richcaption.domain.model.CaptionSpanType
import org.telegram.messenger.feature.richcaption.domain.model.RichCaptionModel
import org.telegram.messenger.feature.richcaption.domain.usecase.ClearRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.GetRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.ObserveRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionCreditUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionLockedUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionTextUseCase
import org.telegram.messenger.feature.richcaption.presentation.RichCaptionEvent
import org.telegram.messenger.feature.richcaption.presentation.RichCaptionViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class RichCaptionDomainTest {

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
    fun testMapperAndSanitization() {
        val rawSpans = listOf(
            CaptionEntitySpan(start = 0, end = 4, type = CaptionSpanType.BOLD),
            CaptionEntitySpan(start = -1, end = 3, type = CaptionSpanType.ITALIC), // Invalid start
            CaptionEntitySpan(start = 5, end = 5, type = CaptionSpanType.CODE), // Empty span
            CaptionEntitySpan(start = 2, end = 10, type = CaptionSpanType.UNDERLINE) // Out of bounds for length 6
        )

        val model = RichCaptionMapper.createCaption(
            plainText = "Hello!",
            credit = " Photographer Name ",
            spans = rawSpans,
            isLocked = false
        )

        assertEquals("Hello!", model.plainText)
        assertEquals("Photographer Name", model.credit)
        assertEquals(1, model.spans.size)
        assertEquals(CaptionSpanType.BOLD, model.spans[0].type)
        assertEquals(4, model.spans[0].length)

        assertEquals(CaptionSpanType.BOLD, RichCaptionMapper.parseSpanType("b"))
        assertEquals(CaptionSpanType.ITALIC, RichCaptionMapper.parseSpanType("italic"))
        assertEquals(CaptionSpanType.UNDERLINE, RichCaptionMapper.parseSpanType("u"))
        assertEquals(CaptionSpanType.STRIKE, RichCaptionMapper.parseSpanType("s"))
        assertEquals(CaptionSpanType.CODE, RichCaptionMapper.parseSpanType("mono"))
        assertEquals(CaptionSpanType.URL, RichCaptionMapper.parseSpanType("url"))
    }

    @Test
    fun testRichCaptionModelProperties() {
        val emptyModel = RichCaptionModel()
        assertTrue(emptyModel.isEmpty)
        assertEquals(0, emptyModel.length)
        assertFalse(emptyModel.hasSpans)
        assertFalse(emptyModel.isLocked)

        val filledModel = RichCaptionModel(
            plainText = "A beautiful sunset",
            credit = "John Doe",
            spans = listOf(CaptionEntitySpan(0, 1, CaptionSpanType.BOLD)),
            isLocked = true
        )
        assertFalse(filledModel.isEmpty)
        assertEquals(18, filledModel.length)
        assertTrue(filledModel.hasSpans)
        assertTrue(filledModel.isLocked)
    }

    @Test
    fun testMeasureSpecAvailableWidth() {
        val spec = CaptionMeasureSpec(
            parentWidthPx = 1080,
            leftInsetPx = 40,
            rightInsetPx = 40,
            horizontalPaddingPx = 16
        )
        // 1080 - 40 - 40 - 2 * 16 = 1080 - 80 - 32 = 968
        assertEquals(968, spec.availableWidth)

        val overconstrainedSpec = CaptionMeasureSpec(
            parentWidthPx = 100,
            leftInsetPx = 60,
            rightInsetPx = 60,
            horizontalPaddingPx = 10
        )
        assertEquals(0, overconstrainedSpec.availableWidth)
    }

    @Test
    fun testRepositoryMutationsAndHitDetection() {
        val repository = LegacyRichCaptionRepository()

        assertTrue(repository.getCaption().isEmpty)

        repository.setCaptionText("Sample caption text")
        assertEquals("Sample caption text", repository.getCaption().plainText)

        repository.setCaptionCredit("Photo by Alex")
        assertEquals("Photo by Alex", repository.getCaption().credit)

        repository.setLocked(true)
        assertTrue(repository.getCaption().isLocked)

        // Hit detection
        val left = 20
        val top = 100
        val width = 200
        val height = 50

        assertTrue(repository.isPressWithinBounds(localX = 50, localY = 120, textLeft = left, textTop = top, textWidth = width, textHeight = height))
        assertFalse(repository.isPressWithinBounds(localX = 10, localY = 120, textLeft = left, textTop = top, textWidth = width, textHeight = height))
        assertFalse(repository.isPressWithinBounds(localX = 50, localY = 90, textLeft = left, textTop = top, textWidth = width, textHeight = height))
        assertFalse(repository.isPressWithinBounds(localX = 250, localY = 120, textLeft = left, textTop = top, textWidth = width, textHeight = height))
        assertFalse(repository.isPressWithinBounds(localX = 50, localY = 160, textLeft = left, textTop = top, textWidth = width, textHeight = height))

        repository.clear()
        assertTrue(repository.getCaption().isEmpty)
        assertNull(repository.getCaption().credit)
        assertFalse(repository.getCaption().isLocked)
    }

    @Test
    fun testViewModelMviEventsAndReactiveFlow() {
        val repository = LegacyRichCaptionRepository()

        val viewModel = RichCaptionViewModel(
            observeRichCaptionUseCase = ObserveRichCaptionUseCase(repository),
            getRichCaptionUseCase = GetRichCaptionUseCase(repository),
            setRichCaptionTextUseCase = SetRichCaptionTextUseCase(repository),
            setRichCaptionCreditUseCase = SetRichCaptionCreditUseCase(repository),
            setRichCaptionLockedUseCase = SetRichCaptionLockedUseCase(repository),
            clearRichCaptionUseCase = ClearRichCaptionUseCase(repository)
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.hasSelection)

        viewModel.onEvent(RichCaptionEvent.SetText("Diagram illustration", listOf(CaptionEntitySpan(0, 7, CaptionSpanType.BOLD))))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Diagram illustration", viewModel.uiState.value.plainText)
        assertEquals(1, viewModel.uiState.value.caption.spans.size)

        viewModel.onEvent(RichCaptionEvent.SetCredit("Source: Science"))
        viewModel.onEvent(RichCaptionEvent.SetLocked(true))
        viewModel.onEvent(RichCaptionEvent.SetEditing(true))
        viewModel.onEvent(RichCaptionEvent.SetHasSelection(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Source: Science", viewModel.uiState.value.credit)
        assertTrue(viewModel.uiState.value.isLocked)
        assertTrue(viewModel.uiState.value.isEditing)
        assertTrue(viewModel.uiState.value.hasSelection)

        viewModel.onEvent(RichCaptionEvent.Clear)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)
        assertNull(viewModel.uiState.value.credit)
    }
}
