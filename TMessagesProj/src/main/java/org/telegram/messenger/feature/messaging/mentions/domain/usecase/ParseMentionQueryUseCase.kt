package org.telegram.messenger.feature.messaging.mentions.domain.usecase

import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionTriggerType

/**
 * Парсит текст сообщения в текущей позиции курсора и извлекает параметры триггера автодополнения.
 */
class ParseMentionQueryUseCase(
    private val validateUsernameUseCase: ValidateUsernameUseCase = ValidateUsernameUseCase()
) {

    operator fun invoke(
        text: CharSequence?,
        cursorPosition: Int,
        allowContextBots: Boolean = true
    ): MentionQuery {
        if (text.isNullOrEmpty() || cursorPosition <= 0 || cursorPosition > text.length) {
            return MentionQuery(MentionTriggerType.NONE)
        }

        val textString = text.toString()

        // 1. Проверка на инлайн-бота в начале сообщения: "@bot query" или "@somebot"
        if (allowContextBots && textString.startsWith("@") && cursorPosition >= 1) {
            val spaceIndex = textString.indexOf(' ')
            if (spaceIndex in 1 until cursorPosition) {
                val botUsername = textString.substring(1, spaceIndex)
                if (validateUsernameUseCase(botUsername)) {
                    val botQuery = textString.substring(spaceIndex + 1, cursorPosition)
                    return MentionQuery(
                        triggerType = MentionTriggerType.BOT_CONTEXT,
                        query = botQuery,
                        startPosition = 0,
                        length = cursorPosition,
                        contextBotUsername = botUsername,
                        contextBotQuery = botQuery
                    )
                }
            } else if (spaceIndex < 0 && textString.length >= 4 && textString.endsWith("bot", ignoreCase = true)) {
                val botUsername = textString.substring(1)
                if (validateUsernameUseCase(botUsername)) {
                    return MentionQuery(
                        triggerType = MentionTriggerType.BOT_CONTEXT,
                        query = "",
                        startPosition = 0,
                        length = cursorPosition,
                        contextBotUsername = botUsername,
                        contextBotQuery = ""
                    )
                }
            }
        }

        // 2. Сканирование назад от текущей позиции курсора
        var querySb = StringBuilder()
        for (i in (cursorPosition - 1) downTo 0) {
            val ch = textString[i]
            val isStartOfToken = (i == 0 || textString[i - 1] == ' ' || textString[i - 1] == '\n')

            if (isStartOfToken || ch == ':') {
                when (ch) {
                    '@' -> {
                        val query = querySb.reverse().toString()
                        return MentionQuery(
                            triggerType = MentionTriggerType.USERNAME,
                            query = query,
                            startPosition = i,
                            length = cursorPosition - i
                        )
                    }
                    '#' -> {
                        val query = querySb.reverse().toString()
                        return MentionQuery(
                            triggerType = MentionTriggerType.HASHTAG,
                            query = query,
                            startPosition = i,
                            length = cursorPosition - i
                        )
                    }
                    '/' -> {
                        if (i == 0) {
                            val query = querySb.reverse().toString()
                            return MentionQuery(
                                triggerType = MentionTriggerType.BOT_COMMAND,
                                query = query,
                                startPosition = i,
                                length = cursorPosition - i
                            )
                        }
                    }
                    ':' -> {
                        if (querySb.isNotEmpty()) {
                            val query = querySb.reverse().toString()
                            return MentionQuery(
                                triggerType = MentionTriggerType.EMOJI_KEYWORD,
                                query = query,
                                startPosition = i,
                                length = cursorPosition - i
                            )
                        }
                    }
                }
            }

            // Пробел разрывает текущий токен (кроме контекстных ботов, обработанных выше)
            if (ch == ' ' || ch == '\n') {
                break
            }

            querySb.append(ch)
        }

        return MentionQuery(MentionTriggerType.NONE)
    }
}
