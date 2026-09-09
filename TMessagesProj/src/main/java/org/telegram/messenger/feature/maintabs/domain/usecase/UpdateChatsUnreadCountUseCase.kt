package org.telegram.messenger.feature.maintabs.domain.usecase

import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository

class UpdateChatsUnreadCountUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(unreadCount: Int) {
        repository.updateChatsUnreadCount(unreadCount)
    }
}
