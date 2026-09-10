package org.telegram.messenger.feature.system.recyclerscroll.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.RecyclerScrollState
import org.telegram.messenger.feature.system.recyclerscroll.domain.repository.RecyclerScrollRepository

class ObserveRecyclerScrollStateUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(): StateFlow<RecyclerScrollState> = repository.observeState()
}
