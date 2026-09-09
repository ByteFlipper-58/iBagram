package org.telegram.messenger.feature.businessbots.data.mapper

import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.businessbots.domain.model.ConnectedBotModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

object BusinessBotMapper {

    fun mapRights(rights: TL_account.TL_businessBotRights?): BusinessBotRightsModel {
        if (rights == null) return BusinessBotRightsModel.makeDefault()

        return BusinessBotRightsModel(
            reply = rights.reply,
            readMessages = rights.read_messages,
            deleteSentMessages = rights.delete_sent_messages,
            deleteReceivedMessages = rights.delete_received_messages,
            editName = rights.edit_name,
            editBio = rights.edit_bio,
            editProfilePhoto = rights.edit_profile_photo,
            editUsername = rights.edit_username,
            viewGifts = rights.view_gifts,
            sellGifts = rights.sell_gifts,
            changeGiftSettings = rights.change_gift_settings,
            transferAndUpgradeGifts = rights.transfer_and_upgrade_gifts,
            transferStars = rights.transfer_stars,
            manageStories = rights.manage_stories
        )
    }

    fun toTlRights(model: BusinessBotRightsModel): TL_account.TL_businessBotRights {
        val rights = TL_account.TL_businessBotRights()
        rights.reply = model.reply
        rights.read_messages = model.readMessages
        rights.delete_sent_messages = model.deleteSentMessages
        rights.delete_received_messages = model.deleteReceivedMessages
        rights.edit_name = model.editName
        rights.edit_bio = model.editBio
        rights.edit_profile_photo = model.editProfilePhoto
        rights.edit_username = model.editUsername
        rights.view_gifts = model.viewGifts
        rights.sell_gifts = model.sellGifts
        rights.change_gift_settings = model.changeGiftSettings
        rights.transfer_and_upgrade_gifts = model.transferAndUpgradeGifts
        rights.transfer_stars = model.transferStars
        rights.manage_stories = model.manageStories
        return rights
    }

    fun mapRecipients(recipients: TL_account.TL_businessBotRecipients?): BusinessBotRecipientsModel {
        if (recipients == null) return BusinessBotRecipientsModel()

        return BusinessBotRecipientsModel(
            excludeSelected = recipients.exclude_selected,
            users = recipients.users ?: emptyList(),
            existingChats = recipients.existing_chats,
            newChats = recipients.new_chats,
            contacts = recipients.contacts,
            nonContacts = recipients.non_contacts
        )
    }

    fun toTlInputRecipients(
        model: BusinessBotRecipientsModel,
        currentAccount: Int
    ): TL_account.TL_inputBusinessBotRecipients {
        val input = TL_account.TL_inputBusinessBotRecipients()
        input.exclude_selected = model.excludeSelected
        input.existing_chats = model.existingChats
        input.new_chats = model.newChats
        input.contacts = model.contacts
        input.non_contacts = model.nonContacts
        input.users = ArrayList()

        val messagesController = MessagesController.getInstance(currentAccount)
        for (userId in model.users) {
            val user = messagesController.getUser(userId)
            val inputUser = messagesController.getInputUser(user)
            if (inputUser != null) {
                input.users.add(inputUser)
            } else {
                val fallback = TLRPC.TL_inputUser()
                fallback.user_id = userId
                input.users.add(fallback)
            }
        }
        return input
    }

    fun toTlRecipients(model: BusinessBotRecipientsModel): TL_account.TL_businessBotRecipients {
        val result = TL_account.TL_businessBotRecipients()
        result.exclude_selected = model.excludeSelected
        result.existing_chats = model.existingChats
        result.new_chats = model.newChats
        result.contacts = model.contacts
        result.non_contacts = model.nonContacts
        result.users = ArrayList(model.users)
        return result
    }

    fun mapConnectedBot(bot: TL_account.TL_connectedBot?): ConnectedBotModel? {
        if (bot == null) return null

        return ConnectedBotModel(
            botId = bot.bot_id,
            recipients = mapRecipients(bot.recipients),
            rights = mapRights(bot.rights),
            device = bot.device,
            location = bot.location,
            date = bot.date
        )
    }

    fun mapConnectedBots(response: TL_account.connectedBots?): List<ConnectedBotModel> {
        if (response == null || response.connected_bots == null) return emptyList()
        return response.connected_bots.mapNotNull { mapConnectedBot(it) }
    }
}
