package org.telegram.messenger.feature.social.di

import java.util.concurrent.ConcurrentHashMap
import org.telegram.messenger.feature.social.birthdays.data.repository.LegacyBirthdaysRepository
import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository
import org.telegram.messenger.feature.social.birthdays.domain.usecase.CheckBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.GetBirthdaysStateUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.HasBirthdaysTodayUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.HideTodayBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.IsBirthdayTodayUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.ObserveBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.presentation.BirthdaysViewModel
import org.telegram.messenger.feature.social.boosts.data.repository.LegacyBoostsRepository
import org.telegram.messenger.feature.social.boosts.domain.repository.BoostsRepository
import org.telegram.messenger.feature.social.boosts.domain.usecase.ApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.CheckCanApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetBoostsStatusUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetMyBoostsUseCase
import org.telegram.messenger.feature.social.boosts.presentation.BoostsViewModel
import org.telegram.messenger.feature.social.contacts.data.repository.LegacyContactsRepository
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository
import org.telegram.messenger.feature.social.contacts.domain.usecase.AddContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.DeleteContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.ObserveContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.SearchContactsUseCase
import org.telegram.messenger.feature.social.contacts.presentation.ContactsViewModel
import org.telegram.messenger.feature.social.joinrequests.data.repository.LegacyJoinRequestsRepository
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ApproveAllJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ApproveJoinRequestUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.DismissAllJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.DismissJoinRequestUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.GetCachedJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.GetPendingRequestsCountUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.LoadJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ObservePendingRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.presentation.JoinRequestsViewModel
import org.telegram.messenger.feature.social.location.data.repository.LegacyLocationRepository
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository
import org.telegram.messenger.feature.social.location.domain.usecase.GetActiveSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.GetLastKnownLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.GetSharingInfoUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.IsSharingLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.LoadPeerLiveLocationsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.MarkLiveLocationsAsReadUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObserveActiveSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObserveLastKnownLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObservePeerLocationsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SendLiveLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SendStaticLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SetProximityAlertUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.StopAllLocationSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.StopLocationSharingUseCase
import org.telegram.messenger.feature.social.location.presentation.LocationViewModel
import org.telegram.messenger.feature.social.profile.data.repository.LegacyProfileRepository
import org.telegram.messenger.feature.social.profile.domain.repository.ProfileRepository
import org.telegram.messenger.feature.social.profile.domain.usecase.BlockPeerUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.GetProfileUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.LoadFullProfileUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.ObserveProfileUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.UnblockPeerUseCase
import org.telegram.messenger.feature.social.profile.presentation.ProfileViewModel

class SocialContainer(val account: Int) {

    private var customProfileRepository: ProfileRepository? = null

    var profileRepository: ProfileRepository
        get() = customProfileRepository ?: LegacyProfileRepository(account)
        set(value) {
            customProfileRepository = value
        }

    val observeProfileUseCase: ObserveProfileUseCase
        get() = ObserveProfileUseCase(profileRepository)

    val getProfileUseCase: GetProfileUseCase
        get() = GetProfileUseCase(profileRepository)

    val loadFullProfileUseCase: LoadFullProfileUseCase
        get() = LoadFullProfileUseCase(profileRepository)

    val blockPeerUseCase: BlockPeerUseCase
        get() = BlockPeerUseCase(profileRepository)

    val unblockPeerUseCase: UnblockPeerUseCase
        get() = UnblockPeerUseCase(profileRepository)

    private val cachedProfileViewModels = ConcurrentHashMap<Long, ProfileViewModel>()

    fun getProfileViewModel(peerId: Long): ProfileViewModel {
        return cachedProfileViewModels.computeIfAbsent(peerId) { createProfileViewModel(it) }
    }

