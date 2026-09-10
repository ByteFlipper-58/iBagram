package org.telegram.messenger.feature.media.cachebychats.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.feature.media.cachebychats.data.mapper.CacheByChatsMapper
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaExceptionModel
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository

/**
 * Legacy adapter implementing [CacheByChatsRepository] on top of [CacheByChatsController].
 */
class LegacyCacheByChatsRepository(
    private val account: Int
) : CacheByChatsRepository {

    private val lock = Any()

    // In-memory cache for headless/unit-test environments
    private val inMemoryDurations = mutableMapOf<CacheChatType, KeepMediaDuration>()
    private val inMemoryExceptions = mutableListOf<KeepMediaExceptionModel>()

    private val _configFlow = MutableStateFlow(CacheByChatsConfigModel())
    override fun observeConfig(): Flow<CacheByChatsConfigModel> = _configFlow.asStateFlow()
    override fun getConfig(): CacheByChatsConfigModel = _configFlow.value

    init {
        // Initialize default durations
        for (type in CacheChatType.entries) {
            inMemoryDurations[type] = KeepMediaDuration.defaultForType(type)
        }
        recomputeConfig()
    }

    private fun isAndroidEnvironment(): Boolean {
        return try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun getLegacyController(): CacheByChatsController? {
        return try {
            if (isAndroidEnvironment()) CacheByChatsController(account) else null
        } catch (_: Throwable) {
            null
        }
    }

    override fun getDuration(type: CacheChatType): KeepMediaDuration {
        synchronized(lock) {
            val controller = getLegacyController()
            if (controller != null) {
                val raw = controller.getKeepMedia(type.rawType)
                return KeepMediaDuration.fromRaw(raw)
            }
            return inMemoryDurations[type] ?: KeepMediaDuration.defaultForType(type)
        }
    }

    override fun setDuration(type: CacheChatType, duration: KeepMediaDuration) {
        synchronized(lock) {
            inMemoryDurations[type] = duration
            val controller = getLegacyController()
            if (controller != null) {
                controller.setKeepMedia(type.rawType, duration.rawValue)
            }
            recomputeConfig()
        }
    }

    override fun getExceptions(type: CacheChatType): List<KeepMediaExceptionModel> {
        synchronized(lock) {
            val controller = getLegacyController()
            if (controller != null) {
                val list = controller.getKeepMediaExceptions(type.rawType) ?: return emptyList()
                return list.map { CacheByChatsMapper.toDomainException(it, type) }
            }
            return inMemoryExceptions.filter { it.type == type }
        }
    }

    override fun setException(dialogId: Long, type: CacheChatType, duration: KeepMediaDuration) {
        synchronized(lock) {
            inMemoryExceptions.removeAll { it.dialogId == dialogId && it.type == type }
            inMemoryExceptions.add(KeepMediaExceptionModel(dialogId, type, duration))

            val controller = getLegacyController()
            if (controller != null) {
                val current = controller.getKeepMediaExceptions(type.rawType) ?: ArrayList()
                current.removeAll { it.dialogId == dialogId }
                current.add(CacheByChatsMapper.toLegacyException(KeepMediaExceptionModel(dialogId, type, duration)))
                controller.saveKeepMediaExceptions(type.rawType, current)
            }
            recomputeConfig()
        }
    }

    override fun removeException(dialogId: Long, type: CacheChatType) {
        synchronized(lock) {
            inMemoryExceptions.removeAll { it.dialogId == dialogId && it.type == type }

            val controller = getLegacyController()
            if (controller != null) {
                val current = controller.getKeepMediaExceptions(type.rawType) ?: ArrayList()
                current.removeAll { it.dialogId == dialogId }
                controller.saveKeepMediaExceptions(type.rawType, current)
            }
            recomputeConfig()
        }
    }

    override fun clearAllExceptions(type: CacheChatType) {
        synchronized(lock) {
            inMemoryExceptions.removeAll { it.type == type }

            val controller = getLegacyController()
            if (controller != null) {
                controller.saveKeepMediaExceptions(type.rawType, ArrayList())
            }
            recomputeConfig()
        }
    }

    private fun recomputeConfig() {
        val userDur = getDuration(CacheChatType.USER)
        val groupDur = getDuration(CacheChatType.GROUP)
        val channelDur = getDuration(CacheChatType.CHANNEL)
        val storiesDur = getDuration(CacheChatType.STORIES)

        val allExceptions = mutableListOf<KeepMediaExceptionModel>()
        for (type in CacheChatType.entries) {
            allExceptions.addAll(getExceptions(type))
        }

        _configFlow.value = CacheByChatsConfigModel(
            userDuration = userDur,
            groupDuration = groupDur,
            channelDuration = channelDur,
            storiesDuration = storiesDur,
            exceptions = allExceptions
        )
    }
}
