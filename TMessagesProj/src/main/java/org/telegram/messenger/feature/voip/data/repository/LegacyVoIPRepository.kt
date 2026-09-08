package org.telegram.messenger.feature.voip.data.repository

import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.voip.data.mapper.CallMapper
import org.telegram.messenger.feature.voip.domain.model.CallModel
import org.telegram.messenger.feature.voip.domain.repository.VoIPRepository
import org.telegram.messenger.voip.VoIPService

/**
 * Clean adapter implementing [VoIPRepository] backed by legacy [VoIPService] and [NotificationCenter].
 */
class LegacyVoIPRepository(
    private val currentAccount: Int
) : VoIPRepository {

    override fun observeCurrentCall(): Flow<CallModel?> = callbackFlow {
        var currentVoipService: VoIPService? = null

        val stateListener = object : VoIPService.StateListener {
            override fun onStateChanged(state: Int) {
                trySend(CallMapper.toDomain(currentVoipService))
            }

            override fun onAudioSettingsChanged() {
                trySend(CallMapper.toDomain(currentVoipService))
            }

            override fun onMediaStateUpdated(audioState: Int, videoState: Int) {
                trySend(CallMapper.toDomain(currentVoipService))
            }
        }

        fun attachService(service: VoIPService?) {
            if (currentVoipService != service) {
                currentVoipService?.unregisterStateListener(stateListener)
                currentVoipService = service
                service?.registerStateListener(stateListener)
            }
            trySend(CallMapper.toDomain(service))
        }

        val notificationCenterDelegate = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
            when (id) {
                NotificationCenter.didStartedCall,
                NotificationCenter.didReceiveCall -> {
                    attachService(VoIPService.getSharedInstance())
                }
                NotificationCenter.didEndCall -> {
                    currentVoipService?.unregisterStateListener(stateListener)
                    currentVoipService = null
                    trySend(null)
                }
            }
        }

        val nc = NotificationCenter.getInstance(currentAccount)
        nc.addObserver(notificationCenterDelegate, NotificationCenter.didStartedCall)
        nc.addObserver(notificationCenterDelegate, NotificationCenter.didReceiveCall)
        nc.addObserver(notificationCenterDelegate, NotificationCenter.didEndCall)

        attachService(VoIPService.getSharedInstance())

        awaitClose {
            nc.removeObserver(notificationCenterDelegate, NotificationCenter.didStartedCall)
            nc.removeObserver(notificationCenterDelegate, NotificationCenter.didReceiveCall)
            nc.removeObserver(notificationCenterDelegate, NotificationCenter.didEndCall)
            currentVoipService?.unregisterStateListener(stateListener)
            currentVoipService = null
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getCurrentCall(): CallModel? = withContext(Dispatchers.Main) {
        CallMapper.toDomain(VoIPService.getSharedInstance())
    }

    override suspend fun startCall(userId: Long, isVideo: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val context = ApplicationLoader.applicationContext
            val intent = Intent(context, VoIPService::class.java).apply {
                putExtra("user_id", userId)
                putExtra("is_outgoing", true)
                putExtra("start_incall_activity", false)
                putExtra("video_call", isVideo)
                putExtra("can_video_call", isVideo)
                putExtra("account", currentAccount)
            }
            context.startService(intent)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to start call to user $userId", e))
        }
    }

    override suspend fun acceptCall(): Result<Unit> = withContext(Dispatchers.Main) {
        val service = VoIPService.getSharedInstance()
        if (service != null) {
            service.acceptIncomingCall()
            Result.success(Unit)
        } else {
            Result.failure(AppError.Generic("No active VoIP service found to accept call"))
        }
    }

    override suspend fun declineCall(): Result<Unit> = withContext(Dispatchers.Main) {
        val service = VoIPService.getSharedInstance()
        if (service != null) {
            service.declineIncomingCall()
            Result.success(Unit)
        } else {
            Result.failure(AppError.Generic("No active VoIP service found to decline call"))
        }
    }

    override suspend fun hangUp(): Result<Unit> = withContext(Dispatchers.Main) {
        val service = VoIPService.getSharedInstance()
        if (service != null) {
            service.hangUp()
            Result.success(Unit)
        } else {
            Result.failure(AppError.Generic("No active VoIP service found to hang up"))
        }
    }

    override suspend fun toggleMute(): Result<Boolean> = withContext(Dispatchers.Main) {
        val service = VoIPService.getSharedInstance()
        if (service != null) {
            val newMute = !service.isMicMute
            service.setMicMute(newMute, false, true)
            Result.success(newMute)
        } else {
            Result.failure(AppError.Generic("No active VoIP service found to toggle mute"))
        }
    }

    override suspend fun toggleSpeakerphone(): Result<Boolean> = withContext(Dispatchers.Main) {
        val service = VoIPService.getSharedInstance()
        if (service != null) {
            service.toggleSpeakerphoneOrShowRouteSheet(ApplicationLoader.applicationContext, false)
            Result.success(service.isSpeakerphoneOn)
        } else {
            Result.failure(AppError.Generic("No active VoIP service found to toggle speakerphone"))
        }
    }
}
