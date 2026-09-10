package org.telegram.messenger.feature.messaging.texthtml.data.mapper

import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpan
import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType
import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText
import java.util.regex.Pattern

/**
 * Pure Kotlin bidirectional mapper for Telegram Rich Text and HTML formatting.
 */
object TextHtmlMapper {

    private val TAG_PATTERN = Pattern.compile("</?([a-zA-Z0-9_-]+)([^>]*)>")
    private val ATTR_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")

    fun escape(text: String): String {
        val out = StringBuilder()
        var i = 0
        val length = text.length
        while (i < length) {
            val c = text[i]
            when (c) {
                '\n' -> out.append("<br>")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '&' -> out.append("&amp;")
                '"' -> out.append("&quot;")
                ' ' -> {
                    var spaces = 1
                    while (i + 1 < length && text[i + 1] == ' ') {
                        spaces++
                        i++
                    }
                    if (spaces > 1) {
                        for (s in 0 until spaces - 1) {
                            out.append("&nbsp;")
                        }
                        out.append(' ')
                    } else {
                        out.append(' ')
                    }
                }
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
    }

    fun unescape(html: String): String {
        return html
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("<br>", "\n")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
    }

    fun stripHtml(html: String): String {
        val unescaped = html.replace("<br/>", "\n").replace("<br>", "\n").replace("<br />", "\n")
        val withoutTags = TAG_PATTERN.matcher(unescaped).replaceAll("")
        return unescape(withoutTags)
    }

    fun toHtml(formattedText: RichFormattedText): String {
        if (!formattedText.hasFormatting) {
            return escape(formattedText.plainText)
        }

        val text = formattedText.plainText
        val spans = formattedText.spans.sortedWith(compareBy({ it.start }, { -it.end }))

        // Generate open and close tags per character position
        val openTags = mutableMapOf<Int, MutableList<HtmlTextSpan>>()
        val closeTags = mutableMapOf<Int, MutableList<HtmlTextSpan>>()

        for (span in spans) {
            val safeStart = span.start.coerceIn(0, text.length)
            val safeEnd = span.end.coerceIn(safeStart, text.length)
            openTags.getOrPut(safeStart) { mutableListOf() }.add(span)
            closeTags.getOrPut(safeEnd) { mutableListOf() }.add(span)
        }

        val out = StringBuilder()
        for (i in 0..text.length) {
            // Close tags at position i (reverse order of opening)
            closeTags[i]?.reversed()?.forEach { span ->
                out.append(getClosingTag(span))
            }

            // Open tags at position i
            openTags[i]?.forEach { span ->
                out.append(getOpeningTag(span))
            }

            if (i < text.length) {
                val c = text[i]
                when (c) {
                    '\n' -> out.append("<br>")
                    '<' -> out.append("&lt;")
                    '>' -> out.append("&gt;")
                    '&' -> out.append("&amp;")
                    '"' -> out.append("&quot;")
                    else -> out.append(c)
                }
            }
        }

        return out.toString()
    }

    fun parseHtml(html: String): RichFormattedText {
        val spans = mutableListOf<HtmlTextSpan>()
        val plainTextBuilder = StringBuilder()

        // Stack to track open tags: (tagType, startIndex, attributes)
        val stack = ArrayDeque<OpenTagInfo>()

        var cursor = 0
        val matcher = TAG_PATTERN.matcher(html)

        while (matcher.find()) {
            // Append text before the tag
            val textBefore = html.substring(cursor, matcher.start())
            if (textBefore.isNotEmpty()) {
                val decoded = unescape(textBefore)
                plainTextBuilder.append(decoded)
            }

            val fullTag = matcher.group(0) ?: ""
            val tagName = (matcher.group(1) ?: "").lowercase()
            val rawAttributes = matcher.group(2) ?: ""
            val isClosing = fullTag.startsWith("</")

            if (isClosing) {
                // Find matching open tag from stack
                val matchedIndex = stack.indexOfLast { it.tagName == tagName }
                if (matchedIndex != -1) {
                    val openTag = stack.removeAt(matchedIndex)
                    val endIndex = plainTextBuilder.length
                    if (endIndex > openTag.startIndex) {
                        spans.add(
                            HtmlTextSpan(
                                type = openTag.type,
                                start = openTag.startIndex,
                                end = endIndex,
                                url = openTag.attributes["href"],
                                language = openTag.attributes["lang"] ?: openTag.attributes["language"],
                                documentId = openTag.attributes["data-document-id"]?.toLongOrNull(),
                                isCollapsed = openTag.isCollapsed
                            )
                        )
                    }
                }
            } else {
                // Handle self-closing br
                if (tagName == "br") {
                    plainTextBuilder.append('\n')
                } else {
                    val type = resolveTagType(tagName, rawAttributes)
                    if (type != null) {
                        val attrs = parseAttributes(rawAttributes)
                        val isCollapsed = (type == HtmlTextSpanType.COLLAPSED_QUOTE) ||
                                rawAttributes.contains("collapsed") ||
                                rawAttributes.contains("telegram-collapsed-quote")
                        stack.add(
                            OpenTagInfo(
                                tagName = tagName,
                                type = type,
                                startIndex = plainTextBuilder.length,
                                attributes = attrs,
                                isCollapsed = isCollapsed
                            )
                        )
                    }
                }
            }

            cursor = matcher.end()
        }

        // Remainder of the text
        if (cursor < html.length) {
            val remaining = html.substring(cursor)
            plainTextBuilder.append(unescape(remaining))
        }

        return RichFormattedText(
            plainText = plainTextBuilder.toString(),
            spans = spans
        )
    }

    private fun resolveTagType(tagName: String, rawAttributes: String): HtmlTextSpanType? {
        return when (tagName) {
            "b", "strong" -> HtmlTextSpanType.BOLD
            "i", "em" -> HtmlTextSpanType.ITALIC
            "u" -> HtmlTextSpanType.UNDERLINE
            "s", "strike", "del" -> HtmlTextSpanType.STRIKE
            "spoiler" -> HtmlTextSpanType.SPOILER
            "code" -> HtmlTextSpanType.MONO
            "pre" -> {
                if (rawAttributes.contains("lang") || rawAttributes.contains("language")) {
                    HtmlTextSpanType.CODE
                } else {
                    HtmlTextSpanType.MONO
                }
            }
            "blockquote" -> {
                if (rawAttributes.contains("collapsed") || rawAttributes.contains("telegram-collapsed-quote")) {
                    HtmlTextSpanType.COLLAPSED_QUOTE
                } else {
                    HtmlTextSpanType.QUOTE
                }
            }
            "details" -> HtmlTextSpanType.COLLAPSED_QUOTE
            "animated-emoji" -> HtmlTextSpanType.CUSTOM_EMOJI
            "a" -> HtmlTextSpanType.URL
            else -> null
        }
    }

    private fun parseAttributes(rawAttributes: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val matcher = ATTR_PATTERN.matcher(rawAttributes)
        while (matcher.find()) {
            val key = matcher.group(1)?.lowercase() ?: ""
            val value = matcher.group(2) ?: ""
            if (key.isNotEmpty()) {
                map[key] = value
            }
        }
        return map
    }

    private fun getOpeningTag(span: HtmlTextSpan): String {
        return when (span.type) {
            HtmlTextSpanType.BOLD -> "<b>"
            HtmlTextSpanType.ITALIC -> "<i>"
            HtmlTextSpanType.UNDERLINE -> "<u>"
            HtmlTextSpanType.STRIKE -> "<s>"
            HtmlTextSpanType.SPOILER -> "<spoiler>"
            HtmlTextSpanType.MONO -> "<pre>"
            HtmlTextSpanType.CODE -> {
                if (!span.language.isNullOrEmpty()) "<pre lang=\"${span.language}\">" else "<pre>"
            }
            HtmlTextSpanType.QUOTE -> "<blockquote>"
            HtmlTextSpanType.COLLAPSED_QUOTE -> "<blockquote collapsed>"
            HtmlTextSpanType.CUSTOM_EMOJI -> {
                if (span.documentId != null) "<animated-emoji data-document-id=\"${span.documentId}\">" else "<animated-emoji>"
            }
            HtmlTextSpanType.URL -> {
                if (!span.url.isNullOrEmpty()) "<a href=\"${span.url}\">" else "<a>"
            }
        }
    }

    private fun getClosingTag(span: HtmlTextSpan): String {
        return when (span.type) {
            HtmlTextSpanType.BOLD -> "</b>"
            HtmlTextSpanType.ITALIC -> "</i>"
            HtmlTextSpanType.UNDERLINE -> "</u>"
            HtmlTextSpanType.STRIKE -> "</s>"
            HtmlTextSpanType.SPOILER -> "</spoiler>"
            HtmlTextSpanType.MONO, HtmlTextSpanType.CODE -> "</pre>"
            HtmlTextSpanType.QUOTE, HtmlTextSpanType.COLLAPSED_QUOTE -> "</blockquote>"
            HtmlTextSpanType.CUSTOM_EMOJI -> "</animated-emoji>"
            HtmlTextSpanType.URL -> "</a>"
        }
    }

    private data class OpenTagInfo(
        val tagName: String,
        val type: HtmlTextSpanType,
        val startIndex: Int,
        val attributes: Map<String, String>,
        val isCollapsed: Boolean
    )
}
