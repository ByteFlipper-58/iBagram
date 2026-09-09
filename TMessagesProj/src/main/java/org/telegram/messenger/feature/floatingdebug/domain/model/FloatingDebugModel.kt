package org.telegram.messenger.feature.floatingdebug.domain.model

enum class DebugItemKind {
    SIMPLE,
    HEADER,
    SEEKBAR
}

data class DebugItemModel(
    val title: String,
    val kind: DebugItemKind = DebugItemKind.SIMPLE,
    val action: (() -> Unit)? = null,
    val from: Float = 0f,
    val to: Float = 0f,
    val currentValue: Float = 0f
)

data class FloatingDebugState(
    val isActive: Boolean = false,
    val items: List<DebugItemModel> = emptyList()
) {
    val itemsCount: Int get() = items.size

    companion object {
        val DEFAULT = FloatingDebugState(isActive = false, items = emptyList())
    }
}
