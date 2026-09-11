package org.telegram.messenger.feature.media.sharedmedia.domain.model

/**
 * Типы вкладок общего медиа (соответствуют TAB_* в SharedMediaLayout).
 */
enum class SharedMediaTabType(val id: Int) {
    PHOTO_VIDEO(0),
    FILES(1),
    VOICE(2),
    LINKS(3),
    AUDIO(4),
    GIF(5),
    COMMON_GROUPS(6),
    GROUP_USERS(7),
    STORIES(8),
    ARCHIVED_STORIES(9),
    RECOMMENDED_CHANNELS(10),
    SAVED_DIALOGS(11),
    SAVED_MESSAGES(12),
    BOT_PREVIEWS(13),
    GIFTS(14),
    POLLS(15);

    companion object {
        @JvmStatic
        fun fromId(id: Int): SharedMediaTabType {
            return entries.firstOrNull { it.id == id } ?: PHOTO_VIDEO
        }
    }
}

/**
 * Тип фильтра для вкладки Фото/Видео (соответствует FILTER_* в SharedMediaLayout).
 */
enum class SharedMediaFilterType(val id: Int) {
    ALL(0),
    PHOTOS_ONLY(1),
    VIDEOS_ONLY(2);

    companion object {
        @JvmStatic
        fun fromId(id: Int): SharedMediaFilterType {
            return entries.firstOrNull { it.id == id } ?: ALL
        }
    }
}

/**
 * Элемент общего медиа.
 */
data class SharedMediaItem(
    val id: Long,
    val messageId: Int,
    val dialogId: Long,
    val date: Long,
    val monthKey: String,
    val isVideo: Boolean = false,
    val isPhoto: Boolean = false,
    val isAudio: Boolean = false,
    val isDocument: Boolean = false,
    val isLink: Boolean = false,
    val isVoice: Boolean = false,
    val caption: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val duration: Int = 0
)

/**
 * Период для быстрого скролла (соответствует Period в SharedMediaLayout).
 */
data class SharedMediaPeriod(
    val date: Int,
    val maxId: Int,
    val startOffset: Int,
    val formattedDate: String
)

/**
 * Спецификация вкладки (видимость, заголовок, счётчик).
 */
data class SharedMediaTabSpec(
    val type: SharedMediaTabType,
    val title: String,
    val count: Int = 0,
    val isEnabled: Boolean = true
)

/**
 * Состояние мультивыбора элементов общего медиа.
 */
data class SharedMediaSelectionState(
    val selectedIds: Set<Int> = emptySet(),
    val canForward: Boolean = false,
    val canDelete: Boolean = false,
    val canPin: Boolean = false
) {
    val isSelectionActive: Boolean get() = selectedIds.isNotEmpty()
    val count: Int get() = selectedIds.size
}

/**
 * Полное доменное состояние общего медиа.
 */
data class SharedMediaState(
    val dialogId: Long = 0L,
    val isEncrypted: Boolean = false,
    val currentTab: SharedMediaTabType = SharedMediaTabType.PHOTO_VIDEO,
    val filterType: SharedMediaFilterType = SharedMediaFilterType.ALL,
    val availableTabs: List<SharedMediaTabSpec> = emptyList(),
    val items: List<SharedMediaItem> = emptyList(),
    val sections: Map<String, List<SharedMediaItem>> = emptyMap(),
    val periods: List<SharedMediaPeriod> = emptyList(),
    val selection: SharedMediaSelectionState = SharedMediaSelectionState(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true
)
