package org.telegram.messenger.feature.security.authtokens.data.mapper

import org.telegram.messenger.Utilities
import org.telegram.messenger.feature.security.authtokens.domain.model.AuthTokenUserInfoModel
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLogoutTokenModel
import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.TLRPC

object AuthTokensMapper {

    fun toSavedLoginToken(auth: TLRPC.TL_auth_authorization, hex: String = ""): SavedLoginTokenModel {
        val user = auth.user
        val userInfo = if (user != null) {
            AuthTokenUserInfoModel(
                userId = user.id,
                firstName = user.first_name ?: "",
                lastName = user.last_name ?: "",
                username = user.username ?: "",
                phone = user.phone ?: ""
            )
        } else null

        val actualHex = if (hex.isNotEmpty()) {
            hex
        } else {
            runCatching {
                val data = SerializedData(auth.objectSize)
                auth.serializeToStream(data)
                Utilities.bytesToHex(data.toByteArray())
            }.getOrDefault("")
        }

        val base64Token = runCatching {
            if (auth.future_auth_token != null) {
                Utilities.bytesToHex(auth.future_auth_token)
            } else ""
        }.getOrDefault("")

        return SavedLoginTokenModel(
            hexToken = actualHex,
            futureAuthTokenBase64 = base64Token,
            userId = user?.id ?: 0L,
            userInfo = userInfo,
            otherwiseReloginDays = auth.otherwise_relogin_days,
            isPasswordSetupRequired = auth.setup_password_required,
            timestampMs = System.currentTimeMillis()
        )
    }

    fun toSavedLogoutToken(loggedOut: TLRPC.TL_auth_loggedOut, hex: String = ""): SavedLogoutTokenModel {
        val actualHex = if (hex.isNotEmpty()) {
            hex
        } else {
            runCatching {
                val data = SerializedData(loggedOut.objectSize)
                loggedOut.serializeToStream(data)
                Utilities.bytesToHex(data.toByteArray())
            }.getOrDefault("")
        }

        val base64Token = runCatching {
            if (loggedOut.future_auth_token != null) {
                Utilities.bytesToHex(loggedOut.future_auth_token)
            } else ""
        }.getOrDefault("")

        return SavedLogoutTokenModel(
            hexToken = actualHex,
            futureAuthTokenBase64 = base64Token,
            flags = loggedOut.flags,
            timestampMs = System.currentTimeMillis()
        )
    }

    fun toTLAuthAuthorization(hex: String): TLRPC.TL_auth_authorization? {
        if (hex.isBlank()) return null
        return runCatching {
            val bytes = Utilities.hexToBytes(hex) ?: return null
            val stream = SerializedData(bytes)
            val constructor = stream.readInt32(true)
            val obj = TLRPC.auth_Authorization.TLdeserialize(stream, constructor, true)
            if (obj is TLRPC.TL_auth_authorization) obj else null
        }.getOrNull()
    }

    fun toTLLoggedOut(hex: String): TLRPC.TL_auth_loggedOut? {
        if (hex.isBlank()) return null
        return runCatching {
            val bytes = Utilities.hexToBytes(hex) ?: return null
            val stream = SerializedData(bytes)
            val constructor = stream.readInt32(true)
            TLRPC.TL_auth_loggedOut.TLdeserialize(stream, constructor, true)
        }.getOrNull()
    }
}