    fun createProfileViewModel(peerId: Long): ProfileViewModel {
        return ProfileViewModel(
            account = account,
            peerId = peerId,
            observeProfileUseCase = observeProfileUseCase,
            loadFullProfileUseCase = loadFullProfileUseCase,
            blockPeerUseCase = blockPeerUseCase,
            unblockPeerUseCase = unblockPeerUseCase
        )
    }

    private var customContactsRepository: ContactsRepository? = null

    var contactsRepository: ContactsRepository
        get() = customContactsRepository ?: LegacyContactsRepository(account)
        set(value) {
            customContactsRepository = value
        }

    val observeContactsUseCase: ObserveContactsUseCase
        get() = ObserveContactsUseCase(contactsRepository)

    val getContactsUseCase: GetContactsUseCase
        get() = GetContactsUseCase(contactsRepository)

    val getContactUseCase: GetContactUseCase
        get() = GetContactUseCase(contactsRepository)

    val addContactUseCase: AddContactUseCase
        get() = AddContactUseCase(contactsRepository)

    val deleteContactUseCase: DeleteContactUseCase
        get() = DeleteContactUseCase(contactsRepository)

    val searchContactsUseCase: SearchContactsUseCase
        get() = SearchContactsUseCase(contactsRepository)

    private var cachedContactsViewModel: ContactsViewModel? = null

    val contactsViewModel: ContactsViewModel
        get() {
            var vm = cachedContactsViewModel
            if (vm == null) {
                vm = createContactsViewModel()
                cachedContactsViewModel = vm
            }
            return vm
        }

    fun createContactsViewModel(): ContactsViewModel {
        return ContactsViewModel(
            observeContactsUseCase = observeContactsUseCase,
            getContactsUseCase = getContactsUseCase,
            getContactUseCase = getContactUseCase,
            addContactUseCase = addContactUseCase,
            deleteContactUseCase = deleteContactUseCase,
            searchContactsUseCase = searchContactsUseCase
        )
    }

    private var customLocationRepository: LocationRepository? = null

    var locationRepository: LocationRepository
        get() = customLocationRepository ?: LegacyLocationRepository(account)
        set(value) {
            customLocationRepository = value
        }

    val observeActiveSharingsUseCase: ObserveActiveSharingsUseCase
        get() = ObserveActiveSharingsUseCase(locationRepository)

    val observePeerLocationsUseCase: ObservePeerLocationsUseCase
        get() = ObservePeerLocationsUseCase(locationRepository)

    val observeLastKnownLocationUseCase: ObserveLastKnownLocationUseCase
        get() = ObserveLastKnownLocationUseCase(locationRepository)

    val getActiveSharingsUseCase: GetActiveSharingsUseCase
        get() = GetActiveSharingsUseCase(locationRepository)

    val isSharingLocationUseCase: IsSharingLocationUseCase
        get() = IsSharingLocationUseCase(locationRepository)

    val getSharingInfoUseCase: GetSharingInfoUseCase
        get() = GetSharingInfoUseCase(locationRepository)

    val getLastKnownLocationUseCase: GetLastKnownLocationUseCase
        get() = GetLastKnownLocationUseCase(locationRepository)

    val loadPeerLiveLocationsUseCase: LoadPeerLiveLocationsUseCase
        get() = LoadPeerLiveLocationsUseCase(locationRepository)

    val stopLocationSharingUseCase: StopLocationSharingUseCase
        get() = StopLocationSharingUseCase(locationRepository)

    val stopAllLocationSharingsUseCase: StopAllLocationSharingsUseCase
        get() = StopAllLocationSharingsUseCase(locationRepository)

    val setProximityAlertUseCase: SetProximityAlertUseCase
        get() = SetProximityAlertUseCase(locationRepository)

    val sendStaticLocationUseCase: SendStaticLocationUseCase
        get() = SendStaticLocationUseCase(locationRepository)

    val sendLiveLocationUseCase: SendLiveLocationUseCase
        get() = SendLiveLocationUseCase(locationRepository)

