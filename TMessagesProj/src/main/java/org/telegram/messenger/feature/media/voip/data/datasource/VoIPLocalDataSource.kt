package org.telegram.messenger.feature.media.voip.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.voip.domain.model.CallModel
import org.telegram.messenger.feature.media.voip.domain.model.CallState

class VoIPLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val lock = Any()
    private val _currentCall = MutableStateFlow<CallModel?>(null)
    val currentCall: StateFlow<CallModel?> = _currentCall.asStateFlow()

    fun getCurrentCall(): CallModel? = synchronized(lock) { _currentCall.value }

    fun setCurrentCall(call: CallModel?) {
        synchronized(lock) {
            _currentCall.value = call
        }
    }

    fun startCall(userId: Long, isVideo: Boolean) {
        synchronized(lock) {
            _currentCall.value = CallModel(
                userId = userId,
                isVideo = isVideo,
                isOutgoing = true,
                state = CallState.REQUESTING
            )
        }
    }

    fun acceptCall() {
        synchronized(lock) {
            val call = _currentCall.value ?: return
            _currentCall.value = call.copy(state = CallState.ACTIVE)
        }
    }

    fun declineCall() {
        synchronized(lock) {
            val call = _currentCall.value ?: return
            _currentCall.value = call.copy(state = CallState.ENDED)
        }
    }

    fun hangUp() {
        synchronized(lock) {
            val call = _currentCall.value ?: return
            _currentCall.value = call.copy(state = CallState.ENDED)
        }
    }

    fun toggleMute(): Boolean {
        synchronized(lock) {
            val call = _currentCall.value ?: return false
            val newMute = !call.isMuted
            _currentCall.value = call.copy(isMuted = newMute)
            return newMute
        }
    }

    fun toggleSpeakerphone(): Boolean {
        synchronized(lock) {
            val call = _currentCall.value ?: return false
            val newSpeaker = !call.isSpeakerphoneOn
            _currentCall.value = call.copy(isSpeakerphoneOn = newSpeaker)
            return newSpeaker
        }
    }
}
