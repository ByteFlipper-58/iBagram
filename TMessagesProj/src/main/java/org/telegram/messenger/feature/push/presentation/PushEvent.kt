package org.telegram.messenger.feature.push.presentation

import org.telegram.messenger.feature.push.domain.model.PushServiceType

sealed class PushEvent {
    object RefreshStatus : PushEvent()
    object RequestPushToken : PushEvent()
    data class RegisterPushToken(val serviceType: PushServiceType, val token: String?) : PushEvent()
    object ResetPushToken : PushEvent()
    object DismissError : PushEvent()
    object DismissInfo : PushEvent()
}