    val markLiveLocationsAsReadUseCase: MarkLiveLocationsAsReadUseCase
        get() = MarkLiveLocationsAsReadUseCase(locationRepository)

    private var cachedLocationViewModel: LocationViewModel? = null

    val locationViewModel: LocationViewModel
        get() {
            var vm = cachedLocationViewModel
            if (vm == null) {
                vm = createLocationViewModel()
                cachedLocationViewModel = vm
            }
            return vm
        }

    fun createLocationViewModel(): LocationViewModel {
        return LocationViewModel(
            observeActiveSharingsUseCase = observeActiveSharingsUseCase,
            observePeerLocationsUseCase = observePeerLocationsUseCase,
            observeLastKnownLocationUseCase = observeLastKnownLocationUseCase,
            getActiveSharingsUseCase = getActiveSharingsUseCase,
            isSharingLocationUseCase = isSharingLocationUseCase,
            getSharingInfoUseCase = getSharingInfoUseCase,
            getLastKnownLocationUseCase = getLastKnownLocationUseCase,
            loadPeerLiveLocationsUseCase = loadPeerLiveLocationsUseCase,
            stopLocationSharingUseCase = stopLocationSharingUseCase,
            stopAllLocationSharingsUseCase = stopAllLocationSharingsUseCase,
            setProximityAlertUseCase = setProximityAlertUseCase,
            sendStaticLocationUseCase = sendStaticLocationUseCase,
            sendLiveLocationUseCase = sendLiveLocationUseCase,
            markLiveLocationsAsReadUseCase = markLiveLocationsAsReadUseCase
        )
    }

    private var customBoostsRepository: BoostsRepository? = null

    var boostsRepository: BoostsRepository
        get() = customBoostsRepository ?: LegacyBoostsRepository(account)
        set(value) {
            customBoostsRepository = value
        }

    val getBoostsStatusUseCase: GetBoostsStatusUseCase
        get() = GetBoostsStatusUseCase(boostsRepository)

    val getMyBoostsUseCase: GetMyBoostsUseCase
        get() = GetMyBoostsUseCase(boostsRepository)

    val checkCanApplyBoostUseCase: CheckCanApplyBoostUseCase
        get() = CheckCanApplyBoostUseCase(boostsRepository)

    val applyBoostUseCase: ApplyBoostUseCase
        get() = ApplyBoostUseCase(boostsRepository)

    private var cachedBoostsViewModel: BoostsViewModel? = null

    val boostsViewModel: BoostsViewModel
        get() {
            var vm = cachedBoostsViewModel
            if (vm == null) {
                vm = createBoostsViewModel()
                cachedBoostsViewModel = vm
            }
            return vm
        }

    fun createBoostsViewModel(): BoostsViewModel {
        return BoostsViewModel(
            getBoostsStatusUseCase = getBoostsStatusUseCase,
            getMyBoostsUseCase = getMyBoostsUseCase,
            checkCanApplyBoostUseCase = checkCanApplyBoostUseCase,
            applyBoostUseCase = applyBoostUseCase
        )
    }

    private var customJoinRequestsRepository: JoinRequestsRepository? = null

    var joinRequestsRepository: JoinRequestsRepository
        get() = customJoinRequestsRepository ?: LegacyJoinRequestsRepository(account)
        set(value) {
            customJoinRequestsRepository = value
        }

    val observePendingRequestsUseCase: ObservePendingRequestsUseCase
        get() = ObservePendingRequestsUseCase(joinRequestsRepository)

    val getPendingRequestsCountUseCase: GetPendingRequestsCountUseCase
        get() = GetPendingRequestsCountUseCase(joinRequestsRepository)

    val getCachedJoinRequestsUseCase: GetCachedJoinRequestsUseCase
        get() = GetCachedJoinRequestsUseCase(joinRequestsRepository)

