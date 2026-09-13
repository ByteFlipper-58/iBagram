package org.telegram.messenger.feature.media.storycustomparams

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.storycustomparams.data.datasource.StoryCustomParamsLocalDataSource
import org.telegram.messenger.feature.media.storycustomparams.data.datasource.StoryCustomParamsRemoteDataSource
import org.telegram.messenger.feature.media.storycustomparams.data.repository.StoryCustomParamsRepositoryImpl
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryTranslationParamsModel
import org.telegram.ui.Stories.StoryCustomParamsHelper

@OptIn(ExperimentalCoroutinesApi::class)
class StoryCustomParamsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: StoryCustomParamsLocalDataSource
    private lateinit var remoteDataSource: StoryCustomParamsRemoteDataSource
    private lateinit var repository: StoryCustomParamsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = StoryCustomParamsLocalDataSource(0)
        remoteDataSource = StoryCustomParamsRemoteDataSource(0)
        repository = StoryCustomParamsRepositoryImpl(
            account = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateEmpty() {
        val state = repository.getState()
        assertTrue(state.paramsByStoryKey.isEmpty())
    }

    @Test
    fun testSaveAndGetParams() = runTest {
        val translation = StoryTranslationParamsModel(
            isTranslated = true,
            detectedLanguage = "en",
            translatedText = "Hello",
            translatedLanguage = "ru"
        )
        val model = StoryCustomParamsModel(
            storyId = 10,
            dialogId = 100L,
            translation = translation
        )

        repository.saveParams(model)

        val retrieved = repository.getParams(100L, 10)
        assertNotNull(retrieved)
        assertEquals(10, retrieved?.storyId)
        assertEquals(100L, retrieved?.dialogId)
        assertTrue(retrieved?.translation?.isTranslated == true)
        assertEquals("en", retrieved?.translation?.detectedLanguage)
        assertEquals("Hello", retrieved?.translation?.translatedText)
        assertEquals("ru", retrieved?.translation?.translatedLanguage)
        assertEquals(15, retrieved?.flags) // 1 | 2 | 4 | 8 = 15

        assertEquals(1, repository.getState().paramsByStoryKey.size)
    }

    @Test
    fun testUpdateTranslation() = runTest {
        repository.updateTranslation(
            dialogId = 200L,
            storyId = 20,
            isTranslated = true,
            detectedLang = "es",
            translatedText = "Hola Mundo",
            targetLang = "en"
        )

        val retrieved = repository.getParams(200L, 20)
        assertNotNull(retrieved)
        assertEquals("Hola Mundo", retrieved?.translation?.translatedText)
        assertEquals("es", retrieved?.translation?.detectedLanguage)
        assertEquals("en", retrieved?.translation?.translatedLanguage)
    }

    @Test
    fun testCopyParams() = runTest {
        repository.updateTranslation(
            dialogId = 300L,
            storyId = 30,
            isTranslated = true,
            detectedLang = "fr",
            translatedText = "Bonjour",
            targetLang = "de"
        )

        repository.copyParams(
            fromDialogId = 300L,
            fromStoryId = 30,
            toDialogId = 400L,
            toStoryId = 40
        )

        val copied = repository.getParams(400L, 40)
        assertNotNull(copied)
        assertEquals(400L, copied?.dialogId)
        assertEquals(40, copied?.storyId)
        assertEquals("Bonjour", copied?.translation?.translatedText)
    }

    @Test
    fun testRemoveParams() = runTest {
        val model = StoryCustomParamsModel(storyId = 50, dialogId = 500L)
        repository.saveParams(model)
        assertNotNull(repository.getParams(500L, 50))

        repository.removeParams(500L, 50)
        assertNull(repository.getParams(500L, 50))
    }

    @Test
    fun testClearAll() = runTest {
        repository.saveParams(StoryCustomParamsModel(storyId = 1, dialogId = 10L))
        repository.saveParams(StoryCustomParamsModel(storyId = 2, dialogId = 20L))
        assertEquals(2, repository.getState().paramsByStoryKey.size)

        repository.clearAll()
        assertTrue(repository.getState().paramsByStoryKey.isEmpty())
    }

    @Test
    fun testStranglerHook() {
        val repo = StoryCustomParamsHelper.getStoryCustomParamsRepository(0)
        assertNotNull(repo)
    }
}
