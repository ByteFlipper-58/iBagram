package org.telegram.messenger.feature.media.sharedmedia.domain.usecase

import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem

/**
 * Юзкейс для группировки медиа-файлов по месяцам (секциям).
 * Сохраняет исходный порядок секций и элементов внутри секций.
 */
class GroupMediaByMonthUseCase {

    operator fun invoke(items: List<SharedMediaItem>): Map<String, List<SharedMediaItem>> {
        val result = linkedMapOf<String, MutableList<SharedMediaItem>>()
        for (item in items) {
            val key = item.monthKey.ifEmpty { "Other" }
            result.getOrPut(key) { mutableListOf() }.add(item)
        }
        return result
    }
}
