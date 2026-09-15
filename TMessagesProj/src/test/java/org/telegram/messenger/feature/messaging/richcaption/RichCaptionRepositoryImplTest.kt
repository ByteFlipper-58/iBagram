package org.telegram.messenger.feature.messaging.richcaption

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.richcaption.data.datasource.RichCaptionLocalDataSource
import org.telegram.messenger.feature.messaging.richcaption.data.datasource.RichCaptionRemoteDataSource
import org.telegram.messenger.feature.messaging.richcaption.data.repository.RichCaptionRepositoryImpl
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionSpanType

class RichCaptionRepositoryImplTest {

    private lateinit var localDataSource: RichCaptionLocalDataSource
    private lateinit var remoteDataSource: RichCaptionRemoteDataSource
    private lateinit var repository: RichCaptionRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = RichCaptionLocalDataSource()
        remoteDataSource = RichCaptionRemoteDataSource(0)
        repository = RichCaptionRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialCaptionState() {
        val caption = repository.getCaption()
        assertTrue(caption.isEmpty)
        assertEquals("", caption.plainText)
        assertEquals(null, caption.credit)
        assertFalse(caption.isLocked)
    }

    @Test
    fun testSetCaptionTextWithSpans() = runBlocking {
        val spans = listOf(
            CaptionEntitySpan(start = 0, end = 5, type = CaptionSpanType.BOLD)
        )
        repository.setCaptionText("Hello world", spans)

        val caption = repository.getCaption()
        assertEquals("Hello world", caption.plainText)
        assertEquals(1, caption.spans.size)
        assertEquals(CaptionSpanType.BOLD, caption.spans[0].type)

        val observed = repository.observeCaption().first()
        assertEquals("Hello world", observed.plainText)
    }

    @Test
    fun testSetCaptionCredit() {
        repository.setCaptionCredit("Photo by John")
        assertEquals("Photo by John", repository.getCaption().credit)

        repository.setCaptionCredit("   ")
        assertEquals(null, repository.getCaption().credit)
    }

    @Test
    fun testSetLocked() {
        repository.setLocked(true)
        assertTrue(repository.getCaption().isLocked)

        repository.setLocked(false)
        assertFalse(repository.getCaption().isLocked)
    }

    @Test
    fun testCalculateAvailableWidth() {
        val spec = CaptionMeasureSpec(
            parentWidthPx = 1000,
            leftInsetPx = 50,
            rightInsetPx = 50,
            horizontalPaddingPx = 20
        )
        // 1000 - 50 - 50 - 2 * 20 = 860
        val width = repository.calculateAvailableWidth(spec)
        assertEquals(860, width)
    }

    @Test
    fun testIsPressWithinBounds() {
        // text bounds: left=100, top=200, width=300, height=50
        assertTrue(repository.isPressWithinBounds(150, 220, 100, 200, 300, 50))
        assertFalse(repository.isPressWithinBounds(50, 220, 100, 200, 300, 50))
        assertFalse(repository.isPressWithinBounds(150, 300, 100, 200, 300, 50))
        assertFalse(repository.isPressWithinBounds(150, 220, 100, 200, 0, 0))
    }

    @Test
    fun testClear() {
        repository.setCaptionText("Hello")
        repository.setCaptionCredit("Credit")
        repository.setLocked(true)

        repository.clear()

        val caption = repository.getCaption()
        assertTrue(caption.isEmpty)
        assertEquals("", caption.plainText)
        assertEquals(null, caption.credit)
        assertFalse(caption.isLocked)
    }
}
