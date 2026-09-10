package org.telegram.messenger.feature.sharedmedia.domain.usecase

import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaTabType

/**
 * Юзкейс для расчёта доступных вкладок SharedMediaLayout на основе прав диалога и счётчиков медиа.
 */
class ResolveAvailableTabsUseCase {

    operator fun invoke(
        dialogId: Long,
        isEncrypted: Boolean,
        isGroupOrChannel: Boolean = false,
        hasMembersTab: Boolean = false,
        mediaCounts: Map<SharedMediaTabType, Int> = emptyMap(),
        hasStories: Boolean = false,
        hasBotPreviews: Boolean = false,
        hasGifts: Boolean = false,
        hasSavedMessages: Boolean = false,
        hasSavedDialogs: Boolean = false,
        hasRecommendations: Boolean = false
    ): List<SharedMediaTabSpec> {
        val tabs = mutableListOf<SharedMediaTabSpec>()

        // 1. Stories
        if (hasStories && !isEncrypted) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.STORIES, "Stories"))
        }

        // 2. Bot Previews
        if (hasBotPreviews && !isEncrypted) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.BOT_PREVIEWS, "Previews"))
        }

        // 3. Gifts
        if (hasGifts && !isEncrypted) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.GIFTS, "Gifts"))
        }

        // 4. Group Users / Members
        if (isGroupOrChannel && hasMembersTab && !isEncrypted) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.GROUP_USERS, "Members"))
        }

        // 5. Photos & Videos
        val photoCount = mediaCounts[SharedMediaTabType.PHOTO_VIDEO] ?: 0
        if (photoCount > 0) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.PHOTO_VIDEO, "Media", count = photoCount))
        }

        // 6. Files / Documents
        val filesCount = mediaCounts[SharedMediaTabType.FILES] ?: 0
        if (filesCount > 0) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.FILES, "Files", count = filesCount))
        }

        // 7. Links (только для обычных чатов, не секретных)
        if (!isEncrypted) {
            val linksCount = mediaCounts[SharedMediaTabType.LINKS] ?: 0
            if (linksCount > 0) {
                tabs.add(SharedMediaTabSpec(SharedMediaTabType.LINKS, "Links", count = linksCount))
            }
        }

        // 8. Music / Audio
        val audioCount = mediaCounts[SharedMediaTabType.AUDIO] ?: 0
        if (audioCount > 0) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.AUDIO, "Music", count = audioCount))
        }

        // 9. Voice Messages
        val voiceCount = mediaCounts[SharedMediaTabType.VOICE] ?: 0
        if (voiceCount > 0) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.VOICE, "Voice", count = voiceCount))
        }

        // 10. GIFs
        val gifCount = mediaCounts[SharedMediaTabType.GIF] ?: 0
        if (gifCount > 0) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.GIF, "GIFs", count = gifCount))
        }

        // 11. Polls (не для секретных)
        if (!isEncrypted) {
            val pollsCount = mediaCounts[SharedMediaTabType.POLLS] ?: 0
            if (pollsCount > 0) {
                tabs.add(SharedMediaTabSpec(SharedMediaTabType.POLLS, "Polls", count = pollsCount))
            }
        }

        // 12. Groups in common
        val commonGroupsCount = mediaCounts[SharedMediaTabType.COMMON_GROUPS] ?: 0
        if (commonGroupsCount > 0 && !isEncrypted) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.COMMON_GROUPS, "Groups", count = commonGroupsCount))
        }

        // 13. Channel recommendations
        if (hasRecommendations && !isEncrypted) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.RECOMMENDED_CHANNELS, "Similar Channels"))
        }

        // 14. Saved Dialogs
        if (hasSavedDialogs) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.SAVED_DIALOGS, "Dialogs"))
        }

        // 15. Saved Messages
        if (hasSavedMessages) {
            tabs.add(SharedMediaTabSpec(SharedMediaTabType.SAVED_MESSAGES, "Saved Messages"))
        }

        return tabs
    }
}
