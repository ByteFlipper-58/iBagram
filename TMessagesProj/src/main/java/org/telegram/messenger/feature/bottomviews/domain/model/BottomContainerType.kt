package org.telegram.messenger.feature.bottomviews.domain.model

/**
 * Known bottom container identifiers in ChatActivity bottom view layout hierarchy.
 */
enum class BottomContainerType(val id: Int) {
    DEFAULT(0),
    MESSAGE_INPUT(1),
    BOTTOM_OVERLAY_TEXT(2),
    BOTTOM_OVERLAY_CHAT(3),
    MESSAGE_SEARCH(4),
    MESSAGE_ACTION(5);

    companion object {
        fun fromId(id: Int): BottomContainerType? {
            return values().firstOrNull { it.id == id }
        }
    }
}
