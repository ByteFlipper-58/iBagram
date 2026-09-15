package org.telegram.messenger.feature.messaging.texthtml

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.texthtml.data.datasource.TextHtmlLocalDataSource
import org.telegram.messenger.feature.messaging.texthtml.data.datasource.TextHtmlRemoteDataSource
import org.telegram.messenger.feature.messaging.texthtml.data.repository.TextHtmlRepositoryImpl
import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpan
import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType
import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText

class TextHtmlRepositoryImplTest {

    private lateinit var localDataSource: TextHtmlLocalDataSource
    private lateinit var remoteDataSource: TextHtmlRemoteDataSource
    private lateinit var repository: TextHtmlRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = TextHtmlLocalDataSource(0)
        remoteDataSource = TextHtmlRemoteDataSource(0)
        repository = TextHtmlRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testEscapeAndUnescape() {
        val raw = "<b>Hello & Welcome</b>\n\"Quote\""
        val escaped = repository.escapeHtml(raw)
        assertTrue(escaped.contains("&lt;b&gt;"))
        assertTrue(escaped.contains("&amp;"))
        assertTrue(escaped.contains("&quot;"))
        assertTrue(escaped.contains("<br>"))

        val unescaped = repository.unescapeHtml(escaped)
        assertEquals(raw, unescaped)
    }

    @Test
    fun testConvertToHtml() {
        val richText = RichFormattedText(
            plainText = "Hello World",
            spans = listOf(
                HtmlTextSpan(
                    type = HtmlTextSpanType.BOLD,
                    start = 0,
                    end = 5
                )
            )
        )
        val html = repository.convertToHtml(richText)
        assertEquals("<b>Hello</b> World", html)

        val state = repository.observeState().value
        assertEquals(html, state.lastHtml)
        assertEquals("Hello World", state.lastPlainText)
        assertEquals(1, state.spansCount)
    }

    @Test
    fun testParseFromHtml() {
        val html = "<b>Bold</b> and <i>Italic</i>"
        val parsed = repository.parseFromHtml(html)
        assertEquals("Bold and Italic", parsed.plainText)
        assertEquals(2, parsed.spans.size)
        assertEquals(HtmlTextSpanType.BOLD, parsed.spans[0].type)
        assertEquals(0, parsed.spans[0].start)
        assertEquals(4, parsed.spans[0].end)
        assertEquals(HtmlTextSpanType.ITALIC, parsed.spans[1].type)
        assertEquals(9, parsed.spans[1].start)
        assertEquals(15, parsed.spans[1].end)
    }

    @Test
    fun testStripFormatting() {
        val html = "<b>Bold</b> <a href=\"https://telegram.org\">Link</a><br>New line"
        val stripped = repository.stripFormatting(html)
        assertEquals("Bold Link\nNew line", stripped)
    }

    @Test
    fun testClearState() {
        val richText = RichFormattedText(
            plainText = "Test",
            spans = emptyList()
        )
        repository.convertToHtml(richText)
        assertNotNull(repository.observeState().value.lastPlainText)

        repository.clearState()
        org.junit.Assert.assertNull(repository.observeState().value.lastPlainText)
    }

    @Test
    fun testValidateRemoteEmoji() = runBlocking {
        val res = remoteDataSource.validateCustomEmoji(listOf(12345L, 67890L))
        assertTrue(res is org.telegram.messenger.core.result.Result.Success)
    }
}
