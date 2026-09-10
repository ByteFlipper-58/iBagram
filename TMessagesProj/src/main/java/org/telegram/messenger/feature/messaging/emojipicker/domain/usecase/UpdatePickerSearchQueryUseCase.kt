package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository

class UpdatePickerSearchQueryUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(query: String) {
        val trimmed = query.trim()
        repository.setSearchQuery(trimmed)
        repository.setSearchActive(trimmed.isNotEmpty())
    }
}
