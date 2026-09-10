package org.telegram.messenger.feature.network.push.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.network.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.network.push.domain.model.PushServiceType
import org.telegram.messenger.feature.network.push.domain.model.PushStatusModel

interface PushRepository {
    fun observePushStatus(): Flow<PushStatusModel>
    fun getPushStatus(): PushStatusModel
    fun isPushServiceAvailable(): Boolean
    suspend fun requestPushToken(): Result<PushRegistrationResult>
    suspend fun registerPushToken(serviceType: PushServiceType, token: String?): Result<Unit>
    suspend fun resetPushToken(): Result<Unit>
}
