package org.telegram.messenger.feature.messaging.stickers.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.feature.messaging.stickers.data.mapper.StickerMapper
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel

/**
 * Локальный источник данных для работы со стикер-паками, недавними стикерами и сопоставлением с emoji.
 */
class StickersLocalDataSource(
    private val currentAccount: Int
) {

    private val lock = Any()
    private val _stickerSets = MutableStateFlow<Map<Int, List<StickerSetModel>>>(emptyMap())
    private val _recentStickers = MutableStateFlow<Map<Int, List<StickerModel>>>(emptyMap())

    private val isLegacyAvailable: Boolean
        get() = try {
            org.telegram.messenger.ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    init {
        trySyncFromLegacy()
    }

    private fun trySyncFromLegacy() {
        if (!isLegacyAvailable) return
        try {
            val controller = MediaDataController.getInstance(currentAccount) ?: return
            val sets0 = controller.getStickerSets(0)
            if (sets0 != null && sets0.isNotEmpty()) {
                val mapped = StickerMapper.toSetDomainList(ArrayList(sets0))
                _stickerSets.value = _stickerSets.value + (0 to mapped)
            }
            val recent0 = controller.getRecentStickers(0)
            if (recent0 != null && recent0.isNotEmpty()) {
                val mapped = StickerMapper.toDomainList(recent0)
                _recentStickers.value = _recentStickers.value + (0 to mapped)
            }
        } catch (_: Throwable) {
            // Headless / mock environment
        }
    }

    fun observeStickerSets(type: Int): Flow<List<StickerSetModel>> {
        return _stickerSets.map { it[type] ?: emptyList() }
    }

    fun getStickerSets(type: Int): List<StickerSetModel> {
        val cached = _stickerSets.value[type]
        if (!cached.isNullOrEmpty()) return cached
        if (!isLegacyAvailable) return emptyList()

        return try {
            val controller = MediaDataController.getInstance(currentAccount)
            val sets = controller?.getStickerSets(type)
            if (sets != null) {
                val mapped = StickerMapper.toSetDomainList(ArrayList(sets))
                setStickerSets(type, mapped)
                mapped
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun setStickerSets(type: Int, sets: List<StickerSetModel>) = synchronized(lock) {
        _stickerSets.value = _stickerSets.value + (type to sets)
    }

    fun getStickerSet(id: Long): StickerSetModel? {
        for (list in _stickerSets.value.values) {
            val found = list.firstOrNull { it.id == id }
            if (found != null) return found
        }
        if (!isLegacyAvailable) return null
        return try {
            val set = MediaDataController.getInstance(currentAccount)?.getStickerSetById(id)
            if (set != null) StickerMapper.toDomain(set) else null
        } catch (_: Throwable) {
            null
        }
    }

    fun getRecentStickers(type: Int): List<StickerModel> {
        val cached = _recentStickers.value[type]
        if (!cached.isNullOrEmpty()) return cached
        if (!isLegacyAvailable) return emptyList()

        return try {
            val recent = MediaDataController.getInstance(currentAccount)?.getRecentStickers(type)
            if (recent != null) {
                val mapped = StickerMapper.toDomainList(recent)
                synchronized(lock) {
                    _recentStickers.value = _recentStickers.value + (type to mapped)
                }
                mapped
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun setRecentStickers(type: Int, stickers: List<StickerModel>) = synchronized(lock) {
        _recentStickers.value = _recentStickers.value + (type to stickers)
    }

    fun getStickersForEmoji(emoji: String): List<StickerModel> {
        if (!isLegacyAvailable) return emptyList()
        return try {
            val all = MediaDataController.getInstance(currentAccount)?.allStickers ?: return emptyList()
            val cleanEmoji = emoji.trim()
            val list = all[cleanEmoji]
            StickerMapper.toDomainList(list)
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
