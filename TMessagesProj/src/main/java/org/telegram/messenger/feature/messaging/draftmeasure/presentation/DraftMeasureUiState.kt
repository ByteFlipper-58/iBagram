package org.telegram.messenger.feature.messaging.draftmeasure.presentation

import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureResult
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureTarget

data class DraftMeasureUiState(
    val target: DraftMeasureTarget = DraftMeasureTarget.EMPTY,
    val previousMessageHeight: Int = 0,
    val hasAdditionalHeight: Boolean = false,
    val lastResult: DraftMeasureResult? = null
)
