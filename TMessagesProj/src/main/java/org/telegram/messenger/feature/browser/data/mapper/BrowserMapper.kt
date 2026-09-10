package org.telegram.messenger.feature.browser.data.mapper

import org.telegram.messenger.feature.browser.domain.model.BrowserHistoryEntryModel
import org.telegram.messenger.feature.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.browser.domain.model.BrowserType
import org.telegram.ui.web.BrowserHistory

object BrowserMapper {

    fun mapHistoryEntry(entry: BrowserHistory.Entry): BrowserHistoryEntryModel {
        val title = entry.meta?.title
        val domain = entry.meta?.domain
        val siteName = entry.meta?.sitename
        val hasFavicon = entry.meta?.favicon != null || entry.meta?.faviconBytes != null
        return BrowserHistoryEntryModel(
            id = entry.id,
            timeMs = entry.time,
            url = entry.url ?: "",
            title = title,
            domain = domain,
            siteName = siteName,
            hasFavicon = hasFavicon
        )
    }

    fun mapToLegacyEntry(model: BrowserHistoryEntryModel): BrowserHistory.Entry {
        val entry = BrowserHistory.Entry()
        entry.id = model.id
        entry.time = model.timeMs
        entry.url = model.url
        return entry
    }

    fun resolveBrowserType(useInApp: Boolean, useCustomTabs: Boolean): BrowserType {
        return when {
            !useInApp && !useCustomTabs -> BrowserType.EXTERNAL_BROWSER
            useCustomTabs -> BrowserType.CUSTOM_TABS
            else -> BrowserType.IN_APP
        }
    }

    fun mapSettingsToFlags(type: BrowserType): Pair<Boolean, Boolean> {
        return when (type) {
            BrowserType.IN_APP -> Pair(true, false)
            BrowserType.CUSTOM_TABS -> Pair(true, true)
            BrowserType.EXTERNAL_BROWSER -> Pair(false, false)
        }
    }
}
