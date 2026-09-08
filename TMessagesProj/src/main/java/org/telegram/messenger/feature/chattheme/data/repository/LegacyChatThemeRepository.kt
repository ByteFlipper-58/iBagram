package org.telegram.messenger.feature.chattheme.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.ChatThemeController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.events.NotificationEvent
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chattheme.data.mapper.ChatThemeMapper
import org.telegram.messenger.feature.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.chattheme.domain.model.DialogThemeStateModel
import org.telegram.messenger.feature.chattheme.domain.repository.ChatThemeRepository
import org.telegram.tgnet.ResultCallback
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.EmojiThemes
import org.telegram.ui.ActionBar.theme.ThemeKey
import kotlin.coroutines.resume

class LegacyChatThemeRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ChatThemeRepository {

    private val chatThemeController: ChatThemeController
        get() = ChatThemeController.getInstance(currentAccount)

    override fun observeDialogTheme(dialogId: Long): Flow<DialogThemeStateModel> {
        return merge(
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.userInfoDidLoad),
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.chatInfoDidLoad),
            NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.wallpapersDidLoad),
            NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didSetNewWallpapper)
        ).onStart {
            emit(NotificationEvent(0, currentAccount, emptyArray()))
        }.map {
            withContext(mainDispatcher) {
                val theme = chatThemeController.getDialogTheme(dialogId)
                val wallpaper = chatThemeController.getDialogWallpaper(dialogId)
                ChatThemeMapper.toDialogThemeState(dialogId, theme, wallpaper, currentAccount)
            }
        }.flowOn(mainDispatcher)
    }

    override suspend fun getDialogThemeState(dialogId: Long): Result<DialogThemeStateModel> =
        withContext(mainDispatcher) {
            try {
                val theme = chatThemeController.getDialogTheme(dialogId)
                val wallpaper = chatThemeController.getDialogWallpaper(dialogId)
                val state = ChatThemeMapper.toDialogThemeState(dialogId, theme, wallpaper, currentAccount)
                Result.Success(state)
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to get dialog theme state", e)
            }
        }

    override suspend fun getAvailableThemes(withDefault: Boolean): Result<List<ChatThemeModel>> =
        withContext(mainDispatcher) {
            suspendCancellableCoroutine { continuation ->
                chatThemeController.requestAllChatThemes(object : ResultCallback<List<EmojiThemes>> {
                    override fun onComplete(result: List<EmojiThemes>?) {
                        if (continuation.isActive) {
                            val domainList = ChatThemeMapper.toDomainList(result, currentAccount)
                            continuation.resume(Result.Success(domainList))
                        }
                    }

                    override fun onError(error: TLRPC.TL_error?) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(error?.text ?: "Failed to load chat themes"))
                        }
                    }
                }, withDefault)
            }
        }

    override suspend fun setDialogTheme(
        dialogId: Long,
        emoticon: String?,
        giftSlug: String?
    ): Result<Unit> = withContext(mainDispatcher) {
        try {
            val themeKey = when {
                !giftSlug.isNullOrEmpty() -> ThemeKey.ofGiftSlug(giftSlug)
                !emoticon.isNullOrEmpty() -> ThemeKey.ofEmoticon(emoticon)
                else -> null
            }
            chatThemeController.setDialogTheme(dialogId, themeKey)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.failure(e.message ?: "Failed to set dialog theme", e)
        }
    }

    override suspend fun resetDialogTheme(dialogId: Long): Result<Unit> =
        withContext(mainDispatcher) {
            try {
                chatThemeController.setDialogTheme(dialogId, null as ThemeKey?)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to reset dialog theme", e)
            }
        }

    override suspend fun saveChatWallpaper(dialogId: Long, wallpaperId: Long?): Result<Unit> =
        withContext(mainDispatcher) {
            try {
                if (wallpaperId == null || wallpaperId == 0L) {
                    chatThemeController.saveChatWallpaper(dialogId, null)
                } else {
                    val wp = TLRPC.TL_wallPaper().apply {
                        id = wallpaperId
                    }
                    chatThemeController.saveChatWallpaper(dialogId, wp)
                }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to save chat wallpaper", e)
            }
        }
}
