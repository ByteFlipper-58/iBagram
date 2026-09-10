package org.telegram.messenger.feature.media.contentpreview.domain.model

/**
 * Типы превью контента (соответствуют CONTENT_TYPE_* в ContentPreviewViewer).
 */
enum class PreviewContentType(val id: Int) {
    NONE(-1),
    STICKER(0),
    GIF(1),
    EMOJI(2),
    CUSTOM_STICKER(3);

    companion object {
        fun fromId(id: Int): PreviewContentType {
            return entries.firstOrNull { it.id == id } ?: NONE
        }
    }
}

/**
 * Типы действий контекстного меню превью.
 */
enum class PreviewActionType {
    SEND,
    SEND_WITHOUT_SOUND,
    SCHEDULE,
    TOGGLE_FAVORITE,
    REMOVE_FROM_RECENT,
    VIEW_PACK,
    SET_EMOJI_STATUS,
    COPY_EMOJI,
    EDIT_STICKER,
    DELETE_STICKER,
    ADD_CAPTION
}

/**
 * Элемент меню действий.
 */
data class PreviewActionItem(
    val action: PreviewActionType,
    val title: String,
    val isDestructive: Boolean = false,
    val isEnabled: Boolean = true
)

/**
 * Параметры сдвига жестом для открытия контекстного меню.
 */
data class ContentPreviewGesture(
    val startY: Float,
    val currentY: Float,
    val maxDragDistance: Float = 200f
) {
    val deltaY: Float get() = (startY - currentY).coerceAtLeast(0f)
    val progress: Float get() = (deltaY / maxDragDistance).coerceIn(0f, 1f)
    val shouldShowMenu: Boolean get() = progress >= 0.75f
}

/**
 * Описание отображаемого элемента превью.
 */
data class ContentPreviewItem(
    val contentType: PreviewContentType,
    val documentId: Long = 0L,
    val emoticon: String? = null,
    val query: String? = null,
    val isRecent: Boolean = false,
    val isFavorite: Boolean = false,
    val canSchedule: Boolean = false,
    val canDelete: Boolean = false,
    val canEdit: Boolean = false,
    val canSetStatus: Boolean = false,
    val canCopy: Boolean = false,
    val canAddCaption: Boolean = false,
    val canSend: Boolean = true
)

/**
 * Полное доменное состояние превью контента.
 */
data class ContentPreviewState(
    val isVisible: Boolean = false,
    val isMenuVisible: Boolean = false,
    val currentItem: ContentPreviewItem? = null,
    val availableActions: List<PreviewActionItem> = emptyList(),
    val dragProgress: Float = 0f
)
