package org.telegram.messenger.feature.messaging.mentions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.telegram.messenger.feature.messaging.mentions.data.repository.LegacyMentionsRepository
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionTriggerType
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ClearMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.DismissMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.FilterMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.FormatMentionReplacementUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.GetMentionsStateUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ObserveMentionsStateUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ParseMentionQueryUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.SetMentionCandidatesUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.UpdateMentionQueryUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ValidateUsernameUseCase
import org.telegram.messenger.feature.messaging.mentions.presentation.MentionsEvent
import org.telegram.messenger.feature.messaging.mentions.presentation.MentionsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class MentionsDomainTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LegacyMentionsRepository
    private lateinit var validateUsernameUseCase: ValidateUsernameUseCase
    private lateinit var parseMentionQueryUseCase: ParseMentionQueryUseCase
    private lateinit var filterMentionsUseCase: FilterMentionsUseCase
    private lateinit var formatMentionReplacementUseCase: FormatMentionReplacementUseCase
    private lateinit var observeMentionsStateUseCase: ObserveMentionsStateUseCase
    private lateinit var getMentionsStateUseCase: GetMentionsStateUseCase
    private lateinit var updateMentionQueryUseCase: UpdateMentionQueryUseCase
    private lateinit var setMentionCandidatesUseCase: SetMentionCandidatesUseCase
    private lateinit var dismissMentionsUseCase: DismissMentionsUseCase
    private lateinit var clearMentionsUseCase: ClearMentionsUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyMentionsRepository()
        validateUsernameUseCase = ValidateUsernameUseCase()
        parseMentionQueryUseCase = ParseMentionQueryUseCase(validateUsernameUseCase)
        filterMentionsUseCase = FilterMentionsUseCase()
        formatMentionReplacementUseCase = FormatMentionReplacementUseCase()
        observeMentionsStateUseCase = ObserveMentionsStateUseCase(repository)
        getMentionsStateUseCase = GetMentionsStateUseCase(repository)
        updateMentionQueryUseCase = UpdateMentionQueryUseCase(repository, parseMentionQueryUseCase)
        setMentionCandidatesUseCase = SetMentionCandidatesUseCase(repository, filterMentionsUseCase)
        dismissMentionsUseCase = DismissMentionsUseCase(repository)
        clearMentionsUseCase = ClearMentionsUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testValidateUsername() {
        assertTrue(validateUsernameUseCase("durov"))
        assertTrue(validateUsernameUseCase("telegram_bot"))
        assertTrue(validateUsernameUseCase("SuperUser123"))
        assertTrue(validateUsernameUseCase("a_b_c_9"))

        assertFalse(validateUsernameUseCase(null))
        assertFalse(validateUsernameUseCase(""))
        assertFalse(validateUsernameUseCase("user@name"))
        assertFalse(validateUsernameUseCase("user-name"))
        assertFalse(validateUsernameUseCase("user name"))
        assertFalse(validateUsernameUseCase("user.name"))
    }

    @Test
    fun testParseMentionQueryTriggers() {
        // 1. Упоминание пользователя
        val qUser = parseMentionQueryUseCase("Hello @ali", cursorPosition = 10)
        assertEquals(MentionTriggerType.USERNAME, qUser.triggerType)
        assertEquals("ali", qUser.query)
        assertEquals(6, qUser.startPosition)
        assertEquals(4, qUser.length)

        // 2. Хэштег
        val qHash = parseMentionQueryUseCase("Check #news and updates", cursorPosition = 11)
        assertEquals(MentionTriggerType.HASHTAG, qHash.triggerType)
        assertEquals("news", qHash.query)
        assertEquals(6, qHash.startPosition)
        assertEquals(5, qHash.length)

        // 3. Команда бота (в начале строки)
        val qCmd = parseMentionQueryUseCase("/start", cursorPosition = 6)
        assertEquals(MentionTriggerType.BOT_COMMAND, qCmd.triggerType)
        assertEquals("start", qCmd.query)
        assertEquals(0, qCmd.startPosition)
        assertEquals(6, qCmd.length)

        // Команда не в начале строки не должна определяться как команда бота
        val qNotCmd = parseMentionQueryUseCase("some /start", cursorPosition = 11)
        assertEquals(MentionTriggerType.NONE, qNotCmd.triggerType)

        // 4. Эмодзи по ключевому слову
        val qEmoji = parseMentionQueryUseCase("Feeling :fire", cursorPosition = 13)
        assertEquals(MentionTriggerType.EMOJI_KEYWORD, qEmoji.triggerType)
        assertEquals("fire", qEmoji.query)
        assertEquals(8, qEmoji.startPosition)
        assertEquals(5, qEmoji.length)

        // 5. Контекстный бот с запросом
        val qBotWithQuery = parseMentionQueryUseCase("@gif cute kittens", cursorPosition = 17)
        assertEquals(MentionTriggerType.BOT_CONTEXT, qBotWithQuery.triggerType)
        assertEquals("gif", qBotWithQuery.contextBotUsername)
        assertEquals("cute kittens", qBotWithQuery.contextBotQuery)

        // 6. Контекстный бот без пробела
        val qBotNoQuery = parseMentionQueryUseCase("@picbot", cursorPosition = 7)
        assertEquals(MentionTriggerType.BOT_CONTEXT, qBotNoQuery.triggerType)
        assertEquals("picbot", qBotNoQuery.contextBotUsername)
        assertEquals("", qBotNoQuery.contextBotQuery)

        // 7. Обычный текст
        val qNone = parseMentionQueryUseCase("Just regular text", cursorPosition = 8)
        assertEquals(MentionTriggerType.NONE, qNone.triggerType)
        assertFalse(qNone.isActive)
    }

    @Test
    fun testFilterMentions() {
        val u1 = MentionCandidate.UserCandidate(
            id = "u1",
            userId = 1L,
            username = "alice",
            firstName = "Alice",
            lastName = "Smith"
        )
        val u2 = MentionCandidate.UserCandidate(
            id = "u2",
            userId = 2L,
            username = "bob",
            firstName = "Robert",
            lastName = "Johnson"
        )
        val c1 = MentionCandidate.BotCommandCandidate(
            id = "c1",
            command = "help",
            helpText = "Show help menu"
        )
        val c2 = MentionCandidate.BotCommandCandidate(
            id = "c2",
            command = "settings",
            helpText = "Adjust preferences"
        )

        val candidates = listOf(u1, u2, c1, c2)

        // Поиск по username
        val r1 = filterMentionsUseCase(candidates, "ali")
        assertEquals(1, r1.size)
        assertEquals("alice", (r1[0] as MentionCandidate.UserCandidate).username)

        // Поиск по firstName / lastName
        val r2 = filterMentionsUseCase(candidates, "johnson")
        assertEquals(1, r2.size)
        assertEquals("bob", (r2[0] as MentionCandidate.UserCandidate).username)

        // Поиск по command
        val r3 = filterMentionsUseCase(candidates, "set")
        assertEquals(1, r3.size)
        assertEquals("settings", (r3[0] as MentionCandidate.BotCommandCandidate).command)

        // Пустой запрос возвращает исходный список
        val rEmpty = filterMentionsUseCase(candidates, "")
        assertEquals(4, rEmpty.size)
    }

    @Test
    fun testFormatMentionReplacement() {
        // Подстановка пользователя
        val queryUser = parseMentionQueryUseCase("Message to @al", cursorPosition = 14)
        val userCandidate = MentionCandidate.UserCandidate(
            id = "u1",
            userId = 1L,
            username = "alice",
            firstName = "Alice",
            lastName = "Smith"
        )
        val replUser = formatMentionReplacementUseCase("Message to @al", queryUser, userCandidate)
        assertEquals("Message to @alice ", replUser.replacementText)
        assertEquals("Message to @alice ".length, replUser.newCursorPosition)

        // Подстановка хэштега
        val queryHash = parseMentionQueryUseCase("Check #te", cursorPosition = 9)
        val hashCandidate = MentionCandidate.HashtagCandidate(id = "h1", hashtag = "telegram")
        val replHash = formatMentionReplacementUseCase("Check #te", queryHash, hashCandidate)
        assertEquals("Check #telegram ", replHash.replacementText)
        assertEquals("Check #telegram ".length, replHash.newCursorPosition)

        // Подстановка команды
        val queryCmd = parseMentionQueryUseCase("/st", cursorPosition = 3)
        val cmdCandidate = MentionCandidate.BotCommandCandidate(id = "c1", command = "start", helpText = null)
        val replCmd = formatMentionReplacementUseCase("/st", queryCmd, cmdCandidate)
        assertEquals("/start ", replCmd.replacementText)
        assertEquals("/start ".length, replCmd.newCursorPosition)

        // Подстановка эмодзи
        val queryEmoji = parseMentionQueryUseCase("Love :fir", cursorPosition = 9)
        val emojiCandidate = MentionCandidate.EmojiKeywordCandidate(id = "e1", emoji = "🔥", keyword = "fire")
        val replEmoji = formatMentionReplacementUseCase("Love :fir", queryEmoji, emojiCandidate)
        assertEquals("Love 🔥 ", replEmoji.replacementText)
        assertEquals("Love 🔥 ".length, replEmoji.newCursorPosition)
    }

    @Test
    fun testRepositoryAndViewModelMvi() = runTest {
        val vm = MentionsViewModel(
            observeMentionsStateUseCase = observeMentionsStateUseCase,
            updateMentionQueryUseCase = updateMentionQueryUseCase,
            setMentionCandidatesUseCase = setMentionCandidatesUseCase,
            formatMentionReplacementUseCase = formatMentionReplacementUseCase,
            dismissMentionsUseCase = dismissMentionsUseCase,
            clearMentionsUseCase = clearMentionsUseCase
        )

        advanceUntilIdle()
        val s0 = vm.uiState.value
        assertFalse(s0.isSearching)
        assertFalse(s0.isPanelVisible)
        assertTrue(s0.candidates.isEmpty())

        // 1. Ввод текста с триггером "@"
        vm.onEvent(MentionsEvent.OnInputTextChanged(text = "Hello @al", cursorPosition = 9))
        advanceUntilIdle()
        val s1 = vm.uiState.value
        assertEquals(MentionTriggerType.USERNAME, s1.query.triggerType)
        assertEquals("al", s1.query.query)
        assertTrue(s1.isSearching)

        // 2. Загрузка кандидатов
        val userCandidate = MentionCandidate.UserCandidate(
            id = "u1",
            userId = 1L,
            username = "alice",
            firstName = "Alice",
            lastName = "Smith"
        )
        vm.onEvent(MentionsEvent.OnCandidatesLoaded(candidates = listOf(userCandidate), query = "al"))
        advanceUntilIdle()
        val s2 = vm.uiState.value
        assertEquals(1, s2.count)
        assertTrue(s2.isPanelVisible)

        // 3. Выбор кандидата
        vm.onEvent(MentionsEvent.OnCandidateSelected(candidate = userCandidate, currentText = "Hello @al"))
        advanceUntilIdle()
        val s3 = vm.uiState.value
        assertFalse(s3.isPanelVisible)
        assertTrue(s3.candidates.isEmpty())

        // 4. Прямой вызов applyCandidate
        vm.onEvent(MentionsEvent.OnInputTextChanged(text = "Hi @al", cursorPosition = 6))
        advanceUntilIdle()
        val replacement = vm.applyCandidate("Hi @al", userCandidate)
        assertEquals("Hi @alice ", replacement.replacementText)
        assertEquals(10, replacement.newCursorPosition)
    }
}
