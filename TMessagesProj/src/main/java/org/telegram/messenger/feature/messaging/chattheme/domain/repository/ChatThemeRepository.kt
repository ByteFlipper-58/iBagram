package org.telegram.messenger.feature.messaging.chattheme.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.messaging.chattheme.domain.model.DialogThemeStateModel

interface ChatThemeRepository {
    fun observeDialogTheme(dialogId: Long): Flow<DialogThemeStateModel>
    suspend fun getDialogThemeState(dialogId: Long): Result<DialogThemeStateModel>
    suspend fun getAvailableThemes(withDefault: Boolean = true): Result<List<ChatThemeModel>>
    suspend fun setDialogTheme(dialogId: Long, emoticon: String?, giftSlug: String? = null): Result<Unit>
    suspend fun resetDialogTheme(dialogId: Long): Result<Unit>
    suspend fun saveChatWallpaper(dialogId: Long, wallpaperId: Long?): Result<Unit>
}
