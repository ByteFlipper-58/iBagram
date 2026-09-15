package org.telegram.messenger.feature.messaging.reactions.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.feature.messaging.reactions.data.mapper.ReactionMapper
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionsSettingsModel

/**
 * Локальный источник данных реакций, управляющий кэшем доступных реакций, недавних реакций
 * и настройкой быстрой реакции (double-tap).
 */
class ReactionsLocalDataSource(
    private val currentAccount: Int
) {

    private val lock = Any()
    private val _availableReactions = MutableStateFlow<List<ReactionItemModel>>(emptyList())
    private val _recentReactions = MutableStateFlow<List<ReactionItemModel>>(emptyList())
    private var _doubleTapReaction: String? = null

    init {
        trySyncFromLegacy()
    }

    private fun trySyncFromLegacy() {
        try {
            val controller = MediaDataController.getInstance(currentAccount) ?: return
            val available = controller.reactionsList?.map { ReactionMapper.mapAvailableReaction(it) } ?: emptyList()
            if (available.isNotEmpty()) {
                _availableReactions.value = available
            }
            val recent = controller.recentReactions?.map { ReactionMapper.mapReaction(it) } ?: emptyList()
            if (recent.isNotEmpty()) {
                _recentReactions.value = recent
            }
            _doubleTapReaction = controller.doubleTapReaction
        } catch (_: Throwable) {
            // Headless / mock environment
        }
    }

    fun observeAvailableReactions(): StateFlow<List<ReactionItemModel>> = _availableReactions.asStateFlow()

    fun getAvailableReactions(): List<ReactionItemModel> {
        trySyncFromLegacy()
        return _availableReactions.value
    }

    fun setAvailableReactions(reactions: List<ReactionItemModel>) = synchronized(lock) {
        _availableReactions.value = reactions
    }

    fun observeRecentReactions(): StateFlow<List<ReactionItemModel>> = _recentReactions.asStateFlow()

    fun getRecentReactions(): List<ReactionItemModel> {
        trySyncFromLegacy()
        return _recentReactions.value
    }

    fun setRecentReactions(reactions: List<ReactionItemModel>) = synchronized(lock) {
        _recentReactions.value = reactions
    }

    fun getDoubleTapReaction(): String? {
        trySyncFromLegacy()
        return _doubleTapReaction
    }

    fun setDoubleTapReaction(reaction: String) = synchronized(lock) {
        _doubleTapReaction = reaction
        try {
            val controller = MediaDataController.getInstance(currentAccount)
            controller?.doubleTapReaction = reaction
        } catch (_: Throwable) {
            // Headless / mock environment
        }
    }

    fun getReactionsSettings(): ReactionsSettingsModel {
        trySyncFromLegacy()
        val top = try {
            MediaDataController.getInstance(currentAccount)?.topReactions?.map { ReactionMapper.mapReaction(it) } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
        return ReactionsSettingsModel(
            doubleTapReaction = _doubleTapReaction,
            availableReactions = _availableReactions.value,
            recentReactions = _recentReactions.value,
            topReactions = top
        )
    }
}
