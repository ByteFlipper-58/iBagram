package org.telegram.messenger.feature.network.push.presentation

import org.telegram.messenger.feature.network.push.domain.model.PushServiceType

sealed class PushEvent {
    object RefreshStatus : PushEvent()
    object RequestPushToken : PushEvent()
    data class RegisterPushToken(val serviceType: PushServiceType, val token: String?) : PushEvent()
    object ResetPushToken : PushEvent()
    object DismissError : PushEvent()
    object DismissInfo : PushEvent()
}
