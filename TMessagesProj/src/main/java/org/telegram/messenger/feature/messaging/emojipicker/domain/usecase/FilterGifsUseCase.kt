package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.GifItem

class FilterGifsUseCase {
    operator fun invoke(gifs: List<GifItem>, query: String): List<GifItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return gifs

        val lowerQuery = trimmed.lowercase()
        return gifs.filter { gif ->
            gif.query?.lowercase()?.contains(lowerQuery) == true ||
                gif.id.lowercase().contains(lowerQuery)
        }
    }
}
