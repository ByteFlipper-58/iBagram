package org.telegram.messenger.feature.emojieffects

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.emojieffects.data.repository.LegacyEmojiEffectsRepository
import org.telegram.messenger.feature.emojieffects.domain.model.EmojiAnimationQuotaStatus
import org.telegram.messenger.feature.emojieffects.domain.model.EmojiEffectItem
import org.telegram.messenger.feature.emojieffects.domain.model.EmojiInteractionSession
import org.telegram.messenger.feature.emojieffects.domain.usecase.CalculateEmojiBoundsUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.CalculateEmojiOverlayPositionUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.ClearEmojiEffectsUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.DecodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.DismissEmojiEffectUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.EncodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.EvaluateAnimationQuotaUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.EvaluateEmojiSupportUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.GetEmojiEffectsStateUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.NormalizeEmojiUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.ObserveEmojiEffectsStateUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.RecordEmojiTapUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.StartEmojiEffectUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.UpdateEmojiEffectProgressUseCase
import org.telegram.messenger.feature.emojieffects.presentation.EmojiEffectsEvent
import org.telegram.messenger.feature.emojieffects.presentation.EmojiEffectsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class EmojiEffectsDomainTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LegacyEmojiEffectsRepository
    private lateinit var normalizeEmojiUseCase: NormalizeEmojiUseCase
    private lateinit var evaluateEmojiSupportUseCase: EvaluateEmojiSupportUseCase
    private lateinit var recordEmojiTapUseCase: RecordEmojiTapUseCase
    private lateinit var encodeEmojiInteractionsJsonUseCase: EncodeEmojiInteractionsJsonUseCase
    private lateinit var decodeEmojiInteractionsJsonUseCase: DecodeEmojiInteractionsJsonUseCase
    private lateinit var calculateEmojiBoundsUseCase: CalculateEmojiBoundsUseCase
    private lateinit var calculateEmojiOverlayPositionUseCase: CalculateEmojiOverlayPositionUseCase
    private lateinit var evaluateAnimationQuotaUseCase: EvaluateAnimationQuotaUseCase
    private lateinit var observeEmojiEffectsStateUseCase: ObserveEmojiEffectsStateUseCase
    private lateinit var getEmojiEffectsStateUseCase: GetEmojiEffectsStateUseCase
    private lateinit var startEmojiEffectUseCase: StartEmojiEffectUseCase
    private lateinit var updateEmojiEffectProgressUseCase: UpdateEmojiEffectProgressUseCase
    private lateinit var dismissEmojiEffectUseCase: DismissEmojiEffectUseCase
    private lateinit var clearEmojiEffectsUseCase: ClearEmojiEffectsUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyEmojiEffectsRepository()
        normalizeEmojiUseCase = NormalizeEmojiUseCase()
        evaluateEmojiSupportUseCase = EvaluateEmojiSupportUseCase(normalizeEmojiUseCase)
        recordEmojiTapUseCase = RecordEmojiTapUseCase(repository, normalizeEmojiUseCase)
        encodeEmojiInteractionsJsonUseCase = EncodeEmojiInteractionsJsonUseCase()
        decodeEmojiInteractionsJsonUseCase = DecodeEmojiInteractionsJsonUseCase()
        calculateEmojiBoundsUseCase = CalculateEmojiBoundsUseCase()
        calculateEmojiOverlayPositionUseCase = CalculateEmojiOverlayPositionUseCase()
        evaluateAnimationQuotaUseCase = EvaluateAnimationQuotaUseCase()
        observeEmojiEffectsStateUseCase = ObserveEmojiEffectsStateUseCase(repository)
        getEmojiEffectsStateUseCase = GetEmojiEffectsStateUseCase(repository)
        startEmojiEffectUseCase = StartEmojiEffectUseCase(repository, evaluateAnimationQuotaUseCase)
        updateEmojiEffectProgressUseCase = UpdateEmojiEffectProgressUseCase(repository)
        dismissEmojiEffectUseCase = DismissEmojiEffectUseCase(repository)
        clearEmojiEffectsUseCase = ClearEmojiEffectsUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNormalizeEmojiVariantsAndSkinTones() {
        // Удаление селектора 0xFE0F
        val heartWithSelector = "\u2764\uFE0F"
        val normalizedHeart = normalizeEmojiUseCase(heartWithSelector)
        assertEquals("\u2764", normalizedHeart)

        // Удаление тона кожи
        val thumbsUpSkin = "\uD83D\uDC4D\uD83C\uDFFD"
        val normalizedThumbsUp = normalizeEmojiUseCase(thumbsUpSkin)
        assertEquals("\uD83D\uDC4D", normalizedThumbsUp)

        // Удаление ZWJ гендерных модификаторов
        val maleRunner = "\uD83C\uDFC3\u200D\u2642"
        val normalizedRunner = normalizeEmojiUseCase(maleRunner)
        assertEquals("\uD83C\uDFC3", normalizedRunner)

        // Null безопасность
        assertNull(normalizeEmojiUseCase(null))
    }

    @Test
    fun testEvaluateEmojiSupportAndHeartsExpansion() {
        val supportedPack = setOf("🎉", "🔥", "❤")

        // Обычные эмодзи из пака
        assertTrue(evaluateEmojiSupportUseCase("🎉", supportedPack))
        assertTrue(evaluateEmojiSupportUseCase("🔥", supportedPack))

        // Цветные сердца поддерживаются благодаря красному сердцу
        assertTrue(evaluateEmojiSupportUseCase("🧡", supportedPack))
        assertTrue(evaluateEmojiSupportUseCase("💙", supportedPack))
        assertTrue(evaluateEmojiSupportUseCase("🖤", supportedPack))

        // Исключаемые кейкапы цифр
        assertFalse(evaluateEmojiSupportUseCase("\u0031\u20E3", supportedPack + "\u0031\u20E3"))

        // Неподдерживаемый эмодзи
        assertFalse(evaluateEmojiSupportUseCase("🚀", supportedPack))
    }

    @Test
    fun testTapSessionAggregationAndSwitching() {
        // Тап 1
        val s1 = recordEmojiTapUseCase(
            messageId = 101,
            rawEmoticon = "🔥",
            animationIndex = 0,
            timestampMs = 1000L
        )
        assertEquals(101, s1.messageId)
        assertEquals(1, s1.actionsCount)
        assertEquals(listOf(0L), s1.relativeTimeOffsetsMs)
        assertEquals(listOf(0), s1.animationIndexes)

        // Тап 2 по тому же сообщению через 120 мс
        val s2 = recordEmojiTapUseCase(
            messageId = 101,
            rawEmoticon = "🔥",
            animationIndex = 1,
            timestampMs = 1120L
        )
        assertEquals(101, s2.messageId)
        assertEquals(2, s2.actionsCount)
        assertEquals(listOf(0L, 120L), s2.relativeTimeOffsetsMs)
        assertEquals(listOf(0, 1), s2.animationIndexes)

        // Тап 3 по новому сообщению - сессия сбрасывается и начинается заново
        val s3 = recordEmojiTapUseCase(
            messageId = 202,
            rawEmoticon = "🎉",
            animationIndex = 2,
            timestampMs = 2000L
        )
        assertEquals(202, s3.messageId)
        assertEquals(1, s3.actionsCount)
        assertEquals(listOf(0L), s3.relativeTimeOffsetsMs)
        assertEquals(listOf(2), s3.animationIndexes)
    }

    @Test
    fun testJsonEncodingAndDecodingInteractions() {
        val session = EmojiInteractionSession(
            messageId = 100,
            emoticon = "🔥",
            initialTimestampMs = 1000L,
            relativeTimeOffsetsMs = listOf(0L, 250L, 600L),
            animationIndexes = listOf(0, 1, 2)
        )

        val json = encodeEmojiInteractionsJsonUseCase(session)
        assertTrue(json.contains(""""v":1"""))
        assertTrue(json.contains(""""i":1"""))
        assertTrue(json.contains(""""i":2"""))
        assertTrue(json.contains(""""i":3"""))

        val decodedActions = decodeEmojiInteractionsJsonUseCase(json)
        assertEquals(3, decodedActions.size)
        assertEquals(0, decodedActions[0].index)
        assertEquals(0.0, decodedActions[0].timeOffsetSeconds, 0.001)
        assertEquals(0L, decodedActions[0].delayMillis)

        assertEquals(1, decodedActions[1].index)
        assertEquals(0.25, decodedActions[1].timeOffsetSeconds, 0.001)
        assertEquals(250L, decodedActions[1].delayMillis)

        assertEquals(2, decodedActions[2].index)
        assertEquals(0.6, decodedActions[2].timeOffsetSeconds, 0.001)
        assertEquals(600L, decodedActions[2].delayMillis)
    }

    @Test
    fun testAnimationQuotaAndLimits() {
        // До 12 глобальных и до 4 на одно сообщение разрешено
        val q1 = evaluateAnimationQuotaUseCase(currentGlobalCount = 5, currentMessageCount = 2)
        assertEquals(EmojiAnimationQuotaStatus.ALLOWED, q1.status)
        assertTrue(q1.isAllowed)

        // Превышение глобального лимита (12)
        val qGlobal = evaluateAnimationQuotaUseCase(currentGlobalCount = 12, currentMessageCount = 1)
        assertEquals(EmojiAnimationQuotaStatus.EXCEEDED_GLOBAL_LIMIT, qGlobal.status)
        assertFalse(qGlobal.isAllowed)

        // Превышение лимита на сообщение (4)
        val qMsg = evaluateAnimationQuotaUseCase(currentGlobalCount = 4, currentMessageCount = 4)
        assertEquals(EmojiAnimationQuotaStatus.EXCEEDED_MESSAGE_LIMIT, qMsg.status)
        assertFalse(qMsg.isAllowed)

        // Генерация кэша
        val qCache = evaluateAnimationQuotaUseCase(currentGlobalCount = 1, currentMessageCount = 1, isCacheGenerating = true)
        assertEquals(EmojiAnimationQuotaStatus.CACHE_GENERATING, qCache.status)
        assertFalse(qCache.isAllowed)
    }

    @Test
    fun testBoundsAndOverlayPositionCalculation() {
        // Расчет размера фильтра на смартфоне
        val phoneBounds = calculateEmojiBoundsUseCase(
            isTablet = false,
            screenWidth = 1080,
            screenHeight = 2400,
            density = 2.0f
        )
        // min(1080, 2400) * 0.5f = 540f; 2 * 540 / 2.0 = 540
        assertEquals(540, phoneBounds)

        // Расчет позиции для исходящего сообщения
        val outGeom = calculateEmojiOverlayPositionUseCase(
            cellX = 200f,
            cellY = 400f,
            imageX = 50f,
            imageY = 60f,
            imageWidth = 120f,
            imageHeight = 120f,
            isOut = true,
            isPremiumSticker = false,
            marginDp24 = 24f,
            listTopPadding = 100f,
            listBottomBound = 1800f
        )
        // Out baseOffsetX = -120 * 2 + 24 = -216; x = 200 + 50 - 216 = 34
        // y = 400 + 60 - 120 = 340
        assertEquals(34f, outGeom.x, 0.01f)
        assertEquals(340f, outGeom.y, 0.01f)
        assertFalse(outGeom.isOutside)

        // Проверка выхода за границы (outside list top padding)
        val outsideGeom = calculateEmojiOverlayPositionUseCase(
            cellX = 200f,
            cellY = 20f,
            imageX = 50f,
            imageY = 60f,
            imageWidth = 50f,
            imageHeight = 50f,
            isOut = false,
            isPremiumSticker = false,
            listTopPadding = 100f,
            listBottomBound = 1800f
        )
        assertTrue(outsideGeom.isOutside)
    }

    @Test
    fun testRepositoryLifecycleAndViewModelMvi() = runTest {
        val vm = EmojiEffectsViewModel(
            observeEmojiEffectsStateUseCase = observeEmojiEffectsStateUseCase,
            recordEmojiTapUseCase = recordEmojiTapUseCase,
            startEmojiEffectUseCase = startEmojiEffectUseCase,
            updateEmojiEffectProgressUseCase = updateEmojiEffectProgressUseCase,
            dismissEmojiEffectUseCase = dismissEmojiEffectUseCase,
            clearEmojiEffectsUseCase = clearEmojiEffectsUseCase,
            repository = repository
        )

        advanceUntilIdle()
        val initialState = vm.uiState.value
        assertTrue(initialState.isIdle)
        assertTrue(initialState.activeEffects.isEmpty())

        // Тап через ViewModel
        vm.onEvent(
            EmojiEffectsEvent.OnEmojiTapped(
                messageId = 55,
                rawEmoji = "🔥",
                animationIndex = 0,
                timestampMs = 5000L
            )
        )
        advanceUntilIdle()
        val stateAfterTap = vm.uiState.value
        assertEquals(1, stateAfterTap.pendingInteractionsCount)
        assertEquals("🔥", stateAfterTap.lastRecordedEmoji)

        // Запуск эффекта
        val effect = EmojiEffectItem(
            id = "eff_1",
            messageId = 55,
            documentId = 777L,
            emoticon = "🔥",
            progress = 0f
        )
        vm.onEvent(EmojiEffectsEvent.OnEffectStarted(effect))
        advanceUntilIdle()
        val stateAfterStart = vm.uiState.value
        assertEquals(1, stateAfterStart.activeEffects.size)
        assertFalse(stateAfterStart.isIdle)
        assertTrue(stateAfterStart.activeEffects[0].isPlaying)

        // Обновление прогресса
        vm.onEvent(EmojiEffectsEvent.OnEffectProgressUpdated(id = "eff_1", progress = 0.5f))
        advanceUntilIdle()
        val stateAfterProgress = vm.uiState.value
        assertEquals(0.5f, stateAfterProgress.activeEffects[0].progress, 0.01f)

        // Завершение эффекта (прогресс = 1.0)
        vm.onEvent(EmojiEffectsEvent.OnEffectProgressUpdated(id = "eff_1", progress = 1.0f))
        advanceUntilIdle()
        val stateAfterFinish = vm.uiState.value
        assertTrue(stateAfterFinish.activeEffects.isEmpty())
        assertTrue(stateAfterFinish.isIdle)

        // Слив тапов
        val drained = vm.drainTapsSession()
        assertNotNull(drained)
        assertEquals(55, drained!!.messageId)
        assertNull(vm.drainTapsSession())
    }
}
