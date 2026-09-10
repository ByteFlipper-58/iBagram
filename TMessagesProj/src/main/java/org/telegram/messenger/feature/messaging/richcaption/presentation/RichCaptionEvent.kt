package org.telegram.messenger.feature.messaging.richcaption.presentation

import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionEntitySpan

sealed interface RichCaptionEvent {
    data class SetText(val text: String, val spans: List<CaptionEntitySpan> = emptyList()) : RichCaptionEvent
    data class SetCredit(val credit: String?) : RichCaptionEvent
    data class SetLocked(val locked: Boolean) : RichCaptionEvent
    data class SetEditing(val isEditing: Boolean) : RichCaptionEvent
    data class SetHasSelection(val hasSelection: Boolean) : RichCaptionEvent
    data object Clear : RichCaptionEvent
}
