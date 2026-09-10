package org.telegram.messenger.feature.social.boosts.domain.model

data class CanApplyBoostModel(
    val canApply: Boolean,
    val empty: Boolean,
    val replaceDialogId: Long,
    val alreadyActive: Boolean,
    val needSelector: Boolean,
    val floodWait: Int,
    val slot: Int,
    val boostCount: Int,
    val isMaxLevel: Boolean
)
