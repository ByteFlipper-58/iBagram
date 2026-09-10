package org.telegram.messenger.feature.system.themes.domain.model

/**
 * Modes for automatic night theme switching.
 */
enum class NightModeType(val value: Int) {
    NONE(0),
    SCHEDULED(1),
    AUTOMATIC(2),
    SYSTEM(3);

    companion object {
        fun fromValue(value: Int): NightModeType {
            return entries.find { it.value == value } ?: NONE
        }
    }
}
