package org.telegram.messenger.feature.translate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.LocaleController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.translate.data.mapper.TranslationMapper
import org.telegram.messenger.feature.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.translate.domain.model.TranslationResultModel
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository
import org.telegram.messenger.feature.translate.domain.usecase.AddDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ApplyAppLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.GetAvailableLanguagesUseCase
import org.telegram.messenger.feature.translate.domain.usecase.GetDialogTranslationStateUseCase
import org.telegram.messenger.feature.translate.domain.usecase.GetTranslateSettingsUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ObserveDialogTranslationStateUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ObserveTranslateSettingsUseCase
import org.telegram.messenger.feature.translate.domain.usecase.RemoveDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetChatTranslateEnabledUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetContextTranslateEnabledUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetDialogTargetLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetDoNotTranslateLanguagesUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ToggleDialogTranslatingUseCase
import org.telegram.messenger.feature.translate.domain.usecase.TranslateTextUseCase
import org.telegram.messenger.feature.translate.presentation.TranslateEvent
import org.telegram.messenger.feature.translate.presentation.TranslateViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class TranslateDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDomainModels() {
        val lang = LanguageModel(
            code = "en",
            name = "English",
            nativeName = "English",
            isOfficial = true,
            isCurrent = true,
            totalStrings = 3000,
            translatedStrings = 3000
        )
        assertEquals("en", lang.code)
        assertEquals("English", lang.name)
        assertTrue(lang.isOfficial)
        assertTrue(lang.isCurrent)

        val settings = TranslateSettingsModel(
            isChatTranslateEnabled = true,
            isContextTranslateEnabled = false,
            doNotTranslateLanguages = setOf("en", "ru"),
            defaultLanguage = "en"
        )
        assertTrue(settings.isChatTranslateEnabled)
        assertFalse(settings.isContextTranslateEnabled)
        assertEquals(2, settings.doNotTranslateLanguages.size)
        assertEquals("en", settings.defaultLanguage)

        val dialogState = DialogTranslationStateModel(
            dialogId = 12345L,
            isTranslatable = true,
            isTranslating = true,
            targetLanguage = "es"
        )
        assertEquals(12345L, dialogState.dialogId)
        assertTrue(dialogState.isTranslatable)
        assertTrue(dialogState.isTranslating)
        assertEquals("es", dialogState.targetLanguage)

        val translationResult = TranslationResultModel(
            text = "Hola mundo",
            fromLanguage = "en",
            toLanguage = "es"
        )
        assertEquals("Hola mundo", translationResult.text)
        assertEquals("en", translationResult.fromLanguage)
        assertEquals("es", translationResult.toLanguage)
    }

    @Test
    fun testTranslationMapper() {
        val localeInfo = LocaleController.LocaleInfo().apply {
            name = "Deutsch"
            nameEnglish = "German"
            shortName = "de"
            pluralLangCode = "de"
            pathToFile = "remote"
        }

        val mappedLang = TranslationMapper.mapLocaleInfo(localeInfo, currentShortName = "de")
        assertEquals("de", mappedLang.code)
        assertEquals("Deutsch", mappedLang.name)
        assertEquals("German", mappedLang.nativeName)
        assertTrue(mappedLang.isOfficial)
        assertTrue(mappedLang.isCurrent)

        val translateResult = TLRPC.TL_messages_translateResult().apply {
            val t1 = TLRPC.TL_textWithEntities().apply { text = "Hello" }
            val t2 = TLRPC.TL_textWithEntities().apply { text = "World" }
            result.add(t1)
            result.add(t2)
        }

        val mappedTranslation = TranslationMapper.mapTranslateResult(translateResult, "ru", "en")
        assertEquals("Hello\n\nWorld", mappedTranslation.text)
        assertEquals("ru", mappedTranslation.fromLanguage)
        assertEquals("en", mappedTranslation.toLanguage)

        val mappedDialogState = TranslationMapper.mapDialogState(9876L, true, false, "fr")
        assertEquals(9876L, mappedDialogState.dialogId)
        assertTrue(mappedDialogState.isTranslatable)
        assertFalse(mappedDialogState.isTranslating)
        assertEquals("fr", mappedDialogState.targetLanguage)

        val mappedSettings = TranslationMapper.mapSettings(
            isChatEnabled = true,
            isContextEnabled = true,
            doNotTranslate = setOf("en"),
            defaultLang = "de"
        )
        assertTrue(mappedSettings.isChatTranslateEnabled)
        assertTrue(mappedSettings.isContextTranslateEnabled)
        assertEquals(setOf("en"), mappedSettings.doNotTranslateLanguages)
        assertEquals("de", mappedSettings.defaultLanguage)
    }

    @Test
    fun testUseCasesWithFakeRepository() = runBlocking {
        val fakeRepo = FakeTranslationRepository()

        val getSettingsUseCase = GetTranslateSettingsUseCase(fakeRepo)
        val setChatUseCase = SetChatTranslateEnabledUseCase(fakeRepo)
        val setContextUseCase = SetContextTranslateEnabledUseCase(fakeRepo)
        val addLangUseCase = AddDoNotTranslateLanguageUseCase(fakeRepo)
        val removeLangUseCase = RemoveDoNotTranslateLanguageUseCase(fakeRepo)
        val setLanguagesUseCase = SetDoNotTranslateLanguagesUseCase(fakeRepo)
        val getDialogStateUseCase = GetDialogTranslationStateUseCase(fakeRepo)
        val toggleDialogUseCase = ToggleDialogTranslatingUseCase(fakeRepo)
        val setDialogTargetUseCase = SetDialogTargetLanguageUseCase(fakeRepo)
        val translateUseCase = TranslateTextUseCase(fakeRepo)
        val getAvailableLanguagesUseCase = GetAvailableLanguagesUseCase(fakeRepo)
        val applyAppLanguageUseCase = ApplyAppLanguageUseCase(fakeRepo)

        // Settings Use Cases
        val initialSettingsResult = getSettingsUseCase()
        assertTrue(initialSettingsResult is Result.Success)
        val initialSettings = (initialSettingsResult as Result.Success).data
        assertTrue(initialSettings.isChatTranslateEnabled)
        assertTrue(initialSettings.isContextTranslateEnabled)
        assertEquals(setOf("en"), initialSettings.doNotTranslateLanguages)

        setChatUseCase(false)
        val updatedChatSettings = (getSettingsUseCase() as Result.Success).data
        assertFalse(updatedChatSettings.isChatTranslateEnabled)

        setContextUseCase(false)
        val updatedContextSettings = (getSettingsUseCase() as Result.Success).data
        assertFalse(updatedContextSettings.isContextTranslateEnabled)

        addLangUseCase("ru")
        val withRu = (getSettingsUseCase() as Result.Success).data
        assertTrue(withRu.doNotTranslateLanguages.contains("ru"))

        removeLangUseCase("en")
        val withoutEn = (getSettingsUseCase() as Result.Success).data
        assertFalse(withoutEn.doNotTranslateLanguages.contains("en"))

        setLanguagesUseCase(setOf("fr", "es"))
        val customLanguages = (getSettingsUseCase() as Result.Success).data
        assertEquals(setOf("fr", "es"), customLanguages.doNotTranslateLanguages)

        // Dialog Translation Use Cases
        val dialogStateResult = getDialogStateUseCase(1001L)
        assertTrue(dialogStateResult is Result.Success)
        val dialogState = (dialogStateResult as Result.Success).data
        assertEquals(1001L, dialogState.dialogId)
        assertFalse(dialogState.isTranslating)
        assertEquals("en", dialogState.targetLanguage)

        toggleDialogUseCase(1001L, true)
        val toggledDialog = (getDialogStateUseCase(1001L) as Result.Success).data
        assertTrue(toggledDialog.isTranslating)

        setDialogTargetUseCase(1001L, "de")
        val retargetedDialog = (getDialogStateUseCase(1001L) as Result.Success).data
        assertEquals("de", retargetedDialog.targetLanguage)

        // Translation Use Case
        val translationResult = translateUseCase("Привет", "ru", "en")
        assertTrue(translationResult is Result.Success)
        assertEquals("Hello", (translationResult as Result.Success).data.text)

        // Languages Use Cases
        val languagesResult = getAvailableLanguagesUseCase()
        assertTrue(languagesResult is Result.Success)
        assertEquals(2, (languagesResult as Result.Success).data.size)

        val applyResult = applyAppLanguageUseCase("ru")
        assertTrue(applyResult is Result.Success)
        val currentLangs = (getAvailableLanguagesUseCase() as Result.Success).data
        assertTrue(currentLangs.first { it.code == "ru" }.isCurrent)
    }

    @Test
    fun testTranslateViewModelFlowAndEvents() = runBlocking {
        val fakeRepo = FakeTranslationRepository()

        val viewModel = TranslateViewModel(
            observeTranslateSettingsUseCase = ObserveTranslateSettingsUseCase(fakeRepo),
            getTranslateSettingsUseCase = GetTranslateSettingsUseCase(fakeRepo),
            setChatTranslateEnabledUseCase = SetChatTranslateEnabledUseCase(fakeRepo),
            setContextTranslateEnabledUseCase = SetContextTranslateEnabledUseCase(fakeRepo),
            setDoNotTranslateLanguagesUseCase = SetDoNotTranslateLanguagesUseCase(fakeRepo),
            addDoNotTranslateLanguageUseCase = AddDoNotTranslateLanguageUseCase(fakeRepo),
            removeDoNotTranslateLanguageUseCase = RemoveDoNotTranslateLanguageUseCase(fakeRepo),
            observeDialogTranslationStateUseCase = ObserveDialogTranslationStateUseCase(fakeRepo),
            getDialogTranslationStateUseCase = GetDialogTranslationStateUseCase(fakeRepo),
            toggleDialogTranslatingUseCase = ToggleDialogTranslatingUseCase(fakeRepo),
            setDialogTargetLanguageUseCase = SetDialogTargetLanguageUseCase(fakeRepo),
            translateTextUseCase = TranslateTextUseCase(fakeRepo),
            getAvailableLanguagesUseCase = GetAvailableLanguagesUseCase(fakeRepo),
            applyAppLanguageUseCase = ApplyAppLanguageUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state.settings.isChatTranslateEnabled)
        assertTrue(state.settings.isContextTranslateEnabled)
        assertEquals(setOf("en"), state.settings.doNotTranslateLanguages)
        assertEquals(2, state.availableLanguages.size)

        // Observe Dialog State
        viewModel.onEvent(TranslateEvent.LoadDialogState(555L))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(555L, state.currentDialogState?.dialogId)
        assertFalse(state.currentDialogState?.isTranslating == true)

        // Set Chat Translate Enabled
        viewModel.onEvent(TranslateEvent.SetChatTranslateEnabled(false))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.settings.isChatTranslateEnabled)
        assertEquals("Chat translate updated", state.actionSuccessMessage)

        // Set Context Translate Enabled
        viewModel.onEvent(TranslateEvent.SetContextTranslateEnabled(false))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.settings.isContextTranslateEnabled)
        assertEquals("Context translate updated", state.actionSuccessMessage)

        // Add exception language
        viewModel.onEvent(TranslateEvent.AddDoNotTranslateLanguage("es"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.settings.doNotTranslateLanguages.contains("es"))
        assertEquals("Language added to exceptions", state.actionSuccessMessage)

        // Remove exception language
        viewModel.onEvent(TranslateEvent.RemoveDoNotTranslateLanguage("en"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.settings.doNotTranslateLanguages.contains("en"))
        assertEquals("Language removed from exceptions", state.actionSuccessMessage)

        // Set multiple exception languages
        viewModel.onEvent(TranslateEvent.SetDoNotTranslateLanguages(setOf("it", "pt")))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(setOf("it", "pt"), state.settings.doNotTranslateLanguages)
        assertEquals("Exception languages updated", state.actionSuccessMessage)

        // Toggle Dialog Translating
        viewModel.onEvent(TranslateEvent.ToggleDialogTranslating(555L, true))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.currentDialogState?.isTranslating == true)
        assertEquals("Translation enabled", state.actionSuccessMessage)

        // Set Dialog Target Language
        viewModel.onEvent(TranslateEvent.SetDialogTargetLanguage(555L, "fr"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("fr", state.currentDialogState?.targetLanguage)
        assertEquals("Target language updated", state.actionSuccessMessage)

        // Translate text
        viewModel.onEvent(TranslateEvent.TranslateText("Bonjour", "fr", "en"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertNotNull(state.lastTranslation)
        assertEquals("Hello", state.lastTranslation?.text)

        // Apply app language
        viewModel.onEvent(TranslateEvent.ApplyAppLanguage("ru"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("App language applied", state.actionSuccessMessage)
        assertTrue(state.availableLanguages.first { it.code == "ru" }.isCurrent)

        // Clear messages
        viewModel.onEvent(TranslateEvent.ClearMessages)
        state = viewModel.uiState.value
        assertNull(state.actionSuccessMessage)
        assertNull(state.errorMessage)
    }

    private class FakeTranslationRepository : TranslationRepository {

        private var settings = TranslateSettingsModel(
            isChatTranslateEnabled = true,
            isContextTranslateEnabled = true,
            doNotTranslateLanguages = setOf("en"),
            defaultLanguage = "en"
        )

        private val dialogStates = mutableMapOf<Long, DialogTranslationStateModel>()
        private var languages = listOf(
            LanguageModel(
                code = "en",
                name = "English",
                nativeName = "English",
                isOfficial = true,
                isCurrent = true,
                totalStrings = 3000,
                translatedStrings = 3000
            ),
            LanguageModel(
                code = "ru",
                name = "Russian",
                nativeName = "Русский",
                isOfficial = true,
                isCurrent = false,
                totalStrings = 3000,
                translatedStrings = 2950
            )
        )

        private val settingsFlow = MutableStateFlow(settings)
        private val dialogFlows = mutableMapOf<Long, MutableStateFlow<DialogTranslationStateModel>>()

        override fun observeTranslateSettings(): Flow<TranslateSettingsModel> = settingsFlow.asStateFlow()

        override suspend fun getTranslateSettings(): Result<TranslateSettingsModel> = Result.Success(settings)

        override suspend fun setChatTranslateEnabled(enabled: Boolean): Result<Unit> {
            settings = settings.copy(isChatTranslateEnabled = enabled)
            settingsFlow.value = settings
            return Result.Success(Unit)
        }

        override suspend fun setContextTranslateEnabled(enabled: Boolean): Result<Unit> {
            settings = settings.copy(isContextTranslateEnabled = enabled)
            settingsFlow.value = settings
            return Result.Success(Unit)
        }

        override suspend fun setDoNotTranslateLanguages(languages: Set<String>): Result<Unit> {
            settings = settings.copy(doNotTranslateLanguages = languages)
            settingsFlow.value = settings
            return Result.Success(Unit)
        }

        override suspend fun addDoNotTranslateLanguage(languageCode: String): Result<Unit> {
            val updated = settings.doNotTranslateLanguages + languageCode
            settings = settings.copy(doNotTranslateLanguages = updated)
            settingsFlow.value = settings
            return Result.Success(Unit)
        }

        override suspend fun removeDoNotTranslateLanguage(languageCode: String): Result<Unit> {
            val updated = settings.doNotTranslateLanguages - languageCode
            settings = settings.copy(doNotTranslateLanguages = updated)
            settingsFlow.value = settings
            return Result.Success(Unit)
        }

        private fun getOrCreateDialogFlow(dialogId: Long): MutableStateFlow<DialogTranslationStateModel> {
            return dialogFlows.getOrPut(dialogId) {
                val state = dialogStates.getOrPut(dialogId) {
                    DialogTranslationStateModel(
                        dialogId = dialogId,
                        isTranslatable = true,
                        isTranslating = false,
                        targetLanguage = "en"
                    )
                }
                MutableStateFlow(state)
            }
        }

        override fun observeDialogTranslationState(dialogId: Long): Flow<DialogTranslationStateModel> {
            return getOrCreateDialogFlow(dialogId).asStateFlow()
        }

        override suspend fun getDialogTranslationState(dialogId: Long): Result<DialogTranslationStateModel> {
            val state = dialogStates.getOrPut(dialogId) {
                DialogTranslationStateModel(
                    dialogId = dialogId,
                    isTranslatable = true,
                    isTranslating = false,
                    targetLanguage = "en"
                )
            }
            return Result.Success(state)
        }

        override suspend fun toggleDialogTranslating(dialogId: Long, enabled: Boolean): Result<Unit> {
            val current = (getDialogTranslationState(dialogId) as Result.Success).data
            val updated = current.copy(isTranslating = enabled)
            dialogStates[dialogId] = updated
            getOrCreateDialogFlow(dialogId).value = updated
            return Result.Success(Unit)
        }

        override suspend fun setDialogTranslateTargetLanguage(dialogId: Long, languageCode: String): Result<Unit> {
            val current = (getDialogTranslationState(dialogId) as Result.Success).data
            val updated = current.copy(targetLanguage = languageCode)
            dialogStates[dialogId] = updated
            getOrCreateDialogFlow(dialogId).value = updated
            return Result.Success(Unit)
        }

        override suspend fun translateText(
            text: String,
            fromLanguage: String?,
            toLanguage: String
        ): Result<TranslationResultModel> {
            return Result.Success(
                TranslationResultModel(
                    text = "Hello",
                    fromLanguage = fromLanguage,
                    toLanguage = toLanguage
                )
            )
        }

        override suspend fun getAvailableLanguages(): Result<List<LanguageModel>> {
            return Result.Success(languages)
        }

        override suspend fun applyAppLanguage(languageCode: String): Result<Unit> {
            languages = languages.map { it.copy(isCurrent = it.code == languageCode) }
            return Result.Success(Unit)
        }
    }
}
