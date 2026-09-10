package org.telegram.messenger.feature.media.contentpreview.domain.usecase

import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionType
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewContentType

/**
 * Юзкейс для формирования набора доступных действий меню в зависимости от типа контента.
 */
class ResolvePreviewActionsUseCase {

    operator fun invoke(item: ContentPreviewItem): List<PreviewActionItem> {
        val actions = mutableListOf<PreviewActionItem>()

        when (item.contentType) {
            PreviewContentType.STICKER, PreviewContentType.CUSTOM_STICKER -> {
                if (item.canSend) {
                    actions.add(PreviewActionItem(PreviewActionType.SEND, "Send"))
                    actions.add(PreviewActionItem(PreviewActionType.SEND_WITHOUT_SOUND, "Send Without Sound"))
                }
                if (item.canSchedule) {
                    actions.add(PreviewActionItem(PreviewActionType.SCHEDULE, "Schedule"))
                }
                val favTitle = if (item.isFavorite) "Remove from Favorites" else "Add to Favorites"
                actions.add(PreviewActionItem(PreviewActionType.TOGGLE_FAVORITE, favTitle))

                if (item.isRecent) {
                    actions.add(PreviewActionItem(PreviewActionType.REMOVE_FROM_RECENT, "Remove from Recent", isDestructive = true))
                }
                actions.add(PreviewActionItem(PreviewActionType.VIEW_PACK, "View Pack"))

                if (item.canEdit) {
                    actions.add(PreviewActionItem(PreviewActionType.EDIT_STICKER, "Edit"))
                }
                if (item.canDelete) {
                    actions.add(PreviewActionItem(PreviewActionType.DELETE_STICKER, "Delete", isDestructive = true))
                }
            }

            PreviewContentType.GIF -> {
                if (item.canSend) {
                    actions.add(PreviewActionItem(PreviewActionType.SEND, "Send"))
                    actions.add(PreviewActionItem(PreviewActionType.SEND_WITHOUT_SOUND, "Send Without Sound"))
                }
                if (item.canSchedule) {
                    actions.add(PreviewActionItem(PreviewActionType.SCHEDULE, "Schedule"))
                }
                if (item.canAddCaption) {
                    actions.add(PreviewActionItem(PreviewActionType.ADD_CAPTION, "Add Caption"))
                }
                val favTitle = if (item.isFavorite) "Remove from Saved" else "Save GIF"
                actions.add(PreviewActionItem(PreviewActionType.TOGGLE_FAVORITE, favTitle))
            }

            PreviewContentType.EMOJI -> {
                if (item.canSend) {
                    actions.add(PreviewActionItem(PreviewActionType.SEND, "Send"))
                }
                if (item.canSetStatus) {
                    actions.add(PreviewActionItem(PreviewActionType.SET_EMOJI_STATUS, "Set as Status"))
                }
                if (item.canCopy) {
                    actions.add(PreviewActionItem(PreviewActionType.COPY_EMOJI, "Copy"))
                }
            }

            PreviewContentType.NONE -> {
                // Нет доступных действий
            }
        }

        return actions
    }
}
