package org.telegram.messenger.feature.businessbots.domain.model

data class BusinessBotRightsModel(
    val reply: Boolean = true,
    val readMessages: Boolean = true,
    val deleteSentMessages: Boolean = true,
    val deleteReceivedMessages: Boolean = true,
    val editName: Boolean = false,
    val editBio: Boolean = false,
    val editProfilePhoto: Boolean = false,
    val editUsername: Boolean = false,
    val viewGifts: Boolean = false,
    val sellGifts: Boolean = false,
    val changeGiftSettings: Boolean = false,
    val transferAndUpgradeGifts: Boolean = false,
    val transferStars: Boolean = false,
    val manageStories: Boolean = false
) {
    companion object {
        fun makeDefault(): BusinessBotRightsModel = BusinessBotRightsModel()

        fun all(): BusinessBotRightsModel = BusinessBotRightsModel(
            reply = true,
            readMessages = true,
            deleteSentMessages = true,
            deleteReceivedMessages = true,
            editName = true,
            editBio = true,
            editProfilePhoto = true,
            editUsername = true,
            viewGifts = true,
            sellGifts = true,
            changeGiftSettings = true,
            transferAndUpgradeGifts = true,
            transferStars = true,
            manageStories = true
        )
    }
}
