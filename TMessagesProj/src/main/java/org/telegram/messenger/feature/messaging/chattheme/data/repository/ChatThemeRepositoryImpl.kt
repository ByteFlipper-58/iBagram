package org.telegram.messenger.feature.messaging.chattheme.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.events.NotificationEvent
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chattheme.data.datasource.ChatThemeLocalDataSource
import org.telegram.messenger.feature.messaging.chattheme.data.datasource.ChatThemeRemoteDataSource
import org.telegram.messenger.feature.messaging.chattheme.data.mapper.ChatThemeMapper
import org.telegram.messenger.feature.messaging.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.messaging.chattheme.domain.model.DialogThemeStateModel
import org.telegram.messenger.feature.messaging.chattheme.domain.repository.ChatThemeRepository
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.ActionBar.theme.ThemeKey

/**
 * Clean implementation of ChatThemeRepository coordinating local and remote data sources.
 */
class ChatThemeRepositoryImpl(
    private val account: Int,
    private val localDataSource: ChatThemeLocalDataSource,
    private val remoteDataSource: ChatThemeRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChatThemeRepository {

    override fun observeDialogTheme(dialogId: Long): Flow<DialogThemeStateModel> {
        return merge(
            NotificationCenterFlowBridge.observeEvent(account, NotificationCenter.userInfoDidLoad),
            NotificationCenterFlowBridge.observeEvent(account, NotificationCenter.chatInfoDidLoad),
            NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.wallpapersDidLoad),
            NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didSetNewWallpapper)
        ).onStart {
            emit(NotificationEvent(0, account, emptyArray()))
        }.map {
            withContext(mainDispatcher) {
                val theme = localDataSource.getDialogTheme(dialogId)
                val wallpaper = localDataSource.getDialogWallpaper(dialogId)
                ChatThemeMapper.toDialogThemeState(dialogId, theme, wallpaper, account)
            }
        }.flowOn(mainDispatcher)
    }

    override suspend fun getDialogThemeState(dialogId: Long): Result<DialogThemeStateModel> =
        withContext(mainDispatcher) {
            try {
                val theme = localDataSource.getDialogTheme(dialogId)
                val wallpaper = localDataSource.getDialogWallpaper(dialogId)
                val state = ChatThemeMapper.toDialogThemeState(dialogId, theme, wallpaper, account)
                Result.Success(state)
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to get dialog theme state", e)
            }
        }

    override suspend fun getAvailableThemes(withDefault: Boolean): Result<List<ChatThemeModel>> =
        withContext(mainDispatcher) {
            try {
                val cachedThemes = localDataSource.getEmojiThemes(withDefault)
                val hash = localDataSource.getThemesHash()
                val lastReload = localDataSource.getLastReloadTimeMs()
                val needReload = (System.currentTimeMillis() - lastReload > RELOAD_TIMEOUT_MS) || cachedThemes.isEmpty() || hash == 0L

                if (needReload) {
                    when (val remoteResult = remoteDataSource.getChatThemes(hash)) {
                        is Result.Success -> {
                            val response = remoteResult.data
                            if (response is TL_account.TL_themes) {
                                withContext(ioDispatcher) {
                                    localDataSource.saveThemesToPrefs(response.themes, response.hash, System.currentTimeMillis())
                                }
                            }
                        }
                        is Result.Failure -> {
                            if (cachedThemes.isEmpty()) {
                                return@withContext Result.failure(remoteResult.error)
                            }
                        }
                    }
                }

                val themes = localDataSource.getEmojiThemes(withDefault)
                val domainList = ChatThemeMapper.toDomainList(themes, account)
                Result.Success(domainList)
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to load chat themes", e)
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
            localDataSource.setDialogTheme(dialogId, themeKey)

            val peer = localDataSource.getInputPeer(dialogId)
            if (peer != null) {
                val inputTheme = ThemeKey.toInputTheme(themeKey)
                if (inputTheme != null) {
                    when (val remoteResult = remoteDataSource.setChatTheme(peer, inputTheme)) {
                        is Result.Success -> {
                            localDataSource.processUpdates(remoteResult.data)
                        }
                        is Result.Failure -> {
                            // Non-fatal if offline/failed RPC, local state updated
                        }
                    }
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.failure(e.message ?: "Failed to set dialog theme", e)
        }
    }

    override suspend fun resetDialogTheme(dialogId: Long): Result<Unit> =
        setDialogTheme(dialogId, null, null)

    override suspend fun saveChatWallpaper(dialogId: Long, wallpaperId: Long?): Result<Unit> =
        withContext(mainDispatcher) {
            try {
                if (wallpaperId == null || wallpaperId == 0L) {
                    localDataSource.saveChatWallpaper(dialogId, null)
                } else {
                    val wp = TLRPC.TL_wallPaper().apply {
                        id = wallpaperId
                    }
                    localDataSource.saveChatWallpaper(dialogId, wp)
                }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.failure(e.message ?: "Failed to save chat wallpaper", e)
            }
        }

    companion object {
        const val RELOAD_TIMEOUT_MS = 2L * 60 * 60 * 1000
    }
}
