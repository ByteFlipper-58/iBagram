package org.telegram.messenger.feature.hints.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.hints.data.mapper.HintMapper
import org.telegram.messenger.feature.hints.domain.model.HintModel
import org.telegram.messenger.feature.hints.domain.model.HintType
import org.telegram.messenger.feature.hints.domain.model.HintsStateModel
import org.telegram.messenger.feature.hints.domain.repository.HintsRepository
import org.telegram.ui.Components.HintsController
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe repository adapter for HintsController with reactive state flow
 * and safe fallback for headless/unit-test environments.
 */
class LegacyHintsRepository(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HintsRepository {

    private val inMemoryCache = ConcurrentHashMap<String, Int>()
    private val _hintsFlow = MutableStateFlow(readCurrentState())

    override fun observeHints(): Flow<HintsStateModel> = _hintsFlow.asStateFlow()

    override fun getHintsState(): HintsStateModel = _hintsFlow.value

    override fun getHint(type: HintType): HintModel {
        val showsCount = getShowsCount(type.preferenceKey)
        return HintMapper.toDomainModel(type, showsCount)
    }

    override fun shouldShowHint(type: HintType): Boolean {
        val legacyHint = try {
            HintMapper.toLegacyHint(type)
        } catch (_: Throwable) {
            null
        }

        if (legacyHint != null && isPreferencesAvailable()) {
            return try {
                legacyHint.show()
            } catch (_: Throwable) {
                checkShouldShowFallback(type)
            }
        }
        return checkShouldShowFallback(type)
    }

    override fun incrementHint(type: HintType) {
        val legacyHint = try {
            HintMapper.toLegacyHint(type)
        } catch (_: Throwable) {
            null
        }

        if (legacyHint != null && isPreferencesAvailable()) {
            try {
                legacyHint.increment()
            } catch (_: Throwable) {
                val current = getShowsCount(type.preferenceKey)
                setShowsCount(type.preferenceKey, current + 1)
            }
        } else {
            val current = getShowsCount(type.preferenceKey)
            setShowsCount(type.preferenceKey, current + 1)
        }
        updateState()
    }

    override fun doNotShowAgain(type: HintType) {
        val legacyHint = try {
            HintMapper.toLegacyHint(type)
        } catch (_: Throwable) {
            null
        }

        if (legacyHint != null && isPreferencesAvailable()) {
            try {
                legacyHint.doNotShowAgain()
            } catch (_: Throwable) {
                setShowsCount(type.preferenceKey, type.showsLimit)
            }
        } else {
            setShowsCount(type.preferenceKey, type.showsLimit)
        }
        updateState()
    }

    override fun resetHint(type: HintType) {
        val legacyHint = try {
            HintMapper.toLegacyHint(type)
        } catch (_: Throwable) {
            null
        }

        if (legacyHint != null && isPreferencesAvailable()) {
            try {
                legacyHint.reset()
            } catch (_: Throwable) {
                removePreference(type.preferenceKey)
            }
        } else {
            removePreference(type.preferenceKey)
        }
        updateState()
    }

    override fun resetAllHints() {
        if (isPreferencesAvailable()) {
            try {
                HintsController.resetAll()
            } catch (_: Throwable) {
                for (type in HintType.entries) {
                    removePreference(type.preferenceKey)
                }
            }
        } else {
            for (type in HintType.entries) {
                removePreference(type.preferenceKey)
            }
        }
        inMemoryCache.clear()
        updateState()
    }

    private fun checkShouldShowFallback(type: HintType): Boolean {
        val count = getShowsCount(type.preferenceKey)
        val model = HintMapper.toDomainModel(type, count)
        val random = kotlin.random.Random.nextFloat()
        return model.canShow(random)
    }

    private fun isPreferencesAvailable(): Boolean {
        return try {
            ApplicationLoader.applicationContext != null && MessagesController.getGlobalMainSettings() != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun getPreferences(): SharedPreferences? {
        return try {
            if (isPreferencesAvailable()) {
                MessagesController.getGlobalMainSettings()
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getShowsCount(key: String): Int {
        val prefs = getPreferences()
        return if (prefs != null) {
            try {
                prefs.getInt(key, inMemoryCache[key] ?: 0)
            } catch (_: Throwable) {
                inMemoryCache[key] ?: 0
            }
        } else {
            inMemoryCache[key] ?: 0
        }
    }

    private fun setShowsCount(key: String, count: Int) {
        inMemoryCache[key] = count
        val prefs = getPreferences()
        if (prefs != null) {
            try {
                prefs.edit().putInt(key, count).apply()
            } catch (_: Throwable) {
                // Keep in memory
            }
        }
    }

    private fun removePreference(key: String) {
        inMemoryCache.remove(key)
        val prefs = getPreferences()
        if (prefs != null) {
            try {
                prefs.edit().remove(key).apply()
            } catch (_: Throwable) {
                // Ignore
            }
        }
    }

    private fun readCurrentState(): HintsStateModel {
        val map = mutableMapOf<HintType, HintModel>()
        for (type in HintType.entries) {
            val count = getShowsCount(type.preferenceKey)
            map[type] = HintMapper.toDomainModel(type, count)
        }
        return HintsStateModel(hints = map)
    }

    private fun updateState() {
        _hintsFlow.value = readCurrentState()
    }
}
