package org.telegram.messenger.feature.business.businessrecipients

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.business.businessrecipients.data.mapper.BusinessRecipientsMapper
import org.telegram.messenger.feature.business.businessrecipients.data.repository.LegacyBusinessRecipientsRepository
import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.AddExcludedUsersUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.AddSelectedUsersUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.CheckRecipientsChangesUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.GetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ObserveBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.RemoveExcludedUserUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.RemoveSelectedUserUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ResetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.SetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ToggleExcludeSelectedUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ToggleRecipientFilterUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ValidateBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.presentation.BusinessRecipientsEvent
import org.telegram.messenger.feature.business.businessrecipients.presentation.BusinessRecipientsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessRecipientsDomainTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LegacyBusinessRecipientsRepository

    private lateinit var observeRecipientsUseCase: ObserveBusinessRecipientsUseCase
    private lateinit var getRecipientsUseCase: GetBusinessRecipientsUseCase
    private lateinit var setRecipientsUseCase: SetBusinessRecipientsUseCase
    private lateinit var toggleExcludeSelectedUseCase: ToggleExcludeSelectedUseCase
    private lateinit var toggleRecipientFilterUseCase: ToggleRecipientFilterUseCase
    private lateinit var addSelectedUsersUseCase: AddSelectedUsersUseCase
    private lateinit var removeSelectedUserUseCase: RemoveSelectedUserUseCase
    private lateinit var addExcludedUsersUseCase: AddExcludedUsersUseCase
    private lateinit var removeExcludedUserUseCase: RemoveExcludedUserUseCase
    private lateinit var checkChangesUseCase: CheckRecipientsChangesUseCase
    private lateinit var validateUseCase: ValidateBusinessRecipientsUseCase
    private lateinit var resetUseCase: ResetBusinessRecipientsUseCase

    private lateinit var viewModel: BusinessRecipientsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyBusinessRecipientsRepository()

        observeRecipientsUseCase = ObserveBusinessRecipientsUseCase(repository)
        getRecipientsUseCase = GetBusinessRecipientsUseCase(repository)
        setRecipientsUseCase = SetBusinessRecipientsUseCase(repository)
        toggleExcludeSelectedUseCase = ToggleExcludeSelectedUseCase(repository)
        toggleRecipientFilterUseCase = ToggleRecipientFilterUseCase(repository)
        addSelectedUsersUseCase = AddSelectedUsersUseCase(repository)
        removeSelectedUserUseCase = RemoveSelectedUserUseCase(repository)
        addExcludedUsersUseCase = AddExcludedUsersUseCase(repository)
        removeExcludedUserUseCase = RemoveExcludedUserUseCase(repository)
        checkChangesUseCase = CheckRecipientsChangesUseCase(repository)
        validateUseCase = ValidateBusinessRecipientsUseCase(repository)
        resetUseCase = ResetBusinessRecipientsUseCase(repository)

        viewModel = BusinessRecipientsViewModel(
            observeRecipientsUseCase = observeRecipientsUseCase,
            getRecipientsUseCase = getRecipientsUseCase,
            setRecipientsUseCase = setRecipientsUseCase,
            toggleExcludeSelectedUseCase = toggleExcludeSelectedUseCase,
            toggleRecipientFilterUseCase = toggleRecipientFilterUseCase,
            addSelectedUsersUseCase = addSelectedUsersUseCase,
            removeSelectedUserUseCase = removeSelectedUserUseCase,
            addExcludedUsersUseCase = addExcludedUsersUseCase,
            removeExcludedUserUseCase = removeExcludedUserUseCase,
            checkChangesUseCase = checkChangesUseCase,
            validateUseCase = validateUseCase,
            resetUseCase = resetUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mapper_shouldCorrectlyConvertFlagsToModelAndBack() {
        // Flags: EXISTING_CHATS(1) | CONTACTS(4) = 5
        val flags = 1 or 4
        val selected = setOf(101L, 102L)
        val excluded = setOf(201L)

        val model = BusinessRecipientsMapper.fromFlags(
            flags = flags,
            exclude = true,
            isBot = false,
            selectedUsers = selected,
            excludedUsers = excluded
        )
        assertTrue(model.existingChats)
        assertFalse(model.newChats)
        assertTrue(model.contacts)
        assertFalse(model.nonContacts)
        assertTrue(model.excludeSelected)
        assertEquals(selected, model.selectedUserIds)
        assertEquals(excluded, model.excludedUserIds)

        val reconstructedFlags = BusinessRecipientsMapper.toFlags(model)
        assertEquals(flags, reconstructedFlags)
    }

    @Test
    fun mapper_validate_shouldReportEmptyRecipients_whenIncludeModeHasNoSelection() {
        val emptyModel = BusinessRecipientsModel(excludeSelected = false)
        val result = BusinessRecipientsMapper.validate(emptyModel)
        assertFalse(result.isValid)
        assertNotNull(result.errorReason)

        val withUser = emptyModel.copy(selectedUserIds = setOf(42L))
        val validUserResult = BusinessRecipientsMapper.validate(withUser)
        assertTrue(validUserResult.isValid)
        assertNull(validUserResult.errorReason)

        val withFilter = emptyModel.copy(existingChats = true)
        val validFilterResult = BusinessRecipientsMapper.validate(withFilter)
        assertTrue(validFilterResult.isValid)
        assertNull(validFilterResult.errorReason)

        val excludeModeModel = emptyModel.copy(excludeSelected = true)
        val validExcludeResult = BusinessRecipientsMapper.validate(excludeModeModel)
        assertTrue(validExcludeResult.isValid)
        assertNull(validExcludeResult.errorReason)
    }

    @Test
    fun mapper_hasChanges_shouldDetectAnyDifference() {
        val base = BusinessRecipientsModel(
            excludeSelected = false,
            existingChats = true,
            selectedUserIds = setOf(1L, 2L)
        )
        assertFalse(BusinessRecipientsMapper.hasChanges(base, base.copy()))

        assertTrue(BusinessRecipientsMapper.hasChanges(base, base.copy(existingChats = false)))
        assertTrue(BusinessRecipientsMapper.hasChanges(base, base.copy(newChats = true)))
        assertTrue(BusinessRecipientsMapper.hasChanges(base, base.copy(excludeSelected = true)))
        assertTrue(BusinessRecipientsMapper.hasChanges(base, base.copy(selectedUserIds = setOf(1L))))

        // In exclude mode, excludedUserIds are compared
        val baseExclude = BusinessRecipientsModel(
            excludeSelected = true,
            excludedUserIds = setOf(10L)
        )
        assertTrue(BusinessRecipientsMapper.hasChanges(baseExclude, baseExclude.copy(excludedUserIds = setOf(10L, 20L))))
    }

    @Test
    fun repository_shouldMaintainMutualExclusionBetweenSelectedAndExcluded() {
        // Start with empty
        repository.addSelectedUsers(listOf(100L, 200L))
        var model = repository.getRecipients()
        assertEquals(setOf(100L, 200L), model.selectedUserIds)
        assertTrue(model.excludedUserIds.isEmpty())

        // Add 200L to excluded: should be removed from selected!
        repository.addExcludedUsers(listOf(200L, 300L))
        model = repository.getRecipients()
        assertEquals(setOf(100L), model.selectedUserIds)
        assertEquals(setOf(200L, 300L), model.excludedUserIds)

        // Add 300L back to selected: should be removed from excluded!
        repository.addSelectedUsers(listOf(300L))
        model = repository.getRecipients()
        assertEquals(setOf(100L, 300L), model.selectedUserIds)
        assertEquals(setOf(200L), model.excludedUserIds)

        // Remove 100L from selected
        repository.removeSelectedUser(100L)
        model = repository.getRecipients()
        assertEquals(setOf(300L), model.selectedUserIds)

        // Remove 200L from excluded
        repository.removeExcludedUser(200L)
        model = repository.getRecipients()
        assertTrue(model.excludedUserIds.isEmpty())
    }

    @Test
    fun repository_shouldToggleFiltersAndExcludeSelected() {
        assertFalse(repository.getRecipients().contacts)
        repository.toggleFilter(RecipientFilterType.CONTACTS, true)
        assertTrue(repository.getRecipients().contacts)

        assertTrue(repository.getRecipients().excludeSelected)
        repository.toggleExcludeSelected(false)
        assertFalse(repository.getRecipients().excludeSelected)

        repository.reset()
        assertFalse(repository.getRecipients().contacts)
        assertTrue(repository.getRecipients().excludeSelected) // Default is excludeSelected = true
    }

    @Test
    fun viewModel_shouldHandleMviEventsAndDetectChanges() = runTest {
        val initial = BusinessRecipientsModel(
            excludeSelected = false,
            contacts = true,
            selectedUserIds = setOf(10L)
        )
        viewModel.onEvent(BusinessRecipientsEvent.SetInitial(initial))
        advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertEquals(initial, uiState.initialModel)
        assertEquals(initial, uiState.currentModel)
        assertFalse(uiState.hasChanges)
        assertTrue(uiState.validationResult.isValid)

        // Modify filter -> hasChanges should become true
        viewModel.onEvent(BusinessRecipientsEvent.ToggleFilter(RecipientFilterType.NEW_CHATS, true))
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertTrue(uiState.currentModel.newChats)
        assertTrue(uiState.hasChanges)

        // Add user -> updates currentModel
        viewModel.onEvent(BusinessRecipientsEvent.AddSelectedUsers(listOf(20L)))
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertTrue(uiState.currentModel.selectedUserIds.contains(20L))

        // Reset
        viewModel.onEvent(BusinessRecipientsEvent.Reset)
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertTrue(uiState.currentModel.selectedUserIds.isEmpty())
    }

    @Test
    fun viewModel_validate_shouldReturnCurrentValidationState() = runTest {
        // Default model has excludeSelected = true, which is valid
        var result = viewModel.validate()
        assertTrue(result.isValid)

        // Switch to include mode with empty selection -> invalid
        viewModel.onEvent(BusinessRecipientsEvent.ToggleExclude(false))
        advanceUntilIdle()
        result = viewModel.validate()
        assertFalse(result.isValid)

        // Add a user -> valid
        viewModel.onEvent(BusinessRecipientsEvent.AddSelectedUsers(listOf(99L)))
        advanceUntilIdle()
        result = viewModel.validate()
        assertTrue(result.isValid)
    }
}
