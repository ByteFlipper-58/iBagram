package org.telegram.messenger.feature.business.botstars.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.botstars.domain.model.StarRefProgramModel
import org.telegram.messenger.feature.business.botstars.domain.repository.BotStarsRepository

class LoadSuggestedStarBotsUseCase(
    private val repository: BotStarsRepository
) {
    suspend operator fun invoke(dialogId: Long, sort: Int = 0): Result<List<StarRefProgramModel>> {
        return repository.loadSuggestedBots(dialogId, sort)
    }
}
