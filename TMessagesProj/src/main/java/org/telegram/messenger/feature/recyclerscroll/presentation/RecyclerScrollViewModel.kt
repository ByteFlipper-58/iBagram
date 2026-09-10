package org.telegram.messenger.feature.recyclerscroll.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollAnimationPlan
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollEligibility
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollViewTranslation
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.CalculateScrollAnimationPlanUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.CalculateScrollLengthUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.CancelRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.ComputeScrollViewTranslationsUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.EvaluateScrollEligibilityUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.FinishRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.GetRecyclerScrollStateUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.ObserveRecyclerScrollStateUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.ResetRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.StartRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.UpdateRecyclerScrollProgressUseCase

class RecyclerScrollViewModel(
    private val observeScrollStateUseCase: ObserveRecyclerScrollStateUseCase,
    private val getScrollStateUseCase: GetRecyclerScrollStateUseCase,
    private val evaluateScrollEligibilityUseCase: EvaluateScrollEligibilityUseCase,
    private val calculateScrollAnimationPlanUseCase: CalculateScrollAnimationPlanUseCase,
    private val calculateScrollLengthUseCase: CalculateScrollLengthUseCase,
    private val computeScrollViewTranslationsUseCase: ComputeScrollViewTranslationsUseCase,
    private val startRecyclerScrollUseCase: StartRecyclerScrollUseCase,
    private val updateRecyclerScrollProgressUseCase: UpdateRecyclerScrollProgressUseCase,
    private val finishRecyclerScrollUseCase: FinishRecyclerScrollUseCase,
    private val cancelRecyclerScrollUseCase: CancelRecyclerScrollUseCase,
    private val resetRecyclerScrollUseCase: ResetRecyclerScrollUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecyclerScrollUiState(
            scrollState = getScrollStateUseCase()
        )
    )
    val uiState: StateFlow<RecyclerScrollUiState> = _uiState.asStateFlow()

    init {
        observeScrollStateUseCase()
            .onEach { state ->
                _uiState.update { it.copy(scrollState = state) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: RecyclerScrollEvent) {
        when (event) {
            is RecyclerScrollEvent.PrepareAndStartScroll -> {
                val plan = calculateScrollAnimationPlanUseCase(event.spec)
                _uiState.update { it.copy(currentPlan = plan) }
                startRecyclerScrollUseCase(
                    position = event.position,
                    offset = event.offset,
                    direction = event.spec.scrollDirection,
                    durationMs = plan.durationMs
                )
            }
            is RecyclerScrollEvent.StartScroll -> {
                startRecyclerScrollUseCase(
                    position = event.position,
                    offset = event.offset,
                    direction = event.direction,
                    durationMs = event.durationMs
                )
            }
            is RecyclerScrollEvent.UpdateProgress -> {
                updateRecyclerScrollProgressUseCase(event.progress)
                val plan = _uiState.value.currentPlan
                if (plan != null) {
                    val translations = computeScrollViewTranslationsUseCase(
                        scrollLength = plan.scrollLength,
                        isScrollDown = plan.isScrollDown,
                        progress = event.progress
                    )
                    _uiState.update { it.copy(currentTranslation = translations) }
                }
            }
            RecyclerScrollEvent.FinishScroll -> {
                finishRecyclerScrollUseCase()
                _uiState.update { it.copy(currentTranslation = null) }
            }
            RecyclerScrollEvent.CancelScroll -> {
                cancelRecyclerScrollUseCase()
                _uiState.update { it.copy(currentTranslation = null, currentPlan = null) }
            }
            RecyclerScrollEvent.Reset -> {
                resetRecyclerScrollUseCase()
                _uiState.update {
                    it.copy(
                        currentPlan = null,
                        currentTranslation = null
                    )
                }
            }
        }
    }

    fun evaluateEligibility(
        fastScrollRunning: Boolean,
        itemAnimatorRunning: Boolean,
        childCount: Int,
        viewAnimationsEnabled: Boolean,
        smooth: Boolean,
        direction: ScrollDirection
    ): ScrollEligibility {
        return evaluateScrollEligibilityUseCase(
            fastScrollRunning = fastScrollRunning,
            itemAnimatorRunning = itemAnimatorRunning,
            childCount = childCount,
            viewAnimationsEnabled = viewAnimationsEnabled,
            smooth = smooth,
            direction = direction
        )
    }

    fun calculatePlan(spec: ScrollAnimationSpec): ScrollAnimationPlan {
        val plan = calculateScrollAnimationPlanUseCase(spec)
        _uiState.update { it.copy(currentPlan = plan) }
        return plan
    }

    fun calculateScrollLength(
        scrollDown: Boolean,
        containerHeight: Int,
        oldViewsCount: Int,
        scrollDiff: Int,
        oldTop: Int,
        oldBottom: Int,
        incomingTop: Int,
        incomingBottom: Int
    ): Int {
        return calculateScrollLengthUseCase(
            scrollDown = scrollDown,
            containerHeight = containerHeight,
            oldViewsCount = oldViewsCount,
            scrollDiff = scrollDiff,
            oldTop = oldTop,
            oldBottom = oldBottom,
            incomingTop = incomingTop,
            incomingBottom = incomingBottom
        )
    }

    fun computeTranslations(
        scrollLength: Int,
        isScrollDown: Boolean,
        progress: Float,
        additionalY: Int = 0
    ): ScrollViewTranslation {
        val translations = computeScrollViewTranslationsUseCase(
            scrollLength = scrollLength,
            isScrollDown = isScrollDown,
            progress = progress,
            additionalY = additionalY
        )
        _uiState.update { it.copy(currentTranslation = translations) }
        return translations
    }
}
