package org.telegram.messenger.feature.emojieffects.domain.usecase

import org.telegram.messenger.feature.emojieffects.domain.model.EmojiInteractionSession
import java.util.Locale

/**
 * Кодирует сессию тапов в JSON-пейлоад для MTProto TL_sendMessageEmojiInteraction.
 * Формат: {"v":1,"a":[{"i":1,"t":0.0},{"i":2,"t":0.15}]}
 */
class EncodeEmojiInteractionsJsonUseCase {

    companion object {
        const val VERSION = 1
    }

    operator fun invoke(session: EmojiInteractionSession): String {
        val actions = session.toActions()
        val actionsJson = actions.joinToString(separator = ",") { action ->
            val iValue = action.index + 1
            val tValue = String.format(Locale.US, "%.3f", action.timeOffsetSeconds).trimEnd('0').let {
                if (it.endsWith(".")) "${it}0" else it
            }
            """{"i":$iValue,"t":$tValue}"""
        }
        return """{"v":$VERSION,"a":[$actionsJson]}"""
    }
}
