package org.telegram.messenger.feature.richcaption.domain.model

enum class CaptionSpanType {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKE,
    CODE,
    URL
}

data class CaptionEntitySpan(
    val start: Int,
    val end: Int,
    val type: CaptionSpanType,
    val url: String? = null
) {
    val length: Int
        get() = maxOf(0, end - start)
}

data class RichCaptionModel(
    val plainText: String = "",
    val credit: String? = null,
    val spans: List<CaptionEntitySpan> = emptyList(),
    val isLocked: Boolean = false
) {
    val isEmpty: Boolean
        get() = plainText.isEmpty() && credit.isNullOrEmpty()

    val length: Int
        get() = plainText.length

    val hasSpans: Boolean
        get() = spans.isNotEmpty()
}

data class CaptionMeasureSpec(
    val parentWidthPx: Int,
    val leftInsetPx: Int = 0,
    val rightInsetPx: Int = 0,
    val horizontalPaddingPx: Int = 0
) {
    val availableWidth: Int
        get() = maxOf(0, parentWidthPx - leftInsetPx - rightInsetPx - 2 * horizontalPaddingPx)
}

data class CaptionHitResult(
    val isHit: Boolean,
    val lineIndex: Int = -1
)
