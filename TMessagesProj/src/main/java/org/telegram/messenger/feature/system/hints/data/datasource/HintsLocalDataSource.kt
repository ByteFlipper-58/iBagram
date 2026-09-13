package org.telegram.messenger.feature.system.hints.data.datasource

import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.system.hints.data.mapper.HintMapper
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.ui.Components.HintsController
import java.util.concurrent.ConcurrentHashMap

/**
 * Local data source encapsulating persistence for hints via SharedPreferences
 * with in-memory fallback for headless/unit-test environments.
 */
class HintsLocalDataSource(
    private val account: Int = 0
) {
    private val inMemoryCache = ConcurrentHashMap<String, Int>()

    fun getShowsCount(type: HintType): Int {
        val prefs = getPreferences()
        return if (prefs != null) {
            try {
                prefs.getInt(type.preferenceKey, inMemoryCache[type.preferenceKey] ?: 0)
            } catch (_: Throwable) {
                inMemoryCache[type.preferenceKey] ?: 0
            }
        } else {
            inMemoryCache[type.preferenceKey] ?: 0
        }
    }

    fun setShowsCount(type: HintType, count: Int) {
        inMemoryCache[type.preferenceKey] = count
        val prefs = getPreferences()
        if (prefs != null) {
            try {
                prefs.edit().putInt(type.preferenceKey, count).apply()
            } catch (_: Throwable) {
                // Keep in-memory
            }
        }
    }

    fun shouldShow(type: HintType): Boolean {
        val legacyHint = try {
            HintMapper.toLegacyHint(type)
        } catch (_: Throwable) {
            null
        }

        if (legacyHint != null && isPreferencesAvailable()) {
            return try {
                legacyHint.show()
            } catch (_: Throwable) {
                checkFallbackShow(type)
            }
        }
        return checkFallbackShow(type)
    }

    fun increment(type: HintType) {
        val legacyHint = try {
            HintMapper.toLegacyHint(type)
        } catch (_: Throwable) {
            null
        }

        if (legacyHint != null && isPreferencesAvailable()) {
            try {
                legacyHint.increment()
            } catch (_: Throwable) {
                val current = getShowsCount(type)
                setShowsCount(type, current + 1)
            }
        } else {
            val current = getShowsCount(type)
            setShowsCount(type, current + 1)
        }
    }

    fun doNotShowAgain(type: HintType) {
        val legacyHint = try {
            HintMapper.toLegacyHint(type)
        } catch (_: Throwable) {
            null
        }

        if (legacyHint != null && isPreferencesAvailable()) {
            try {
                legacyHint.doNotShowAgain()
            } catch (_: Throwable) {
                setShowsCount(type, type.showsLimit)
            }
        } else {
            setShowsCount(type, type.showsLimit)
        }
    }

    fun reset(type: HintType) {
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
    }

    fun resetAll() {
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
    }

    private fun checkFallbackShow(type: HintType): Boolean {
        val count = getShowsCount(type)
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
}
