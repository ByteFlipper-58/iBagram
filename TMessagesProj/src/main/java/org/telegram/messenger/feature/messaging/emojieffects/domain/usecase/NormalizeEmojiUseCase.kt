package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

/**
 * Нормализует эмодзи, удаляя селекторы оформления (0xFE0F), модификаторы цвета кожи
 * (0xD83C 0xDFFB..0xDFFF) и гендерные модификаторы ZWJ (0x200D 0x2640/0x2642),
 * обеспечивая точное совпадение с ключами пака интерактивных стикеров.
 */
class NormalizeEmojiUseCase {

    operator fun invoke(emoji: String?): String? {
        if (emoji == null) return null
        val sb = StringBuilder(emoji)
        var i = 0
        while (i < sb.length) {
            val c = sb[i]
            if (i < sb.length - 1) {
                val next = sb[i + 1]
                val isSkinTone = (c == '\uD83C' && next in '\uDFFB'..'\uDFFF')
                val isGender = (c == '\u200D' && (next == '\u2640' || next == '\u2642'))
                if (isSkinTone || isGender) {
                    sb.delete(i, i + 2)
                    continue
                }
            }
            if (c == '\uFE0F') {
                sb.deleteCharAt(i)
                continue
            }
            i++
        }
        return sb.toString()
    }
}
