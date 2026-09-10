package org.telegram.messenger.feature.localization.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.localization.domain.model.LocalizationState
import org.telegram.messenger.feature.localization.domain.model.NameDisplayOrder
import org.telegram.messenger.feature.localization.domain.model.PluralQuantity
import org.telegram.messenger.feature.localization.domain.model.RelativeTimeModel
import org.telegram.messenger.feature.localization.domain.repository.LocalizationRepository
import java.util.Locale

class ResolvePluralQuantityUseCase {
    operator fun invoke(langCode: String?, count: Int): PluralQuantity {
        val lang = langCode?.lowercase()?.split("-", "_")?.firstOrNull() ?: "en"
        val absCount = Math.abs(count)

        return when (lang) {
            "ru", "uk", "be" -> {
                val rem10 = absCount % 10
                val rem100 = absCount % 100
                when {
                    rem10 == 1 && rem100 != 11 -> PluralQuantity.ONE
                    rem10 in 2..4 && rem100 !in 12..14 -> PluralQuantity.FEW
                    else -> PluralQuantity.MANY
                }
            }
            "pl" -> {
                val rem10 = absCount % 10
                val rem100 = absCount % 100
                when {
                    absCount == 1 -> PluralQuantity.ONE
                    rem10 in 2..4 && rem100 !in 12..14 -> PluralQuantity.FEW
                    else -> PluralQuantity.MANY
                }
            }
            "ar" -> {
                val rem100 = absCount % 100
                when {
                    absCount == 0 -> PluralQuantity.ZERO
                    absCount == 1 -> PluralQuantity.ONE
                    absCount == 2 -> PluralQuantity.TWO
                    rem100 in 3..10 -> PluralQuantity.FEW
                    rem100 in 11..99 -> PluralQuantity.MANY
                    else -> PluralQuantity.OTHER
                }
            }
            else -> {
                if (absCount == 1) PluralQuantity.ONE else PluralQuantity.OTHER
            }
        }
    }
}

class FormatRelativeTimestampUseCase {
    operator fun invoke(nowMs: Long, targetMs: Long): RelativeTimeModel {
        val diff = nowMs - targetMs
        if (diff < 0L) {
            return RelativeTimeModel(formatted = "just now", isRecent = true)
        }

        val minuteMs = 60_000L
        val hourMs = 60 * minuteMs
        val dayMs = 24 * hourMs

        return when {
            diff < minuteMs -> RelativeTimeModel(formatted = "just now", isRecent = true)
            diff < hourMs -> {
                val minutes = (diff / minuteMs).coerceAtLeast(1)
                RelativeTimeModel(formatted = "${minutes}m ago", isRecent = true)
            }
            diff < dayMs -> {
                val hours = (diff / hourMs).coerceAtLeast(1)
                RelativeTimeModel(formatted = "${hours}h ago", isRecent = false)
            }
            else -> {
                val days = (diff / dayMs).coerceAtLeast(1)
                RelativeTimeModel(formatted = "${days}d ago", isRecent = false)
            }
        }
    }
}

class FormatFullNameUseCase {
    operator fun invoke(firstName: String?, lastName: String?, order: NameDisplayOrder): String {
        val first = firstName?.trim() ?: ""
        val last = lastName?.trim() ?: ""

        return when (order) {
            NameDisplayOrder.FIRST_LAST -> {
                if (first.isNotEmpty() && last.isNotEmpty()) "$first $last"
                else first.ifEmpty { last }
            }
            NameDisplayOrder.LAST_FIRST -> {
                if (first.isNotEmpty() && last.isNotEmpty()) "$last $first"
                else last.ifEmpty { first }
            }
        }
    }
}

class FormatNumberWithSuffixUseCase {
    operator fun invoke(number: Long): String {
        val abs = Math.abs(number)
        val sign = if (number < 0) "-" else ""

        return when {
            abs >= 1_000_000_000L -> {
                String.format(Locale.US, "%s%.1fB", sign, abs.toDouble() / 1_000_000_000.0)
            }
            abs >= 1_000_000L -> {
                String.format(Locale.US, "%s%.1fM", sign, abs.toDouble() / 1_000_000.0)
            }
            abs >= 1_000L -> {
                String.format(Locale.US, "%s%.1fK", sign, abs.toDouble() / 1_000.0)
            }
            else -> "$number"
        }
    }
}

class DetectRtlLanguageUseCase {
    private val rtlCodes = setOf("ar", "fa", "he", "ur", "ug", "yi", "arc", "ckb")

    operator fun invoke(langCode: String?): Boolean {
        if (langCode == null) return false
        val base = langCode.lowercase().split("-", "_").firstOrNull() ?: ""
        return rtlCodes.contains(base)
    }
}

class ObserveLocalizationStateUseCase(private val repository: LocalizationRepository) {
    operator fun invoke(): Flow<LocalizationState> = repository.observeState()
}

class GetLocalizationStateUseCase(private val repository: LocalizationRepository) {
    operator fun invoke(): LocalizationState = repository.getState()
}

class ApplyLocaleUseCase(
    private val repository: LocalizationRepository,
    private val detectRtl: DetectRtlLanguageUseCase
) {
    operator fun invoke(locale: LocaleModel) {
        val isRtl = detectRtl(locale.code)
        repository.setCurrentLocale(locale.copy(isRtl = isRtl))
    }
}

class Toggle24HourFormatUseCase(private val repository: LocalizationRepository) {
    operator fun invoke(enabled: Boolean) {
        repository.set24HourFormat(enabled)
    }
}

class SetNameDisplayOrderUseCase(private val repository: LocalizationRepository) {
    operator fun invoke(order: NameDisplayOrder) {
        repository.setNameDisplayOrder(order)
    }
}
