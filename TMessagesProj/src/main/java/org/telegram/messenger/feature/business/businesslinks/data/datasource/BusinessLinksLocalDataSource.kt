package org.telegram.messenger.feature.business.businesslinks.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.feature.business.businesslinks.data.mapper.BusinessLinkMapper
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.Business.BusinessLinksController
import java.util.ArrayList

/**
 * Local data source managing cached business chat links and bridging to BusinessLinksController.
 */
open class BusinessLinksLocalDataSource(
    private val currentAccount: Int
) {
    private val testLinks = mutableListOf<TL_account.TL_businessChatLink>()
    private var testLimit: Int = 10

    open fun setTestLinks(list: List<TL_account.TL_businessChatLink>) {
        testLinks.clear()
        testLinks.addAll(list)
    }

    open fun setTestLimit(limit: Int) {
        testLimit = limit
    }

    open fun getLinks(): List<TL_account.TL_businessChatLink> {
        if (testLinks.isNotEmpty()) {
            return ArrayList(testLinks)
        }
        return try {
            val controller = BusinessLinksController.getInstance(currentAccount)
            controller.links ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun addLink(link: TL_account.TL_businessChatLink) {
        testLinks.add(link)
        try {
            val controller = BusinessLinksController.getInstance(currentAccount)
            controller.links.add(link)
        } catch (_: Throwable) {}
    }

    open fun updateLink(link: TL_account.TL_businessChatLink) {
        val slug = BusinessLinkMapper.extractSlug(link.link ?: "")
        val index = testLinks.indexOfFirst { BusinessLinkMapper.extractSlug(it.link ?: "") == slug }
        if (index != -1) {
            testLinks[index] = link
        }
        try {
            val controller = BusinessLinksController.getInstance(currentAccount)
            val existing = controller.findLink(slug)
            if (existing != null) {
                val idx = controller.links.indexOf(existing)
                if (idx != -1) {
                    controller.links[idx] = link
                }
            }
        } catch (_: Throwable) {}
    }

    open fun removeLink(slug: String) {
        testLinks.removeAll { BusinessLinkMapper.extractSlug(it.link ?: "") == slug }
        try {
            val controller = BusinessLinksController.getInstance(currentAccount)
            val existing = controller.findLink(slug)
            if (existing != null) {
                controller.links.remove(existing)
            }
        } catch (_: Throwable) {}
    }

    open fun findLink(slug: String): TL_account.TL_businessChatLink? {
        testLinks.find { BusinessLinkMapper.extractSlug(it.link ?: "") == slug }?.let { return it }
        return try {
            BusinessLinksController.getInstance(currentAccount).findLink(slug)
        } catch (_: Throwable) {
            null
        }
    }

    open fun canAddNew(): Boolean {
        return getLinks().size < getLinksLimit()
    }

    open fun getLinksLimit(): Int {
        return try {
            MessagesController.getInstance(currentAccount).businessChatLinksLimit
        } catch (_: Throwable) {
            testLimit
        }
    }

    open fun load(forceReload: Boolean) {
        try {
            BusinessLinksController.getInstance(currentAccount).load(forceReload)
        } catch (_: Throwable) {}
    }

    open fun observeBusinessLinksUpdated(): Flow<Unit> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.businessLinksUpdated).map { }
    }
}
