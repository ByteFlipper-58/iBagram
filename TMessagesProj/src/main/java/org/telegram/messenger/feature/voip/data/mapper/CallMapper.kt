package org.telegram.messenger.feature.voip.data.mapper

import org.telegram.messenger.ContactsController
import org.telegram.messenger.UserObject
import org.telegram.messenger.feature.voip.domain.model.CallModel
import org.telegram.messenger.feature.voip.domain.model.CallState
import org.telegram.messenger.voip.Instance
import org.telegram.messenger.voip.VoIPService
import org.telegram.tgnet.TLRPC

/**
 * Maps legacy Telegram VoIP service state and models to clean domain representations.
 */
object CallMapper {

    fun mapState(legacyState: Int): CallState {
        return when (legacyState) {
            VoIPService.STATE_REQUESTING -> CallState.REQUESTING
            VoIPService.STATE_WAITING_INCOMING -> CallState.WAITING_INCOMING
            VoIPService.STATE_RINGING -> CallState.RINGING
            VoIPService.STATE_WAITING,
            VoIPService.STATE_WAIT_INIT,
            VoIPService.STATE_WAIT_INIT_ACK,
            VoIPService.STATE_CREATING -> CallState.CONNECTING
            VoIPService.STATE_EXCHANGING_KEYS -> CallState.EXCHANGING_KEYS
            VoIPService.STATE_ESTABLISHED -> CallState.ACTIVE
            VoIPService.STATE_RECONNECTING -> CallState.RECONNECTING
            VoIPService.STATE_BUSY -> CallState.BUSY
            VoIPService.STATE_HANGING_UP,
            VoIPService.STATE_ENDED -> CallState.ENDED
            VoIPService.STATE_FAILED -> CallState.FAILED
            else -> CallState.IDLE
        }
    }

    fun toDomain(
        user: TLRPC.User?,
        isOutgoing: Boolean,
        isVideo: Boolean,
        callStateInt: Int,
        durationSeconds: Long,
        isMuted: Boolean,
        isSpeakerphoneOn: Boolean
    ): CallModel {
        val userId = user?.id ?: 0L
        val userName = if (user != null) {
            val formatted = ContactsController.formatName(user.first_name, user.last_name)
            if (!formatted.isNullOrBlank()) formatted else (UserObject.getUserName(user) ?: "")
        } else ""

        return CallModel(
            userId = userId,
            userName = userName,
            isOutgoing = isOutgoing,
            isVideo = isVideo,
            state = mapState(callStateInt),
            durationSeconds = durationSeconds,
            isMuted = isMuted,
            isSpeakerphoneOn = isSpeakerphoneOn
        )
    }

    fun toDomain(service: VoIPService?): CallModel? {
        if (service == null) return null
        val user = service.user
        return toDomain(
            user = user,
            isOutgoing = service.isOutgoing,
            isVideo = service.isCallingVideo,
            callStateInt = service.callState,
            durationSeconds = service.callDuration / 1000L,
            isMuted = service.isMicMute,
            isSpeakerphoneOn = service.isSpeakerphoneOn
        )
    }
}
