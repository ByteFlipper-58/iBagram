package org.telegram.messenger.feature.business.businesslinks.data.mapper

import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkModel
import org.telegram.tgnet.tl.TL_account

object BusinessLinkMapper {

    fun stripHttps(link: String): String {
        return if (link.startsWith("https://")) {
            link.substring(8)
        } else if (link.startsWith("http://")) {
            link.substring(7)
        } else {
            link
        }
    }

    fun extractSlug(link: String): String {
        val clean = stripHttps(link)
        return when {
            clean.startsWith("t.me/m/") -> clean.removePrefix("t.me/m/")
            clean.startsWith("tg://message?slug=") -> clean.removePrefix("tg://message?slug=")
            else -> clean
        }
    }

    fun mapLink(link: TL_account.TL_businessChatLink?): BusinessLinkModel? {
        if (link == null) return null

        val rawLink = link.link ?: ""
        val slug = extractSlug(rawLink)

        return BusinessLinkModel(
            link = rawLink,
            slug = slug,
            title = link.title,
            message = link.message ?: "",
            views = link.views,
            hasEntities = !link.entities.isNullOrEmpty()
        )
    }

    fun toInputLink(input: BusinessLinkInputModel?): TL_account.TL_inputBusinessChatLink {
        val inputLink = TL_account.TL_inputBusinessChatLink()
        if (input == null) {
            inputLink.message = ""
            return inputLink
        }

        inputLink.message = input.message
        if (!input.title.isNullOrBlank()) {
            inputLink.title = input.title
            inputLink.flags = inputLink.flags or 2
        }
        return inputLink
    }
}
