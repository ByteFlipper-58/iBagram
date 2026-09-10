package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.recyclerscroll.domain.model.RecyclerScrollState
import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class ObserveRecyclerScrollStateUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(): StateFlow<RecyclerScrollState> = repository.observeState()
}
