package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

/**
 * Проверяет, поддерживается ли заданный эмодзи для полноэкранных интерактивных анимаций.
 */
class EvaluateEmojiSupportUseCase(
    private val normalizeEmojiUseCase: NormalizeEmojiUseCase = NormalizeEmojiUseCase()
) {

    companion object {
        val EXCLUDED_KEYCAP_EMOJI: Set<String> = setOf(
            "\u0030\u20E3", "\u0031\u20E3", "\u0032\u20E3", "\u0033\u20E3", "\u0034\u20E3",
            "\u0035\u20E3", "\u0036\u20E3", "\u0037\u20E3", "\u0038\u20E3", "\u0039\u20E3"
        )

        val COLORED_HEARTS: Set<String> = setOf(
            "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎"
        )
    }

    operator fun invoke(
        rawEmoji: String?,
        supportedPackEmoticons: Set<String>
    ): Boolean {
        if (rawEmoji.isNullOrEmpty()) return false
        val normalized = normalizeEmojiUseCase(rawEmoji) ?: return false

        if (EXCLUDED_KEYCAP_EMOJI.contains(normalized)) {
            return false
        }

        if (supportedPackEmoticons.contains(normalized)) {
            return true
        }

        if (COLORED_HEARTS.contains(normalized) && (supportedPackEmoticons.contains("❤") || supportedPackEmoticons.contains("\u2764\uFE0F") || supportedPackEmoticons.contains("\u2764"))) {
            return true
        }

        return false
    }
}
