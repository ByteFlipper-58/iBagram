package org.telegram.messenger.feature.system.launchericon.domain.model

enum class LauncherIconType(
    val key: String,
    val isPremium: Boolean
) {
    DEFAULT("DefaultIcon", false),
    VINTAGE("VintageIcon", false),
    AQUA("AquaIcon", false),
    PREMIUM("PremiumIcon", true),
    TURBO("TurboIcon", true),
    NOX("NoxIcon", true);

    companion object {
        fun fromKey(key: String): LauncherIconType {
            return values().firstOrNull { it.key.equals(key, ignoreCase = true) } ?: DEFAULT
        }
    }
}
