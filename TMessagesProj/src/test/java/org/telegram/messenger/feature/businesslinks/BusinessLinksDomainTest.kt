package org.telegram.messenger.feature.businesslinks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businesslinks.data.mapper.BusinessLinkMapper
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinksStateModel
import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository
import org.telegram.messenger.feature.businesslinks.domain.usecase.CanAddNewBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.CreateBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.DeleteBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.EditBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.FindBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.GetBusinessLinksUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.LoadBusinessLinksUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.ObserveBusinessLinksUseCase
import org.telegram.messenger.feature.businesslinks.presentation.BusinessLinksEvent
import org.telegram.messenger.feature.businesslinks.presentation.BusinessLinksViewModel
import org.telegram.tgnet.tl.TL_account

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessLinksDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBusinessLinksRepository : BusinessLinksRepository {
        val linksList = mutableListOf<BusinessLinkModel>()
        var limit = 5
        var shouldFail = false
        var lastLoadedForce = false

        private val linksFlow = MutableStateFlow<List<BusinessLinkModel>>(emptyList())

        fun emitLinks() {
            linksFlow.value = linksList.toList()
        }

        override fun observeBusinessLinks(): Flow<List<BusinessLinkModel>> = linksFlow

        override suspend fun getBusinessLinks(): List<BusinessLinkModel> = linksList.toList()

        override suspend fun loadBusinessLinks(forceReload: Boolean): Result<List<BusinessLinkModel>> {
            lastLoadedForce = forceReload
            return if (shouldFail) {
                Result.failure("Failed to load business links")
            } else {
                emitLinks()
                Result.Success(linksList.toList())
            }
        }

        override suspend fun createLink(input: BusinessLinkInputModel?): Result<BusinessLinkModel> {
            if (shouldFail) return Result.failure("Failed to create link")
            val slug = "slug_${linksList.size + 1}"
            val newLink = BusinessLinkModel(
                link = "https://t.me/m/$slug",
                slug = slug,
                title = input?.title,
                message = input?.message ?: "",
                views = 0,
                hasEntities = false
            )
            linksList.add(newLink)
            emitLinks()
            return Result.Success(newLink)
        }

        override suspend fun editLink(slug: String, title: String?, message: String): Result<BusinessLinkModel> {
            if (shouldFail) return Result.failure("Failed to edit link")
            val index = linksList.indexOfFirst { it.slug == slug }
            if (index == -1) return Result.failure("Link not found")
            val updated = linksList[index].copy(title = title, message = message)
            linksList[index] = updated
            emitLinks()
            return Result.Success(updated)
        }

        override suspend fun deleteLink(slug: String): Result<Unit> {
            if (shouldFail) return Result.failure("Failed to delete link")
            val removed = linksList.removeAll { it.slug == slug }
            if (!removed) return Result.failure("Link not found")
            emitLinks()
            return Result.Success(Unit)
        }

        override fun findLink(slug: String): BusinessLinkModel? {
            return linksList.find { it.slug == slug }
        }

        override fun canAddNew(): Boolean {
            return linksList.size < limit
        }

        override fun getLinksLimit(): Int = limit
    }

    @Test
    fun testBusinessLinkModelProperties() {
        val linkWithTitle = BusinessLinkModel(
            link = "t.me/m/support_chat",
            slug = "support_chat",
            title = "Customer Support",
            message = "Hello! How can we help you?",
            views = 42,
            hasEntities = true
        )

        assertEquals("Customer Support", linkWithTitle.displayTitle)
        assertEquals("https://t.me/m/support_chat", linkWithTitle.fullUrl)
        assertEquals(42, linkWithTitle.views)
        assertTrue(linkWithTitle.hasEntities)

        val linkWithoutTitle = BusinessLinkModel(
            link = "https://t.me/m/faq",
            slug = "faq",
            title = null,
            message = "FAQ",
            views = 10
        )

        assertEquals("faq", linkWithoutTitle.displayTitle)
        assertEquals("https://t.me/m/faq", linkWithoutTitle.fullUrl)
    }

    @Test
    fun testBusinessLinksStateModel() {
        val state = BusinessLinksStateModel(
            links = listOf(
                BusinessLinkModel("t.me/m/1", "1"),
                BusinessLinkModel("t.me/m/2", "2")
            ),
            maxLimit = 3,
            isLoading = false
        )

        assertTrue(state.canAddNew)

        val fullState = state.copy(
            links = listOf(
                BusinessLinkModel("t.me/m/1", "1"),
                BusinessLinkModel("t.me/m/2", "2"),
                BusinessLinkModel("t.me/m/3", "3")
            )
        )
        assertFalse(fullState.canAddNew)
    }

    @Test
    fun testBusinessLinkMapper() {
        assertEquals("t.me/m/test", BusinessLinkMapper.stripHttps("https://t.me/m/test"))
        assertEquals("t.me/m/test", BusinessLinkMapper.stripHttps("http://t.me/m/test"))
        assertEquals("t.me/m/test", BusinessLinkMapper.stripHttps("t.me/m/test"))

        assertEquals("test_slug", BusinessLinkMapper.extractSlug("https://t.me/m/test_slug"))
        assertEquals("test_slug", BusinessLinkMapper.extractSlug("t.me/m/test_slug"))
        assertEquals("direct_slug", BusinessLinkMapper.extractSlug("tg://message?slug=direct_slug"))
        assertEquals("raw_slug", BusinessLinkMapper.extractSlug("raw_slug"))

        assertNull(BusinessLinkMapper.mapLink(null))

        val tlLink = TL_account.TL_businessChatLink().apply {
            link = "https://t.me/m/order_now"
            title = "Order Now"
            message = "I would like to place an order."
            views = 128
        }

        val mapped = BusinessLinkMapper.mapLink(tlLink)
        assertNotNull(mapped)
        assertEquals("order_now", mapped!!.slug)
        assertEquals("Order Now", mapped.title)
        assertEquals("I would like to place an order.", mapped.message)
        assertEquals(128, mapped.views)
        assertFalse(mapped.hasEntities)

        val emptyInput = BusinessLinkMapper.toInputLink(null)
        assertEquals("", emptyInput.message)
        assertEquals(0, emptyInput.flags)

        val customInput = BusinessLinkMapper.toInputLink(
            BusinessLinkInputModel(title = "Promo", message = "Special discount!")
        )
        assertEquals("Promo", customInput.title)
        assertEquals("Special discount!", customInput.message)
        assertTrue(customInput.flags and 2 != 0)
    }

    @Test
    fun testUseCasesOperations() = runTest {
        val fakeRepo = FakeBusinessLinksRepository()
        val observeUseCase = ObserveBusinessLinksUseCase(fakeRepo)
        val getUseCase = GetBusinessLinksUseCase(fakeRepo)
        val loadUseCase = LoadBusinessLinksUseCase(fakeRepo)
        val createUseCase = CreateBusinessLinkUseCase(fakeRepo)
        val editUseCase = EditBusinessLinkUseCase(fakeRepo)
        val deleteUseCase = DeleteBusinessLinkUseCase(fakeRepo)
        val findUseCase = FindBusinessLinkUseCase(fakeRepo)
        val canAddNewUseCase = CanAddNewBusinessLinkUseCase(fakeRepo)

        assertTrue(canAddNewUseCase())
        assertTrue(getUseCase().isEmpty())

        val createResult = createUseCase(BusinessLinkInputModel(title = "Welcome", message = "Hi!"))
        assertTrue(createResult is Result.Success)
        val created = (createResult as Result.Success).data
        assertEquals("slug_1", created.slug)
        assertEquals("Welcome", created.title)

        assertEquals(1, getUseCase().size)
        assertEquals(created, findUseCase("slug_1"))
        assertNull(findUseCase("non_existent"))

        val editResult = editUseCase("slug_1", "Updated Welcome", "Hi there!")
        assertTrue(editResult is Result.Success)
        val edited = (editResult as Result.Success).data
        assertEquals("Updated Welcome", edited.title)
        assertEquals("Hi there!", edited.message)

        val loadResult = loadUseCase(forceReload = true)
        assertTrue(loadResult is Result.Success)
        assertTrue(fakeRepo.lastLoadedForce)

        val deleteResult = deleteUseCase("slug_1")
        assertTrue(deleteResult is Result.Success)
        assertTrue(getUseCase().isEmpty())
        assertNull(findUseCase("slug_1"))

        fakeRepo.shouldFail = true
        val failCreate = createUseCase(BusinessLinkInputModel(title = "Fail", message = "Fail"))
        assertTrue(failCreate is Result.Failure)
        val failEdit = editUseCase("slug_1", "New", "New")
        assertTrue(failEdit is Result.Failure)
        val failDelete = deleteUseCase("slug_1")
        assertTrue(failDelete is Result.Failure)
        val failLoad = loadUseCase()
        assertTrue(failLoad is Result.Failure)
    }

    @Test
    fun testBusinessLinksViewModelFlow() = runTest {
        val fakeRepo = FakeBusinessLinksRepository()
        val viewModel = BusinessLinksViewModel(
            observeBusinessLinksUseCase = ObserveBusinessLinksUseCase(fakeRepo),
            loadBusinessLinksUseCase = LoadBusinessLinksUseCase(fakeRepo),
            createBusinessLinkUseCase = CreateBusinessLinkUseCase(fakeRepo),
            editBusinessLinkUseCase = EditBusinessLinkUseCase(fakeRepo),
            deleteBusinessLinkUseCase = DeleteBusinessLinkUseCase(fakeRepo),
            canAddNewBusinessLinkUseCase = CanAddNewBusinessLinkUseCase(fakeRepo)
        )

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.links.isEmpty())
        assertTrue(viewModel.uiState.value.canAddNew)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onEvent(BusinessLinksEvent.CreateLink(BusinessLinkInputModel("Shop", "Browse items")))
        advanceUntilIdle()

        val stateAfterCreate = viewModel.uiState.value
        assertEquals(1, stateAfterCreate.links.size)
        assertEquals("Shop", stateAfterCreate.links[0].title)
        assertNotNull(stateAfterCreate.actionSuccessMessage)
        assertFalse(stateAfterCreate.isProcessing)

        viewModel.onEvent(BusinessLinksEvent.ClearMessages)
        assertNull(viewModel.uiState.value.actionSuccessMessage)

        val slug = stateAfterCreate.links[0].slug
        viewModel.onEvent(BusinessLinksEvent.EditLink(slug, "Shop Online", "Browse catalog"))
        advanceUntilIdle()

        val stateAfterEdit = viewModel.uiState.value
        assertEquals("Shop Online", stateAfterEdit.links[0].title)
        assertEquals("Browse catalog", stateAfterEdit.links[0].message)

        viewModel.onEvent(BusinessLinksEvent.Load(forceReload = true))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onEvent(BusinessLinksEvent.DeleteLink(slug))
        advanceUntilIdle()

        val stateAfterDelete = viewModel.uiState.value
        assertTrue(stateAfterDelete.links.isEmpty())
        assertEquals("Business link deleted", stateAfterDelete.actionSuccessMessage)

        fakeRepo.shouldFail = true
        viewModel.onEvent(BusinessLinksEvent.CreateLink(BusinessLinkInputModel("Fail", "Fail")))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        val fakeRepo = FakeBusinessLinksRepository()
        container.businessLinksRepository = fakeRepo

        val vm = container.createBusinessLinksViewModel()
        assertNotNull(vm)
        assertTrue(container.canAddNewBusinessLinkUseCase())
        assertEquals(0, container.getBusinessLinksUseCase().size)
    }
}
