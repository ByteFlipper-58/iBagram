package org.telegram.messenger.feature.localization

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.localization.data.mapper.LocalizationMapper
import org.telegram.messenger.feature.localization.data.repository.LegacyLocalizationRepository
import org.telegram.messenger.feature.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.localization.domain.model.NameDisplayOrder
import org.telegram.messenger.feature.localization.domain.model.PluralQuantity
import org.telegram.messenger.feature.localization.domain.usecase.ApplyLocaleUseCase
import org.telegram.messenger.feature.localization.domain.usecase.DetectRtlLanguageUseCase
import org.telegram.messenger.feature.localization.domain.usecase.FormatFullNameUseCase
import org.telegram.messenger.feature.localization.domain.usecase.FormatNumberWithSuffixUseCase
import org.telegram.messenger.feature.localization.domain.usecase.FormatRelativeTimestampUseCase
import org.telegram.messenger.feature.localization.domain.usecase.GetLocalizationStateUseCase
import org.telegram.messenger.feature.localization.domain.usecase.ObserveLocalizationStateUseCase
import org.telegram.messenger.feature.localization.domain.usecase.ResolvePluralQuantityUseCase
import org.telegram.messenger.feature.localization.domain.usecase.SetNameDisplayOrderUseCase
import org.telegram.messenger.feature.localization.domain.usecase.Toggle24HourFormatUseCase
import org.telegram.messenger.feature.localization.presentation.LocalizationEvent
import org.telegram.messenger.feature.localization.presentation.LocalizationViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LocalizationDomainTest {

    private lateinit var repository: LegacyLocalizationRepository
    private lateinit var resolvePluralQuantityUseCase: ResolvePluralQuantityUseCase
    private lateinit var formatRelativeTimestampUseCase: FormatRelativeTimestampUseCase
    private lateinit var formatFullNameUseCase: FormatFullNameUseCase
    private lateinit var formatNumberWithSuffixUseCase: FormatNumberWithSuffixUseCase
    private lateinit var detectRtlLanguageUseCase: DetectRtlLanguageUseCase
    private lateinit var observeStateUseCase: ObserveLocalizationStateUseCase
    private lateinit var getStateUseCase: GetLocalizationStateUseCase
    private lateinit var applyLocaleUseCase: ApplyLocaleUseCase
    private lateinit var toggle24HourFormatUseCase: Toggle24HourFormatUseCase
    private lateinit var setNameDisplayOrderUseCase: SetNameDisplayOrderUseCase

    @Before
    fun setUp() {
        repository = LegacyLocalizationRepository(currentAccount = 0)
        resolvePluralQuantityUseCase = ResolvePluralQuantityUseCase()
        formatRelativeTimestampUseCase = FormatRelativeTimestampUseCase()
        formatFullNameUseCase = FormatFullNameUseCase()
        formatNumberWithSuffixUseCase = FormatNumberWithSuffixUseCase()
        detectRtlLanguageUseCase = DetectRtlLanguageUseCase()
        observeStateUseCase = ObserveLocalizationStateUseCase(repository)
        getStateUseCase = GetLocalizationStateUseCase(repository)
        applyLocaleUseCase = ApplyLocaleUseCase(repository, detectRtlLanguageUseCase)
        toggle24HourFormatUseCase = Toggle24HourFormatUseCase(repository)
        setNameDisplayOrderUseCase = SetNameDisplayOrderUseCase(repository)
    }

    @Test
    fun testPluralRulesResolution() {
        // English (default)
        assertEquals(PluralQuantity.ONE, resolvePluralQuantityUseCase("en", 1))
        assertEquals(PluralQuantity.OTHER, resolvePluralQuantityUseCase("en", 0))
        assertEquals(PluralQuantity.OTHER, resolvePluralQuantityUseCase("en", 2))
        assertEquals(PluralQuantity.OTHER, resolvePluralQuantityUseCase("en", 10))

        // Russian / Ukrainian / Belarusian
        assertEquals(PluralQuantity.ONE, resolvePluralQuantityUseCase("ru", 1))
        assertEquals(PluralQuantity.ONE, resolvePluralQuantityUseCase("ru", 21))
        assertEquals(PluralQuantity.ONE, resolvePluralQuantityUseCase("ru", 101))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("ru", 11)) // 11 is special
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("ru", 2))
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("ru", 3))
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("ru", 4))
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("ru", 24))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("ru", 12)) // 12-14 are special
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("ru", 0))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("ru", 5))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("ru", 25))

        // Polish
        assertEquals(PluralQuantity.ONE, resolvePluralQuantityUseCase("pl", 1))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("pl", 21)) // Unlike RU, 21 in Polish is MANY
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("pl", 2))
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("pl", 22))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("pl", 5))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("pl", 12))

        // Arabic
        assertEquals(PluralQuantity.ZERO, resolvePluralQuantityUseCase("ar", 0))
        assertEquals(PluralQuantity.ONE, resolvePluralQuantityUseCase("ar", 1))
        assertEquals(PluralQuantity.TWO, resolvePluralQuantityUseCase("ar", 2))
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("ar", 3))
        assertEquals(PluralQuantity.FEW, resolvePluralQuantityUseCase("ar", 10))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("ar", 11))
        assertEquals(PluralQuantity.MANY, resolvePluralQuantityUseCase("ar", 99))
        assertEquals(PluralQuantity.OTHER, resolvePluralQuantityUseCase("ar", 100))
        assertEquals(PluralQuantity.OTHER, resolvePluralQuantityUseCase("ar", 102))
    }

    @Test
    fun testRelativeTimestampFormatting() {
        val baseTime = 1_700_000_000_000L

        // Future or same time
        val justNow = formatRelativeTimestampUseCase(baseTime, baseTime + 5000L)
        assertEquals("just now", justNow.formatted)
        assertTrue(justNow.isRecent)

        // 30 seconds ago
        val secondsAgo = formatRelativeTimestampUseCase(baseTime, baseTime - 30_000L)
        assertEquals("just now", secondsAgo.formatted)
        assertTrue(secondsAgo.isRecent)

        // 5 minutes ago
        val minutesAgo = formatRelativeTimestampUseCase(baseTime, baseTime - 5 * 60_000L)
        assertEquals("5m ago", minutesAgo.formatted)
        assertTrue(minutesAgo.isRecent)

        // 3 hours ago
        val hoursAgo = formatRelativeTimestampUseCase(baseTime, baseTime - 3 * 3600_000L)
        assertEquals("3h ago", hoursAgo.formatted)
        assertFalse(hoursAgo.isRecent)

        // 2 days ago
        val daysAgo = formatRelativeTimestampUseCase(baseTime, baseTime - 2 * 24 * 3600_000L)
        assertEquals("2d ago", daysAgo.formatted)
        assertFalse(daysAgo.isRecent)
    }

    @Test
    fun testNameFormattingAndRtlDetection() {
        // Name formatting
        assertEquals("John Doe", formatFullNameUseCase("John", "Doe", NameDisplayOrder.FIRST_LAST))
        assertEquals("Doe John", formatFullNameUseCase("John", "Doe", NameDisplayOrder.LAST_FIRST))
        assertEquals("John", formatFullNameUseCase("John", "", NameDisplayOrder.FIRST_LAST))
        assertEquals("Doe", formatFullNameUseCase("", "Doe", NameDisplayOrder.FIRST_LAST))
        assertEquals("John", formatFullNameUseCase("John", null, NameDisplayOrder.LAST_FIRST))
        assertEquals("", formatFullNameUseCase(null, null, NameDisplayOrder.FIRST_LAST))

        // RTL detection
        assertTrue(detectRtlLanguageUseCase("ar"))
        assertTrue(detectRtlLanguageUseCase("ar-SA"))
        assertTrue(detectRtlLanguageUseCase("fa_IR"))
        assertTrue(detectRtlLanguageUseCase("he"))
        assertTrue(detectRtlLanguageUseCase("ur"))
        assertTrue(detectRtlLanguageUseCase("ckb"))
        assertFalse(detectRtlLanguageUseCase("en"))
        assertFalse(detectRtlLanguageUseCase("en-US"))
        assertFalse(detectRtlLanguageUseCase("ru"))
        assertFalse(detectRtlLanguageUseCase("de"))
        assertFalse(detectRtlLanguageUseCase("zh"))
        assertFalse(detectRtlLanguageUseCase(null))
    }

    @Test
    fun testNumberWithSuffixAndLocalizationMapper() {
        // Number with suffix (Locale.US)
        assertEquals("500", formatNumberWithSuffixUseCase(500L))
        assertEquals("1.5K", formatNumberWithSuffixUseCase(1500L))
        assertEquals("2.5M", formatNumberWithSuffixUseCase(2_500_000L))
        assertEquals("3.1B", formatNumberWithSuffixUseCase(3_100_000_000L))
        assertEquals("-1.5K", formatNumberWithSuffixUseCase(-1500L))

        // LocalizationMapper formatPlural
        val ruQuantity1 = resolvePluralQuantityUseCase("ru", 1)
        val formattedRu1 = LocalizationMapper.formatPlural(
            quantity = ruQuantity1,
            count = 1,
            one = "%d сообщение",
            few = "%d сообщения",
            many = "%d сообщений",
            other = "%d сообщений"
        )
        assertEquals("1 сообщение", formattedRu1)

        val ruQuantity3 = resolvePluralQuantityUseCase("ru", 3)
        val formattedRu3 = LocalizationMapper.formatPlural(
            quantity = ruQuantity3,
            count = 3,
            one = "%d сообщение",
            few = "%d сообщения",
            many = "%d сообщений",
            other = "%d сообщений"
        )
        assertEquals("3 сообщения", formattedRu3)

        val ruQuantity5 = resolvePluralQuantityUseCase("ru", 5)
        val formattedRu5 = LocalizationMapper.formatPlural(
            quantity = ruQuantity5,
            count = 5,
            one = "%d сообщение",
            few = "%d сообщения",
            many = "%d сообщений",
            other = "%d сообщений"
        )
        assertEquals("5 сообщений", formattedRu5)
    }

    @Test
    fun testViewModelAndRepositoryMviEvents() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = LocalizationViewModel(
            observeLocalizationStateUseCase = observeStateUseCase,
            applyLocaleUseCase = applyLocaleUseCase,
            toggle24HourFormatUseCase = toggle24HourFormatUseCase,
            setNameDisplayOrderUseCase = setNameDisplayOrderUseCase,
            repository = repository,
            scope = testScope
        )

        testScheduler.advanceUntilIdle()
        var uiState = viewModel.uiState.value
        assertEquals("en", uiState.currentLocale.code)
        assertEquals(6, uiState.availableLocales.size)
        assertFalse(uiState.isRtl)
        assertFalse(uiState.is24HourFormat)

        // 1. Search locales
        viewModel.onEvent(LocalizationEvent.SearchLocales("рус"))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals("рус", uiState.searchQuery)
        assertEquals(1, uiState.filteredLocales.size)
        assertEquals("ru", uiState.filteredLocales[0].code)

        // 2. Switch locale to Arabic (RTL)
        val arabicLocale = uiState.availableLocales.first { it.code == "ar" }
        viewModel.onEvent(LocalizationEvent.SelectLocale(arabicLocale))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals("ar", uiState.currentLocale.code)
        assertTrue(uiState.isRtl)

        // 3. Toggle 24-hour format
        viewModel.onEvent(LocalizationEvent.Toggle24HourFormat(true))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertTrue(uiState.is24HourFormat)

        // 4. Change name display order
        viewModel.onEvent(LocalizationEvent.ChangeNameDisplayOrder(NameDisplayOrder.LAST_FIRST))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(NameDisplayOrder.LAST_FIRST, uiState.nameDisplayOrder)

        // 5. Apply custom string overrides
        val customStrings = mapOf("AppName" to "iBagram Super", "Cancel" to "Отмена")
        viewModel.onEvent(LocalizationEvent.ApplyCustomStrings(customStrings))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(2, uiState.customStringsCount)
        assertEquals("iBagram Super", repository.getString("AppName", "Fallback"))
        assertEquals("Отмена", repository.getString("Cancel", "Fallback"))
        assertEquals("DefaultVal", repository.getString("UnknownKey", "DefaultVal"))

        // 6. Reset custom strings
        viewModel.onEvent(LocalizationEvent.ResetCustomStrings)
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(0, uiState.customStringsCount)
        assertEquals("Fallback", repository.getString("AppName", "Fallback"))
    }
}
