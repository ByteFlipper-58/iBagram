package org.telegram.messenger.feature.system.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollViewTranslation
import org.telegram.messenger.feature.system.recyclerscroll.domain.repository.RecyclerScrollRepository

class ComputeScrollViewTranslationsUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(
        scrollLength: Int,
        isScrollDown: Boolean,
        progress: Float,
        additionalY: Int = 0
    ): ScrollViewTranslation = repository.computeViewTranslations(
        scrollLength = scrollLength,
        isScrollDown = isScrollDown,
        progress = progress,
        additionalY = additionalY
    )
}
