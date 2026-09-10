package org.telegram.messenger.feature.messaging.translate.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.TranslateController
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.data.mapper.TranslationMapper
import org.telegram.messenger.feature.messaging.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.messaging.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslationResultModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.RestrictedLanguagesSelectActivity
import java.util.HashSet
import kotlin.coroutines.resume

/**
 * Adapter implementing [TranslationRepository] on top of Telegram's [TranslateController],
 * [LocaleController], [RestrictedLanguagesSelectActivity] and MTProto translation API.
 * All state queries and mutations run safely on [Dispatchers.Main].
 */
class LegacyTranslationRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TranslationRepository {

    private val translateController: TranslateController
        get() = MessagesController.getInstance(currentAccount).translateController

    private val localeController: LocaleController
        get() = LocaleController.getInstance()

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    override fun observeTranslateSettings(): Flow<TranslateSettingsModel> {
        return NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.suggestedLangpack)
            .map {
                getTranslateSettings().getOrDefault(
                    TranslateSettingsModel(
                        isChatTranslateEnabled = true,
                        isContextTranslateEnabled = false,
                        doNotTranslateLanguages = emptySet(),
                        defaultLanguage = "en"
                    )
                )
            }
            .onStart {
                emit(
                    getTranslateSettings().getOrDefault(
                        TranslateSettingsModel(
                            isChatTranslateEnabled = true,
                            isContextTranslateEnabled = false,
                            doNotTranslateLanguages = emptySet(),
                            defaultLanguage = "en"
                        )
                    )
                )
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getTranslateSettings(): Result<TranslateSettingsModel> = withContext(mainDispatcher) {
        try {
            val isChatEnabled = translateController.isChatTranslateEnabled
            val isContextEnabled = translateController.isContextTranslateEnabled
            val restricted = RestrictedLanguagesSelectActivity.getRestrictedLanguages() ?: emptySet<String>()
            val currentLang = TranslateController.currentLanguage() ?: "en"

            val settings = TranslationMapper.mapSettings(
                isChatEnabled = isChatEnabled,
                isContextEnabled = isContextEnabled,
                doNotTranslate = HashSet(restricted),
                defaultLang = currentLang
            )
            Result.Success(settings)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to get translate settings", e))
        }
    }

    override suspend fun setChatTranslateEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            translateController.isChatTranslateEnabled = enabled
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set chat translate enabled", e))
        }
    }

    override suspend fun setContextTranslateEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            translateController.isContextTranslateEnabled = enabled
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set context translate enabled", e))
        }
    }

    override suspend fun addDoNotTranslateLanguage(languageCode: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            val current = HashSet(RestrictedLanguagesSelectActivity.getRestrictedLanguages() ?: emptySet())
            current.add(languageCode)
            RestrictedLanguagesSelectActivity.updateRestrictedLanguages(current, true)
            translateController.checkRestrictedLanguagesUpdate()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to add do-not-translate language", e))
        }
    }

    override suspend fun removeDoNotTranslateLanguage(languageCode: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            val current = HashSet(RestrictedLanguagesSelectActivity.getRestrictedLanguages() ?: emptySet())
            current.remove(languageCode)
            RestrictedLanguagesSelectActivity.updateRestrictedLanguages(current, true)
            translateController.checkRestrictedLanguagesUpdate()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to remove do-not-translate language", e))
        }
    }

    override suspend fun setDoNotTranslateLanguages(languages: Set<String>): Result<Unit> = withContext(mainDispatcher) {
        try {
            RestrictedLanguagesSelectActivity.updateRestrictedLanguages(HashSet(languages), true)
            translateController.checkRestrictedLanguagesUpdate()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set do-not-translate languages", e))
        }
    }

    override fun observeDialogTranslationState(dialogId: Long): Flow<DialogTranslationStateModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.dialogTranslate)
            .filter { event ->
                if (event.args.isEmpty()) true
                else {
                    val eventDialogId = (event.args[0] as? Number)?.toLong() ?: 0L
                    eventDialogId == 0L || eventDialogId == dialogId
                }
            }
            .map {
                getDialogTranslationState(dialogId).getOrDefault(
                    DialogTranslationStateModel(
                        dialogId = dialogId,
                        isTranslatable = false,
                        isTranslating = false,
                        targetLanguage = "en"
                    )
                )
            }
            .onStart {
                emit(
                    getDialogTranslationState(dialogId).getOrDefault(
                        DialogTranslationStateModel(
                            dialogId = dialogId,
                            isTranslatable = false,
                            isTranslating = false,
                            targetLanguage = "en"
                        )
                    )
                )
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getDialogTranslationState(dialogId: Long): Result<DialogTranslationStateModel> = withContext(mainDispatcher) {
        try {
            val isTranslatable = translateController.isDialogTranslatable(dialogId)
            val isTranslating = translateController.isTranslatingDialog(dialogId)
            val targetLang = translateController.getDialogTranslateTo(dialogId) ?: "en"

            val state = TranslationMapper.mapDialogState(
                dialogId = dialogId,
                isTranslatable = isTranslatable,
                isTranslating = isTranslating,
                targetLanguage = targetLang
            )
            Result.Success(state)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to get dialog translation state", e))
        }
    }

    override suspend fun toggleDialogTranslating(dialogId: Long, enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            translateController.toggleTranslatingDialog(dialogId, enabled)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to toggle dialog translating", e))
        }
    }

    override suspend fun setDialogTranslateTargetLanguage(dialogId: Long, languageCode: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            translateController.setDialogTranslateTo(dialogId, languageCode)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set dialog target language", e))
        }
    }

    override suspend fun translateText(
        text: String,
        fromLanguage: String?,
        toLanguage: String
    ): Result<TranslationResultModel> = withContext(mainDispatcher) {
        if (text.isBlank()) {
            return@withContext Result.Failure(AppError.InvalidInput("Text to translate cannot be empty"))
        }

        val req = TLRPC.TL_messages_translateText().apply {
            this.flags = 2 // raw text mode
            val textEntity = TLRPC.TL_textWithEntities().apply {
                this.text = text
            }
            this.text.add(textEntity)
            this.to_lang = TranslateController.normalizeLanguage(toLanguage)
        }

        val result: Result<TLRPC.TL_messages_translateResult> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                val mapped = TranslationMapper.mapTranslateResult(result.data, fromLanguage, toLanguage)
                Result.Success(mapped)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun getAvailableLanguages(): Result<List<LanguageModel>> = withContext(mainDispatcher) {
        try {
            val currentShortName = localeController.currentLocaleInfo?.shortName
            val official = localeController.languages ?: emptyList<LocaleController.LocaleInfo>()
            val unofficial = localeController.unofficialLanguages ?: emptyList<LocaleController.LocaleInfo>()
            val remote = localeController.remoteLanguages ?: emptyList<LocaleController.LocaleInfo>()

            val all = LinkedHashMap<String, LanguageModel>()
            for (info in official) {
                val mapped = TranslationMapper.mapLocaleInfo(info, currentShortName)
                all[mapped.code] = mapped
            }
            for (info in unofficial) {
                val mapped = TranslationMapper.mapLocaleInfo(info, currentShortName)
                if (!all.containsKey(mapped.code)) {
                    all[mapped.code] = mapped
                }
            }
            for (info in remote) {
                val mapped = TranslationMapper.mapLocaleInfo(info, currentShortName)
                if (!all.containsKey(mapped.code)) {
                    all[mapped.code] = mapped
                }
            }

            Result.Success(all.values.toList())
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to get available languages", e))
        }
    }

    override suspend fun applyAppLanguage(languageCode: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            val allInfos = (localeController.languages ?: emptyList<LocaleController.LocaleInfo>()) +
                (localeController.unofficialLanguages ?: emptyList()) +
                (localeController.remoteLanguages ?: emptyList())

            val target = allInfos.find { it.pluralLangCode == languageCode || it.shortName == languageCode }
                ?: return@withContext Result.Failure(AppError.NotFound("Language code '$languageCode' not found"))

            localeController.applyLanguage(target, true, false, currentAccount)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to apply app language", e))
        }
    }

    private suspend inline fun <reified T : TLObject> sendTlRequest(req: TLObject): Result<T> {
        return suspendCancellableCoroutine { continuation ->
            val reqId = connectionsManager.sendRequest(req) { response, error ->
                if (error != null) {
                    continuation.resume(Result.Failure(AppError.Network(error.text ?: "Network error", error.code)))
                } else if (response is T) {
                    continuation.resume(Result.Success(response))
                } else if (response == null) {
                    continuation.resume(Result.Failure(AppError.Network("Empty response from Telegram server", 0)))
                } else {
                    continuation.resume(Result.Failure(AppError.Generic("Unexpected response type: ${response.javaClass.simpleName}")))
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }
    }
}
