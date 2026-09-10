package org.telegram.messenger.feature.mentions.domain.model

/**
 * Тип триггера автодополнения в поле ввода сообщения.
 */
enum class MentionTriggerType {
    NONE,
    USERNAME,
    HASHTAG,
    BOT_COMMAND,
    EMOJI_KEYWORD,
    STICKER_SUGGESTION,
    BOT_CONTEXT
}

/**
 * Распарсенный запрос автодополнения из текста ввода.
 *
 * @property triggerType Тип обнаруженного триггера
 * @property query Строка поиска (без префикса триггера)
 * @property startPosition Начальная позиция триггера в тексте
 * @property length Длина заменяемого фрагмента
 * @property contextBotUsername Имя инлайн-бота при контекстном поиске (@gif, @bot)
 * @property contextBotQuery Запрос к инлайн-боту
 */
data class MentionQuery(
    val triggerType: MentionTriggerType = MentionTriggerType.NONE,
    val query: String = "",
    val startPosition: Int = 0,
    val length: Int = 0,
    val contextBotUsername: String? = null,
    val contextBotQuery: String? = null
) {
    val isActive: Boolean
        get() = triggerType != MentionTriggerType.NONE
}

/**
 * Кандидат подсказки для вставки в сообщение.
 */
sealed interface MentionCandidate {
    val id: String

    data class UserCandidate(
        override val id: String,
        val userId: Long,
        val username: String?,
        val firstName: String?,
        val lastName: String?,
        val isBot: Boolean = false,
        val isVerified: Boolean = false,
        val isPremium: Boolean = false
    ) : MentionCandidate {
        val displayName: String
            get() = buildString {
                if (!firstName.isNullOrBlank()) append(firstName)
                if (!lastName.isNullOrBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(lastName)
                }
                if (isEmpty() && !username.isNullOrBlank()) {
                    append(username)
                }
            }
    }

    data class HashtagCandidate(
        override val id: String,
        val hashtag: String,
        val isChannelHint: Boolean = false
    ) : MentionCandidate

    data class BotCommandCandidate(
        override val id: String,
        val command: String,
        val helpText: String?,
        val botId: Long = 0L,
        val isEphemeral: Boolean = false
    ) : MentionCandidate

    data class EmojiKeywordCandidate(
        override val id: String,
        val emoji: String,
        val keyword: String
    ) : MentionCandidate

    data class QuickReplyCandidate(
        override val id: String,
        val shortcut: String,
        val text: String
    ) : MentionCandidate
}

/**
 * Результат форматирования вставки выбранного кандидата.
 *
 * @property replacementText Текст для вставки
 * @property newCursorPosition Новая позиция курсора после вставки
 */
data class MentionReplacement(
    val replacementText: String,
    val newCursorPosition: Int
)

/**
 * Состояние автодополнения и подсказок в чате.
 */
data class MentionsState(
    val query: MentionQuery = MentionQuery(),
    val candidates: List<MentionCandidate> = emptyList(),
    val isSearching: Boolean = false,
    val isPanelVisible: Boolean = false
) {
    val isEmpty: Boolean
        get() = candidates.isEmpty()
}
