package org.telegram.messenger.feature.social.birthdays.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.BirthdayController
import org.telegram.messenger.BuildVars
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.tgnet.tl.TL_account
import java.util.Calendar

/**
 * Local data source managing birthday state, SharedPreferences caching,
 * and coordination with BirthdayController and MessagesController.
 */
open class BirthdayLocalDataSource(
    currentAccount: Int
) : BaseLocalDataSource(currentAccount) {

    private val birthdayController: BirthdayController?
        get() = try {
            BirthdayController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private val messagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    /**
     * Returns the raw cached BirthdayState without hidden day filtration.
     */
    open fun getRawBirthdaysState(): BirthdayController.BirthdayState? {
        val controller = birthdayController ?: return null
        return try {
            val field = BirthdayController::class.java.getDeclaredField("state")
            field.isAccessible = true
            field.get(controller) as? BirthdayController.BirthdayState
        } catch (e: Throwable) {
            controller.state
        }
    }

    /**
     * Returns the active BirthdayState filtered by hidden days.
     */
    open fun getBirthdaysState(): BirthdayController.BirthdayState? {
        return birthdayController?.state
    }

    /**
     * Determines whether birthday list should be refreshed from MTProto server.
     */
    open fun shouldCheckBirthdays(force: Boolean = false): Boolean {
        if (force) return true
        val controller = birthdayController ?: return true
        val lastCheckDate = controller.lastCheckDate
        if (lastCheckDate == 0L) return true

        val now = System.currentTimeMillis()
        val interval = if (BuildVars.DEBUG_PRIVATE_VERSION) 1000L * 25 else 1000L * 60 * 60 * 12
        if (now - lastCheckDate > interval) return true

        val checkDate = Calendar.getInstance().apply { timeInMillis = lastCheckDate }
        val nowDate = Calendar.getInstance().apply { timeInMillis = now }

        return (
            checkDate.get(Calendar.DAY_OF_MONTH) != nowDate.get(Calendar.DAY_OF_MONTH) ||
            checkDate.get(Calendar.MONTH) != nowDate.get(Calendar.MONTH) ||
            checkDate.get(Calendar.YEAR) != nowDate.get(Calendar.YEAR)
        )
    }

    /**
     * Saves remote contact birthdays response into legacy controller, caches, and SharedPreferences.
     */
    open suspend fun saveBirthdays(response: TL_account.contactBirthdays): Unit = withContext(Dispatchers.Main) {
        val controller = birthdayController ?: return@withContext
        controller.applyResponse(response)
    }

    /**
     * Hides today's birthday banner.
     */
    open suspend fun hideTodayBirthdays(): Unit = withContext(Dispatchers.Main) {
        birthdayController?.hide()
    }

    /**
     * Checks if a specific user has a birthday today.
     */
    open fun isToday(userId: Long): Boolean {
        return birthdayController?.isToday(userId) ?: false
    }

    /**
     * Checks if any contact has a birthday today.
     */
    open fun hasBirthdaysToday(): Boolean {
        return birthdayController?.contains() ?: false
    }
}
