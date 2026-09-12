package org.telegram.messenger.feature.media.cachebychats.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.feature.media.cachebychats.data.datasource.CacheByChatsLocalDataSource
import org.telegram.messenger.feature.media.cachebychats.data.datasource.CacheByChatsRemoteDataSource
import org.telegram.messenger.feature.media.cachebychats.data.mapper.CacheByChatsMapper
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaExceptionModel
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository

/**
 * Modern repository implementation for managing media cache retention periods
 * and per-dialog exceptions across chat categories.
 */
class CacheByChatsRepositoryImpl(
    private val account: Int,
    private val localDataSource: CacheByChatsLocalDataSource,
    private val remoteDataSource: CacheByChatsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CacheByChatsRepository {

    private val lock = Any()
    private val _configFlow = MutableStateFlow(CacheByChatsConfigModel())

    init {
        recomputeConfigLocked()
    }

    override fun observeConfig(): Flow<CacheByChatsConfigModel> = _configFlow.asStateFlow()

    override fun getConfig(): CacheByChatsConfigModel = _configFlow.value

    override fun getDuration(type: CacheChatType): KeepMediaDuration {
        synchronized(lock) {
            val raw = localDataSource.getKeepMedia(type.rawType)
            return KeepMediaDuration.fromRaw(raw)
        }
    }

    override fun setDuration(type: CacheChatType, duration: KeepMediaDuration) {
        synchronized(lock) {
            localDataSource.setKeepMedia(type.rawType, duration.rawValue)
            recomputeConfigLocked()
        }
    }

    override fun getExceptions(type: CacheChatType): List<KeepMediaExceptionModel> {
        synchronized(lock) {
            val list = localDataSource.getKeepMediaExceptions(type.rawType)
            return list.map { CacheByChatsMapper.toDomainException(it, type) }
        }
    }

    override fun setException(dialogId: Long, type: CacheChatType, duration: KeepMediaDuration) {
        synchronized(lock) {
            val current = localDataSource.getKeepMediaExceptions(type.rawType)
            current.removeAll { it.dialogId == dialogId }
            current.add(CacheByChatsMapper.toLegacyException(KeepMediaExceptionModel(dialogId, type, duration)))
            localDataSource.saveKeepMediaExceptions(type.rawType, current)
            recomputeConfigLocked()
        }
    }

    override fun removeException(dialogId: Long, type: CacheChatType) {
        synchronized(lock) {
            val current = localDataSource.getKeepMediaExceptions(type.rawType)
            val removed = current.removeAll { it.dialogId == dialogId }
            if (removed) {
                localDataSource.saveKeepMediaExceptions(type.rawType, current)
                recomputeConfigLocked()
            }
        }
    }

    override fun clearAllExceptions(type: CacheChatType) {
        synchronized(lock) {
            localDataSource.saveKeepMediaExceptions(type.rawType, ArrayList())
            recomputeConfigLocked()
        }
    }

    private fun recomputeConfigLocked() {
        val userDur = getDuration(CacheChatType.USER)
        val groupDur = getDuration(CacheChatType.GROUP)
        val channelDur = getDuration(CacheChatType.CHANNEL)
        val storiesDur = getDuration(CacheChatType.STORIES)

        val allExceptions = mutableListOf<KeepMediaExceptionModel>()
        allExceptions.addAll(getExceptions(CacheChatType.USER))
        allExceptions.addAll(getExceptions(CacheChatType.GROUP))
        allExceptions.addAll(getExceptions(CacheChatType.CHANNEL))
        allExceptions.addAll(getExceptions(CacheChatType.STORIES))

        _configFlow.value = CacheByChatsConfigModel(
            userDuration = userDur,
            groupDuration = groupDur,
            channelDuration = channelDur,
            storiesDuration = storiesDur,
            exceptions = allExceptions
        )
    }
}
