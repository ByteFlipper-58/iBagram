package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.recyclerscroll.domain.model.RecyclerScrollState
import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class GetRecyclerScrollStateUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(): RecyclerScrollState = repository.getState()
}
