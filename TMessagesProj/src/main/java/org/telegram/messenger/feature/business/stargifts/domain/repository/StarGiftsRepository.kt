package org.telegram.messenger.feature.business.stargifts.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.business.stargifts.domain.model.SavedStarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftFilter
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftsCatalogModel

interface StarGiftsRepository {
    fun observeCatalog(): Flow<StarGiftsCatalogModel>
    suspend fun getCatalog(forceRefresh: Boolean = false): Result<List<StarGiftModel>>
    suspend fun getGift(giftId: Long): Result<StarGiftModel?>
    fun observeProfileGifts(dialogId: Long): Flow<ProfileGiftsModel>
    suspend fun loadProfileGifts(
        dialogId: Long,
        offset: String? = null,
        limit: Int = 30,
        filter: StarGiftFilter = StarGiftFilter()
    ): Result<ProfileGiftsModel>
    suspend fun togglePinGift(dialogId: Long, giftId: Long, pin: Boolean): Result<Boolean>
    suspend fun toggleHideGift(dialogId: Long, giftId: Long, hide: Boolean): Result<Boolean>
}
