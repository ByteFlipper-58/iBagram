package org.telegram.messenger.feature.drafts.domain.model

/**
 * Type of multimedia or story draft.
 */
enum class DraftType(val code: Int) {
    NEW(0),
    EDIT(1),
    FAILED(2);

    companion object {
        fun fromCode(code: Int): DraftType = values().firstOrNull { it.code == code } ?: NEW
    }
}
