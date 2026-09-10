package org.telegram.messenger.feature.social.birthdays.data.mapper

import org.telegram.messenger.BirthdayController
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayDateModel
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayUserModel
import org.telegram.messenger.feature.social.birthdays.domain.model.ContactBirthdayModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

object BirthdayMapper {

    fun mapUser(user: TLRPC.User?): BirthdayUserModel? {
        if (user == null) return null
        return BirthdayUserModel(
            id = user.id,
            firstName = user.first_name.orEmpty(),
            lastName = user.last_name.orEmpty(),
            username = user.username
        )
    }

    fun mapBirthday(birthday: TL_account.TL_birthday?): BirthdayDateModel? {
        if (birthday == null) return null
        return BirthdayDateModel(
            day = birthday.day,
            month = birthday.month,
            year = if (birthday.year != 0) birthday.year else null
        )
    }

    fun mapContact(contact: TL_account.TL_contactBirthday?, user: TLRPC.User?): ContactBirthdayModel? {
        if (contact == null || contact.birthday == null) return null
        val bday = mapBirthday(contact.birthday) ?: return null
        return ContactBirthdayModel(
            contactId = contact.contact_id,
            birthday = bday,
            user = mapUser(user)
        )
    }

    fun mapState(state: BirthdayController.BirthdayState?): BirthdayStateModel? {
        if (state == null) return null
        return BirthdayStateModel(
            yesterdayKey = state.yesterdayKey.orEmpty(),
            todayKey = state.todayKey.orEmpty(),
            tomorrowKey = state.tomorrowKey.orEmpty(),
            yesterday = state.yesterday.mapNotNull { mapUser(it) },
            today = state.today.mapNotNull { mapUser(it) },
            tomorrow = state.tomorrow.mapNotNull { mapUser(it) }
        )
    }
}
