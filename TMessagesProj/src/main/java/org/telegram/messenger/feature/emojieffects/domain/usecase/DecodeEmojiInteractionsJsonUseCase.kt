package org.telegram.messenger.feature.emojieffects.domain.usecase

import org.telegram.messenger.feature.emojieffects.domain.model.EmojiInteractionAction

/**
 * Декодирует входящий JSON-пейлоад действий эмодзи в список действий [EmojiInteractionAction].
 */
class DecodeEmojiInteractionsJsonUseCase {

    private val objectRegex = Regex("""\{[^{}]*\}""")
    private val indexRegex = Regex(""""i"\s*:\s*(\d+)""")
    private val timeRegex = Regex(""""t"\s*:\s*([0-9.]+)""")

    operator fun invoke(json: String?): List<EmojiInteractionAction> {
        if (json.isNullOrBlank()) return emptyList()

        val actions = mutableListOf<EmojiInteractionAction>()
        val matches = objectRegex.findAll(json)
        for (match in matches) {
            val content = match.value
            val indexMatch = indexRegex.find(content)
            val timeMatch = timeRegex.find(content)

            val rawIndex = indexMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val index = (rawIndex - 1).coerceAtLeast(0)
            val timeSeconds = timeMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

            actions.add(
                EmojiInteractionAction(
                    index = index,
                    timeOffsetSeconds = timeSeconds
                )
            )
        }
        return actions
    }
}
