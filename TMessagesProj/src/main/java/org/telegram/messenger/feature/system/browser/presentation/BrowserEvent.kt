package org.telegram.messenger.feature.system.browser.presentation

import org.telegram.messenger.feature.system.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.system.browser.domain.model.BrowserType

sealed class BrowserEvent {
    data class SetBrowserType(val type: BrowserType) : BrowserEvent()
    data class UpdateSettings(val settings: BrowserSettingsModel) : BrowserEvent()
    data class ToggleWarnExternalLinks(val enabled: Boolean) : BrowserEvent()
    data class CheckUrl(val url: String) : BrowserEvent()
    data class OpenUrl @JvmOverloads constructor(val url: String, val forceExternal: Boolean = false) : BrowserEvent()
    object ClearHistory : BrowserEvent()
    object ClearCacheAndCookies : BrowserEvent()
    object DismissError : BrowserEvent()
}
