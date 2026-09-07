package org.telegram.messenger.core.di

import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.savedmessages.data.repository.LegacySavedMessagesRepository
import org.telegram.messenger.feature.savedmessages.domain.repository.SavedMessagesRepository
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.TogglePinSavedDialogUseCase
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesViewModel
import java.util.concurrent.ConcurrentHashMap

/**
 * Scoped service container that manages feature dependencies per [account].
 * Ensures clean lifecycle isolation between multi-account instances without heavyweight reflection.
 */
class AccountFeatureContainer private constructor(val account: Int) {

    // SavedMessages pilot feature dependencies
    val savedMessagesRepository: SavedMessagesRepository by lazy {
        LegacySavedMessagesRepository(account)
    }

    val getSavedDialogsUseCase: GetSavedDialogsUseCase by lazy {
        GetSavedDialogsUseCase(savedMessagesRepository)
    }

    val togglePinSavedDialogUseCase: TogglePinSavedDialogUseCase by lazy {
        TogglePinSavedDialogUseCase(savedMessagesRepository)
    }

    fun createSavedMessagesViewModel(): SavedMessagesViewModel {
        return SavedMessagesViewModel(
            account = account,
            getSavedDialogsUseCase = getSavedDialogsUseCase,
            togglePinSavedDialogUseCase = togglePinSavedDialogUseCase
        )
    }

    companion object {
        private val instances = ConcurrentHashMap<Int, AccountFeatureContainer>()

        @JvmStatic
        fun get(account: Int): AccountFeatureContainer {
            val safeAccount = if (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) account else 0
            return instances.computeIfAbsent(safeAccount) { AccountFeatureContainer(it) }
        }

        @JvmStatic
        fun reset(account: Int) {
            instances.remove(account)
        }

        @JvmStatic
        fun resetAll() {
            instances.clear()
        }
    }
}
