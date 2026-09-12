package org.telegram.messenger.feature.messaging.factcheck.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.factcheck.data.datasource.FactCheckLocalDataSource
import org.telegram.messenger.feature.messaging.factcheck.data.datasource.FactCheckRemoteDataSource
import org.telegram.messenger.feature.messaging.factcheck.data.mapper.FactCheckMapper
import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckModel
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Modern repository implementation coordinating remote MTProto RPCs and local SQLite/in-memory caches
 * for message fact checks.
 */
class FactCheckRepositoryImpl(
    private val account: Int,
    private val localDataSource: FactCheckLocalDataSource,
    private val remoteDataSource: FactCheckRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FactCheckRepository {

    private val fastCache = ConcurrentHashMap<String, FactCheckModel>()

    override fun observeFactCheckLoaded(): Flow<Unit> {
        return NotificationCenterFlowBridge.observeEvent(account, NotificationCenter.factCheckLoaded)
            .map { }
            .flowOn(mainDispatcher)
    }

    override suspend fun getFactCheck(dialogId: Long, messageId: Int, hash: Long): FactCheckModel? =
        withContext(ioDispatcher) {
            val key = "$dialogId:$messageId"
            fastCache[key]?.let { return@withContext it }

            if (hash != 0L) {
                fastCache["hash:$hash"]?.let { return@withContext it }

                val memoryTl = localDataSource.getFactCheckFromMemory(hash)
                if (memoryTl != null) {
                    val domain = FactCheckMapper.toDomain(memoryTl, dialogId, messageId)
                    fastCache[key] = domain
                    fastCache["hash:$hash"] = domain
                    return@withContext domain
                }

                val dbTl = localDataSource.getFactCheckFromDatabase(hash)
                if (dbTl != null) {
                    localDataSource.putFactCheckToMemory(hash, dbTl)
                    val domain = FactCheckMapper.toDomain(dbTl, dialogId, messageId)
                    fastCache[key] = domain
                    fastCache["hash:$hash"] = domain
                    return@withContext domain
                }
            }

            null
        }

    override suspend fun loadFactCheck(dialogId: Long, messageId: Int): Result<FactCheckModel?> =
        withContext(mainDispatcher) {
            try {
                val peer = localDataSource.getInputPeer(dialogId)
                    ?: return@withContext Result.failure(AppError.NotFound("InputPeer not found for dialogId: $dialogId"))

                when (val remoteResult = remoteDataSource.getFactCheck(peer, messageId)) {
                    is Result.Success -> {
                        val factCheckTl = remoteResult.data
                        if (factCheckTl != null) {
                            localDataSource.putFactCheckToMemory(factCheckTl.hash, factCheckTl)
                            localDataSource.saveFactCheckToDatabase(factCheckTl)
                            localDataSource.notifyFactCheckLoaded()

                            val domain = FactCheckMapper.toDomain(factCheckTl, dialogId, messageId)
                            val key = "$dialogId:$messageId"
                            fastCache[key] = domain
                            fastCache["hash:${factCheckTl.hash}"] = domain
                            Result.Success(domain)
                        } else {
                            Result.Success(null)
                        }
                    }
                    is Result.Failure -> Result.failure(remoteResult.error)
                }
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to load fact check", e)
            }
        }

    override suspend fun applyFactCheck(
        dialogId: Long,
        messageId: Int,
        text: String,
        entities: List<FactCheckEntityModel>?
    ): Result<Unit> = withContext(mainDispatcher) {
        try {
            if (text.isBlank()) {
                return@withContext deleteFactCheck(dialogId, messageId)
            }

            val peer = localDataSource.getInputPeer(dialogId)
                ?: return@withContext Result.failure(AppError.NotFound("InputPeer not found for dialogId: $dialogId"))

            val tlText = FactCheckMapper.toTlTextWithEntities(text, entities)
            when (val remoteResult = remoteDataSource.editFactCheck(peer, messageId, tlText)) {
                is Result.Success -> {
                    localDataSource.processUpdates(remoteResult.data)
                    Result.Success(Unit)
                }
                is Result.Failure -> Result.failure(remoteResult.error)
            }
        } catch (e: Exception) {
            Result.failure(e.message ?: "Failed to apply fact check", e)
        }
    }

    override suspend fun deleteFactCheck(dialogId: Long, messageId: Int): Result<Unit> =
        withContext(mainDispatcher) {
            try {
                val peer = localDataSource.getInputPeer(dialogId)
                    ?: return@withContext Result.failure(AppError.NotFound("InputPeer not found for dialogId: $dialogId"))

                when (val remoteResult = remoteDataSource.deleteFactCheck(peer, messageId)) {
                    is Result.Success -> {
                        localDataSource.processUpdates(remoteResult.data)
                        val key = "$dialogId:$messageId"
                        fastCache.remove(key)
                        Result.Success(Unit)
                    }
                    is Result.Failure -> Result.failure(remoteResult.error)
                }
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to delete fact check", e)
            }
        }

    override suspend fun getFactCheckLimit(): Int = withContext(mainDispatcher) {
        localDataSource.getFactCheckLimit()
    }
}
