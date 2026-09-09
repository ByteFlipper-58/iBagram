package org.telegram.messenger.feature.bottomviews.presentation

import org.telegram.messenger.feature.bottomviews.domain.model.BottomContainerType
import org.telegram.messenger.feature.bottomviews.domain.model.BottomViewsVisibilityState

data class BottomViewsUiState(
    val state: BottomViewsVisibilityState = BottomViewsVisibilityState.DEFAULT,
    val priorityContainerType: BottomContainerType? = BottomContainerType.DEFAULT
)
