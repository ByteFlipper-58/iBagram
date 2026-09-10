package org.telegram.messenger.feature.network.pushlistener.data.mapper

import org.telegram.messenger.PushListenerController
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType

object PushListenerMapper {

    fun toLegacyPushType(type: PushType): Int {
        return when (type) {
            PushType.FIREBASE -> PushListenerController.PUSH_TYPE_FIREBASE
            PushType.HUAWEI -> PushListenerController.PUSH_TYPE_HUAWEI
        }
    }

    fun toPushType(legacyType: Int): PushType {
        return when (legacyType) {
            PushListenerController.PUSH_TYPE_FIREBASE -> PushType.FIREBASE
            PushListenerController.PUSH_TYPE_HUAWEI -> PushType.HUAWEI
            else -> PushType.FIREBASE
        }
    }
}
