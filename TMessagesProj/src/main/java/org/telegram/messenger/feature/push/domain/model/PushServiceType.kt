package org.telegram.messenger.feature.push.domain.model

enum class PushServiceType(val typeId: Int) {
    FIREBASE(2),
    HUAWEI(13),
    UNKNOWN(0);

    companion object {
        fun fromTypeId(typeId: Int): PushServiceType = when (typeId) {
            2 -> FIREBASE
            13 -> HUAWEI
            else -> UNKNOWN
        }
    }
}
