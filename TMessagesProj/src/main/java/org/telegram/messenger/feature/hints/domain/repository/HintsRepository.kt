package org.telegram.messenger.feature.hints.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.hints.domain.model.HintModel
import org.telegram.messenger.feature.hints.domain.model.HintType
import org.telegram.messenger.feature.hints.domain.model.HintsStateModel

/**
 * Domain repository contract for in-app hints, tips, and feature discovery prompts.
 */
interface HintsRepository {
    fun observeHints(): Flow<HintsStateModel>
    fun getHintsState(): HintsStateModel
    fun getHint(type: HintType): HintModel
    fun shouldShowHint(type: HintType): Boolean
    fun incrementHint(type: HintType)
    fun doNotShowAgain(type: HintType)
    fun resetHint(type: HintType)
    fun resetAllHints()
}
