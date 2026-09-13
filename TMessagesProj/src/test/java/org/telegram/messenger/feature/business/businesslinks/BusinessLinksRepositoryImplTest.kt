package org.telegram.messenger.feature.business.businesslinks

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businesslinks.data.datasource.BusinessLinksLocalDataSource
import org.telegram.messenger.feature.business.businesslinks.data.datasource.BusinessLinksRemoteDataSource
import org.telegram.messenger.feature.business.businesslinks.data.mapper.BusinessLinkMapper
import org.telegram.messenger.feature.business.businesslinks.data.repository.BusinessLinksRepositoryImpl
import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

class BusinessLinksRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeBusinessLinksRemoteDataSource(account: Int) : BusinessLinksRemoteDataSource(account) {
        var createResponse: TL_account.TL_businessChatLink? = null
        var editResponse: TL_account.TL_businessChatLink? = null
        var deleteSuccess: Boolean = true
        var shouldFail: Boolean = false

        override suspend fun createLink(inputLink: TL_account.TL_inputBusinessChatLink): Result<TL_account.TL_businessChatLink> {
            if (shouldFail) return Result.failure("Remote create failed")
            val link = createResponse ?: TL_account.TL_businessChatLink().apply {
                this.link = "https://t.me/m/new_slug"
                this.title = inputLink.title
                this.message = inputLink.message
                this.views = 0
                this.entities = inputLink.entities
            }
            return Result.success(link)
        }

        override suspend fun editLink(slug: String, inputLink: TL_account.TL_inputBusinessChatLink): Result<TL_account.TL_businessChatLink> {
            if (shouldFail) return Result.failure("Remote edit failed")
            val link = editResponse ?: TL_account.TL_businessChatLink().apply {
                this.link = "https://t.me/m/$slug"
                this.title = inputLink.title
                this.message = inputLink.message
                this.views = 5
                this.entities = inputLink.entities
            }
            return Result.success(link)
        }

        override suspend fun deleteLink(slug: String): Result<Unit> {
            if (shouldFail || !deleteSuccess) return Result.failure("Remote delete failed")
            return Result.success(Unit)
        }
    }

    private fun createSampleLink(slug: String, title: String?, message: String, views: Int): TL_account.TL_businessChatLink {
        return TL_account.TL_businessChatLink().apply {
            this.link = "https://t.me/m/$slug"
            this.title = title
            this.message = message
            this.views = views
            this.entities = ArrayList()
        }
    }

    @Test
    fun testGetBusinessLinksFromLocalCache() = runTest(testDispatcher) {
        val local = BusinessLinksLocalDataSource(0)
        val remote = FakeBusinessLinksRemoteDataSource(0)
        val repo = BusinessLinksRepositoryImpl(0, local, remote, testDispatcher)

        local.setTestLinks(listOf(
            createSampleLink("promo1", "Summer Promo", "Hello from summer promo", 42),
            createSampleLink("support", null, "Need help with order", 10)
        ))

        val links = repo.getBusinessLinks()
        assertEquals(2, links.size)
        assertEquals("promo1", links[0].slug)
        assertEquals("Summer Promo", links[0].title)
        assertEquals("Hello from summer promo", links[0].message)
        assertEquals(42, links[0].views)
        assertEquals("support", links[1].slug)
        assertNull(links[1].title)
    }

    @Test
    fun testCreateBusinessLink() = runTest(testDispatcher) {
        val local = BusinessLinksLocalDataSource(0)
        val remote = FakeBusinessLinksRemoteDataSource(0)
        val repo = BusinessLinksRepositoryImpl(0, local, remote, testDispatcher)

        val input = BusinessLinkInputModel(title = "VIP Club", message = "Join VIP")
        val result = repo.createLink(input)
        assertTrue(result is Result.Success)
        val created = (result as Result.Success).data
        assertEquals("new_slug", created.slug)
        assertEquals("VIP Club", created.title)
        assertEquals("Join VIP", created.message)

        // Verify it was added to local cache
        assertEquals(1, local.getLinks().size)
    }

    @Test
    fun testEditBusinessLink() = runTest(testDispatcher) {
        val local = BusinessLinksLocalDataSource(0)
        val remote = FakeBusinessLinksRemoteDataSource(0)
        val repo = BusinessLinksRepositoryImpl(0, local, remote, testDispatcher)

        local.setTestLinks(listOf(
            createSampleLink("order_help", "Orders", "Old message", 1)
        ))

        val result = repo.editLink(
            slug = "order_help",
            title = "Orders Updated",
            message = "New message with instructions"
        )
        assertTrue(result is Result.Success)
        val edited = (result as Result.Success).data
        assertEquals("order_help", edited.slug)
        assertEquals("Orders Updated", edited.title)
        assertEquals("New message with instructions", edited.message)

        // Verify local was updated
        val found = repo.findLink("order_help")
        assertEquals("Orders Updated", found?.title)
    }

    @Test
    fun testDeleteBusinessLink() = runTest(testDispatcher) {
        val local = BusinessLinksLocalDataSource(0)
        val remote = FakeBusinessLinksRemoteDataSource(0)
        val repo = BusinessLinksRepositoryImpl(0, local, remote, testDispatcher)

        local.setTestLinks(listOf(
            createSampleLink("to_delete", "Old Link", "Bye", 0),
            createSampleLink("keep", "Keep Link", "Stay", 10)
        ))

        val result = repo.deleteLink("to_delete")
        assertTrue(result is Result.Success)
        assertEquals(1, local.getLinks().size)
        assertNull(repo.findLink("to_delete"))
        assertNotNull(repo.findLink("keep"))
    }

    @Test
    fun testCanAddNewAndLimit() = runTest(testDispatcher) {
        val local = BusinessLinksLocalDataSource(0)
        val remote = FakeBusinessLinksRemoteDataSource(0)
        val repo = BusinessLinksRepositoryImpl(0, local, remote, testDispatcher)

        local.setTestLimit(2)
        local.setTestLinks(listOf(
            createSampleLink("link1", null, "msg1", 0)
        ))

        assertTrue(repo.canAddNew())
        assertEquals(2, repo.getLinksLimit())

        local.addLink(createSampleLink("link2", null, "msg2", 0))
        assertFalse(repo.canAddNew())
    }

    @Test
    fun testObserveBusinessLinksEmitsCurrent() = runTest(testDispatcher) {
        val local = BusinessLinksLocalDataSource(0)
        local.setTestLinks(listOf(
            createSampleLink("flow_link", "Flow Title", "Flow Message", 99)
        ))
        val remote = FakeBusinessLinksRemoteDataSource(0)
        val repo = BusinessLinksRepositoryImpl(0, local, remote, testDispatcher)

        val emitted = repo.observeBusinessLinks().first()
        assertEquals(1, emitted.size)
        assertEquals("flow_link", emitted[0].slug)
        assertEquals(99, emitted[0].views)
    }

    @Test
    fun testMapperExtractSlug() {
        assertEquals("my_link", BusinessLinkMapper.extractSlug("https://t.me/m/my_link"))
        assertEquals("my_link", BusinessLinkMapper.extractSlug("http://t.me/m/my_link"))
        assertEquals("my_link", BusinessLinkMapper.extractSlug("tg://message?slug=my_link"))
        assertEquals("custom_slug", BusinessLinkMapper.extractSlug("custom_slug"))
    }
}
