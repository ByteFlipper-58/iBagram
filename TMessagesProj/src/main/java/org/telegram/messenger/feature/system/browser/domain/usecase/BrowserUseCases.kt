package org.telegram.messenger.feature.system.browser.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserState
import org.telegram.messenger.feature.system.browser.domain.model.BrowserType
import org.telegram.messenger.feature.system.browser.domain.model.UrlSafetyCheckResult
import org.telegram.messenger.feature.system.browser.domain.model.UrlTargetType
import org.telegram.messenger.feature.system.browser.domain.repository.BrowserRepository
import java.util.Locale
import java.util.regex.Pattern

class ClassifyUrlTargetUseCase {
    private val telegraphPattern = Pattern.compile("^(?:https?://)?(?:te\\.?legra\\.ph|graph\\.org)(?:[/?#].*|$)", Pattern.CASE_INSENSITIVE)
    private val instantViewPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?t\\.me/iv(?:[?#/].*|$)", Pattern.CASE_INSENSITIVE)
    private val telegramBlogPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?telegram\\.org/(?:blog|tour)(?:[/?#].*|$)", Pattern.CASE_INSENSITIVE)
    private val fragmentPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?fragment\\.com(?:[/?#].*|$)", Pattern.CASE_INSENSITIVE)
    private val tMePattern = Pattern.compile("^(?:https?://)?(?:www\\.)?(?:t\\.me|telegram\\.me|telegram\\.dog)(?:[/?#].*|$)", Pattern.CASE_INSENSITIVE)

    operator fun invoke(rawUrl: String): UrlTargetType {
        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        if (lower.startsWith("tg://") || lower.startsWith("tg:")) {
            return UrlTargetType.TELEGRAM_INTERNAL
        }
        if (lower.startsWith("ton://") || lower.contains(".ton/") || lower.endsWith(".ton")) {
            return UrlTargetType.TON_SITE
        }
        if (telegraphPattern.matcher(trimmed).find() || instantViewPattern.matcher(trimmed).find()) {
            return UrlTargetType.INSTANT_VIEW
        }
        if (fragmentPattern.matcher(trimmed).find() || telegramBlogPattern.matcher(trimmed).find()) {
            return UrlTargetType.EXTERNAL_SAFE
        }
        if (tMePattern.matcher(trimmed).find()) {
            return UrlTargetType.TELEGRAM_INTERNAL
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return UrlTargetType.EXTERNAL_UNTRUSTED
        }
        return UrlTargetType.EXTERNAL_UNTRUSTED
    }
}

class ExtractUsernameFromUrlUseCase {
    private val tMePrefixPattern = Pattern.compile("^(?:https?://)?(?:www\\.)?(?:t\\.me|telegram\\.me|telegram\\.dog)/+([a-zA-Z0-9_]{3,32})(?:[/?#].*)?$", Pattern.CASE_INSENSITIVE)

    operator fun invoke(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.startsWith("@")) {
            val candidate = trimmed.substring(1)
            return if (candidate.matches(Regex("^[a-zA-Z0-9_]{3,32}$"))) candidate else null
        }
        val matcher = tMePrefixPattern.matcher(trimmed)
        if (matcher.matches()) {
            val username = matcher.group(1)
            // Filter out system words
            if (username != null && !username.equals("iv", ignoreCase = true) && !username.equals("share", ignoreCase = true) && !username.equals("joinchat", ignoreCase = true)) {
                return username
            }
        }
        return null
    }
}

class CheckUrlSafetyUseCase(
    private val classifyUrlTarget: ClassifyUrlTargetUseCase = ClassifyUrlTargetUseCase(),
    private val extractUsername: ExtractUsernameFromUrlUseCase = ExtractUsernameFromUrlUseCase()
) {
    // Lookalike characters commonly used in homoglyph / punycode spoofing
    private val spoofCharacters = setOf('а', 'с', 'е', 'о', 'р', 'х', 'у', 'і', 'ј', 'ѕ')

    operator fun invoke(url: String): UrlSafetyCheckResult {
        val trimmed = url.trim()
        val targetType = classifyUrlTarget(trimmed)
        val extractedUser = extractUsername(trimmed)

        val host = extractHost(trimmed)
        val isPunycode = host?.contains("xn--", ignoreCase = true) == true || hasMixedScriptHomoglyphs(host ?: "")

        val requiresConfirmation = when {
            isPunycode -> true
            targetType == UrlTargetType.EXTERNAL_UNTRUSTED -> true
            targetType == UrlTargetType.TON_SITE -> false
            targetType == UrlTargetType.INSTANT_VIEW -> false
            targetType == UrlTargetType.TELEGRAM_INTERNAL -> false
            targetType == UrlTargetType.EXTERNAL_SAFE -> false
            else -> true
        }

        val isSafe = !isPunycode && (targetType != UrlTargetType.EXTERNAL_UNTRUSTED || !trimmed.startsWith("http://"))

        return UrlSafetyCheckResult(
            url = trimmed,
            targetType = targetType,
            requiresConfirmation = requiresConfirmation,
            isPunycodeSpoof = isPunycode,
            isSafe = isSafe,
            extractedUsername = extractedUser,
            host = host
        )
    }

    private fun extractHost(url: String): String? {
        val clean = url.substringAfter("://").substringBefore("/").substringBefore("?").substringBefore("#").substringBefore(":")
        return if (clean.isNotBlank()) clean.lowercase(Locale.ROOT) else null
    }

    private fun hasMixedScriptHomoglyphs(host: String): Boolean {
        var hasLatin = false
        var hasNonAscii = false
        for (ch in host) {
            if ((ch in 'a'..'z') || (ch in 'A'..'Z')) {
                hasLatin = true
            } else if (ch.code > 127) {
                hasNonAscii = true
            }
            if (hasLatin && hasNonAscii) return true
        }
        return false
    }
}

class ObserveBrowserStateUseCase(private val repository: BrowserRepository) {
    operator fun invoke(): Flow<BrowserState> = repository.observeBrowserState()
}

class GetBrowserStateUseCase(private val repository: BrowserRepository) {
    operator fun invoke(): BrowserState = repository.getBrowserState()
}

class UpdateBrowserSettingsUseCase(private val repository: BrowserRepository) {
    suspend operator fun invoke(settings: BrowserSettingsModel) {
        repository.updateSettings(settings)
    }

    suspend fun setBrowserType(type: BrowserType) {
        repository.updateBrowserType(type)
    }
}

class OpenBrowserUrlUseCase(
    private val repository: BrowserRepository,
    private val checkUrlSafety: CheckUrlSafetyUseCase = CheckUrlSafetyUseCase()
) {
    suspend operator fun invoke(url: String, forceExternal: Boolean = false): Boolean {
        val safety = checkUrlSafety(url)
        // Record in history if it's external or instant view
        if (safety.targetType != UrlTargetType.TELEGRAM_INTERNAL) {
            repository.addHistoryEntry(url, title = null, domain = safety.host, siteName = null, hasFavicon = false)
        }
        return repository.openUrl(url, forceExternal)
    }
}

class ManageBrowserHistoryUseCase(private val repository: BrowserRepository) {
    suspend fun getHistory(): List<BrowserHistoryEntryModel> = repository.getHistory()
    suspend fun clearHistory() = repository.clearHistory()
    suspend fun clearCacheAndCookies() = repository.clearCacheAndCookies()
    suspend fun addEntry(url: String, title: String? = null, domain: String? = null, siteName: String? = null, hasFavicon: Boolean = false) {
        repository.addHistoryEntry(url, title, domain, siteName, hasFavicon)
    }
}
