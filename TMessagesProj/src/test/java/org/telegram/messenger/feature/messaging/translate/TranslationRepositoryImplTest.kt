package org.telegram.messenger.feature.messaging.translate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.LocaleController
import org.telegram.messenger.TranslateController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.data.datasource.TranslationLocalDataSource
import org.telegram.messenger.feature.messaging.translate.data.datasource.TranslationRemoteDataSource
import org.telegram.messenger.feature.messaging.translate.data.repository.TranslationRepositoryImpl
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeTranslationLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeTranslationRemoteDataSource
    private lateinit var repository: TranslationRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeTranslationLocalDataSource()
        fakeRemoteDataSource = FakeTranslationRemoteDataSource()
        repository = TranslationRepositoryImpl(
            account = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetTranslateSettings() = runTest {
        fakeLocalDataSource.isChatEnabled = true
        fakeLocalDataSource.isContextEnabled = false
        fakeLocalDataSource.restrictedLangs = setOf("en", "fr")
        fakeLocalDataSource.currentLang = "en"

        val result = repository.getTranslateSettings()
        assertTrue(result is Result.Success)
        val settings = (result as Result.Success).data
        assertTrue(settings.isChatTranslateEnabled)
        assertFalse(settings.isContextTranslateEnabled)
        assertEquals(setOf("en", "fr"), settings.doNotTranslateLanguages)
        assertEquals("en", settings.defaultLanguage)
    }

    @Test
    fun testSetChatTranslateEnabled() = runTest {
        val result = repository.setChatTranslateEnabled(true)
        assertTrue(result is Result.Success)
        assertTrue(fakeLocalDataSource.isChatEnabled)

        val resultDisable = repository.setChatTranslateEnabled(false)
        assertTrue(resultDisable is Result.Success)
        assertFalse(fakeLocalDataSource.isChatEnabled)
    }

    @Test
    fun testSetContextTranslateEnabled() = runTest {
        val result = repository.setContextTranslateEnabled(true)
        assertTrue(result is Result.Success)
        assertTrue(fakeLocalDataSource.isContextEnabled)

        val resultDisable = repository.setContextTranslateEnabled(false)
        assertTrue(resultDisable is Result.Success)
        assertFalse(fakeLocalDataSource.isContextEnabled)
    }

    @Test
    fun testAddDoNotTranslateLanguage() = runTest {
        fakeLocalDataSource.restrictedLangs = setOf("en")
        val result = repository.addDoNotTranslateLanguage("es")
        assertTrue(result is Result.Success)
        assertTrue(fakeLocalDataSource.restrictedLangs.contains("es"))
        assertTrue(fakeLocalDataSource.restrictedLangs.contains("en"))
    }

    @Test
    fun testRemoveDoNotTranslateLanguage() = runTest {
        fakeLocalDataSource.restrictedLangs = setOf("en", "es")
        val result = repository.removeDoNotTranslateLanguage("es")
        assertTrue(result is Result.Success)
        assertFalse(fakeLocalDataSource.restrictedLangs.contains("es"))
        assertTrue(fakeLocalDataSource.restrictedLangs.contains("en"))
    }

    @Test
    fun testSetDoNotTranslateLanguages() = runTest {
        val newSet = setOf("de", "it")
        val result = repository.setDoNotTranslateLanguages(newSet)
        assertTrue(result is Result.Success)
        assertEquals(newSet, fakeLocalDataSource.restrictedLangs)
    }

    @Test
    fun testGetDialogTranslationState() = runTest {
        fakeLocalDataSource.dialogTranslatable[100L] = true
        fakeLocalDataSource.dialogTranslating[100L] = false
        fakeLocalDataSource.dialogTranslateTo[100L] = "de"

        val result = repository.getDialogTranslationState(100L)
        assertTrue(result is Result.Success)
        val state = (result as Result.Success).data
        assertEquals(100L, state.dialogId)
        assertTrue(state.isTranslatable)
        assertFalse(state.isTranslating)
        assertEquals("de", state.targetLanguage)
    }

    @Test
    fun testToggleDialogTranslating() = runTest {
        val result = repository.toggleDialogTranslating(200L, true)
        assertTrue(result is Result.Success)
        assertTrue(fakeLocalDataSource.dialogTranslating[200L] == true)
    }

    @Test
    fun testSetDialogTranslateTargetLanguage() = runTest {
        val result = repository.setDialogTranslateTargetLanguage(300L, "ru")
        assertTrue(result is Result.Success)
        assertEquals("ru", fakeLocalDataSource.dialogTranslateTo[300L])
    }

    @Test
    fun testTranslateTextEmptyFails() = runTest {
        val result = repository.translateText("   ", "en", "es")
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidInput)
    }

    @Test
    fun testTranslateTextSuccess() = runTest {
        val fakeResult = TLRPC.TL_messages_translateResult().apply {
            val textWithEntities = TLRPC.TL_textWithEntities().apply {
                this.text = "Hello world translated"
            }
            this.result.add(textWithEntities)
        }
        fakeRemoteDataSource.resultToReturn = fakeResult

        val result = repository.translateText("Hello world", "en", "es")
        assertTrue(result is Result.Success)
        val model = (result as Result.Success).data
        assertEquals("Hello world translated", model.text)
        assertEquals("en", model.fromLanguage)
        assertEquals("es", model.toLanguage)
    }

    @Test
    fun testGetAvailableLanguages() = runTest {
        val info1 = LocaleController.LocaleInfo().apply {
            shortName = "en"
            name = "English"
            nameEnglish = "English"
            pluralLangCode = "en"
        }
        val info2 = LocaleController.LocaleInfo().apply {
            shortName = "ru"
            name = "Русский"
            nameEnglish = "Russian"
            pluralLangCode = "ru"
        }
        fakeLocalDataSource.availableLocales = listOf(info1, info2)
        fakeLocalDataSource.fakeCurrentLocaleShortName = "en"

        val result = repository.getAvailableLanguages()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(2, list.size)
        assertEquals("en", list[0].code)
        assertTrue(list[0].isCurrent)
        assertEquals("ru", list[1].code)
        assertFalse(list[1].isCurrent)
    }

    @Test
    fun testApplyAppLanguageSuccess() = runTest {
        fakeLocalDataSource.applyAppLanguageSuccess = true
        val result = repository.applyAppLanguage("en")
        assertTrue(result is Result.Success)
    }

    @Test
    fun testApplyAppLanguageNotFound() = runTest {
        fakeLocalDataSource.applyAppLanguageSuccess = false
        val result = repository.applyAppLanguage("unknown_code")
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NotFound)
    }

    @Test
    fun testTranslateControllerStranglerHook() {
        val repo = TranslateController.getTranslationRepository(0)
        assertNotNull(repo)
    }

    private class FakeTranslationLocalDataSource : TranslationLocalDataSource(0) {
        var isChatEnabled: Boolean = false
        var isContextEnabled: Boolean = false
        var restrictedLangs: Set<String> = emptySet()
        var currentLang: String = "en"
        val dialogTranslatable = mutableMapOf<Long, Boolean>()
        val dialogTranslating = mutableMapOf<Long, Boolean>()
        val dialogTranslateTo = mutableMapOf<Long, String>()
        var availableLocales: List<LocaleController.LocaleInfo> = emptyList()
        var fakeCurrentLocaleShortName: String? = "en"
        var applyAppLanguageSuccess: Boolean = true

        override fun isChatTranslateEnabled(): Boolean = isChatEnabled
        override fun setChatTranslateEnabled(enabled: Boolean) { isChatEnabled = enabled }

        override fun isContextTranslateEnabled(): Boolean = isContextEnabled
        override fun setContextTranslateEnabled(enabled: Boolean) { isContextEnabled = enabled }

        override fun getRestrictedLanguages(): Set<String> = restrictedLangs
        override fun updateRestrictedLanguages(languages: Set<String>) { restrictedLangs = languages }

        override fun getCurrentLanguage(): String = currentLang

        override fun isDialogTranslatable(dialogId: Long): Boolean = dialogTranslatable[dialogId] ?: false
        override fun isTranslatingDialog(dialogId: Long): Boolean = dialogTranslating[dialogId] ?: false
        override fun getDialogTranslateTo(dialogId: Long): String? = dialogTranslateTo[dialogId]
        override fun setDialogTranslateTo(dialogId: Long, lang: String) { dialogTranslateTo[dialogId] = lang }
        override fun toggleTranslatingDialog(dialogId: Long, enabled: Boolean) { dialogTranslating[dialogId] = enabled }

        override fun getAvailableLocaleInfos(): List<LocaleController.LocaleInfo> = availableLocales
        override fun getCurrentLocaleShortName(): String? = fakeCurrentLocaleShortName

        override suspend fun applyAppLanguage(languageCode: String): Boolean = applyAppLanguageSuccess
    }

    private class FakeTranslationRemoteDataSource : TranslationRemoteDataSource(0) {
        var resultToReturn: TLRPC.TL_messages_translateResult? = null
        var errorToReturn: AppError? = null

        override suspend fun translateText(text: String, toLanguage: String): Result<TLRPC.TL_messages_translateResult> {
            val err = errorToReturn
            if (err != null) return Result.Failure(err)
            val res = resultToReturn
            if (res != null) return Result.Success(res)
            return Result.Failure(AppError.Generic("No result set"))
        }
    }
}
