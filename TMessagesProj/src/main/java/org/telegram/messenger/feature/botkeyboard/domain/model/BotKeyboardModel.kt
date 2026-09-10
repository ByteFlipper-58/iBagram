package org.telegram.messenger.feature.botkeyboard.domain.model

enum class BotButtonColor {
    NONE,
    PRIMARY,
    SUCCESS,
    DANGER
}

enum class BotCustomButtonType(val id: Int) {
    SUGGESTION_DECLINE(1),
    SUGGESTION_ACCEPT(2),
    SUGGESTION_EDIT(3),
    OPEN_MESSAGE_THREAD(4),
    GIFT_OFFER_DECLINE(5),
    GIFT_OFFER_ACCEPT(6),
    SHARING_OFFER_DECLINE(7),
    SHARING_OFFER_ACCEPT(8);

    companion object {
        fun fromId(id: Int): BotCustomButtonType? = values().firstOrNull { it.id == id }
    }
}

enum class BotButtonTypeCategory {
    CALLBACK,
    URL,
    WEB_VIEW,
    BUY,
    GAME,
    SWITCH_INLINE,
    URL_AUTH,
    COPY,
    CUSTOM,
    DISABLED,
    UNKNOWN
}

sealed class BotButtonItem {
    abstract val text: String
    abstract val color: BotButtonColor

    data class BotAction(
        override val text: String,
        override val color: BotButtonColor = BotButtonColor.NONE,
        val category: BotButtonTypeCategory = BotButtonTypeCategory.UNKNOWN,
        val iconEmoji: Long = 0L,
        val data: ByteArray? = null,
        val url: String? = null
    ) : BotButtonItem() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BotAction) return false
            if (text != other.text) return false
            if (color != other.color) return false
            if (category != other.category) return false
            if (iconEmoji != other.iconEmoji) return false
            if (url != other.url) return false
            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false
            return true
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + color.hashCode()
            result = 31 * result + category.hashCode()
            result = 31 * result + iconEmoji.hashCode()
            result = 31 * result + (data?.contentHashCode() ?: 0)
            result = 31 * result + (url?.hashCode() ?: 0)
            return result
        }
    }

    data class CustomAction(
        val customType: BotCustomButtonType,
        override val text: String,
        override val color: BotButtonColor = BotButtonColor.NONE,
        val iconRes: Int = 0
    ) : BotButtonItem()
}

data class BotKeyboardRow(
    val buttons: List<BotButtonItem> = emptyList(),
    val hasSeparator: Boolean = false
)

data class BotKeyboardLayout(
    val rows: List<BotKeyboardRow> = emptyList()
) {
    val isEmpty: Boolean get() = rows.isEmpty()
    val totalButtonsCount: Int get() = rows.sumOf { it.buttons.size }
}

data class BotKeyboardState(
    val activeKeyboards: Map<Long, BotKeyboardLayout> = emptyMap(),
    val lastPressedButton: BotButtonItem? = null
)
