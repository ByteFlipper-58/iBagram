package org.telegram.messenger.feature.profile.data.mapper

import org.telegram.messenger.feature.profile.domain.model.ProfileModel
import org.telegram.tgnet.TLRPC

/**
 * Pure mapper transforming legacy [TLRPC.User] / [TLRPC.UserFull] or
 * [TLRPC.Chat] / [TLRPC.ChatFull] into unified [ProfileModel] domain instances.
 */
object ProfileMapper {

    fun mapToDomain(
        user: TLRPC.User,
        userFull: TLRPC.UserFull? = null,
        isBlocked: Boolean = false
    ): ProfileModel {
        val fullName = "${user.first_name ?: ""} ${user.last_name ?: ""}".trim()
        val title = fullName.ifEmpty { user.first_name ?: user.phone ?: "User" }

        return ProfileModel(
            id = user.id,
            title = title,
            firstName = user.first_name,
            lastName = user.last_name,
            username = user.username,
            phone = user.phone,
            bio = userFull?.about,
            isBot = user.bot,
            isChannel = false,
            isGroup = false,
            isVerified = user.verified,
            isPremium = user.premium,
            isBlocked = isBlocked || userFull?.blocked == true,
            membersCount = null
        )
    }

    fun mapToDomain(
        chat: TLRPC.Chat,
        chatFull: TLRPC.ChatFull? = null,
        isBlocked: Boolean = false
    ): ProfileModel {
        val count = chatFull?.participants_count ?: chat.participants_count

        return ProfileModel(
            id = if (chat.id > 0) -chat.id else chat.id,
            title = chat.title ?: "Chat",
            firstName = null,
            lastName = null,
            username = chat.username,
            phone = null,
            bio = chatFull?.about,
            isBot = false,
            isChannel = chat.broadcast,
            isGroup = !chat.broadcast,
            isVerified = chat.verified,
            isPremium = false,
            isBlocked = isBlocked,
            membersCount = if (count > 0) count else null
        )
    }

    fun mapToDomain(
        id: Long,
        title: String,
        firstName: String? = null,
        lastName: String? = null,
        username: String? = null,
        phone: String? = null,
        bio: String? = null,
        isBot: Boolean = false,
        isChannel: Boolean = false,
        isGroup: Boolean = false,
        isVerified: Boolean = false,
        isPremium: Boolean = false,
        isBlocked: Boolean = false,
        membersCount: Int? = null
    ): ProfileModel {
        return ProfileModel(
            id = id,
            title = title,
            firstName = firstName,
            lastName = lastName,
            username = username,
            phone = phone,
            bio = bio,
            isBot = isBot,
            isChannel = isChannel,
            isGroup = isGroup,
            isVerified = isVerified,
            isPremium = isPremium,
            isBlocked = isBlocked,
            membersCount = membersCount
        )
    }
}
