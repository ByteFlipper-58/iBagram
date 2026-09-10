package org.telegram.messenger.feature.messaging.bottomviews.presentation

import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomContainerType
import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomViewsVisibilityState

data class BottomViewsUiState(
    val state: BottomViewsVisibilityState = BottomViewsVisibilityState.DEFAULT,
    val priorityContainerType: BottomContainerType? = BottomContainerType.DEFAULT
)
