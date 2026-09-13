package org.telegram.messenger.feature.network.pushlistener.data.datasource

import org.telegram.messenger.PushListenerController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType

class PushListenerRemoteDataSource(
    val account: Int
) : BaseRemoteDataSource(account) {

    suspend fun sendRegistrationToServer(pushType: PushType, token: String): Result<Unit> {
        return runCatching {
            PushListenerController.sendRegistrationToServer(pushType.id, token)
            Result.success(Unit)
        }.getOrElse { e ->
            Result.failure(e.message ?: "Failed to send registration", e)
        }
    }

    suspend fun reportDecryptError(pushType: PushType): Result<Unit> {
        return runCatching {
            // Report to app logs or metrics if needed
            Result.success(Unit)
        }.getOrElse { e ->
            Result.failure(e.message ?: "Failed to report decrypt error", e)
        }
    }
}
