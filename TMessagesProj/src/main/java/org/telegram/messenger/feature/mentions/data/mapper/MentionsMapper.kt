package org.telegram.messenger.feature.mentions.data.mapper

import org.telegram.messenger.feature.mentions.domain.model.MentionCandidate
import org.telegram.tgnet.TLRPC

/**
 * Маппер сущностей Telegram (TLRPC.User, TL_botCommand, хэштеги) в доменные модели кандидатов упоминаний.
 */
class MentionsMapper {

    fun toUserCandidate(user: TLRPC.User?): MentionCandidate.UserCandidate? {
        if (user == null) return null
        return MentionCandidate.UserCandidate(
            id = "user_${user.id}",
            userId = user.id,
            username = user.username,
            firstName = user.first_name,
            lastName = user.last_name,
            isBot = user.bot,
            isVerified = user.verified,
            isPremium = user.premium
        )
    }

    fun toBotCommandCandidate(
        command: TLRPC.TL_botCommand?,
        botId: Long = 0L,
        isEphemeral: Boolean = false
    ): MentionCandidate.BotCommandCandidate? {
        if (command == null || command.command.isNullOrBlank()) return null
        return MentionCandidate.BotCommandCandidate(
            id = "cmd_${botId}_${command.command}",
            command = command.command,
            helpText = command.description,
            botId = botId,
            isEphemeral = isEphemeral
        )
    }

    fun toHashtagCandidate(hashtag: String?, isChannelHint: Boolean = false): MentionCandidate.HashtagCandidate? {
        if (hashtag.isNullOrBlank()) return null
        val cleanTag = hashtag.removePrefix("#")
        return MentionCandidate.HashtagCandidate(
            id = "tag_$cleanTag",
            hashtag = cleanTag,
            isChannelHint = isChannelHint
        )
    }

    fun toEmojiKeywordCandidate(emoji: String?, keyword: String?): MentionCandidate.EmojiKeywordCandidate? {
        if (emoji.isNullOrBlank() || keyword.isNullOrBlank()) return null
        return MentionCandidate.EmojiKeywordCandidate(
            id = "emoji_${keyword}_$emoji",
            emoji = emoji,
            keyword = keyword
        )
    }
}
