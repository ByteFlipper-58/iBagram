package org.telegram.messenger.feature.social.boosts.domain.repository

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.social.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.social.boosts.domain.model.MyBoostsModel

interface BoostsRepository {
    suspend fun getBoostsStatus(dialogId: Long): Result<BoostStatusModel>
    suspend fun getMyBoosts(): Result<MyBoostsModel>
    suspend fun checkCanApplyBoost(dialogId: Long): Result<CanApplyBoostModel>
    suspend fun applyBoost(dialogId: Long, slots: List<Int>): Result<MyBoostsModel>
}
