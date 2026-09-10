package org.telegram.messenger.feature.richcaption.data.mapper

import org.telegram.messenger.feature.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.richcaption.domain.model.CaptionSpanType
import org.telegram.messenger.feature.richcaption.domain.model.RichCaptionModel

object RichCaptionMapper {

    fun createCaption(
        plainText: String,
        credit: String? = null,
        spans: List<CaptionEntitySpan> = emptyList(),
        isLocked: Boolean = false
    ): RichCaptionModel {
        val sanitizedSpans = spans.filter { span ->
            span.start >= 0 && span.end <= plainText.length && span.start < span.end
        }
        return RichCaptionModel(
            plainText = plainText,
            credit = credit?.trim()?.ifEmpty { null },
            spans = sanitizedSpans,
            isLocked = isLocked
        )
    }

    fun parseSpanType(typeString: String?): CaptionSpanType {
        return when (typeString?.lowercase()) {
            "bold", "b" -> CaptionSpanType.BOLD
            "italic", "i" -> CaptionSpanType.ITALIC
            "underline", "u" -> CaptionSpanType.UNDERLINE
            "strike", "s" -> CaptionSpanType.STRIKE
            "code", "mono" -> CaptionSpanType.CODE
            "url", "link" -> CaptionSpanType.URL
            else -> CaptionSpanType.BOLD
        }
    }
}
