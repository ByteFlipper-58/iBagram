package org.telegram.messenger.feature.push.data.mapper

import org.telegram.messenger.PushListenerController
import org.telegram.messenger.feature.push.domain.model.PushServiceType
import org.telegram.messenger.feature.push.domain.model.PushStatusModel

object PushMapper {
    fun toPushServiceType(legacyPushType: Int): PushServiceType =
        when (legacyPushType) {
            PushListenerController.PUSH_TYPE_FIREBASE -> PushServiceType.FIREBASE
            PushListenerController.PUSH_TYPE_HUAWEI -> PushServiceType.HUAWEI
            else -> PushServiceType.UNKNOWN
        }

    fun toLegacyPushType(serviceType: PushServiceType): Int =
        when (serviceType) {
            PushServiceType.FIREBASE -> PushListenerController.PUSH_TYPE_FIREBASE
            PushServiceType.HUAWEI -> PushListenerController.PUSH_TYPE_HUAWEI
            PushServiceType.UNKNOWN -> PushListenerController.PUSH_TYPE_FIREBASE
        }

    fun mapToPushStatus(
        token: String?,
        legacyPushType: Int,
        status: String?,
        hasServices: Boolean,
        isRegisteredForAccount: Boolean,
        registeredAccountsCount: Int,
        providerTitle: String
    ): PushStatusModel = PushStatusModel(
        token = token,
        serviceType = toPushServiceType(legacyPushType),
        status = status,
        hasServices = hasServices,
        isRegisteredForCurrentAccount = isRegisteredForAccount,
        registeredAccountsCount = registeredAccountsCount,
        providerTitle = providerTitle
    )
}
