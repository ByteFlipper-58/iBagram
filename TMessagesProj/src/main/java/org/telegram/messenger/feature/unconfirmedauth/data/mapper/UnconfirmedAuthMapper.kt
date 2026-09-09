package org.telegram.messenger.feature.unconfirmedauth.data.mapper

import org.telegram.messenger.UnconfirmedAuthController
import org.telegram.messenger.feature.unconfirmedauth.domain.model.UnconfirmedAuthModel
import org.telegram.messenger.feature.unconfirmedauth.domain.model.UnconfirmedAuthStateModel

object UnconfirmedAuthMapper {

    fun mapAuth(auth: UnconfirmedAuthController.UnconfirmedAuth?): UnconfirmedAuthModel? {
        if (auth == null) return null
        return UnconfirmedAuthModel(
            hash = auth.hash,
            date = auth.date,
            device = auth.device ?: "",
            location = auth.location ?: "",
            isBot = auth.bot,
            botId = auth.bot_id,
            expiresAfterSeconds = auth.expiresAfter(),
            isExpired = auth.expired(),
        )
    }

    fun mapAuthList(auths: List<UnconfirmedAuthController.UnconfirmedAuth>?): List<UnconfirmedAuthModel> {
        if (auths == null) return emptyList()
        return auths.mapNotNull { mapAuth(it) }
    }

    fun mapState(
        auths: List<UnconfirmedAuthController.UnconfirmedAuth>?,
        isLoading: Boolean = false
    ): UnconfirmedAuthStateModel {
        return UnconfirmedAuthStateModel(
            auths = mapAuthList(auths),
            isLoading = isLoading
        )
    }
}
