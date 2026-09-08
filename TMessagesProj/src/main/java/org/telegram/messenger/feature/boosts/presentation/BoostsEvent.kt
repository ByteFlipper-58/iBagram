package org.telegram.messenger.feature.boosts.presentation

sealed interface BoostsEvent {
    data class LoadStatus(val dialogId: Long) : BoostsEvent
    object LoadMyBoosts : BoostsEvent
    data class CheckCanApply(val dialogId: Long) : BoostsEvent
    data class ApplyBoost(val dialogId: Long, val slots: List<Int>) : BoostsEvent
    object ClearMessages : BoostsEvent
}