    val loadJoinRequestsUseCase: LoadJoinRequestsUseCase
        get() = LoadJoinRequestsUseCase(joinRequestsRepository)

    val approveJoinRequestUseCase: ApproveJoinRequestUseCase
        get() = ApproveJoinRequestUseCase(joinRequestsRepository)

    val dismissJoinRequestUseCase: DismissJoinRequestUseCase
        get() = DismissJoinRequestUseCase(joinRequestsRepository)

    val approveAllJoinRequestsUseCase: ApproveAllJoinRequestsUseCase
        get() = ApproveAllJoinRequestsUseCase(joinRequestsRepository)

    val dismissAllJoinRequestsUseCase: DismissAllJoinRequestsUseCase
        get() = DismissAllJoinRequestsUseCase(joinRequestsRepository)

    private var cachedJoinRequestsViewModel: JoinRequestsViewModel? = null

    val joinRequestsViewModel: JoinRequestsViewModel
        get() {
            var vm = cachedJoinRequestsViewModel
            if (vm == null) {
                vm = createJoinRequestsViewModel()
                cachedJoinRequestsViewModel = vm
            }
            return vm
        }

    fun createJoinRequestsViewModel(): JoinRequestsViewModel {
        return JoinRequestsViewModel(
            observePendingRequestsUseCase = observePendingRequestsUseCase,
            getCachedJoinRequestsUseCase = getCachedJoinRequestsUseCase,
            loadJoinRequestsUseCase = loadJoinRequestsUseCase,
            approveJoinRequestUseCase = approveJoinRequestUseCase,
            dismissJoinRequestUseCase = dismissJoinRequestUseCase,
            approveAllJoinRequestsUseCase = approveAllJoinRequestsUseCase,
            dismissAllJoinRequestsUseCase = dismissAllJoinRequestsUseCase
        )
    }

    private var customBirthdaysRepository: BirthdaysRepository? = null

    var birthdaysRepository: BirthdaysRepository
        get() = customBirthdaysRepository ?: LegacyBirthdaysRepository(account)
        set(value) {
            customBirthdaysRepository = value
        }

    val observeBirthdaysUseCase: ObserveBirthdaysUseCase
        get() = ObserveBirthdaysUseCase(birthdaysRepository)

    val getBirthdaysStateUseCase: GetBirthdaysStateUseCase
        get() = GetBirthdaysStateUseCase(birthdaysRepository)

    val checkBirthdaysUseCase: CheckBirthdaysUseCase
        get() = CheckBirthdaysUseCase(birthdaysRepository)

    val hideTodayBirthdaysUseCase: HideTodayBirthdaysUseCase
        get() = HideTodayBirthdaysUseCase(birthdaysRepository)

    val isBirthdayTodayUseCase: IsBirthdayTodayUseCase
        get() = IsBirthdayTodayUseCase(birthdaysRepository)

    val hasBirthdaysTodayUseCase: HasBirthdaysTodayUseCase
        get() = HasBirthdaysTodayUseCase(birthdaysRepository)

    private var cachedBirthdaysViewModel: BirthdaysViewModel? = null

    val birthdaysViewModel: BirthdaysViewModel
        get() {
            var vm = cachedBirthdaysViewModel
            if (vm == null) {
                vm = createBirthdaysViewModel()
                cachedBirthdaysViewModel = vm
            }
            return vm
        }

    fun createBirthdaysViewModel(): BirthdaysViewModel {
        return BirthdaysViewModel(
            observeBirthdaysUseCase = observeBirthdaysUseCase,
            getBirthdaysStateUseCase = getBirthdaysStateUseCase,
            checkBirthdaysUseCase = checkBirthdaysUseCase,
            hideTodayBirthdaysUseCase = hideTodayBirthdaysUseCase,
            isBirthdayTodayUseCase = isBirthdayTodayUseCase,
            hasBirthdaysTodayUseCase = hasBirthdaysTodayUseCase
        )
    }

}
