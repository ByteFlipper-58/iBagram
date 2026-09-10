package org.telegram.messenger.feature.media.sharedmedia.data.mapper

import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaPeriod
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType
import java.util.Calendar
import java.util.Locale

/**
 * Чистый маппер данных общего медиа.
 */
object SharedMediaMapper {

    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun mapTabIdToType(tabId: Int): SharedMediaTabType {
        return SharedMediaTabType.fromId(tabId)
    }

    fun mapTypeToTabId(type: SharedMediaTabType): Int {
        return type.id
    }

    fun mapFilterIdToType(filterId: Int): SharedMediaFilterType {
        return SharedMediaFilterType.fromId(filterId)
    }

    fun mapTypeToFilterId(type: SharedMediaFilterType): Int {
        return type.id
    }

    /**
     * Форматирует ключ месяца ("Month YYYY") из секундного Unix-таймстемпа.
     */
    fun formatMonthKey(timestampSeconds: Long): String {
        if (timestampSeconds <= 0) return "Other"
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampSeconds * 1000L
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val monthName = if (month in 0..11) MONTH_NAMES[month] else "Unknown"
        return "$monthName $year"
    }

    /**
     * Формирует список периодов для быстрого скролла (FastScroll Periods).
     */
    fun calculateFastScrollPeriods(items: List<SharedMediaItem>): List<SharedMediaPeriod> {
        if (items.isEmpty()) return emptyList()

        val periods = mutableListOf<SharedMediaPeriod>()
        var currentKey = ""
        var currentOffset = 0

        for ((index, item) in items.withIndex()) {
            val key = item.monthKey.ifEmpty { formatMonthKey(item.date) }
            if (key != currentKey) {
                currentKey = key
                currentOffset = index
                periods.add(
                    SharedMediaPeriod(
                        date = (item.date).toInt(),
                        maxId = item.messageId,
                        startOffset = currentOffset,
                        formattedDate = currentKey
                    )
                )
            }
        }
        return periods
    }
}
