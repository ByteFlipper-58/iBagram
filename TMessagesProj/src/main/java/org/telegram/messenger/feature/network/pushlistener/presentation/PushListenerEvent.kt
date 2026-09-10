package org.telegram.messenger.feature.network.pushlistener.presentation

import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType

sealed class PushListenerEvent {
    data class ProcessPush(val pushType: PushType, val rawData: String) : PushListenerEvent()
    data class RegisterToken(val pushType: PushType, val token: String) : PushListenerEvent()
    data class ToggleListening(val enabled: Boolean) : PushListenerEvent()
    object ClearHistory : PushListenerEvent()
    object ClearMessage : PushListenerEvent()
}
