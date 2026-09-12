package org.telegram.messenger.feature.messaging.translate.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.data.datasource.TranslationLocalDataSource
import org.telegram.messenger.feature.messaging.translate.data.datasource.TranslationRemoteDataSource
import org.telegram.messenger.feature.messaging.translate.data.mapper.TranslationMapper
import org.telegram.messenger.feature.messaging.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.messaging.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslationResultModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

/**
 * Modern repository implementation coordinating translation remote MTProto RPCs and local
 * state/preferences.
 */
class TranslationRepositoryImpl(
    private val account: Int,
    private val localDataSource: TranslationLocalDataSource,
    private val remoteDataSource: TranslationRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TranslationRepository {

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
            val isChatEnabled = localDataSource.isChatTranslateEnabled()
            val isContextEnabled = localDataSource.isContextTranslateEnabled()
            val restricted = localDataSource.getRestrictedLanguages()
            val currentLang = localDataSource.getCurrentLanguage()

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
            localDataSource.setChatTranslateEnabled(enabled)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set chat translate enabled", e))
        }
    }

    override suspend fun setContextTranslateEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setContextTranslateEnabled(enabled)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set context translate enabled", e))
        }
    }

    override suspend fun addDoNotTranslateLanguage(languageCode: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            val current = HashSet(localDataSource.getRestrictedLanguages())
            current.add(languageCode)
            localDataSource.updateRestrictedLanguages(current)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to add do-not-translate language", e))
        }
    }

    override suspend fun removeDoNotTranslateLanguage(languageCode: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            val current = HashSet(localDataSource.getRestrictedLanguages())
            current.remove(languageCode)
            localDataSource.updateRestrictedLanguages(current)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to remove do-not-translate language", e))
        }
    }

    override suspend fun setDoNotTranslateLanguages(languages: Set<String>): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.updateRestrictedLanguages(languages)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set do-not-translate languages", e))
        }
    }

    override fun observeDialogTranslationState(dialogId: Long): Flow<DialogTranslationStateModel> {
        return NotificationCenterFlowBridge.observeEvent(account, NotificationCenter.dialogTranslate)
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
            val isTranslatable = localDataSource.isDialogTranslatable(dialogId)
            val isTranslating = localDataSource.isTranslatingDialog(dialogId)
            val targetLang = localDataSource.getDialogTranslateTo(dialogId) ?: "en"

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
            localDataSource.toggleTranslatingDialog(dialogId, enabled)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to toggle dialog translating", e))
        }
    }

    override suspend fun setDialogTranslateTargetLanguage(dialogId: Long, languageCode: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setDialogTranslateTo(dialogId, languageCode)
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

        when (val remoteResult = remoteDataSource.translateText(text, toLanguage)) {
            is Result.Success -> {
                val mapped = TranslationMapper.mapTranslateResult(remoteResult.data, fromLanguage, toLanguage)
                Result.Success(mapped)
            }
            is Result.Failure -> remoteResult
        }
    }

    override suspend fun getAvailableLanguages(): Result<List<LanguageModel>> = withContext(mainDispatcher) {
        try {
            val currentShortName = localDataSource.getCurrentLocaleShortName()
            val infos = localDataSource.getAvailableLocaleInfos()
            val all = LinkedHashMap<String, LanguageModel>()
            for (info in infos) {
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
            val applied = localDataSource.applyAppLanguage(languageCode)
            if (applied) {
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.NotFound("Language code '$languageCode' not found"))
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to apply app language", e))
        }
    }
}
