package org.telegram.messenger.feature.sessions.data.mapper

import org.telegram.messenger.feature.sessions.domain.model.SessionModel
import org.telegram.messenger.feature.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.sessions.domain.model.WebSessionModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

object SessionMapper {

    fun mapSession(auth: TLRPC.TL_authorization): SessionModel {
        val isCurrent = (auth.flags and 1) != 0 || auth.current
        val canSecretChats = auth.api_id != 2040 && auth.api_id != 2496
        val canCalls = auth.api_id != 22

        return SessionModel(
            hash = auth.hash,
            deviceModel = auth.device_model ?: "",
            platform = auth.platform ?: "",
            systemVersion = auth.system_version ?: "",
            appName = auth.app_name ?: "",
            appVersion = auth.app_version ?: "",
            dateCreated = auth.date_created,
            dateActive = auth.date_active,
            ip = auth.ip ?: "",
            country = auth.country ?: "",
            region = auth.region ?: "",
            isCurrent = isCurrent,
            isOfficialApp = auth.official_app,
            isPasswordPending = auth.password_pending,
            acceptSecretChats = !auth.encrypted_requests_disabled,
            acceptCalls = !auth.call_requests_disabled,
            canAcceptSecretChats = canSecretChats,
            canAcceptCalls = canCalls,
            isUnconfirmed = auth.unconfirmed
        )
    }

    fun mapWebSession(auth: TLRPC.TL_webAuthorization): WebSessionModel {
        return WebSessionModel(
            hash = auth.hash,
            botId = auth.bot_id,
            domain = auth.domain ?: "",
            browser = auth.browser ?: "",
            platform = auth.platform ?: "",
            dateCreated = auth.date_created,
            dateActive = auth.date_active,
            ip = auth.ip ?: "",
            region = auth.region ?: ""
        )
    }

    fun mapAuthorizations(res: TL_account.authorizations): SessionsListModel {
        var current: SessionModel? = null
        val other = ArrayList<SessionModel>()
        val passwordPending = ArrayList<SessionModel>()

        for (auth in res.authorizations) {
            val model = mapSession(auth)
            if (model.isCurrent) {
                current = model
            } else if (model.isPasswordPending) {
                passwordPending.add(model)
            } else {
                other.add(model)
            }
        }

        return SessionsListModel(
            currentSession = current,
            otherSessions = other,
            passwordPendingSessions = passwordPending,
            ttlDays = res.authorization_ttl_days
        )
    }
}
