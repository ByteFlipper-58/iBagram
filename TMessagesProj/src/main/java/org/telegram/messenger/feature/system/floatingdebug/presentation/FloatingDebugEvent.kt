package org.telegram.messenger.feature.system.floatingdebug.presentation

import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel

sealed class FloatingDebugEvent {
    data class SetActive(val active: Boolean, val saveConfig: Boolean = true) : FloatingDebugEvent()
    data class ToggleActive(val saveConfig: Boolean = true) : FloatingDebugEvent()
    data class RegisterItems(val items: List<DebugItemModel>) : FloatingDebugEvent()
    object ClearItems : FloatingDebugEvent()
    data class SetMenuOpen(val isOpen: Boolean) : FloatingDebugEvent()
}
