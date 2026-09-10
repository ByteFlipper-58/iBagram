package org.telegram.messenger.feature.messaging.richcaption.presentation

import org.telegram.messenger.feature.messaging.richcaption.domain.model.RichCaptionModel

data class RichCaptionUiState(
    val caption: RichCaptionModel = RichCaptionModel(),
    val isEditing: Boolean = false,
    val hasSelection: Boolean = false
) {
    val plainText: String
        get() = caption.plainText

    val credit: String?
        get() = caption.credit

    val isLocked: Boolean
        get() = caption.isLocked

    val isEmpty: Boolean
        get() = caption.isEmpty
}
