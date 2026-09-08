package org.telegram.messenger.feature.passkeys.data.mapper

import org.telegram.messenger.feature.passkeys.domain.model.PasskeyModel
import org.telegram.messenger.feature.passkeys.domain.model.PasskeysStateModel
import org.telegram.tgnet.tl.TL_account

object PasskeyMapper {

    fun toDomain(passkey: TL_account.Passkey?): PasskeyModel? {
        if (passkey == null) return null
        val id = passkey.id ?: ""
        val name = passkey.name ?: ""
        val createdDate = passkey.date.toLong()
        val lastUsageDate = if (passkey.last_usage_date != 0) passkey.last_usage_date.toLong() else null
        val softwareEmojiId = passkey.software_emoji_id

        return PasskeyModel(
            id = id,
            name = name,
            createdDate = createdDate,
            lastUsageDate = lastUsageDate,
            softwareEmojiId = softwareEmojiId
        )
    }

    fun toDomainList(passkeys: List<TL_account.Passkey>?): List<PasskeyModel> {
        if (passkeys.isNullOrEmpty()) return emptyList()
        return passkeys.mapNotNull { toDomain(it) }
    }

    fun toState(
        passkeys: List<PasskeyModel>,
        maxPasskeys: Int,
        isSupported: Boolean
    ): PasskeysStateModel {
        return PasskeysStateModel(
            passkeys = passkeys,
            maxPasskeys = maxPasskeys,
            isSupported = isSupported
        )
    }
}
