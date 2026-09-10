package org.telegram.messenger.feature.messaging.texthtml

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.texthtml.data.repository.LegacyTextHtmlRepository
import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpan
import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType
import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ClearTextHtmlStateUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ConvertToHtmlUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.EscapeHtmlUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ExtractHtmlSpansUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.HasRichFormattingUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ObserveTextHtmlStateUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ParseFromHtmlUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.StripHtmlFormattingUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.UnescapeHtmlUseCase
import org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlEvent
import org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class TextHtmlDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyTextHtmlRepository
    private lateinit var convertToHtmlUseCase: ConvertToHtmlUseCase
    private lateinit var parseFromHtmlUseCase: ParseFromHtmlUseCase
    private lateinit var escapeHtmlUseCase: EscapeHtmlUseCase
    private lateinit var unescapeHtmlUseCase: UnescapeHtmlUseCase
    private lateinit var stripHtmlFormattingUseCase: StripHtmlFormattingUseCase
    private lateinit var extractHtmlSpansUseCase: ExtractHtmlSpansUseCase
    private lateinit var hasRichFormattingUseCase: HasRichFormattingUseCase
    private lateinit var observeTextHtmlStateUseCase: ObserveTextHtmlStateUseCase
    private lateinit var clearTextHtmlStateUseCase: ClearTextHtmlStateUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyTextHtmlRepository()
        convertToHtmlUseCase = ConvertToHtmlUseCase(repository)
        parseFromHtmlUseCase = ParseFromHtmlUseCase(repository)
        escapeHtmlUseCase = EscapeHtmlUseCase(repository)
        unescapeHtmlUseCase = UnescapeHtmlUseCase(repository)
        stripHtmlFormattingUseCase = StripHtmlFormattingUseCase(repository)
        extractHtmlSpansUseCase = ExtractHtmlSpansUseCase()
        hasRichFormattingUseCase = HasRichFormattingUseCase()
        observeTextHtmlStateUseCase = ObserveTextHtmlStateUseCase(repository)
        clearTextHtmlStateUseCase = ClearTextHtmlStateUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBasicHtmlEscapingAndUnescaping() {
        val raw = "Hello <world> & \"peace\"\nTwo  spaces"
        val escaped = escapeHtmlUseCase(raw)

        assertTrue(escaped.contains("&lt;world&gt;"))
        assertTrue(escaped.contains("&amp;"))
        assertTrue(escaped.contains("&quot;peace&quot;"))
        assertTrue(escaped.contains("<br>"))
        assertTrue(escaped.contains("&nbsp; "))

        val unescaped = unescapeHtmlUseCase(escaped)
        assertEquals("Hello <world> & \"peace\"\nTwo  spaces", unescaped)
    }

    @Test
    fun testRichTextToHtmlConversion() {
        val plainText = "Hello bold and italic text"
        val spans = listOf(
            HtmlTextSpan(type = HtmlTextSpanType.BOLD, start = 6, end = 10),
            HtmlTextSpan(type = HtmlTextSpanType.ITALIC, start = 15, end = 21)
        )
        val richText = RichFormattedText(plainText = plainText, spans = spans)

        val html = convertToHtmlUseCase(richText)
        assertEquals("Hello <b>bold</b> and <i>italic</i> text", html)

        val state = repository.observeState().value
        assertEquals(html, state.lastHtml)
        assertEquals(2, state.spansCount)
        assertFalse(state.hasCustomEmoji)
        assertFalse(state.hasQuotes)
    }

    @Test
    fun testHtmlParsingToRichText() {
        val html = "Test <spoiler>secret</spoiler> with <pre lang=\"kotlin\">val x = 1</pre> and <animated-emoji data-document-id=\"12345\">star</animated-emoji>"
        val parsed = parseFromHtmlUseCase(html)

        assertEquals("Test secret with val x = 1 and star", parsed.plainText)
        assertEquals(3, parsed.spans.size)

        val spoilerSpan = parsed.spans.find { it.type == HtmlTextSpanType.SPOILER }
        assertNotNull(spoilerSpan)
        assertEquals(5, spoilerSpan!!.start)
        assertEquals(11, spoilerSpan.end)

        val codeSpan = parsed.spans.find { it.type == HtmlTextSpanType.CODE }
        assertNotNull(codeSpan)
        assertEquals("kotlin", codeSpan!!.language)

        val emojiSpan = parsed.spans.find { it.type == HtmlTextSpanType.CUSTOM_EMOJI }
        assertNotNull(emojiSpan)
        assertEquals(12345L, emojiSpan!!.documentId)
    }

    @Test
    fun testStripHtmlFormatting() {
        val html = "<b>Header</b><br>Quote: <blockquote>Citation text</blockquote><br>Link: <a href=\"https://telegram.org\">Telegram</a>"
        val stripped = stripHtmlFormattingUseCase(html)

        assertEquals("Header\nQuote: Citation text\nLink: Telegram", stripped)
    }

    @Test
    fun testHasRichFormattingDetection() {
        val emptyRich = RichFormattedText("Simple string")
        assertFalse(hasRichFormattingUseCase(emptyRich))

        val formattedRich = RichFormattedText(
            plainText = "Formatted",
            spans = listOf(HtmlTextSpan(HtmlTextSpanType.UNDERLINE, 0, 9))
        )
        assertTrue(hasRichFormattingUseCase(formattedRich))

        assertTrue(hasRichFormattingUseCase("<b>Bold</b>"))
        assertFalse(hasRichFormattingUseCase("Plain text without tags"))
    }

    @Test
    fun testTextHtmlViewModelFlow() = runTest {
        val viewModel = TextHtmlViewModel(
            convertToHtmlUseCase = convertToHtmlUseCase,
            parseFromHtmlUseCase = parseFromHtmlUseCase,
            escapeHtmlUseCase = escapeHtmlUseCase,
            stripHtmlFormattingUseCase = stripHtmlFormattingUseCase,
            observeTextHtmlStateUseCase = observeTextHtmlStateUseCase,
            clearTextHtmlStateUseCase = clearTextHtmlStateUseCase
        )

        assertEquals("", viewModel.uiState.value.currentHtml)
        assertEquals(0, viewModel.uiState.value.spansCount)

        // Convert rich text
        val rich = RichFormattedText(
            plainText = "Hello link",
            spans = listOf(HtmlTextSpan(type = HtmlTextSpanType.URL, start = 6, end = 10, url = "https://t.me"))
        )
        viewModel.onEvent(TextHtmlEvent.ConvertToHtml(rich))
        testScheduler.runCurrent()

        assertEquals("Hello <a href=\"https://t.me\">link</a>", viewModel.uiState.value.currentHtml)
        assertEquals(1, viewModel.uiState.value.spansCount)

        // Parse HTML
        val sampleHtml = "<blockquote collapsed>Hidden quote</blockquote>"
        viewModel.onEvent(TextHtmlEvent.ParseHtml(sampleHtml))
        testScheduler.runCurrent()

        assertEquals("Hidden quote", viewModel.uiState.value.currentPlainText)
        assertEquals(1, viewModel.uiState.value.spansCount)
        assertTrue(viewModel.uiState.value.hasQuotes)

        // Escape text
        viewModel.onEvent(TextHtmlEvent.EscapeText("1 < 2 & 3 > 2"))
        testScheduler.runCurrent()

        assertEquals("1 &lt; 2 &amp; 3 &gt; 2", viewModel.uiState.value.currentHtml)

        // Clear state
        viewModel.onEvent(TextHtmlEvent.ClearState)
        testScheduler.runCurrent()

        assertEquals("", viewModel.uiState.value.currentHtml)
        assertEquals(0, viewModel.uiState.value.spansCount)
    }
}
