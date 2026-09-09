package org.telegram.messenger.feature.unconfirmedauth.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UnconfirmedAuthController
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.unconfirmedauth.data.mapper.UnconfirmedAuthMapper
import org.telegram.messenger.feature.unconfirmedauth.domain.model.UnconfirmedAuthStateModel
import org.telegram.messenger.feature.unconfirmedauth.domain.repository.UnconfirmedAuthRepository
import java.util.ArrayList
import kotlin.coroutines.resume

class LegacyUnconfirmedAuthRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : UnconfirmedAuthRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val controller: UnconfirmedAuthController?
        get() = messagesController.getUnconfirmedAuthController()

    override fun observeUnconfirmedAuths(): Flow<UnconfirmedAuthStateModel> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.unconfirmedAuthUpdate
        )
            .map {
                val current = controller?.auths
                UnconfirmedAuthMapper.mapState(current)
            }
            .onStart {
                val current = controller?.auths
                emit(UnconfirmedAuthMapper.mapState(current))
            }
            .flowOn(mainDispatcher)
    }

    override suspend fun getUnconfirmedAuths(): UnconfirmedAuthStateModel =
        withContext(mainDispatcher) {
            val current = controller?.auths
            UnconfirmedAuthMapper.mapState(current)
        }

    override suspend fun confirmAuth(hash: Long): Result<Boolean> =
        withContext(mainDispatcher) {
            val ctrl = controller ?: return@withContext Result.failure("UnconfirmedAuthController unavailable")
            val target = ctrl.auths.firstOrNull { it.hash == hash }
                ?: return@withContext Result.failure("Unconfirmed auth with hash $hash not found")

            suspendCancellableCoroutine<Result<Boolean>> { continuation ->
                val list = ArrayList<UnconfirmedAuthController.UnconfirmedAuth>()
                list.add(target)
                ctrl.confirm(list) { successList ->
                    if (continuation.isActive) {
                        val isConfirmed = successList != null && successList.any { it.hash == hash }
                        continuation.resume(Result.Success(isConfirmed))
                    }
                }
            }
        }

    override suspend fun denyAuth(hash: Long): Result<Boolean> =
        withContext(mainDispatcher) {
            val ctrl = controller ?: return@withContext Result.failure("UnconfirmedAuthController unavailable")
            val target = ctrl.auths.firstOrNull { it.hash == hash }
                ?: return@withContext Result.failure("Unconfirmed auth with hash $hash not found")

            suspendCancellableCoroutine<Result<Boolean>> { continuation ->
                val list = ArrayList<UnconfirmedAuthController.UnconfirmedAuth>()
                list.add(target)
                ctrl.deny(list) { successList ->
                    if (continuation.isActive) {
                        val isDenied = successList != null && successList.any { it.hash == hash }
                        continuation.resume(Result.Success(isDenied))
                    }
                }
            }
        }

    override suspend fun confirmAll(): Result<Int> =
        withContext(mainDispatcher) {
            val ctrl = controller ?: return@withContext Result.failure("UnconfirmedAuthController unavailable")
            if (ctrl.auths.isEmpty()) {
                return@withContext Result.Success(0)
            }
            val copy = ArrayList(ctrl.auths)
            suspendCancellableCoroutine<Result<Int>> { continuation ->
                ctrl.confirm(copy) { successList ->
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(successList?.size ?: 0))
                    }
                }
            }
        }

    override suspend fun denyAll(): Result<Int> =
        withContext(mainDispatcher) {
            val ctrl = controller ?: return@withContext Result.failure("UnconfirmedAuthController unavailable")
            if (ctrl.auths.isEmpty()) {
                return@withContext Result.Success(0)
            }
            val copy = ArrayList(ctrl.auths)
            suspendCancellableCoroutine<Result<Int>> { continuation ->
                ctrl.deny(copy) { successList ->
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(successList?.size ?: 0))
                    }
                }
            }
        }

    override suspend fun clear(): Result<Unit> =
        withContext(mainDispatcher) {
            val ctrl = controller ?: return@withContext Result.failure("UnconfirmedAuthController unavailable")
            ctrl.cleanup()
            Result.Success(Unit)
        }
}
