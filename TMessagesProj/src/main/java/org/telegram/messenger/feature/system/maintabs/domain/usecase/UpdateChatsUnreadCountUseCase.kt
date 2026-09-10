package org.telegram.messenger.feature.system.maintabs.domain.usecase

import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository

class UpdateChatsUnreadCountUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(unreadCount: Int) {
        repository.updateChatsUnreadCount(unreadCount)
    }
}
