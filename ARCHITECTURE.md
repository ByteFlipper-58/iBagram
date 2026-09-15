# Telegram Android Client — Architectural Guide & Migration Strategy

> **Status:** Active Living Document  
> **Approach:** Strangler Fig Pattern, Incremental Legacy Modularization, Upstream Synchronization First

---

## 1. Executive Summary & Core Principles

This repository is a long-lived fork of the official Telegram Android client (`iBagram`).
The primary objective is **not** to rewrite Telegram from scratch, but to gradually transform the existing codebase from a tightly coupled legacy monolithic architecture into a modular, maintainable architecture while strictly preserving compatibility with upstream Telegram updates.

### Fundamental Rules
1. **Upstream compatibility is a first-class requirement:** Official Telegram updates must periodically be merged into this repository. Avoid modifying upstream files unnecessarily. If a feature or fix can be achieved via an adapter, wrapper, interface, repository, mapper, or facade, that approach is strictly preferred.
2. **Strangler Fig Pattern:** Do not rewrite God objects wholesale. Introduce clean domain/repository boundaries around legacy components first, redirect callers to those boundaries, and gradually replace legacy internals behind the boundary.
3. **Domain Independence:** The domain layer must never know about Android UI, Activities, Fragments, Views, `MessagesController`, `MessagesStorage`, `NotificationCenter`, or raw network/database classes.
4. **No Useless Abstractions:** Every interface and abstraction must have a concrete architectural justification (e.g. isolating legacy code, protecting upstream sync, enabling unit testing, or decoupling presentation from data).
5. **Continuous Buildability:** The project must remain buildable, runnable, and functionally identical at every step of migration.

---

## 2. Architecture: Current vs Target

### Legacy Architecture (Current State)
Historically, Telegram Android combines UI, business logic, global state, persistence, and networking in giant singleton controllers and custom views:

```text
┌─────────────────────────────────────────────────────────────┐
│                      Legacy UI Layer                        │
│   (Activities, BaseFragments, Custom Views, DialogCell)     │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│      MessagesController      │ │     NotificationCenter     │
│ (God object: state, sync,    │ │ (Global untyped event bus, │
│  business rules, UI hooks)   │ │  integer IDs + Object[])   │
└──────────────┬───────────────┘ └─────────────▲──────────────┘
               │                               │
       ┌───────┴───────────────┐               │
       ▼                       ▼               │
┌──────────────┐       ┌──────────────┐        │
│MessagesStorage       │ConnectionsMgr│        │
│(SQLite, raw) │       │(MTProto, TL) ├────────┘
└──────────────┘       └──────────────┘
```

### Target Architecture (Target State)
The target architecture introduces clear separation of concerns with unidirectional data flow and strong boundaries:

```text
                    ┌──────────────────────────────┐
                    │      Presentation Layer      │
                    │ UI / ViewModels / StateFlow  │
                    └──────────────┬───────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │         Domain Layer         │
                    │  Use Cases / Domain Models   │
                    │     Repository Contracts     │
                    └──────────────┬───────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │          Data Layer          │
                    │  Repositories / Mappers /    │
                    │   Legacy Controller Adapters │
                    └──────────────┬───────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │        Telegram Core         │
                    │   MTProto / TL Serialization │
                    │     Crypto / Raw Storage     │
                    └──────────────┬───────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │       Legacy Telegram        │
                    │  MessagesController, etc.    │
                    │  (Gradually strangled)       │
                    └──────────────────────────────┘
```

---

## 3. Package Structure & Module Responsibilities

During the initial phases, code resides inside `TMessagesProj` to prevent breaking existing Gradle native/JNI builds and APK packaging, organized into distinct packages:

```text
TMessagesProj/src/main/java/org/telegram/messenger/
│
├── core/                                  # Core architectural infrastructure
│   ├── result/
│   │   └── Result.kt                      # Functional Result (Success/Failure)
│   ├── events/
│   │   └── NotificationCenterFlowBridge.kt# Cold Flow wrapper for NotificationCenter
│   └── di/
│       └── AccountFeatureContainer.kt     # Facade delegating to 7 domain containers per currentAccount
│
└── feature/                               # Migrated feature slices (7 Clean Domains, 105 Features)
    ├── business/                          # Commercial, Telegram Business, Stars (10 features)
    │   ├── billing/
    │   ├── botstars/
    │   ├── businessbots/
    │   ├── businesslinks/
    │   ├── businessrecipients/
    │   ├── giftauctions/
    │   ├── payments/
    │   ├── quickreplies/
    │   ├── stargifts/
    │   └── timezones/
    │
    ├── media/                             # Gallery, Audio, Camera, Stories, VoIP (18 features)
    │   ├── audioplayer/, autodeletemedia/, cachebychats/, camera/, chromecast/
    │   ├── contentpreview/, downloadmanager/, fileloader/, fileref/, gallerysave/
    │   ├── imageloader/, mediadata/, photoviewer/, pip/, sharedmedia/
    │   └── stories/, storycustomparams/, voip/
    │
    ├── messaging/                         # Chats, Messages, Drafts, Reactions (31 features)
    │   ├── aitones/, autodelete/, botforum/, botkeyboard/, bottomviews/
    │   ├── chat/, chatattach/, chatinput/, chatmeta/, chattheme/
    │   ├── dialogs/, draftmeasure/, drafts/, emojieffects/, emojipicker/
    │   ├── ephemeralmessages/, factcheck/, folders/, groupcallmsg/, hashtagsearch/
    │   ├── mentions/, messagecustomparams/, reactions/, richcaption/, savedmessages/
    │   └── search/, sendmessages/, stickers/, texthtml/, topics/, translate/
    │
    ├── network/                           # Network Stats, Proxy, Push (4 features)
    │   ├── networkstats/, proxy/, push/, pushlistener/
    │
    ├── security/                          # Auth, Passkeys, Biometrics, Privacy (10 features)
    │   ├── authtokens/, biometrics/, botguard/, captcha/, flagsecure/
    │   ├── passkeys/, privacy/, secretchat/, sessions/, unconfirmedauth/
    │
    ├── social/                            # Contacts, Profiles, Boosts, Birthdays (6 features)
    │   ├── birthdays/, boosts/, contacts/, joinrequests/, location/, profile/
    │
    └── system/                            # Insets, Watchdogs, Detectors, Themes (26 features)
        ├── adjustpan/, animationlocker/, anrwatchdog/, appconfig/, browser/
        ├── countdowntimer/, datastorage/, emudetector/, floatingdebug/, fpscontent/
        ├── hints/, keyboardhide/, keyboardinsets/, launchericon/, leakdetector/
        ├── litemode/, localization/, maintabs/, notifications/, pinchtozoom/
        └── recyclerscroll/, refreshrate/, ringtones/, settings/, themes/, windowvisibility/
            │
            └── <feature_slice>/                # Standard feature internal anatomy
                ├── domain/
                │   ├── model/                 # Pure Kotlin domain entities & enums
                │   ├── repository/            # Abstract repository contracts
                │   └── usecase/               # Isolated single-responsibility business use cases
                │
                ├── data/
                │   ├── mapper/                # Pure bidirectional mappers (Legacy <-> Domain)
                │   └── repository/            # Adapter implementing repository via legacy core
                │
                └── presentation/              # Reactive MVI / MVVM presentation layer
                    ├── <Feature>UiState.kt    # Immutable state data class
                    ├── <Feature>Event.kt      # One-off UI events (SharedFlow / Channel)
                    └── <Feature>ViewModel.kt  # AndroidX Lifecycle ViewModel (StateFlow)
```



### Layer Rules
- **Domain Layer (`feature.<name>.domain`):**
  - Dependent on: Nothing (pure Kotlin/Java Standard Library).
  - Must **never** import `android.*`, `org.telegram.tgnet.*`, `MessagesController`, `MessagesStorage`, or `NotificationCenter`.
- **Data Layer (`feature.<name>.data`):**
  - Dependent on: Domain Layer + Legacy Core.
  - Implements Domain repository contracts using legacy Telegram classes (`SavedMessagesController`, `MessagesStorage`, etc.).
  - Translates legacy objects and TL models into clean Domain models via dedicated Mappers.
  - **Threading Policy:** Telegram's in-memory collections (such as `allDialogs`, user caches, and chat lists) are unsynchronized and modified exclusively on the Android Main thread. All Data layer operations reading or mutating these structures must run on `Dispatchers.Main` to prevent `ConcurrentModificationException` and data races.
- **Presentation Layer (`feature.<name>.presentation`):**
  - Dependent on: Domain Layer + Android Lifecycle/Coroutines.
  - Uses `ViewModel` and exposes immutable `StateFlow<UiState>` and `SharedFlow<UiEvent>`.
  - Preserves composite state (such as search results and tags) when reactive stream updates arrive.
  - Does **not** communicate with legacy controllers directly.
- **Core Infrastructure (`core.*`):**
  - Shared cross-feature utilities: functional `Result`, reactive bridges (`NotificationCenterFlowBridge`), and per-account dependency containers with test substitution support.

---

## 4. Legacy Boundaries & Upstream Protection

| Subsystem | Legacy Class | Boundary Component | Upstream Sensitivity | Migration Policy |
| :--- | :--- | :--- | :--- | :--- |
| **Networking & MTProto** | `ConnectionsManager`, `TLRPC` | Do not wrap prematurely | **CRITICAL** | Keep intact. Never touch crypto, handshake, or TL code for aesthetics. |
| **Database** | `MessagesStorage`, `SQLiteDatabase` | Domain Repositories | High | Wrap queries inside repository adapters. Never modify schema without upstream alignment. |
| **Global Events** | `NotificationCenter` | `NotificationCenterFlowBridge` | High | Wrap into typed Kotlin `Flow`. Do not remove legacy event dispatches. |
| **State & Business Logic** | `MessagesController`, `SavedMessagesController` | `*Repository`, `*UseCase` | High | Implement repository on top of controller methods. Strangler pattern. |
| **UI** | `ChatActivity`, `DialogsActivity`, `Cells` | `ViewModel`, `UiState` | Medium | Extract presentation state and delegate user intents to ViewModel. |

> [!TIP]
> Подробный пошаговый регламент слияния обновлений официального Telegram, стратегию веток и матрицу разрешения конфликтов см. в файле [`UPSTREAM_SYNC.md`](UPSTREAM_SYNC.md).

---

## 5. Migration Checklist

- [x] Baseline Git repository created and initial commit tagged.
- [x] Architectural documentation (`ARCHITECTURE.md`) established.
- [x] Gradle configuration updated: `kotlin-android`, `kotlinx-coroutines`, and `lifecycle-viewmodel-ktx` enabled.
- [x] Core boundaries introduced:
  - [x] `Result.kt` (safe error handling without throwing exceptions across layers)
  - [x] `NotificationCenterFlowBridge.kt` (reactive `callbackFlow` bridge for `NotificationCenter`)
  - [x] `AccountFeatureContainer.kt` (scoped dependency management per `currentAccount`, test-mockable)
- [x] Pilot feature migrated: **Saved Messages**
  - [x] Domain entities: `SavedDialogModel`, `SavedTagModel`
  - [x] Repository contract: `SavedMessagesRepository`
  - [x] Use cases: `GetSavedDialogsUseCase`, `TogglePinSavedDialogUseCase`, `DeleteSavedDialogUseCase`, `GetSavedTagsUseCase`, `SearchSavedDialogsUseCase`
  - [x] Data layer: `SavedMessagesMapper` (dialog and tag mapping), `LegacySavedMessagesRepository` (Main-thread safe)
  - [x] Presentation layer: `SavedMessagesUiState` (with query/results/tags), `SavedMessagesEvent`, `SavedMessagesViewModel` (search, delete, pin, tags, state preservation)
  - [x] UI Integration: Connected `SavedMessagesViewModel` to `SharedMediaLayout.java`
- [x] Dialogs list (`feature.dialogs`)
  - [x] Domain entities: `DialogModel`
  - [x] Repository contract: `DialogsRepository`
  - [x] Use cases: `GetDialogsUseCase`, `LoadMoreDialogsUseCase`, `PinDialogUseCase`, `DeleteDialogUseCase`, `MarkDialogAsReadUseCase`
  - [x] Data layer: `DialogMapper`, `LegacyDialogsRepository` (Main-thread safe, reactive `callbackFlow` bridge for `dialogsNeedReload`)
  - [x] Presentation layer: `DialogsUiState`, `DialogsEvent`, `DialogsViewModel`
- [x] Chat & Messaging (`feature.chat`)
  - [x] Domain entities: `MessageModel`, `MessageDeliveryStatus`
  - [x] Repository contract: `ChatRepository`
  - [x] Use cases: `ObserveMessagesUseCase`, `GetMessagesUseCase`, `LoadHistoryUseCase`, `SendMessageUseCase`, `DeleteMessagesUseCase`
  - [x] Data layer: `ChatMessageMapper`, `LegacyChatRepository` (Main-thread safe, hooked into `didReceiveNewMessages`, `messagesDidLoad`, `messagesDeleted`, `messageReceivedByAck`)
  - [x] Presentation layer: `ChatUiState`, `ChatEvent`, `ChatViewModel`
- [x] Profiles & User Info (`feature.profile`)
  - [x] Domain entities: `ProfileModel` (unified model for user, chat, channel, bot)
  - [x] Repository contract: `ProfileRepository`
  - [x] Use cases: `ObserveProfileUseCase`, `GetProfileUseCase`, `LoadFullProfileUseCase`, `BlockPeerUseCase`, `UnblockPeerUseCase`
  - [x] Data layer: `ProfileMapper`, `LegacyProfileRepository` (Main-thread safe, hooked into `userInfoDidLoad`, `chatInfoDidLoad`)
  - [x] Presentation layer: `ProfileUiState`, `ProfileEvent`, `ProfileViewModel`
- [x] Settings & Preferences (`feature.settings`)
  - [x] Domain entities: `SettingsModel` (pure model decoupling from SharedConfig and UserConfig)
  - [x] Repository contract: `SettingsRepository`
  - [x] Use cases: `ObserveSettingsUseCase`, `GetSettingsUseCase`, `UpdateFontSizeUseCase`, `UpdateBubbleRadiusUseCase`, `UpdateSaveToGalleryUseCase`, `UpdateStreamMediaUseCase`, `UpdateSyncContactsUseCase`
  - [x] Data layer: `SettingsMapper`, `LegacySettingsRepository` (Main-thread safe, hooked into `updateInterfaces`, `mainUserInfoChanged`, `notificationsSettingsUpdated`)
  - [x] Presentation layer: `SettingsUiState`, `SettingsEvent`, `SettingsViewModel`
- [x] Media & Gallery (`feature.media`)
  - [x] Domain entities: `MediaItemModel`, `MediaAlbumModel`
  - [x] Repository contract: `MediaRepository`
  - [x] Use cases: `ObserveMediaAlbumsUseCase`, `GetMediaAlbumsUseCase`, `GetAlbumMediaUseCase`, `GetAllMediaUseCase`
  - [x] Data layer: `MediaMapper`, `LegacyMediaRepository` (Main-thread safe, hooked into `MediaController.allMediaAlbums`, `allPhotosAlbumEntry`)
  - [x] Presentation layer: `MediaUiState`, `MediaEvent`, `MediaViewModel`
- [x] Calls & VoIP (`feature.voip`)
  - [x] Domain entities: `CallModel`, `CallState` (typed enum replacing legacy state ints)
  - [x] Repository contract: `VoIPRepository`
  - [x] Use cases: `ObserveCurrentCallUseCase`, `GetCurrentCallUseCase`, `StartCallUseCase`, `AcceptCallUseCase`, `DeclineCallUseCase`, `HangUpCallUseCase`, `ToggleMuteUseCase`, `ToggleSpeakerphoneUseCase`
  - [x] Data layer: `CallMapper`, `LegacyVoIPRepository` (Main-thread safe, hooked into `VoIPService.StateListener` and `NotificationCenter.didStartedCall`/`didEndCall`)
  - [x] Presentation layer: `CallUiState`, `CallEvent`, `CallViewModel`
- [x] Secret Chats & End-to-End Encryption (`feature.secretchat`)
  - [x] Domain entities: `SecretChatModel`, `SecretChatState` (typed enum replacing TLRPC polymorphic subtypes)
  - [x] Repository contract: `SecretChatRepository`
  - [x] Use cases: `ObserveSecretChatUseCase`, `ObserveSecretChatsUseCase`, `GetSecretChatUseCase`, `StartSecretChatUseCase`, `AcceptSecretChatUseCase`, `DeclineSecretChatUseCase`, `SetSecretChatTtlUseCase`, `SendScreenshotNotificationUseCase`
  - [x] Data layer: `SecretChatMapper`, `LegacySecretChatRepository` (Main-thread safe, hooked into `NotificationCenter.encryptedChatUpdated`, `encryptedChatCreated`, `dialogsNeedReload`)
  - [x] Presentation layer: `SecretChatUiState`, `SecretChatEvent`, `SecretChatViewModel`
- [x] Contacts & Phonebook (`feature.contacts`)
  - [x] Domain entities: `ContactModel` (pure model decoupling from TLRPC.TL_contact and TLRPC.User)
  - [x] Repository contract: `ContactsRepository`
  - [x] Use cases: `ObserveContactsUseCase`, `GetContactsUseCase`, `GetContactUseCase`, `AddContactUseCase`, `DeleteContactUseCase`, `SearchContactsUseCase`
  - [x] Data layer: `ContactMapper`, `LegacyContactsRepository` (Main-thread safe, hooked into `NotificationCenter.contactsDidLoad`, `updateInterfaces`)
  - [x] Presentation layer: `ContactsUiState`, `ContactsEvent`, `ContactsViewModel`
- [x] Folders & Chat Filters (`feature.folders`)
  - [x] Domain entities: `FolderModel`, `SuggestedFolderModel` (pure models decoupling from MessagesController.DialogFilter and TLRPC.TL_dialogFilterSuggested)
  - [x] Repository contract: `FoldersRepository`
  - [x] Use cases: `ObserveFoldersUseCase`, `GetFoldersUseCase`, `GetFolderUseCase`, `CreateFolderUseCase`, `UpdateFolderUseCase`, `DeleteFolderUseCase`, `ReorderFoldersUseCase`, `GetSuggestedFoldersUseCase`
  - [x] Data layer: `FolderMapper`, `LegacyFoldersRepository` (Main-thread safe, hooked into `NotificationCenter.dialogFiltersUpdated`, `suggestedFiltersLoaded`)
  - [x] Presentation layer: `FoldersUiState`, `FoldersEvent`, `FoldersViewModel`
- [x] Stickers & Emojis (`feature.stickers`)
  - [x] Domain entities: `StickerModel`, `StickerSetModel`, `StickerType` (pure models decoupling from TLRPC.Document and TLRPC.TL_messages_stickerSet)
  - [x] Repository contract: `StickersRepository`
  - [x] Use cases: `ObserveStickerSetsUseCase`, `GetStickerSetsUseCase`, `GetStickerSetUseCase`, `GetRecentStickersUseCase`, `GetStickersForEmojiUseCase`, `ToggleStickerSetInstalledUseCase`, `ToggleStickerSetArchivedUseCase`
  - [x] Data layer: `StickerMapper`, `LegacyStickersRepository` (Main-thread safe, hooked into `NotificationCenter.stickersDidLoad`, `recentDocumentsDidLoad`)
  - [x] Presentation layer: `StickersUiState`, `StickersEvent`, `StickersViewModel`
- [x] File Loader & Background Downloads (`feature.fileloader`)
  - [x] Domain entities: `FileTransferModel`, `FileTransferType`, `FileTransferStatus`, `FileDownloadRequest`, `FileUploadRequest`
  - [x] Repository contract: `FileLoaderRepository`
  - [x] Use cases: `ObserveTransfersUseCase`, `ObserveTransferUseCase`, `GetActiveDownloadsUseCase`, `GetRecentDownloadsUseCase`, `LoadFileUseCase`, `CancelLoadFileUseCase`, `CancelAllDownloadsUseCase`, `UploadFileUseCase`, `CancelFileUploadUseCase`
  - [x] Data layer: `FileTransferMapper`, `LegacyFileLoaderRepository` (Main-thread safe, hooked into `fileLoadProgressChanged`, `fileLoaded`, `fileLoadFailed`, `fileUploadProgressChanged`, `fileUploaded`, `fileUploadFailed`, `onDownloadingFilesChanged`)
  - [x] Presentation layer: `FileLoaderUiState`, `FileLoaderEvent`, `FileLoaderViewModel`
- [x] Search & Global Search (`feature.search`)
  - [x] Domain entities: `SearchResultModel`, `SearchResultType`, `SearchFilter`
  - [x] Repository contract: `SearchRepository`
  - [x] Use cases: `SearchGlobalUseCase`, `SearchLocalUseCase`, `GetRecentSearchesUseCase`, `ClearRecentSearchesUseCase`, `RemoveRecentSearchUseCase`, `GetRecentHashtagsUseCase`, `PutRecentHashtagUseCase`, `ClearRecentHashtagsUseCase`
  - [x] Data layer: `SearchMapper`, `LegacySearchRepository` (Main-thread safe, integrating `SearchAdapterHelper`, `ConnectionsManager`, `MessagesController`, `MessagesStorage`)
  - [x] Presentation layer: `SearchUiState`, `SearchEvent`, `SearchViewModel` (with 300ms query debouncing)
- [x] Notifications, Push & Badges (`feature.notifications`)
  - [x] Domain entities: `NotificationSettingsModel`, `BadgeSettingsModel`, `BadgeCountModel`, `DialogMuteState`, `NotificationPeerType`
  - [x] Repository contract: `NotificationsRepository`
  - [x] Use cases: `ObserveNotificationSettingsUseCase`, `GetNotificationSettingsUseCase`, `ObserveBadgeUseCase`, `GetBadgeUseCase`, `ObserveBadgeSettingsUseCase`, `GetBadgeSettingsUseCase`, `TogglePeerNotificationsUseCase`, `ToggleInChatSoundUseCase`, `ToggleInAppSoundsUseCase`, `ToggleInAppVibrateUseCase`, `ToggleInAppPreviewUseCase`, `ToggleContactJoinedNotificationsUseCase`, `TogglePinnedMessagesNotificationsUseCase`, `UpdateBadgeSettingsUseCase`, `MuteDialogUseCase`, `IsDialogMutedUseCase`, `RefreshBadgeUseCase`
  - [x] Data layer: `NotificationMapper`, `LegacyNotificationsRepository` (Main-thread safe, adapting `NotificationsController`, `MessagesController`, `SharedPreferences`, and `NotificationCenterFlowBridge`)
  - [x] Presentation layer: `NotificationsUiState`, `NotificationsEvent`, `NotificationsViewModel`
- [x] Privacy, Security, Passcode & 2FA (`feature.privacy`)
  - [x] Domain entities: `PrivacyRuleModel`, `PrivacyRuleType`, `PrivacyRuleMode`, `BlockedPeerModel`, `PasscodeSettingsModel`, `TwoStepVerificationModel`
  - [x] Repository contract: `PrivacyRepository`
  - [x] Use cases: `ObservePrivacyRulesUseCase`, `GetPrivacyRulesUseCase`, `SetPrivacyRuleUseCase`, `LoadPrivacyRulesUseCase`, `ObserveBlockedPeersUseCase`, `GetBlockedPeersUseCase`, `BlockPrivacyPeerUseCase`, `UnblockPrivacyPeerUseCase`, `GetPasscodeSettingsUseCase`, `SetPasscodeUseCase`, `CheckPasscodeUseCase`, `ClearPasscodeUseCase`, `ObserveTwoStepVerificationUseCase`, `LoadTwoStepVerificationUseCase`
  - [x] Data layer: `PrivacyMapper`, `LegacyPrivacyRepository` (Main-thread safe, adapting `ContactsController`, `MessagesController`, `SharedConfig`, and `NotificationCenterFlowBridge`)
  - [x] Presentation layer: `PrivacyUiState`, `PrivacyEvent`, `PrivacyViewModel`
- [x] Themes, Night Mode, Appearance & Wallpaper (`feature.themes`)
  - [x] Domain entities: `NightModeType`, `ThemeAccentModel`, `ThemeModel`, `NightModeSettingsModel`, `WallpaperModel`, `AppearanceSettingsModel`
  - [x] Repository contract: `ThemeRepository`
  - [x] Use cases: `ObserveAppearanceSettingsUseCase`, `GetAppearanceSettingsUseCase`, `ObserveAvailableThemesUseCase`, `GetAvailableThemesUseCase`, `ApplyThemeUseCase`, `ObserveNightModeUseCase`, `SetNightModeTypeUseCase`, `SetNightModeSettingsUseCase`, `SetThemeAccentUseCase`, `SetBubbleRadiusUseCase`, `ResetAppearanceSettingsUseCase`
  - [x] Data layer: `ThemeMapper`, `LegacyThemeRepository` (Main-thread safe, adapting `Theme.java`, `SharedConfig`, and `NotificationCenterFlowBridge` global events)
  - [x] Presentation layer: `ThemeUiState`, `ThemeEvent`, `ThemeViewModel`
- [x] Stories, Statuses & Stealth Mode (`feature.stories`)
  - [x] Domain entities: `StoryModel`, `PeerStoriesModel`, `StealthModeModel`, `StoryLimitModel`
  - [x] Repository contract: `StoriesRepository`
  - [x] Use cases: `ObserveStoriesUseCase`, `ObserveHiddenStoriesUseCase`, `ObserveStealthModeUseCase`, `ObserveSelfStoriesUseCase`, `GetPeerStoriesUseCase`, `MarkStoryAsReadUseCase`, `DeleteStoryUseCase`, `ToggleStoryPinUseCase`, `ToggleStoryHiddenUseCase`, `ActivateStealthModeUseCase`, `GetStoryLimitUseCase`, `RefreshStoriesUseCase`
  - [x] Data layer: `StoryMapper`, `LegacyStoriesRepository` (Main-thread safe, adapting `StoriesController` and `NotificationCenterFlowBridge` events)
  - [x] Presentation layer: `StoriesUiState`, `StoriesEvent`, `StoriesViewModel`
- [x] Payments & Telegram Stars (`feature.payments`)
  - [x] Domain entities: `StarsBalanceModel`, `StarTransactionModel`, `StarSubscriptionModel`, `StarTopupOptionModel`
  - [x] Repository contract: `PaymentsRepository`
  - [x] Use cases: `ObserveStarsBalanceUseCase`, `ObserveStarTransactionsUseCase`, `ObserveStarSubscriptionsUseCase`, `GetStarsBalanceUseCase`, `GetStarTransactionsUseCase`, `GetStarSubscriptionsUseCase`, `GetStarTopupOptionsUseCase`, `RefreshStarsBalanceUseCase`, `RefreshStarTransactionsUseCase`, `RefreshStarSubscriptionsUseCase`
  - [x] Data layer: `PaymentMapper`, `LegacyPaymentsRepository` (Main-thread safe, adapting `StarsController` and `NotificationCenterFlowBridge` events: `starBalanceUpdated`, `starTransactionsLoaded`, `starSubscriptionsLoaded`)
  - [x] Presentation layer: `PaymentsUiState`, `PaymentsEvent`, `PaymentsViewModel`
- [x] Data & Storage Usage, Cache Control (`feature.datastorage`)
  - [x] Domain entities: `NetworkUsageType`, `NetworkUsageModel`, `StorageUsageModel`, `AutoDownloadNetworkType`, `AutoDownloadPresetModel`, `KeepMediaSettingsModel`
  - [x] Repository contract: `DataStorageRepository`
  - [x] Use cases: `ObserveNetworkUsageUseCase`, `ObserveStorageUsageUseCase`, `ObserveAutoDownloadPresetUseCase`, `ObserveKeepMediaSettingsUseCase`, `GetNetworkUsageUseCase`, `ResetNetworkUsageUseCase`, `GetStorageUsageUseCase`, `ClearCacheUseCase`, `ClearDatabaseUseCase`, `GetAutoDownloadPresetUseCase`, `UpdateAutoDownloadPresetUseCase`, `GetKeepMediaSettingsUseCase`, `UpdateKeepMediaUseCase`, `RefreshStorageUsageUseCase`
  - [x] Data layer: `DataStorageMapper`, `LegacyDataStorageRepository` (Main/IO thread safe, adapting `StatsController`, `DownloadController`, `CacheByChatsController`, `FileLoader`, `MessagesStorage`)
  - [x] Presentation layer: `DataStorageUiState`, `DataStorageEvent`, `DataStorageViewModel`
- [x] Forum Topics & Supergroup Threads (`feature.topics`)
  - [x] Domain entities: `TopicModel` (with `isGeneral`), `TopicFilterType`, `ForumUnreadCountModel`
  - [x] Repository contract: `TopicsRepository`
  - [x] Use cases: `ObserveTopicsUseCase`, `ObserveForumUnreadCountUseCase`, `GetTopicsUseCase`, `GetTopicUseCase`, `LoadTopicsUseCase`, `ReloadTopicsUseCase`, `ToggleCloseTopicUseCase`, `TogglePinTopicUseCase`, `ToggleShowTopicUseCase`, `DeleteTopicsUseCase`, `ReorderPinnedTopicsUseCase`, `MarkTopicReactionsAsReadUseCase`, `GetForumUnreadCountUseCase`
  - [x] Data layer: `TopicMapper`, `LegacyTopicsRepository` (Main-thread safe, adapting `TopicsController` via `MessagesController` and `NotificationCenterFlowBridge` listening to `topicsDidLoaded`)
  - [x] Presentation layer: `TopicsUiState`, `TopicsEvent`, `TopicsViewModel` (filtering by OPEN/CLOSED/PINNED/HIDDEN, title search, unread counter badge observation)
- [x] Live Locations, GPS & Proximity Alerts (`feature.location`)
  - [x] Domain entities: `GeoPointModel`, `LiveLocationSharingModel`, `PeerLiveLocationModel`
  - [x] Repository contract: `LocationRepository`
  - [x] Use cases: `ObserveActiveSharingsUseCase`, `ObservePeerLocationsUseCase`, `ObserveLastKnownLocationUseCase`, `GetActiveSharingsUseCase`, `IsSharingLocationUseCase`, `GetSharingInfoUseCase`, `GetLastKnownLocationUseCase`, `LoadPeerLiveLocationsUseCase`, `StopLocationSharingUseCase`, `StopAllLocationSharingsUseCase`, `SetProximityAlertUseCase`, `SendStaticLocationUseCase`, `SendLiveLocationUseCase`, `MarkLiveLocationsAsReadUseCase`
  - [x] Data layer: `LocationMapper`, `LegacyLocationRepository` (Main-thread safe, adapting `LocationController` and `SendMessagesHelper` with `NotificationCenterFlowBridge` observing `liveLocationsChanged`, `liveLocationsCacheChanged`, `newLocationAvailable`)
  - [x] Presentation layer: `LocationUiState`, `LocationEvent`, `LocationViewModel`
- [x] Active Sessions, Devices & QR Login (`feature.sessions`)
  - [x] Domain entities: `SessionModel`, `WebSessionModel`, `SessionsListModel`
  - [x] Repository contract: `SessionsRepository`
  - [x] Use cases: `ObserveSessionsUseCase`, `ObserveWebSessionsUseCase`, `GetSessionsUseCase`, `LoadSessionsUseCase`, `GetWebSessionsUseCase`, `LoadWebSessionsUseCase`, `TerminateSessionUseCase`, `TerminateAllOtherSessionsUseCase`, `TerminateWebSessionUseCase`, `TerminateAllWebSessionsUseCase`, `UpdateSessionSettingsUseCase`, `SetSessionsTtlUseCase`, `AcceptQrLoginUseCase`
  - [x] Data layer: `SessionMapper`, `LegacySessionsRepository` (Main-thread safe, adapting MTProto account requests with `cancelRequest` cancellation and `NotificationCenterFlowBridge` observing `NotificationCenter.newSessionReceived`)
  - [x] Presentation layer: `SessionsUiState`, `SessionsEvent`, `SessionsViewModel`
- [x] In-App Translation, Auto-Translate & Language Preferences (`feature.translate`)
  - [x] Domain entities: `LanguageModel`, `TranslateSettingsModel`, `DialogTranslationStateModel`, `TranslationResultModel`
  - [x] Repository contract: `TranslationRepository`
  - [x] Use cases: `ObserveTranslateSettingsUseCase`, `GetTranslateSettingsUseCase`, `SetChatTranslateEnabledUseCase`, `SetContextTranslateEnabledUseCase`, `SetDoNotTranslateLanguagesUseCase`, `AddDoNotTranslateLanguageUseCase`, `RemoveDoNotTranslateLanguageUseCase`, `ObserveDialogTranslationStateUseCase`, `GetDialogTranslationStateUseCase`, `ToggleDialogTranslatingUseCase`, `SetDialogTargetLanguageUseCase`, `TranslateTextUseCase`, `GetAvailableLanguagesUseCase`, `ApplyAppLanguageUseCase`
  - [x] Data layer: `TranslationMapper`, `LegacyTranslationRepository` (Main-thread safe, adapting `TranslateController`, `LocaleController`, `RestrictedLanguagesSelectActivity`, and MTProto `TLRPC.TL_messages_translateText` with `suspendCancellableCoroutine` and `NotificationCenterFlowBridge` observing `dialogTranslate` and `suggestedLangpack`)
  - [x] Presentation layer: `TranslateUiState`, `TranslateEvent`, `TranslateViewModel`
- [x] Message Reactions, Quick Double-Tap Reaction & Poll Voting (`feature.reactions`)
  - [x] Domain entities: `ReactionItemModel`, `MessageReactionCountModel`, `MessageReactionsStateModel`, `ReactionsSettingsModel`
  - [x] Repository contract: `ReactionsRepository`
  - [x] Use cases: `ObserveAvailableReactionsUseCase`, `GetAvailableReactionsUseCase`, `LoadAvailableReactionsUseCase`, `ObserveRecentReactionsUseCase`, `GetRecentReactionsUseCase`, `GetReactionsSettingsUseCase`, `GetDoubleTapReactionUseCase`, `SetDoubleTapReactionUseCase`, `SendReactionUseCase`, `ClearReactionsUseCase`, `SendVoteUseCase`
  - [x] Data layer: `ReactionMapper`, `LegacyReactionsRepository` (Main-thread safe, adapting `MediaDataController`, `SendMessagesHelper`, `MessagesController`, and MTProto `TL_messages_sendReaction` / `TL_messages_sendVote` with `suspendCancellableCoroutine` and `NotificationCenterFlowBridge` observing `NotificationCenter.reactionsDidLoad`)
  - [x] Presentation layer: `ReactionsUiState`, `ReactionsEvent`, `ReactionsViewModel`
- [x] Channel Boosts, Status, Slots & Perks (`feature.boosts`)
  - [x] Domain entities: `BoostStatusModel`, `BoostSlotModel`, `MyBoostsModel`, `CanApplyBoostModel`
  - [x] Repository contract: `BoostsRepository`
  - [x] Use cases: `GetBoostsStatusUseCase`, `GetMyBoostsUseCase`, `CheckCanApplyBoostUseCase`, `ApplyBoostUseCase`
  - [x] Data layer: `BoostMapper`, `LegacyBoostsRepository` (Main/IO thread safe, adapting `ChannelBoostsController`, `MessagesController`, MTProto `TL_stories.TL_premium_getBoostsStatus`, `TL_stories.TL_premium_getMyBoosts`, and `TL_stories.TL_premium_applyBoost` with `suspendCancellableCoroutine`)
  - [x] Presentation layer: `BoostsUiState`, `BoostsEvent`, `BoostsViewModel`
- [x] Business Quick Replies & Shortcuts (`feature.quickreplies`)
  - [x] Domain entities: `QuickReplyModel`, `QuickRepliesLimitModel`
  - [x] Repository contract: `QuickRepliesRepository`
  - [x] Use cases: `ObserveQuickRepliesUseCase`, `GetQuickRepliesUseCase`, `LoadQuickRepliesUseCase`, `FindQuickReplyUseCase`, `CheckQuickReplyNameBusyUseCase`, `CanAddNewQuickReplyUseCase`, `RenameQuickReplyUseCase`, `ReorderQuickRepliesUseCase`, `DeleteQuickRepliesUseCase`, `SendQuickReplyUseCase`
  - [x] Data layer: `QuickReplyMapper`, `QuickRepliesLocalDataSource`, `QuickRepliesRemoteDataSource`, `QuickRepliesRepositoryImpl` (Main-thread safe, adapting `QuickRepliesController` and `SendMessagesHelper` with `NotificationCenterFlowBridge` observing `NotificationCenter.quickRepliesUpdated`)
  - [x] Presentation layer: `QuickRepliesUiState`, `QuickRepliesEvent`, `QuickRepliesViewModel`
- [x] Join Requests & Chat Administration (`feature.joinrequests`)
  - [x] Domain entities: `JoinRequestUserModel`, `JoinRequestModel`, `JoinRequestsListModel`, `ChatPendingRequestsModel`
  - [x] Repository contract: `JoinRequestsRepository`
  - [x] Use cases: `ObservePendingRequestsUseCase`, `GetPendingRequestsCountUseCase`, `GetCachedJoinRequestsUseCase`, `LoadJoinRequestsUseCase`, `ApproveJoinRequestUseCase`, `DismissJoinRequestUseCase`, `ApproveAllJoinRequestsUseCase`, `DismissAllJoinRequestsUseCase`
  - [x] Data layer: `JoinRequestMapper`, `LegacyJoinRequestsRepository` (Main-thread safe, adapting `MemberRequestsController` and `ConnectionsManager` MTProto requests with `suspendCancellableCoroutine` and `NotificationCenterFlowBridge` observing `NotificationCenter.chatInfoDidLoad`)
  - [x] Presentation layer: `JoinRequestsUiState`, `JoinRequestsEvent`, `JoinRequestsViewModel`
- [x] Message Fact-Checks & Verification Annotations (`feature.factcheck`)
  - [x] Domain entities: `FactCheckEntityModel`, `FactCheckModel`, `FactCheckLimitsModel`
  - [x] Repository contract: `FactCheckRepository`
  - [x] Use cases: `ObserveFactCheckLoadedUseCase`, `GetFactCheckUseCase`, `LoadFactCheckUseCase`, `ApplyFactCheckUseCase`, `DeleteFactCheckUseCase`, `GetFactCheckLimitUseCase`
  - [x] Data layer: `FactCheckMapper`, `LegacyFactCheckRepository` (Main-thread safe, adapting `FactCheckController`, `MessagesController`, and `ConnectionsManager` MTProto `TL_getFactCheck`, `TL_editFactCheck`, `TL_deleteFactCheck` with `suspendCancellableCoroutine` and `NotificationCenterFlowBridge` observing `NotificationCenter.factCheckLoaded`)
  - [x] Presentation layer: `FactCheckUiState`, `FactCheckEvent`, `FactCheckViewModel`
- [x] User Birthdays, Contacts' Birthdays & Birthday Wishes (`feature.birthdays`)
  - [x] Domain entities: `BirthdayDateModel`, `BirthdayUserModel`, `ContactBirthdayModel`, `BirthdayStateModel`
  - [x] Repository contract: `BirthdaysRepository`
  - [x] Use cases: `ObserveBirthdaysUseCase`, `GetBirthdaysStateUseCase`, `CheckBirthdaysUseCase`, `HideTodayBirthdaysUseCase`, `IsBirthdayTodayUseCase`, `HasBirthdaysTodayUseCase`
  - [x] Data layer: `BirthdayMapper`, `LegacyBirthdaysRepository` (Main-thread safe, adapting `BirthdayController` with `NotificationCenterFlowBridge` observing `NotificationCenter.premiumPromoUpdated`)
  - [x] Presentation layer: `BirthdaysUiState`, `BirthdaysEvent`, `BirthdaysViewModel`
- [x] Chat Themes & Wallpapers (`feature.chattheme`)
  - [x] Domain entities: `ChatThemeModel`, `DialogThemeStateModel`
  - [x] Repository contract: `ChatThemeRepository`
  - [x] Use cases: `ObserveDialogThemeUseCase`, `GetDialogThemeStateUseCase`, `GetAvailableChatThemesUseCase`, `SetDialogThemeUseCase`, `ResetDialogThemeUseCase`, `SaveChatWallpaperUseCase`
  - [x] Data layer: `ChatThemeMapper`, `LegacyChatThemeRepository` (Main-thread safe, adapting `ChatThemeController` with `suspendCancellableCoroutine` for theme loading and `NotificationCenterFlowBridge` observing dialog updates)
  - [x] Presentation layer: `ChatThemeUiState`, `ChatThemeEvent`, `ChatThemeViewModel`
- [x] Passkeys & WebAuthn Authentication (`feature.passkeys`)
  - [x] Domain entities: `PasskeyModel`, `PasskeysStateModel`
  - [x] Repository contract: `PasskeysRepository`
  - [x] Use cases: `ObservePasskeysUseCase`, `GetPasskeysUseCase`, `DeletePasskeyUseCase`, `CheckCanAddPasskeyUseCase`, `IsPasskeysSupportedUseCase`
  - [x] Data layer: `PasskeyMapper`, `LegacyPasskeysRepository` (Main-thread safe, adapting `PasskeysController`, `ConnectionsManager` MTProto `TL_account.getPasskeys`, `TL_account.deletePasskey` with `suspendCancellableCoroutine`)
  - [x] Presentation layer: `PasskeysUiState`, `PasskeysEvent`, `PasskeysViewModel`
- [x] Proxy Configuration, Server Management & Auto-Rotation (`feature.proxy`)
  - [x] Domain entities: `ProxyType`, `ProxyModel`, `ProxySettingsModel`
  - [x] Repository contract: `ProxyRepository`
  - [x] Use cases: `ObserveProxySettingsUseCase`, `GetProxySettingsUseCase`, `AddProxyUseCase`, `DeleteProxyUseCase`, `EnableProxyUseCase`, `DisableProxyUseCase`, `ToggleProxyRotationUseCase`, `CheckProxyPingUseCase`
  - [x] Data layer: `ProxyMapper`, `LegacyProxyRepository` (Main-thread safe, adapting `SharedConfig`, `ProxyRotationController`, and `ConnectionsManager.checkProxy` with `suspendCancellableCoroutine` and `NotificationCenterFlowBridge` observation)
  - [x] Presentation layer: `ProxyUiState`, `ProxyEvent`, `ProxyViewModel`
- [x] Auto-Delete Messages, Global History TTL & Chat Lifetime (`feature.autodelete`)
  - [x] Domain entities: `AutoDeleteTtlModel`, `GlobalAutoDeleteStateModel`, `ChatAutoDeleteStateModel`
  - [x] Repository contract: `AutoDeleteRepository`
  - [x] Use cases: `ObserveGlobalAutoDeleteUseCase`, `GetGlobalAutoDeleteUseCase`, `SetGlobalAutoDeleteUseCase`, `GetChatAutoDeleteUseCase`, `SetChatAutoDeleteUseCase`, `SetChatsAutoDeleteBatchUseCase`
  - [x] Data layer: `AutoDeleteMapper`, `LegacyAutoDeleteRepository` (Main-thread safe, adapting `UserConfig.getGlobalTTl`, `MessagesController.setDialogHistoryTTL`, `TL_messages_setDefaultHistoryTTL` with `suspendCancellableCoroutine` and `NotificationCenterFlowBridge` observation)
  - [x] Presentation layer: `AutoDeleteUiState`, `AutoDeleteEvent`, `AutoDeleteViewModel`
- [x] Unconfirmed Auth Sessions & Login Approvals (`feature.unconfirmedauth`)
  - [x] Domain entities: `UnconfirmedAuthModel`, `UnconfirmedAuthStateModel`
  - [x] Repository contract: `UnconfirmedAuthRepository`
  - [x] Use cases: `ObserveUnconfirmedAuthsUseCase`, `GetUnconfirmedAuthsUseCase`, `ConfirmAuthUseCase`, `DenyAuthUseCase`, `ConfirmAllAuthsUseCase`, `DenyAllAuthsUseCase`, `ClearUnconfirmedAuthsUseCase`
  - [x] Data layer: `UnconfirmedAuthMapper`, `LegacyUnconfirmedAuthRepository` (Main-thread safe, adapting `UnconfirmedAuthController`, `MessagesController.getUnconfirmedAuthController()`, `confirm` and `deny` with `suspendCancellableCoroutine`, and `NotificationCenterFlowBridge` observing `NotificationCenter.unconfirmedAuthUpdate`)
  - [x] Presentation layer: `UnconfirmedAuthUiState`, `UnconfirmedAuthEvent`, `UnconfirmedAuthViewModel`
- [x] Telegram Star Gifts, Catalog & Profile Saved Gifts (`feature.stargifts`)
  - [x] Domain entities: `StarGiftModel`, `SavedStarGiftModel`, `StarGiftsCatalogModel`, `StarGiftFilter`, `ProfileGiftsModel`
  - [x] Repository contract: `StarGiftsRepository`
  - [x] Use cases: `ObserveStarGiftsCatalogUseCase`, `GetStarGiftsCatalogUseCase`, `GetStarGiftByIdUseCase`, `ObserveProfileGiftsUseCase`, `LoadProfileGiftsUseCase`, `TogglePinProfileGiftUseCase`, `ToggleHideProfileGiftUseCase`
  - [x] Data layer: `StarGiftMapper`, `LegacyStarGiftsRepository` (Main-thread safe, adapting `StarsController` and `NotificationCenterFlowBridge` observing `NotificationCenter.starGiftsLoaded`)
  - [x] Presentation layer: `StarGiftsUiState`, `StarGiftsEvent`, `StarGiftsViewModel`
- [x] AI Compose Tones & Styles (`feature.aitones`)
  - [x] Domain entities: `AiToneModel`, `AiTonesStateModel`
  - [x] Repository contract: `AiTonesRepository`
  - [x] Use cases: `ObserveAiTonesUseCase`, `GetAiTonesStateUseCase`, `LoadAiTonesUseCase`, `AddAiToneUseCase`, `RemoveAiToneUseCase`, `UnsaveAiToneUseCase`, `EditAiToneUseCase`
  - [x] Data layer: `AiToneMapper`, `LegacyAiTonesRepository` (Main-thread safe, adapting `AiTonesController` via `MessagesController.getTonesController()` and `NotificationCenterFlowBridge` observing `NotificationCenter.loadedAiComposeTones`)
  - [x] Presentation layer: `AiTonesUiState`, `AiTonesEvent`, `AiTonesViewModel`
- [x] reCAPTCHA Enterprise Verification (`feature.captcha`)
  - [x] Domain entities: `CaptchaAction`, `CaptchaRequestModel`, `CaptchaResult`
  - [x] Repository contract: `CaptchaRepository`
  - [x] Use cases: `ObserveActiveCaptchaRequestsUseCase`, `GetActiveCaptchaRequestsUseCase`, `VerifyCaptchaUseCase`, `SubmitCaptchaResultUseCase`, `CancelCaptchaUseCase`
  - [x] Data layer: `CaptchaMapper`, `LegacyCaptchaRepository` (Main-thread safe, adapting Google Play Services reCAPTCHA Enterprise Tasks API and MTProto `ConnectionsManager.native_receivedCaptchaResult`)
  - [x] Presentation layer: `CaptchaUiState`, `CaptchaEvent`, `CaptchaViewModel`
- [x] Hashtag Search & Search History (`feature.hashtagsearch`)
  - [x] Domain entities: `HashtagSearchType`, `HashtagMessageModel`, `HashtagSearchResultModel`
  - [x] Repository contract: `HashtagSearchRepository`
  - [x] Use cases: `ObserveHashtagHistoryUseCase`, `GetHashtagHistoryUseCase`, `AddHashtagToHistoryUseCase`, `RemoveHashtagFromHistoryUseCase`, `ClearHashtagHistoryUseCase`, `ObserveHashtagSearchResultUseCase`, `SearchHashtagUseCase`, `JumpToHashtagMessageUseCase`, `ClearHashtagSearchResultsUseCase`
  - [x] Data layer: `HashtagMapper`, `LegacyHashtagSearchRepository` (Main-thread safe, adapting `HashtagSearchController`, SharedPreferences history, and MTProto global/channel search)
  - [x] Presentation layer: `HashtagSearchUiState`, `HashtagSearchEvent`, `HashtagSearchViewModel`
- [x] Biometrics, Hardware Keystore & Passcode Authentication (`feature.biometrics`)
  - [x] Domain entities: `BiometricStatus`, `BiometricKeyStateModel` (pure entities decoupled from AndroidKeyStore and FingerprintManagerCompat)
  - [x] Repository contract: `BiometricsRepository`
  - [x] Use cases: `ObserveBiometricKeyStateUseCase`, `GetBiometricKeyStateUseCase`, `CheckBiometricKeyReadyUseCase`, `DeleteInvalidBiometricKeyUseCase`, `IsBiometricKeyReadyUseCase`, `HasDeviceBiometricsChangedUseCase`
  - [x] Data layer: `BiometricMapper`, `LegacyBiometricsRepository` (Safe across Android versions M+, adapts `FingerprintController`, `NotificationCenter.didGenerateFingerprintKeyPair`)
  - [x] Presentation layer: `BiometricsUiState`, `BiometricsEvent`, `BiometricsViewModel`
- [x] Telegram Star Gift Auctions & Real-Time Bidding (`feature.giftauctions`)
  - [x] Domain entities: `GiftAuctionStatus`, `GiftAuctionModel`, `GiftAuctionBidParamsModel`, `GiftAuctionAcquiredGiftModel`
  - [x] Repository contract: `GiftAuctionsRepository`
  - [x] Use cases: `ObserveActiveAuctionsUseCase`, `ObserveAuctionUseCase`, `GetActiveAuctionsUseCase`, `GetAuctionByIdUseCase`, `GetAuctionBySlugUseCase`, `SendAuctionBidUseCase`, `LoadAuctionAcquiredGiftsUseCase`, `RefreshActiveAuctionsUseCase`
  - [x] Data layer: `GiftAuctionMapper`, `LegacyGiftAuctionsRepository` (Main-thread safe, adapting `GiftAuctionController`, `OnAuctionUpdateListener`, `OnActiveAuctionsUpdateListeners`, and MTProto Star Gift auction protocols)
  - [x] Presentation layer: `GiftAuctionsUiState`, `GiftAuctionsEvent`, `GiftAuctionsViewModel`
- [x] Telegram Business Chat Links & Shortcuts (`feature.businesslinks`)
  - [x] Domain entities: `BusinessLinkModel`, `BusinessLinkInputModel`, `BusinessLinksStateModel`
  - [x] Repository contract: `BusinessLinksRepository`
  - [x] Use cases: `ObserveBusinessLinksUseCase`, `GetBusinessLinksUseCase`, `LoadBusinessLinksUseCase`, `CreateBusinessLinkUseCase`, `EditBusinessLinkUseCase`, `DeleteBusinessLinkUseCase`, `FindBusinessLinkUseCase`, `CanAddNewBusinessLinkUseCase`
  - [x] Data layer: `BusinessLinkMapper`, `BusinessLinksLocalDataSource`, `BusinessLinksRemoteDataSource`, `BusinessLinksRepositoryImpl` (Main-thread safe, adapting `BusinessLinksController`, `NotificationCenter.businessLinksUpdated`, and MTProto business chat links protocol)
  - [x] Presentation layer: `BusinessLinksUiState`, `BusinessLinksEvent`, `BusinessLinksViewModel`
- [x] Telegram Business Chatbots & Connected Bots (`feature.businessbots`)
  - [x] Domain entities: `BusinessBotRightsModel`, `BusinessBotRecipientsModel`, `ConnectedBotModel`, `BusinessBotsStateModel`
  - [x] Repository contract: `BusinessBotsRepository`
  - [x] Use cases: `ObserveConnectedBotsUseCase`, `GetConnectedBotsUseCase`, `LoadConnectedBotsUseCase`, `UpdateConnectedBotUseCase`, `DeleteConnectedBotUseCase`, `FindConnectedBotUseCase`
  - [x] Data layer: `BusinessBotMapper`, `BusinessBotsLocalDataSource`, `BusinessBotsRemoteDataSource`, `BusinessBotsRepositoryImpl` (Main-thread safe, adapting `BusinessChatbotController`, `NotificationCenter.updatedChatbot`, and MTProto connected bots protocol)
  - [x] Presentation layer: `BusinessBotsUiState`, `BusinessBotsEvent`, `BusinessBotsViewModel`
- [x] Telegram Timezones & Business Hours Offset (`feature.timezones`)
  - [x] Domain entities: `TimezoneModel` (with formatted UTC offset and display name), `TimezonesStateModel`
  - [x] Repository contract: `TimezonesRepository`
  - [x] Use cases: `ObserveTimezonesUseCase`, `GetTimezonesUseCase`, `LoadTimezonesUseCase`, `FindTimezoneUseCase`, `GetSystemTimezoneIdUseCase`, `GetTimezoneNameUseCase`
  - [x] Data layer: `TimezoneMapper`, `TimezonesLocalDataSource`, `TimezonesRemoteDataSource`, `TimezonesRepositoryImpl` (Main-thread safe, adapting `TimezonesController`, `mainSettings` cache, and `NotificationCenter.timezonesUpdated`)
  - [x] Presentation layer: `TimezonesUiState`, `TimezonesEvent`, `TimezonesViewModel`
- [x] Telegram Stars Bot Revenue, Balance & Transactions (`feature.botstars`)
  - [x] Domain entities: `BotStarsRevenueStatusModel`, `BotStarsRevenueStatsModel`, `BotStarsTransactionType`, `BotStarsTransactionModel`, `ConnectedBotStarRefModel`, `StarRefProgramModel`, `BotStarsStateModel`
  - [x] Repository contract: `BotStarsRepository`
  - [x] Use cases: `ObserveBotStarsStatsUseCase`, `GetBotStarsStatsUseCase`, `ObserveTonStatsUseCase`, `GetTonStatsUseCase`, `ObserveBotTransactionsUseCase`, `LoadBotTransactionsUseCase`, `ObserveConnectedStarBotsUseCase`, `LoadConnectedStarBotsUseCase`, `LoadSuggestedStarBotsUseCase`, `GetAdminedBotsAndChannelsUseCase`
  - [x] Data layer: `BotStarsMapper`, `LegacyBotStarsRepository` (Main-thread safe, adapting `BotStarsController`, `NotificationCenter.botStarsUpdated`, `botStarsTransactionsLoaded`, `channelConnectedBotsUpdate`, and MTProto star revenue protocols)
  - [x] Presentation layer: `BotStarsUiState`, `BotStarsEvent`, `BotStarsViewModel`
- [x] Google Play Billing, Subscriptions & Currency Formatting (`feature.billing`)
  - [x] Domain entities: `BillingProductType`, `BillingPriceModel`, `BillingProductModel`, `BillingPurchaseState`, `BillingPurchaseModel`, `BillingStateModel`
  - [x] Repository contract: `BillingRepository`
  - [x] Use cases: `ObserveBillingStateUseCase`, `GetBillingStateUseCase`, `StartBillingConnectionUseCase`, `GetPremiumProductUseCase`, `FormatCurrencyUseCase`, `GetCurrencyExpUseCase`, `QueryBillingPurchasesUseCase`, `ManageSubscriptionUseCase`
  - [x] Data layer: `BillingMapper`, `LegacyBillingRepository` (Main-thread safe, adapting `BillingController`, `NotificationCenter.billingProductDetailsUpdated`, `billingConfirmPurchaseError`, and Google Play BillingClient query/consume APIs)
  - [x] Presentation layer: `BillingUiState`, `BillingEvent`, `BillingViewModel`
- [x] App Dynamic Launcher Icons & Premium Badging (`feature.launchericon`)
  - [x] Domain entities: `LauncherIconType`, `LauncherIconModel`, `LauncherIconsStateModel`
  - [x] Repository contract: `LauncherIconRepository`
  - [x] Use cases: `ObserveLauncherIconsUseCase`, `GetLauncherIconsUseCase`, `GetActiveLauncherIconUseCase`, `IsLauncherIconEnabledUseCase`, `SetLauncherIconUseCase`, `FixLauncherIconIfNeededUseCase`
  - [x] Data layer: `LauncherIconMapper`, `LegacyLauncherIconRepository` (Main-thread safe, adapting `LauncherIconController` and Android `PackageManager.setComponentEnabledSetting`)
  - [x] Presentation layer: `LauncherIconUiState`, `LauncherIconEvent`, `LauncherIconViewModel`
- [x] Push Notifications, FCM/HMS Registration & Device Tokens (`feature.push`)
  - [x] Domain entities: `PushServiceType`, `PushStatusModel`, `PushRegistrationResult`
  - [x] Repository contract: `PushRepository`
  - [x] Use cases: `ObservePushStatusUseCase`, `GetPushStatusUseCase`, `IsPushAvailableUseCase`, `RequestPushTokenUseCase`, `RegisterPushTokenUseCase`, `ResetPushTokenUseCase`
  - [x] Data layer: `PushMapper`, `LegacyPushRepository` (Main/IO thread safe, adapting `PushListenerController`, `ApplicationLoader.getPushProvider()`, `SharedConfig`, `UserConfig`, and `ConnectionsManager`)
  - [x] Presentation layer: `PushUiState`, `PushEvent`, `PushViewModel`
- [x] Google Cast, Remote Media Client & Media Streaming (`feature.chromecast`)
  - [x] Domain entities: `ChromecastMediaModel`, `ChromecastStateModel`
  - [x] Repository contract: `ChromecastRepository`
  - [x] Use cases: `ObserveChromecastStateUseCase`, `GetChromecastStateUseCase`, `IsCastingUseCase`, `IsMediaPlayingOnCastUseCase`, `CastMediaUseCase`, `StopCastingUseCase`, `SetCastCoverFileUseCase`
  - [x] Data layer: `ChromecastMapper`, `LegacyChromecastRepository` (Main-thread safe, adapting `ChromecastController`, `CastContext`, `SessionManager`, and `RemoteMediaClient`)
  - [x] Presentation layer: `ChromecastUiState`, `ChromecastEvent`, `ChromecastViewModel`
- [x] In-App Hints, Tips & Feature Discovery (`feature.hints`)
  - [x] Domain entities: `HintType`, `HintModel`, `HintsStateModel`
  - [x] Repository contract: `HintsRepository`
  - [x] Use cases: `ObserveHintsUseCase`, `GetHintsStateUseCase`, `GetHintUseCase`, `ShouldShowHintUseCase`, `IncrementHintUseCase`, `DoNotShowAgainHintUseCase`, `ResetHintUseCase`, `ResetAllHintsUseCase`
  - [x] Data layer: `HintMapper`, `LegacyHintsRepository` (Main/IO thread safe, adapting `HintsController`, `MessagesController.getGlobalMainSettings()`, and in-memory test fallback)
  - [x] Presentation layer: `HintsUiState`, `HintsEvent`, `HintsViewModel`
- [x] Group Call & Conference In-Call Messages (`feature.groupcallmsg`)
  - [x] Domain entities: `GroupCallMessageSendStatus`, `GroupCallMessageModel`, `GroupCallMessagesStateModel`
  - [x] Repository contract: `GroupCallMessagesRepository`
  - [x] Use cases: `ObserveGroupCallMessagesUseCase`, `GetGroupCallMessagesUseCase`, `SendGroupCallMessageUseCase`, `PopGroupCallMessageUseCase`, `ClearGroupCallMessagesUseCase`
  - [x] Data layer: `GroupCallMessageMapper`, `LegacyGroupCallMessagesRepository` (adapting `GroupCallMessagesController`, `VoIPService`, and in-memory test fallback)
  - [x] Presentation layer: `GroupCallMessagesUiState`, `GroupCallMessagesEvent`, `GroupCallMessagesViewModel`
- [x] Auto-Save to Gallery Settings, Video Limits & Dialog Exceptions (`feature.gallerysave`)
  - [x] Domain entities: `GallerySavePeerType`, `GallerySaveTargetSettingsModel`, `GallerySaveDialogExceptionModel`, `GallerySaveConfigModel`
  - [x] Repository contract: `GallerySaveRepository`
  - [x] Use cases: `ObserveGallerySaveConfigUseCase`, `GetGallerySaveConfigUseCase`, `GetGallerySaveSettingsUseCase`, `UpdateGallerySaveSettingsUseCase`, `ToggleGallerySavePeerTypeUseCase`, `SetGallerySaveVideoLimitUseCase`, `GetGallerySaveExceptionsUseCase`, `SetGallerySaveExceptionUseCase`, `RemoveGallerySaveExceptionUseCase`, `RemoveAllGallerySaveExceptionsUseCase`
  - [x] Data layer: `GallerySaveMapper`, `LegacyGallerySaveRepository` (Main-thread safe, adapting `SaveToGallerySettingsHelper`, `UserConfig.getSaveGalleryExceptions()`, and in-memory fallback)
  - [x] Presentation layer: `GallerySaveUiState`, `GallerySaveEvent`, `GallerySaveViewModel`
- [x] Adaptive Display Refresh Rate & FPS Metrics (`feature.refreshrate`)
  - [x] Domain entities: `DisplayRefreshModeModel`, `RefreshRateDirection`, `RefreshRateHysteresisConfig`, `RefreshRateStateModel`
  - [x] Repository contract: `RefreshRateRepository`
  - [x] Use cases: `ObserveRefreshRateStateUseCase`, `GetRefreshRateStateUseCase`, `StartRefreshRateTrackingUseCase`, `StopRefreshRateTrackingUseCase`, `ToggleAdaptiveRefreshRateUseCase`, `SetPreferredRefreshRateModeUseCase`, `RecordFrameMetricUseCase`, `ResetRefreshRateStatsUseCase`, `GetDisplayRefreshModesUseCase`
  - [x] Data layer: `RefreshRateMapper`, `LegacyRefreshRateRepository` (Main-thread safe, adapting `RefreshRateController` logic, ring buffer FPS calculations, and hysteresis control)
  - [x] Presentation layer: `RefreshRateUiState`, `RefreshRateEvent`, `RefreshRateViewModel`
- [x] Chat Messages Metadata (Reactions, Paid Media & Stories) (`feature.chatmeta`)
  - [x] Domain entities: `MessageMetadataType`, `MessageMetadataCheckItem`, `ChatMetadataStatsModel`, `ChatMetadataBatchResult`
  - [x] Repository contract: `ChatMessagesMetadataRepository`
  - [x] Use cases: `ObserveChatMetadataStatsUseCase`, `GetChatMetadataStatsUseCase`, `CheckMessagesMetadataUseCase`, `LoadMessagesReactionsUseCase`, `LoadMessagesExtendedMediaUseCase`, `CancelPendingMetadataRequestsUseCase`
  - [x] Data layer: `ChatMetadataMapper`, `LegacyChatMessagesMetadataRepository` (Main-thread safe, adapting `ChatMessagesMetadataController`, `ConnectionsManager` batching, and request cancellation)
  - [x] Presentation layer: `ChatMetadataUiState`, `ChatMetadataEvent`, `ChatMetadataViewModel`
- [x] Picture-in-Picture & Video Window Session (`feature.pip`)
  - [x] Domain entities: `PipState`, `PipSourceModel`, `PipSessionInfo`
  - [x] Repository contract: `PipRepository`
  - [x] Use cases: `ObservePipSessionUseCase`, `GetPipSessionUseCase`, `RegisterPipSourceUseCase`, `UnregisterPipSourceUseCase`, `UpdatePipSourceStateUseCase`, `DispatchPipStateUseCase`, `TriggerPipActionUseCase`, `EvaluatePipEligibilityUseCase`
  - [x] Data layer: `PipMapper`, `LegacyPipRepository` (Main-thread safe, adapting `PipActivityController`, source priority arbitration, MediaSession management, and remote actions)
  - [x] Presentation layer: `PipUiState`, `PipEvent`, `PipViewModel`
- [x] Story & Media Creation Drafts (`feature.drafts`)
  - [x] Domain entities: `DraftType`, `StoryDraftModel`, `DraftsStateModel`
  - [x] Repository contract: `DraftsRepository`
  - [x] Use cases: `ObserveDraftsStateUseCase`, `GetDraftsStateUseCase`, `LoadDraftsUseCase`, `SaveDraftUseCase`, `DeleteDraftUseCase`, `DeleteForEditUseCase`, `GetDraftForEditUseCase`, `CleanupExpiredDraftsUseCase`
  - [x] Data layer: `DraftsMapper`, `LegacyDraftsRepository` (Main-thread safe, adapting `DraftsController`, `MessagesStorage` SQLite persistence, and 7-day expiration cleanup)
  - [x] Presentation layer: `DraftsUiState`, `DraftsEvent`, `DraftsViewModel`
- [x] MTProto File Reference Renewal & Parent Cache (`feature.fileref`)
  - [x] Domain entities: `FileRefParentType`, `FileRefRequestItem`, `FileRefCacheEntry`, `FileRefStatsModel`
  - [x] Repository contract: `FileRefRepository`
  - [x] Use cases: `ObserveFileRefStatsUseCase`, `GetFileRefStatsUseCase`, `RequestReferenceRenewalUseCase`, `NotifyReferenceRenewedUseCase`, `CancelFileRefRequestUseCase`, `ClearFileRefCacheUseCase`
  - [x] Data layer: `FileRefMapper`, `LegacyFileRefRepository` (Main/thread safe, adapting `FileRefController`, 60-second parent response caching, request deduplication by location and parent keys)
  - [x] Presentation layer: `FileRefUiState`, `FileRefEvent`, `FileRefViewModel`
- [x] Hardware Camera & Video Recording (`feature.camera`)
  - [x] Domain entities: `CameraFacing`, `CameraResolutionModel`, `CameraFlashMode`, `CameraRecordingState`, `CameraDeviceModel`, `CameraStateModel`
  - [x] Repository contract: `CameraRepository`
  - [x] Use cases: `ObserveCameraStateUseCase`, `GetCameraStateUseCase`, `InitCamerasUseCase`, `SelectCameraUseCase`, `SwitchCameraUseCase`, `SetCameraFlashModeUseCase`, `ToggleMirrorFrontCameraUseCase`, `ChooseOptimalResolutionUseCase`, `NotifyCameraRecordingUseCase`
  - [x] Data layer: `CameraMapper`, `LegacyCameraRepository` (thread safe, adapting `CameraController`, resolution selection heuristics, headless fallback)
  - [x] Presentation layer: `CameraUiState`, `CameraEvent`, `CameraViewModel`
- [x] Keep-Media Cache Retention & Dialog Exceptions (`feature.cachebychats`)
  - [x] Domain entities: `CacheChatType`, `KeepMediaDuration`, `KeepMediaExceptionModel`, `CacheByChatsConfigModel`
  - [x] Repository contract: `CacheByChatsRepository`
  - [x] Use cases: `ObserveCacheByChatsConfigUseCase`, `GetCacheByChatsConfigUseCase`, `SetKeepMediaDurationUseCase`, `SetKeepMediaExceptionUseCase`, `RemoveKeepMediaExceptionUseCase`, `ClearKeepMediaExceptionsUseCase`
  - [x] Data layer: `CacheByChatsMapper`, `LegacyCacheByChatsRepository` (thread safe, adapting `CacheByChatsController`, duration presets, dialog exceptions binary serialization)
  - [x] Presentation layer: `CacheByChatsUiState`, `CacheByChatsEvent`, `CacheByChatsViewModel`
- [x] Chat Draft Message Height Measure (`feature.draftmeasure`)
  - [x] Domain entities: `DraftMeasureTarget`, `DraftMeasureViewport`, `DraftMeasureResult`, `DraftMeasureConfig`
  - [x] Repository contract: `DraftMeasureRepository`
  - [x] Use cases: `CalculateDraftMeasureOverrideUseCase`, `SetDraftMeasureTargetUseCase`, `OnDraftMessageIdChangedUseCase`, `SetPreviousMessageHeightUseCase`, `ResetDraftMeasureTargetUseCase`, `ObserveDraftMeasureConfigUseCase`, `GetDraftMeasureConfigUseCase`
  - [x] Data layer: `DraftMeasureMapper`, `LegacyDraftMeasureRepository` (thread safe, adapting `ChatActivityDraftMessageMeasureController`, viewport measurement constraints, headless in-memory fallback)
  - [x] Presentation layer: `DraftMeasureUiState`, `DraftMeasureEvent`, `DraftMeasureViewModel`
- [x] Chat Bottom Views Visibility Arbitration (`feature.bottomviews`)
  - [x] Domain entities: `BottomContainerType`, `BottomViewsVisibilityState`
  - [x] Repository contract: `BottomViewsVisibilityRepository`
  - [x] Use cases: `GetBottomViewVisibilityUseCase`, `SetBottomViewVisibleUseCase`, `GetPriorityBottomContainerUseCase`, `GetBottomViewsStateUseCase`, `ObserveBottomViewsVisibilityUseCase`
  - [x] Data layer: `BottomViewsVisibilityMapper`, `LegacyBottomViewsVisibilityRepository` (thread safe, adapting `ChatActivityBottomViewsVisibilityController`, bitwise priority calculation, headless in-memory fallback)
  - [x] Presentation layer: `BottomViewsUiState`, `BottomViewsEvent`, `BottomViewsViewModel`
- [x] Floating Debug Tools & Overlay (`feature.floatingdebug`)
  - [x] Domain entities: `DebugItemKind`, `DebugItemModel`, `FloatingDebugState`
  - [x] Repository contract: `FloatingDebugRepository`
  - [x] Use cases: `IsFloatingDebugActiveUseCase`, `SetFloatingDebugActiveUseCase`, `ToggleFloatingDebugActiveUseCase`, `GetFloatingDebugItemsUseCase`, `RegisterFloatingDebugItemsUseCase`, `ClearFloatingDebugItemsUseCase`, `ObserveFloatingDebugStateUseCase`, `GetFloatingDebugStateUseCase`
  - [x] Data layer: `FloatingDebugMapper`, `LegacyFloatingDebugRepository` (thread safe, adapting `FloatingDebugController`, items registration, headless in-memory fallback)
  - [x] Presentation layer: `FloatingDebugUiState`, `FloatingDebugEvent`, `FloatingDebugViewModel`
- [x] Window Insets & In-App Keyboard Offsets (`feature.keyboardinsets`)
  - [x] Domain entities: `KeyboardVisibilityState`, `InAppImeMode`, `KeyboardInsetsModel`
  - [x] Repository contract: `KeyboardInsetsRepository`
  - [x] Use cases: `RequestInAppKeyboardHeightUseCase`, `ResetInAppKeyboardHeightUseCase`, `RequestInAppKeyboardHeightWithNavbarUseCase`, `UpdateSystemInsetsUseCase`, `GetKeyboardInsetsUseCase`, `ObserveKeyboardInsetsUseCase`
  - [x] Data layer: `KeyboardInsetsMapper`, `LegacyKeyboardInsetsRepository` (thread safe, adapting `WindowInsetsInAppController`, effective bottom inset calculation, headless in-memory fallback)
  - [x] Presentation layer: `KeyboardInsetsUiState`, `KeyboardInsetsEvent`, `KeyboardInsetsViewModel`
- [x] Main Navigation Tabs (`feature.maintabs`)
  - [x] Domain entities: `MainTabType`, `MainTabBadgeModel`, `MainTabsConfigModel`
  - [x] Repository contract: `MainTabsRepository`
  - [x] Use cases: `ObserveMainTabsConfigUseCase`, `GetMainTabsConfigUseCase`, `SetMainTabsVisibleUseCase`, `SelectMainTabUseCase`, `SetShowCallsTabUseCase`, `UpdateChatsUnreadCountUseCase`, `SetContactsPermissionWarningUseCase`
  - [x] Data layer: `MainTabsMapper`, `LegacyMainTabsRepository` (thread safe, adapting `MainTabsActivityController`, tab positioning, showCallsTab arbitration, badges, headless in-memory fallback)
  - [x] Presentation layer: `MainTabsUiState`, `MainTabsEvent`, `MainTabsViewModel`
- [x] Instant View Rich Captions & Formatting (`feature.richcaption`)
  - [x] Domain entities: `CaptionSpanType`, `CaptionEntitySpan`, `RichCaptionModel`, `CaptionMeasureSpec`, `CaptionHitResult`
  - [x] Repository contract: `RichCaptionRepository`
  - [x] Use cases: `ObserveRichCaptionUseCase`, `GetRichCaptionUseCase`, `SetRichCaptionTextUseCase`, `SetRichCaptionCreditUseCase`, `SetRichCaptionLockedUseCase`, `CalculateCaptionMeasureWidthUseCase`, `CheckCaptionPressHitUseCase`, `ClearRichCaptionUseCase`
  - [x] Data layer: `RichCaptionMapper`, `LegacyRichCaptionRepository` (thread safe, adapting `RichCaptionController`, available width calculation, press hit detection, headless in-memory fallback)
  - [x] Presentation layer: `RichCaptionUiState`, `RichCaptionEvent`, `RichCaptionViewModel`
- [x] AdjustPan Layout Animation & Geometry (`feature.adjustpan`)
  - [x] Domain entities: `PanCalculationSpec`, `PanTransitionPlan`, `PanProgressResult`, `PanTransitionState`
  - [x] Repository contract: `AdjustPanRepository`
  - [x] Use cases: `CalculatePanTransitionPlanUseCase`, `ComputePanProgressUseCase`, `ObserveAdjustPanStateUseCase`, `GetAdjustPanStateUseCase`, `SetAdjustPanEnabledUseCase`, `StartAdjustPanTransitionUseCase`, `UpdateAdjustPanTransitionUseCase`, `StopAdjustPanTransitionUseCase`, `ResetAdjustPanUseCase`
  - [x] Data layer: `AdjustPanMapper`, `LegacyAdjustPanRepository` (thread safe, adapting `AdjustPanLayoutHelper` math, trajectory calculation, headless in-memory fallback)
  - [x] Presentation layer: `AdjustPanUiState`, `AdjustPanEvent`, `AdjustPanViewModel`
- [x] Interactive Pull-Down Keyboard Dismissal (`feature.keyboardhide`)
  - [x] Domain entities: `KeyboardDragSpec`, `KeyboardDismissDecision`, `KeyboardHideProgressResult`, `KeyboardHideState`
  - [x] Repository contract: `KeyboardHideRepository`
  - [x] Use cases: `CalculateKeyboardHideProgressUseCase`, `EvaluateKeyboardDismissDecisionUseCase`, `ObserveKeyboardHideStateUseCase`, `GetKeyboardHideStateUseCase`, `SetKeyboardHideEnabledUseCase`, `StartKeyboardHideMovingUseCase`, `UpdateKeyboardHideMovingUseCase`, `EndKeyboardHideMovingUseCase`, `FinishKeyboardHideDismissUseCase`, `ResetKeyboardHideUseCase`
  - [x] Data layer: `KeyboardHideMapper`, `LegacyKeyboardHideRepository` (thread safe, adapting `KeyboardHideHelper`, threshold evaluation, scroll arbitration, headless in-memory fallback)
  - [x] Presentation layer: `KeyboardHideUiState`, `KeyboardHideEvent`, `KeyboardHideViewModel`
- [x] Business Recipients Configuration & Targeting (`feature.businessrecipients`)
  - [x] Domain entities: `RecipientFilterType`, `BusinessRecipientsModel`, `RecipientValidationResult`
  - [x] Repository contract: `BusinessRecipientsRepository`
  - [x] Use cases: `ObserveBusinessRecipientsUseCase`, `GetBusinessRecipientsUseCase`, `SetBusinessRecipientsUseCase`, `ToggleExcludeSelectedUseCase`, `ToggleRecipientFilterUseCase`, `AddSelectedUsersUseCase`, `RemoveSelectedUserUseCase`, `AddExcludedUsersUseCase`, `RemoveExcludedUserUseCase`, `CheckRecipientsChangesUseCase`, `ValidateBusinessRecipientsUseCase`, `ResetBusinessRecipientsUseCase`
  - [x] Data layer: `BusinessRecipientsMapper`, `BusinessRecipientsLocalDataSource`, `BusinessRecipientsRepositoryImpl` (thread safe, adapting `BusinessRecipientsHelper`, bitmask flags, mutual exclusion, validation, change detection)
  - [x] Presentation layer: `BusinessRecipientsUiState`, `BusinessRecipientsEvent`, `BusinessRecipientsViewModel`
- [x] Interactive Pinch-To-Zoom Media Gestures & Overlay (`feature.pinchtozoom`)
  - [x] Domain entities: `PinchTouchPoint`, `PinchGestureSpec`, `PinchGestureDecision`, `PinchTransform`, `PinchImageDimensions`, `PinchBoundsResult`, `PinchZoomState`
  - [x] Repository contract: `PinchToZoomRepository`
  - [x] Use cases: `ObservePinchZoomStateUseCase`, `GetPinchZoomStateUseCase`, `CalculatePinchScaleUseCase`, `CalculatePinchTranslationUseCase`, `CalculatePinchTransformUseCase`, `CalculatePinchImageBoundsUseCase`, `EvaluatePinchGestureUseCase`, `StartPinchZoomUseCase`, `UpdatePinchZoomUseCase`, `FinishPinchZoomUseCase`, `ResetPinchZoomUseCase`
  - [x] Data layer: `PinchToZoomMapper`, `LegacyPinchToZoomRepository` (thread safe, adapting `PinchToZoomHelper`, gesture detection threshold 1.005f, 2D transform calculations, aspect ratio padding interpolation)
  - [x] Presentation layer: `PinchToZoomUiState`, `PinchToZoomEvent`, `PinchToZoomViewModel`
- [x] Recycler List Animated Scroll & Transitions (`feature.recyclerscroll`)
  - [x] Domain entities: `ScrollDirection`, `ScrollAnimationSpec`, `ScrollAnimationPlan`, `ScrollViewTranslation`, `ScrollEligibility`, `RecyclerScrollState`
  - [x] Repository contract: `RecyclerScrollRepository`
  - [x] Use cases: `ObserveRecyclerScrollStateUseCase`, `GetRecyclerScrollStateUseCase`, `EvaluateScrollEligibilityUseCase`, `CalculateScrollAnimationPlanUseCase`, `CalculateScrollLengthUseCase`, `ComputeScrollViewTranslationsUseCase`, `StartRecyclerScrollUseCase`, `UpdateRecyclerScrollProgressUseCase`, `FinishRecyclerScrollUseCase`, `CancelRecyclerScrollUseCase`, `ResetRecyclerScrollUseCase`
  - [x] Data layer: `RecyclerScrollMapper`, `LegacyRecyclerScrollRepository` (thread safe, adapting `RecyclerAnimationScrollHelper`, duration formulas 150ms/600ms/dynamic 300..1300ms, scrollLength and translation math, eligibility rules)
  - [x] Presentation layer: `RecyclerScrollUiState`, `RecyclerScrollEvent`, `RecyclerScrollViewModel`
- [x] Interactive Emoji Animations, Tap Protocols & Overlay (`feature.emojieffects`)
  - [x] Domain entities: `EmojiInteractionAction`, `EmojiInteractionSession`, `EmojiAnimationQuotaStatus`, `EmojiAnimationQuotaResult`, `EmojiOverlayGeometry`, `EmojiEffectItem`, `EmojiEffectsState`
  - [x] Repository contract: `EmojiEffectsRepository`
  - [x] Use cases: `NormalizeEmojiUseCase`, `EvaluateEmojiSupportUseCase`, `RecordEmojiTapUseCase`, `EncodeEmojiInteractionsJsonUseCase`, `DecodeEmojiInteractionsJsonUseCase`, `CalculateEmojiBoundsUseCase`, `CalculateEmojiOverlayPositionUseCase`, `EvaluateAnimationQuotaUseCase`, `ObserveEmojiEffectsStateUseCase`, `GetEmojiEffectsStateUseCase`, `StartEmojiEffectUseCase`, `UpdateEmojiEffectProgressUseCase`, `DismissEmojiEffectUseCase`, `ClearEmojiEffectsUseCase`
  - [x] Data layer: `EmojiEffectsMapper`, `LegacyEmojiEffectsRepository` (thread safe, adapting `EmojiAnimationsOverlay`, interaction JSON encoding/decoding, quota limits 12 global / 4 per msg, bounds and position calculations, emoji normalization)
  - [x] Presentation layer: `EmojiEffectsUiState`, `EmojiEffectsEvent`, `EmojiEffectsViewModel`
- [x] Mentions, Hashtags, Bot Commands & Autocomplete (`feature.mentions`)
  - [x] Domain entities: `MentionTriggerType`, `MentionQuery`, `MentionCandidate`, `MentionReplacement`, `MentionsState`
  - [x] Repository contract: `MentionsRepository`
  - [x] Use cases: `ValidateUsernameUseCase`, `ParseMentionQueryUseCase`, `FilterMentionsUseCase`, `FormatMentionReplacementUseCase`, `ObserveMentionsStateUseCase`, `GetMentionsStateUseCase`, `UpdateMentionQueryUseCase`, `SetMentionCandidatesUseCase`, `DismissMentionsUseCase`, `ClearMentionsUseCase`
  - [x] Data layer: `MentionsMapper`, `LegacyMentionsRepository` (thread safe, adapting `MentionsAdapter`, trigger parsing for @, #, /, :, inline context bots, quick replies, candidate filtering and replacement formatting)
  - [x] Presentation layer: `MentionsUiState`, `MentionsEvent`, `MentionsViewModel`
- [x] Shared Media, Tabs, Filters, Periods & Selection (`feature.sharedmedia`)
  - [x] Domain entities: `SharedMediaTabType`, `SharedMediaFilterType`, `SharedMediaItem`, `SharedMediaPeriod`, `SharedMediaTabSpec`, `SharedMediaSelectionState`, `SharedMediaState`
  - [x] Repository contract: `SharedMediaRepository`
  - [x] Use cases: `ResolveAvailableTabsUseCase`, `FilterSharedMediaUseCase`, `GroupMediaByMonthUseCase`, `CalculateMediaSelectionUseCase`, `ObserveSharedMediaStateUseCase`, `GetSharedMediaStateUseCase`, `SelectSharedMediaTabUseCase`, `SetSharedMediaFilterUseCase`, `ToggleMediaSelectionUseCase`, `ClearMediaSelectionUseCase`
  - [x] Data layer: `SharedMediaMapper`, `LegacySharedMediaRepository` (thread safe, adapting `SharedMediaLayout` and `SharedMediaData`, tab availability rules across dialog types, photo/video filtering, monthly section grouping, fast scroll periods calculation, multi-selection rights)
  - [x] Presentation layer: `SharedMediaUiState`, `SharedMediaEvent`, `SharedMediaViewModel`
- [x] Content Preview & Long-Press Gestures (`feature.contentpreview`)
  - [x] Domain entities: `PreviewContentType`, `PreviewActionType`, `PreviewActionItem`, `ContentPreviewGesture`, `ContentPreviewItem`, `ContentPreviewState`
  - [x] Repository contract: `ContentPreviewRepository`
  - [x] Use cases: `EvaluatePreviewEligibilityUseCase`, `CalculatePreviewDragUseCase`, `ResolvePreviewActionsUseCase`, `ObserveContentPreviewStateUseCase`, `GetContentPreviewStateUseCase`, `OpenContentPreviewUseCase`, `UpdatePreviewDragUseCase`, `DismissContentPreviewUseCase`, `ClearContentPreviewUseCase`, `TriggerPreviewActionUseCase`
  - [x] Data layer: `ContentPreviewMapper`, `LegacyContentPreviewRepository` (thread safe, adapting `ContentPreviewViewer`, gesture drag calculations, action resolution for stickers, GIFs, emoji, and haptic feedback thresholds)
  - [x] Presentation layer: `ContentPreviewUiState`, `ContentPreviewEvent`, `ContentPreviewViewModel`
- [x] Emoji, Sticker & GIF Picker Keyboard Panel (`feature.emojipicker`)
  - [x] Domain entities: `EmojiPickerTabType`, `EmojiCategoryType`, `EmojiItem`, `StickerItem`, `GifItem`, `StickerPackItem`, `EmojiPickerFilter`, `EmojiPickerState`
  - [x] Repository contract: `EmojiPickerRepository`
  - [x] Use cases: `ResolveAvailablePickerTabsUseCase`, `FilterEmojiItemsUseCase`, `FilterStickersUseCase`, `FilterGifsUseCase`, `ObserveEmojiPickerStateUseCase`, `GetEmojiPickerStateUseCase`, `SelectPickerTabUseCase`, `UpdatePickerSearchQueryUseCase`, `ToggleStickerFavoriteUseCase`, `ClearRecentPickerItemsUseCase`
  - [x] Data layer: `EmojiPickerMapper`, `LegacyEmojiPickerRepository` (thread safe, adapting `EmojiView`, tab configuration, item searching & filtering, sticker favorites toggle, and clear recent history)
  - [x] Presentation layer: `EmojiPickerUiState`, `EmojiPickerEvent`, `EmojiPickerViewModel`
- [x] Chat Attachment Dialog & Layouts (`feature.chatattach`)
  - [x] Domain entities: `ChatAttachLayoutType`, `ChatAttachItem`, `ChatAttachSendOptions`, `ChatAttachPermissions`, `CaptionLimitInfo`, `ChatAttachState`
  - [x] Repository contract: `ChatAttachRepository`
  - [x] Use cases: `ResolveAvailableAttachLayoutsUseCase`, `CalculateAttachCaptionLimitUseCase`, `ToggleAttachItemSelectionUseCase`, `ValidateSendOptionsUseCase`, `ObserveChatAttachStateUseCase`, `GetChatAttachStateUseCase`, `SelectAttachLayoutUseCase`, `UpdateAttachSendOptionsUseCase`, `ClearAttachSelectionUseCase`, `OpenChatAttachAlertUseCase`
  - [x] Data layer: `ChatAttachMapper`, `LegacyChatAttachRepository` (thread safe, adapting `ChatAttachAlert`, layout permissions arbitration, multi-selection ordering, caption limit calculations, and send options)
  - [x] Presentation layer: `ChatAttachUiState`, `ChatAttachEvent`, `ChatAttachViewModel`
- [x] PhotoViewer / Fullscreen Media Viewer, Gestures, Playback & Editor (`feature.photoviewer`)
  - [x] Domain entities: `ViewerMediaType`, `ViewerSelectType`, `ViewerEditMode`, `ViewerActionType`, `PhotoViewerMediaItem`, `PhotoViewerPlaybackState`, `PhotoViewerTransform`, `PhotoViewerState`
  - [x] Repository contract: `PhotoViewerRepository`
  - [x] Use cases: `CalculateMediaPagingUseCase`, `CalculateZoomTransformUseCase`, `ValidateViewerActionsUseCase`, `ResolveMediaQualityUseCase`, `ObservePhotoViewerStateUseCase`, `GetPhotoViewerStateUseCase`, `OpenPhotoViewerUseCase`, `NavigatePhotoViewerUseCase`, `UpdatePlaybackStateUseCase`, `ClosePhotoViewerUseCase`
  - [x] Data layer: `PhotoViewerMapper`, `LegacyPhotoViewerRepository` (thread safe, adapting `PhotoViewer.java`'s 23k+ lines, pinch/zoom clamp math, 90° rotation snapping, video quality resolution, timecode formatters, and select/edit mode conversions)
  - [x] Presentation layer: `PhotoViewerUiState`, `PhotoViewerEvent`, `PhotoViewerViewModel`
- [x] Chat Message Input, Formatting, Voice Recording, Panels & Reply Bar (`feature.chatinput`)
  - [x] Domain entities: `RecordType`, `RecordStatus`, `EnterViewPanelMode`, `TextFormatStyle`, `ChatInputReplyQuote`, `ChatInputSendOptions`, `ChatInputRecordState`, `ChatInputState`
  - [x] Repository contract: `ChatInputRepository`
  - [x] Use cases: `CalculateSendButtonStateUseCase`, `FormatTextSelectionUseCase`, `ValidateVoiceRecordActionUseCase`, `ResolvePanelVisibilityUseCase`, `ObserveChatInputStateUseCase`, `GetChatInputStateUseCase`, `SetChatInputTextUseCase`, `SetChatInputPanelModeUseCase`, `SetChatInputReplyUseCase`, `ClearChatInputReplyUseCase`
  - [x] Data layer: `ChatInputMapper`, `LegacyChatInputRepository` (thread safe, adapting `ChatActivityEnterView.java`'s 15k+ lines, recording duration formatters, send button vs mic arbitration, and reply/edit preview quotes)
  - [x] Presentation layer: `ChatInputUiState`, `ChatInputEvent`, `ChatInputViewModel`
- [x] Audio & Voice/Video Playback, Playlist Queue, Speed & Proximity (`feature.audioplayer`)
  - [x] Domain entities: `AudioTrackType`, `AudioPlaybackStatus`, `RepeatMode`, `AudioOutputRoute`, `AudioTrackModel`, `EqualizerBand`, `EqualizerState`, `AudioPlaybackState`
  - [x] Repository contract: `AudioPlayerRepository`
  - [x] Use cases: `ObservePlaybackStateUseCase`, `GetPlaybackStateUseCase`, `PlayTrackUseCase`, `TogglePlayPauseUseCase`, `SeekAudioUseCase`, `NavigatePlaylistUseCase`, `CyclePlaybackSpeedUseCase`, `CycleRepeatModeUseCase`, `ToggleShuffleUseCase`, `HandleProximitySensorUseCase`, `ConfigureEqualizerUseCase`
  - [x] Data layer: `AudioPlayerMapper`, `LegacyAudioPlayerRepository` (thread safe StateFlow engine adapting `MediaController.java`, playlist shuffle/repeat arbitration, seek clamping, and proximity ear-piece routing)
  - [x] Presentation layer: `AudioPlayerUiState`, `AudioPlayerEvent`, `AudioPlayerViewModel`
- [x] Message Sending Pipeline, Media Uploads, Albums & Forwards (`feature.sendmessages`)
  - [x] Domain entities: `SendMediaType`, `SendStatus`, `ForwardMode`, `SendOptionsModel`, `SendMediaItem`, `SendAlbumModel`, `ForwardRequestModel`, `PendingSendModel`, `SendMessagesState`
  - [x] Repository contract: `SendMessagesRepository`
  - [x] Use cases: `SendTextMessageUseCase`, `SendMediaMessageUseCase`, `SendMediaAlbumUseCase`, `ForwardMessagesUseCase`, `RetrySendMessageUseCase`, `CancelSendMessageUseCase`, `ObservePendingSendsUseCase`, `ValidateSendEligibilityUseCase`
  - [x] Data layer: `SendMessagesMapper`, `LegacySendMessagesRepository` (thread-safe StateFlow engine adapting `SendMessagesHelper.java`'s 14k+ lines, batched album constraints, chunked upload tracking, retry/cancellation handling, and forward modes)
  - [x] Presentation layer: `SendMessagesUiState`, `SendMessagesEvent`, `SendMessagesViewModel`
- [x] Memory Cache Tiers, Downscaling, Filter Specs & Request Pipeline (`feature.imageloader`)
  - [x] Domain entities: `ImageCacheTier`, `ImageLoadingStatus`, `FrameExtractType`, `ImageFilterSpec`, `ImageDownscaleSpec`, `ImageRequestModel`, `ImageCacheStatsModel`, `ImageLoaderState`
  - [x] Repository contract: `ImageLoaderRepository`
  - [x] Use cases: `ParseImageFilterUseCase`, `FormatImageFilterUseCase`, `BuildImageCacheKeyUseCase`, `CalculateImageDownscaleUseCase`, `EvaluateImageCacheEligibilityUseCase`, `ObserveImageLoaderStateUseCase`, `GetImageLoaderStateUseCase`, `EnqueueImageRequestUseCase`, `CancelImageRequestUseCase`, `TrimImageMemoryUseCase`, `ClearImageCacheUseCase`
  - [x] Data layer: `ImageLoaderMapper`, `LegacyImageLoaderRepository` (thread-safe LRU tier caches, memory pressure level trimming, hit/miss tracking, and request deduplication)
  - [x] Presentation layer: `ImageLoaderUiState`, `ImageLoaderEvent`, `ImageLoaderViewModel`
- [x] Downloads Queue, Auto-Download Rules & Network Presets (`feature.downloadmanager`)
  - [x] Domain entities: `AutoDownloadMediaType`, `AutoDownloadNetwork`, `PeerTypePreset`, `DownloadItemStatus`, `DownloadPresetModel`, `DownloadItemModel`, `DownloadManagerStats`, `DownloadManagerState`
  - [x] Repository contract: `DownloadManagerRepository`
  - [x] Use cases: `EvaluateAutoDownloadEligibilityUseCase`, `ObserveDownloadManagerStateUseCase`, `GetDownloadManagerStateUseCase`, `EnqueueDownloadUseCase`, `PauseDownloadUseCase`, `ResumeDownloadUseCase`, `CancelDownloadUseCase`, `RetryDownloadUseCase`, `ClearRecentDownloadsUseCase`, `MarkDownloadsAsViewedUseCase`, `UpdateDownloadProgressUseCase`, `CalculateDownloadSpeedUseCase`, `SetDownloadNetworkTypeUseCase`, `UpdateDownloadPresetUseCase`
  - [x] Data layer: `DownloadManagerMapper`, `LegacyDownloadManagerRepository` (thread-safe StateFlow engine adapting `DownloadController.java`'s 1800+ lines, multi-network preset masks, moving speed tracking, and download lifecycle states)
  - [x] Presentation layer: `DownloadManagerUiState`, `DownloadManagerEvent`, `DownloadManagerViewModel`
- [x] Language Packs, Pluralization, Relative Timestamps & RTL Detection (`feature.localization`)
  - [x] Domain entities: `PluralQuantity`, `NameDisplayOrder`, `LocaleModel`, `RelativeTimeModel`, `LocalizationConfigModel`, `LocalizationState`
  - [x] Repository contract: `LocalizationRepository`
  - [x] Use cases: `ResolvePluralQuantityUseCase`, `FormatRelativeTimestampUseCase`, `FormatFullNameUseCase`, `FormatNumberWithSuffixUseCase`, `DetectRtlLanguageUseCase`, `ObserveLocalizationStateUseCase`, `GetLocalizationStateUseCase`, `ApplyLocaleUseCase`, `Toggle24HourFormatUseCase`, `SetNameDisplayOrderUseCase`
  - [x] Data layer: `LocalizationMapper`, `LegacyLocalizationRepository` (thread-safe StateFlow engine adapting `LocaleController.java`'s 4,500+ lines, pluralization rules for Slavic/Polish/Arabic/Germanic languages, string overrides, and RTL detection)
  - [x] Presentation layer: `LocalizationUiState`, `LocalizationEvent`, `LocalizationViewModel`
- [x] Custom Notification Sounds, Ringtones & Cloud Uploader (`feature.ringtones`)
  - [x] Domain entities: `RingtoneErrorCode`, `RingtoneUploadStatus`, `RingtoneModel`, `RingtoneValidationResult`, `RingtoneLimitsModel`, `RingtoneState`
  - [x] Repository contract: `RingtoneRepository`
  - [x] Use cases: `ValidateRingtoneEligibilityUseCase`, `ObserveRingtonesUseCase`, `ObserveRingtoneStateUseCase`, `GetRingtonesUseCase`, `GetRingtoneByIdUseCase`, `GetRingtoneSoundPathUseCase`, `AddRingtoneUseCase`, `RemoveRingtoneUseCase`, `SaveRingtoneFromDocumentUseCase`, `UploadRingtoneUseCase`, `CancelRingtoneUploadUseCase`, `RefreshRingtonesUseCase`, `SelectRingtoneUseCase`
  - [x] Data layer: `RingtoneMapper`, `LegacyRingtoneRepository` (thread-safe StateFlow engine adapting `RingtoneDataStore.java` and `RingtoneUploader.java`, custom ringtone duration & size limits, sound path resolution, and upload cancellation)
  - [x] Presentation layer: `RingtoneUiState`, `RingtoneEvent`, `RingtoneViewModel`
- [x] Network Traffic & Data Usage Statistics (`feature.networkstats`)
  - [x] Domain entities: `NetworkType`, `TrafficCategory`, `TrafficItemModel`, `NetworkStatsSummaryModel`, `NetworkStatsState`
  - [x] Repository contract: `NetworkStatsRepository`
  - [x] Use cases: `ObserveNetworkStatsUseCase`, `ObserveAllNetworkStatsUseCase`, `GetNetworkStatsUseCase`, `GetAllNetworkStatsUseCase`, `IncrementTrafficBytesUseCase`, `IncrementTrafficItemsUseCase`, `IncrementCallsTimeUseCase`, `ResetNetworkStatsUseCase`, `RefreshNetworkStatsUseCase`, `CalculateMessagesTrafficUseCase`, `FormatTrafficBytesUseCase`, `FormatCallsDurationUseCase`
  - [x] Data layer: `NetworkStatsMapper`, `LegacyNetworkStatsRepository` (thread-safe StateFlow engine adapting `StatsController.java`'s cellular/WiFi/roaming tracking, sent/received bytes & items counters, total call duration, messages traffic deduction formula, and stats resetting)
  - [x] Presentation layer: `NetworkStatsUiState`, `NetworkStatsEvent`, `NetworkStatsViewModel`
- [x] Inbound Push Notifications, Payloads & Remote Actions (`feature.pushlistener`)
  - [x] Domain entities: `PushType`, `PushActionType`, `PushDecryptStatus`, `PushPayloadModel`, `PushProcessResult`, `PushListenerState`
  - [x] Repository contract: `PushListenerRepository`
  - [x] Use cases: `ObservePushListenerStateUseCase`, `ObserveIncomingPushesUseCase`, `GetPushListenerStateUseCase`, `ProcessIncomingPushUseCase`, `RegisterPushTokenUseCase`, `TogglePushListeningUseCase`, `DeterminePushActionTypeUseCase`, `ParsePushJsonPayloadUseCase`
  - [x] Data layer: `PushListenerMapper`, `LegacyPushListenerRepository` (thread-safe StateFlow/SharedFlow engine adapting `PushListenerController.java`'s 1700+ lines, FCM/Huawei multi-provider handling, JSON/TL remote payload parsing, VoIP call triggers, datacenter updates, and decryption error tracking)
  - [x] Presentation layer: `PushListenerUiState`, `PushListenerEvent`, `PushListenerViewModel`
- [x] In-App Web Browser, Custom Tabs & Deep Link Routing (`feature.browser`)
  - [x] Domain entities: `BrowserType`, `UrlTargetType`, `UrlSafetyCheckResult`, `BrowserHistoryEntryModel`, `BrowserSettingsModel`, `BrowserState`
  - [x] Repository contract: `BrowserRepository`
  - [x] Use cases: `ClassifyUrlTargetUseCase`, `ExtractUsernameFromUrlUseCase`, `CheckUrlSafetyUseCase`, `ObserveBrowserStateUseCase`, `GetBrowserStateUseCase`, `UpdateBrowserSettingsUseCase`, `OpenBrowserUrlUseCase`, `ManageBrowserHistoryUseCase`
  - [x] Data layer: `BrowserMapper`, `LegacyBrowserRepository` (thread-safe StateFlow engine adapting `Browser.java`'s 870 lines, Custom Tabs service binding, in-app webview mode, anti-phishing IDN homoglyph punycode detection, and browser history)
  - [x] Presentation layer: `BrowserUiState`, `BrowserEvent`, `BrowserViewModel`
- [x] Power Saving, Battery Optimization & Animation Throttling (`feature.litemode`)
  - [x] Domain entities: `LiteModeFlag`, `LiteModePreset`, `LiteModeState`
  - [x] Repository contract: `LiteModeRepository`
  - [x] Use cases: `CalculateEffectiveFlagsUseCase`, `CheckLiteModeFlagUseCase`, `ResolvePresetUseCase`, `ObserveLiteModeStateUseCase`, `GetLiteModeStateUseCase`, `ToggleLiteModeFlagUseCase`, `SetLiteModePresetUseCase`, `UpdatePowerSaverThresholdUseCase`
  - [x] Data layer: `LiteModeMapper`, `LegacyLiteModeRepository` (thread-safe StateFlow engine adapting `LiteMode.java`'s 365 lines, battery capacity monitoring, power-saver auto-activation, premium emoji flag preprocessing, and tablet layout overrides)
  - [x] Presentation layer: `LiteModeUiState`, `LiteModeEvent`, `LiteModeViewModel`
- [x] Global Server Limits, Stars/TON Configuration & Feature Flags (`feature.appconfig`)
  - [x] Domain entities: `StarsConfigModel`, `TonConfigModel`, `PollsConfigModel`, `RichMessageConfigModel`, `AiComposeConfigModel`, `AppLimitsConfigModel`, `AppGlobalConfigState`
  - [x] Repository contract: `AppConfigRepository`
  - [x] Use cases: `GetAppConfigUseCase`, `ObserveAppConfigUseCase`, `GetMessageLimitsUseCase`, `GetStarsPricingConfigUseCase`, `GetTonPricingConfigUseCase`, `GetRichMessageLimitsUseCase`, `GetPollsConfigUseCase`, `GetAiComposeConfigUseCase`, `GetAppLimitsUseCase`, `ReloadAppConfigUseCase`, `UpdateAppConfigValueUseCase`
  - [x] Data layer: `AppConfigMapper`, `LegacyAppConfigRepository` (thread-safe StateFlow engine adapting `AppGlobalConfig.java`'s 411 lines and `MessagesController.java` config fields, reactive to `NotificationCenter.appConfigUpdated`)
  - [x] Presentation layer: `AppConfigUiState`, `AppConfigEvent`, `AppConfigViewModel`
- [x] Background Media Auto-Delete & Cache Eviction (`feature.autodeletemedia`)
  - [x] Domain entities: `CacheLimitConfig`, `MediaScanFileModel`, `AutoDeleteRunResult`, `AutoDeleteTaskState`
  - [x] Repository contract: `AutoDeleteMediaRepository`
  - [x] Use cases: `CheckShouldRunCleanupUseCase`, `CalculateEvictionCandidatesUseCase`, `LockFileUseCase`, `UnlockFileUseCase`, `IsFileLockedUseCase`, `RunAutoDeleteCleanupUseCase`, `ObserveAutoDeleteStateUseCase`, `GetAutoDeleteStateUseCase`
  - [x] Data layer: `AutoDeleteMediaMapper`, `LegacyAutoDeleteMediaRepository` (thread-safe StateFlow engine adapting `AutoDeleteMediaTask.java`'s 278 lines, LRU file eviction, cache limit threshold calculation, 24-hour interval guards, and in-use file locking)
  - [x] Presentation layer: `AutoDeleteMediaUiState`, `AutoDeleteMediaEvent`, `AutoDeleteMediaViewModel`
- [x] Fast Re-Login & Session Logout Tokens (`feature.authtokens`)
  - [x] Domain entities: `AuthTokenUserInfoModel`, `SavedLoginTokenModel`, `SavedLogoutTokenModel`, `AuthTokensState`
  - [x] Repository contract: `AuthTokensRepository`
  - [x] Use cases: `PruneTokensListUseCase`, `ValidateAuthTokenFormatUseCase`, `ObserveAuthTokensStateUseCase`, `GetAuthTokensStateUseCase`, `GetSavedLoginTokensUseCase`, `SaveLoginTokenUseCase`, `GetSavedLogoutTokensUseCase`, `SaveLogoutTokensUseCase`, `AddLogoutTokenUseCase`, `RemoveTokenUseCase`, `ClearAllTokensUseCase`, `RefreshAuthTokensUseCase`
  - [x] Data layer: `AuthTokensMapper`, `LegacyAuthTokensRepository` (thread-safe StateFlow engine adapting `AuthTokensHelper.java`'s 129 lines, SharedPreferences persistence `saved_tokens` and `saved_tokens_login`, 20-token ceiling, and BackupAgent synchronization)
  - [x] Presentation layer: `AuthTokensUiState`, `AuthTokensEvent`, `AuthTokensViewModel`
- [x] Local Message Custom Parameters & Transcription/Summary State (`feature.messagecustomparams`)
  - [x] Domain entities: `VoiceTranscriptionParamsModel`, `MessageSummaryParamsModel`, `MessageTranslationParamsModel`, `StarsErrorParamsModel`, `MessageCustomParamsModel`, `MessageCustomParamsState`
  - [x] Repository contract: `MessageCustomParamsRepository`
  - [x] Use cases: `CheckMessageCustomParamsEmptyUseCase`, `MergeMessageCustomParamsUseCase`, `ObserveMessageCustomParamsStateUseCase`, `GetMessageCustomParamsStateUseCase`, `GetMessageCustomParamsUseCase`, `SetMessageCustomParamsUseCase`, `UpdateVoiceTranscriptionUseCase`, `UpdateMessageTranslationUseCase`, `UpdateMessageSummaryUseCase`, `CopyMessageCustomParamsUseCase`, `RemoveMessageCustomParamsUseCase`, `ClearAllMessageCustomParamsUseCase`
  - [x] Data layer: `MessageCustomParamsMapper`, `LegacyMessageCustomParamsRepository` (thread-safe StateFlow engine adapting `MessageCustomParamsHelper.java`'s 223 lines, Params_v1 binary serialization, SQLite storage caching, and copy/clear operations)
  - [x] Presentation layer: `MessageCustomParamsUiState`, `MessageCustomParamsEvent`, `MessageCustomParamsViewModel`
- [x] Bot Forum Topics & AI Streaming Drafts (`feature.botforum`)
  - [x] Domain entities: `StreamingSendButtonState`, `BotDraftMessageModel`, `BotForumTopicModel`, `BotForumState`, notification models
  - [x] Repository contract: `BotForumRepository`
  - [x] Use cases: `DeriveTopicNameFromMessageUseCase`, `ResolveStreamingButtonStateUseCase`, `ObserveBotForumStateUseCase`, `GetBotForumStateUseCase`, `GetStreamingSendButtonStateUseCase`, `CheckIsStreamingTopicUseCase`, `SaveIsStreamingTopicUseCase`, `CheckHasBotForumDraftsUseCase`, `StopStreamingDraftUseCase`, `UpdateBotForumDraftUseCase`, `RemoveMarkedRemovedDraftsUseCase`, `CheckNewMessageDraftReplacementUseCase`, `CheckIsBotForumUseCase`
  - [x] Data layer: `BotForumMapper`, `LegacyBotForumRepository` (thread-safe StateFlow engine adapting `BotForumHelper.java`'s 787 lines, draft timeouts, blocklists, auto-topic creation, and streaming send button states)
  - [x] Presentation layer: `BotForumUiState`, `BotForumEvent`, `BotForumViewModel`
- [x] Story Custom Parameters & Local Story Translation State (`feature.storycustomparams`)
  - [x] Domain entities: `StoryTranslationParamsModel`, `StoryCustomParamsModel`, `StoryCustomParamsState`
  - [x] Repository contract: `StoryCustomParamsRepository`
  - [x] Use cases: `CheckStoryCustomParamsEmptyUseCase`, `ComputeStoryCustomParamsFlagsUseCase`, `ObserveStoryCustomParamsStateUseCase`, `GetStoryCustomParamsStateUseCase`, `GetStoryCustomParamsUseCase`, `SaveStoryCustomParamsUseCase`, `UpdateStoryTranslationUseCase`, `CopyStoryCustomParamsUseCase`, `RemoveStoryCustomParamsUseCase`, `ClearAllStoryCustomParamsUseCase`
  - [x] Data layer: `StoryCustomParamsMapper`, `LegacyStoryCustomParamsRepository` (thread-safe StateFlow engine adapting `StoryCustomParamsHelper.java`'s 100 lines, Params_v1 binary serialization, SQLite stories storage caching, and copy/clear operations)
  - [x] Presentation layer: `StoryCustomParamsUiState`, `StoryCustomParamsEvent`, `StoryCustomParamsViewModel`
- [x] Bot Guard WebApp Verification, Sessions & Decisions (`feature.botguard`)
  - [x] Domain entities: `BotGuardDecisionStatus`, `BotGuardDecisionResult`, `BotGuardSession`, `BotGuardLaunchDecision`, `BotGuardBulletinType`, `BotGuardBulletinInfo`, `BotGuardState`
  - [x] Repository contract: `BotGuardRepository`
  - [x] Use cases: `IsGuardBotConfirmationNeededUseCase`, `DetermineGuardBotLaunchFlowUseCase`, `RegisterGuardBotSessionUseCase`, `GetGuardBotSessionUseCase`, `GetAllActiveGuardBotSessionsUseCase`, `CloseGuardBotSessionUseCase`, `SetGuardBotConfirmationShownUseCase`, `ClearAllGuardBotSessionsUseCase`, `ObserveGuardBotDecisionsUseCase`, `ObserveGuardBotStateUseCase`, `MapJoinChatBotResultUseCase`, `FormatGuardBotBulletinUseCase`
  - [x] Data layer: `BotGuardMapper`, `LegacyBotGuardRepository` (thread-safe StateFlow and SharedFlow engine adapting `BotGuardHelper.java`'s 119 lines, `SharedPrefsHelper` webview confirmation flags, `MessagesController.whitelistedBots`, active sheet dismissals, and `NotificationCenter.guardBotDecisionResult`)
  - [x] Presentation layer: `BotGuardUiState`, `BotGuardEvent`, `BotGuardViewModel`
- [x] Ephemeral Bot Messages, Commands & Welcome Anchors (`feature.ephemeralmessages`)
  - [x] Domain entities: `EphemeralBotCommandInfo`, `EphemeralMessageItem`, `WelcomeAnchorBinding`, `EphemeralMessagesState`, `EphemeralMessageIdHelper`
  - [x] Repository contract: `EphemeralMessagesRepository`
  - [x] Use cases: `ParseBotCommandUseCase`, `GetEphemeralCommandBotIdUseCase`, `IsEphemeralCommandUseCase`, `PackEphemeralMessageIdUseCase`, `UnpackEphemeralMessageIdUseCase`, `IsEphemeralMessageIdUseCase`, `PutWelcomeAnchorBindingUseCase`, `RemoveWelcomeAnchorBindingUseCase`, `GetWelcomeAnchorBindingsUseCase`, `ClearAllWelcomeAnchorBindingsUseCase`, `ObserveEphemeralMessagesStateUseCase`, `GetEphemeralMessagesStateUseCase`
  - [x] Data layer: `EphemeralMessagesMapper`, `LegacyEphemeralMessagesRepository` (thread-safe StateFlow engine adapting `EphemeralMessagesHelper.java`'s 489 lines, request transformation `beforeSendingFinalRequest` to `TL_ephemeral.TL_sendMessage`, ID bitmasking, and `WelcomeAnchorsState`)
  - [x] Presentation layer: `EphemeralMessagesUiState`, `EphemeralMessagesEvent`, `EphemeralMessagesViewModel`
- [x] Bot Inline Keyboards, Custom Action Buttons & Markup (`feature.botkeyboard`)
  - [x] Domain entities: `BotButtonColor`, `BotCustomButtonType`, `BotButtonTypeCategory`, `BotButtonItem`, `BotKeyboardRow`, `BotKeyboardLayout`, `BotKeyboardState`
  - [x] Repository contract: `BotKeyboardRepository`
  - [x] Use cases: `BuildBotKeyboardLayoutUseCase`, `ResolveBotButtonColorUseCase`, `ParseCustomButtonTypeUseCase`, `IsForceReplyMarkupUseCase`, `IsButtonWebViewUseCase`, `GetBotKeyboardUseCase`, `SetBotKeyboardUseCase`, `ClearBotKeyboardUseCase`, `ObserveBotKeyboardUseCase`, `ClearAllKeyboardsUseCase`, `FormatButtonBadgeUseCase`
  - [x] Data layer: `BotKeyboardMapper`, `LegacyBotKeyboardRepository` (thread-safe StateFlow engine adapting `BotInlineKeyboard.java`'s 224 lines, `TLKeyboardHelper.java`'s 49 lines, button styling, row bitmask separators, and custom dialog action buttons)
  - [x] Presentation layer: `BotKeyboardUiState`, `BotKeyboardEvent`, `BotKeyboardViewModel`
- [x] Window Visibility Arbitration & Reference-Counting Controllers (`feature.windowvisibility`)
  - [x] Domain entities: `WindowVisibilityScope`, `WindowVisibilityReason`, `WindowVisibilityState`, `WindowVisibilityChangeResult`, `WindowVisibilityController`
  - [x] Repository contract: `WindowVisibilityRepository`
  - [x] Use cases: `RequestHideWindowUseCase`, `ReleaseHideWindowUseCase`, `ToggleWindowHideUseCase`, `CheckIsWindowVisibleUseCase`, `GetWindowVisibilityStateUseCase`, `GetActiveHideReasonsUseCase`, `ResetWindowVisibilityUseCase`, `ObserveWindowVisibilityStateUseCase`, `ObserveWindowVisibilityChangesUseCase`, `CreateVisibilityControllerUseCase`
  - [x] Data layer: `WindowVisibilityMapper`, `LegacyWindowVisibilityRepository` (thread-safe StateFlow engine adapting `WindowVisibilityManager.java`'s 76 lines, reference-counting `reasonsToHide`, `OnVisibilityChangedListener`, and subsystem controllers)
  - [x] Presentation layer: `WindowVisibilityUiState`, `WindowVisibilityEvent`, `WindowVisibilityViewModel`
- [x] Countdown Timer Engine & Time Formatting (`feature.countdowntimer`)
  - [x] Domain entities: `CountdownTimerStatus`, `CountdownTimeComponents`, `CountdownTimerTick`, `CountdownTimerState`
  - [x] Repository contract: `CountdownTimerRepository`
  - [x] Use cases: `StartCountdownTimerUseCase`, `StopCountdownTimerUseCase`, `PauseCountdownTimerUseCase`, `ResumeCountdownTimerUseCase`, `GetCountdownTimerUseCase`, `IsCountdownTimerRunningUseCase`, `TickCountdownTimerUseCase`, `ClearAllCountdownTimersUseCase`, `ObserveCountdownTimerUseCase`, `ObserveCountdownStateUseCase`, `DecomposeCountdownTimeUseCase`, `FormatCountdownTimeUseCase`
  - [x] Data layer: `CountdownTimerMapper`, `LegacyCountdownTimerRepository` (thread-safe StateFlow and ticker engine adapting `CountdownTimer.java`'s 62 lines, time component math, and formatted strings)
  - [x] Presentation layer: `CountdownTimerUiState`, `CountdownTimerEvent`, `CountdownTimerViewModel`
- [x] Text & HTML Entity Conversion Engine (`feature.texthtml`)
  - [x] Domain entities: `HtmlTextSpanType`, `HtmlTextSpan`, `RichFormattedText`, `TextHtmlConversionResult`, `TextHtmlState`
  - [x] Repository contract: `TextHtmlRepository`
  - [x] Use cases: `ConvertToHtmlUseCase`, `ParseFromHtmlUseCase`, `EscapeHtmlUseCase`, `UnescapeHtmlUseCase`, `StripHtmlFormattingUseCase`, `ExtractHtmlSpansUseCase`, `HasRichFormattingUseCase`, `ObserveTextHtmlStateUseCase`, `ClearTextHtmlStateUseCase`
  - [x] Data layer: `TextHtmlMapper`, `LegacyTextHtmlRepository` (thread-safe StateFlow and bidirectional HTML/rich text converter adapting `CustomHtml.java`'s 294 lines, `CopyUtilities.java`'s 386 lines, and SAX tag attributes)
  - [x] Presentation layer: `TextHtmlUiState`, `TextHtmlEvent`, `TextHtmlViewModel`
- [x] Memory Leak Detection & Instance Reference Tracking Engine (`feature.leakdetector`)
  - [x] Domain entities: `TrackedClassStats`, `LeakReport`, `LeakDetectorConfig`, `LeakDetectorState`
  - [x] Repository contract: `LeakDetectorRepository`
  - [x] Use cases: `StartLeakDetectionUseCase`, `StopLeakDetectionUseCase`, `TrackInstanceUseCase`, `TriggerLeakCheckUseCase`, `ConfirmLeakUseCase`, `GetTrackedClassesStatsUseCase`, `GetConfirmedLeaksUseCase`, `ResetLeakDetectorUseCase`, `ObserveLeakDetectorStateUseCase`, `ObserveConfirmedLeaksUseCase`
  - [x] Data layer: `LeakDetectorMapper`, `LegacyLeakDetectorRepository` (thread-safe WeakReference tracking, coroutine periodic scanning, and two-phase GC confirmation adapting `LeakDetector.java`'s 215 lines)
  - [x] Presentation layer: `LeakDetectorUiState`, `LeakDetectorEvent`, `LeakDetectorViewModel`
- [x] Frame Rate Adaptation & 60 FPS V-Sync Content Arbitration (`feature.fpscontent`)
  - [x] Domain entities: `FrameCallbackType`, `FpsGroupConfig`, `FrameTick`, `FrameCallbackSubscription`, `FpsContentStats`, `FpsTimingUtils`
  - [x] Repository contract: `FpsContentRepository`
  - [x] Use cases: `RegisterFrameCallbackUseCase`, `RegisterRunnableCallbackUseCase`, `UnregisterCallbackUseCase`, `RequestViewInvalidationUseCase`, `RequestDrawableInvalidationUseCase`, `DispatchVsyncTickUseCase`, `CalculateFpsTimingUseCase`, `GetFpsContentStatsUseCase`, `GetFpsSubscriptionsUseCase`, `ObserveFpsContentStatsUseCase`, `ObserveFpsTicksUseCase`, `ResetFpsContentUseCase`
  - [x] Data layer: `FpsContentMapper`, `LegacyFpsContentRepository` (thread-safe StateFlow and ticker engine adapting `Choreographer60FpsContent.java`'s 360 lines, stride groups for 60/30/20/15 fps, accumulator groups for arbitrary rates, and view/drawable invalidation scheduling)
  - [x] Presentation layer: `FpsContentUiState`, `FpsContentEvent`, `FpsContentViewModel`
- [x] Main Thread ANR Watchdog & UI Freeze Diagnostics (`feature.anrwatchdog`)
  - [x] Domain entities: `AnrSeverity`, `AppLifecycleState`, `PingRecord`, `AnrIncident`, `AnrWatchdogConfig`, `AnrWatchdogState`
  - [x] Repository contract: `AnrWatchdogRepository`
  - [x] Use cases: `StartAnrMonitoringUseCase`, `StopAnrMonitoringUseCase`, `SetAppForegroundStatusUseCase`, `SendMainThreadPingUseCase`, `AcknowledgePingUseCase`, `CheckMainThreadFreezeUseCase`, `ResolveIncidentUseCase`, `GetAnrWatchdogStateUseCase`, `GetAnrIncidentsUseCase`, `ClearAnrHistoryUseCase`, `ObserveAnrWatchdogStateUseCase`, `ObserveAnrIncidentsUseCase`
  - [x] Data layer: `AnrWatchdogMapper`, `LegacyAnrWatchdogRepository` (thread-safe generation tracking, timeout evaluation, and deduplication adapting `ANRDetector.java`'s 224 lines)
  - [x] Presentation layer: `AnrWatchdogUiState`, `AnrWatchdogEvent`, `AnrWatchdogViewModel`
- [x] Hardware & Emulator / Virtual Environment Detection (`feature.emudetector`)
  - [x] Domain entities: `EmulatorType`, `DetectionCategory`, `DetectionIndicator`, `EnvironmentVerdict`, `EmulatorDiagnostics`, `EmulatorDetectorConfig`, `EmulatorHeuristics`
  - [x] Repository contract: `EmuDetectorRepository`
  - [x] Use cases: `DetectEnvironmentUseCase`, `IsEmulatorUseCase`, `GetCachedDiagnosticsUseCase`, `ObserveDiagnosticsUseCase`, `ObserveIsEmulatorUseCase`, `GetDetectorConfigUseCase`, `UpdateDetectorConfigUseCase`, `AddCustomPackageNameUseCase`, `ClearDetectorCacheUseCase`, `CalculateConfidenceScoreUseCase`, `EvaluateEnvironmentVerdictUseCase`
  - [x] Data layer: `EmuDetectorMapper`, `LegacyEmuDetectorRepository` (thread-safe detection engine adapting `EmuDetector.java`'s 434 lines, `EmuInputDevicesDetector.java`'s 64 lines, weighted multi-factor heuristic, and in-memory test provider)
  - [x] Presentation layer: `EmuDetectorUiState`, `EmuDetectorEvent`, `EmuDetectorViewModel`
- [x] Window Security & FLAG_SECURE Arbitration (`feature.flagsecure`)
  - [x] Domain entities: `SecurityReasonType`, `WindowSecurityState`, `SecurityRuleSpec`, `SecurityEvaluationResult`, `SecurityRulesEvaluator`
  - [x] Repository contract: `FlagSecureRepository`
  - [x] Use cases: `AttachSecurityReasonUseCase`, `DetachSecurityReasonUseCase`, `InvalidateWindowSecurityUseCase`, `IsWindowSecuredUseCase`, `GetWindowSecurityStateUseCase`, `GetAllWindowStatesUseCase`, `ResetWindowSecurityUseCase`, `ObserveWindowStateUseCase`, `ObserveAllWindowStatesUseCase`, `EvaluateSecurityRuleUseCase`
  - [x] Data layer: `FlagSecureMapper`, `FlagSecureLocalDataSource`, `FlagSecureRepositoryImpl` (thread-safe reference-counting engine adapting window security state flows, dynamic condition evaluation, and in-memory test fallback)
  - [x] Presentation layer: `FlagSecureUiState`, `FlagSecureEvent`, `FlagSecureViewModel`
- [x] Animation Notifications Locker & UI Stutter Prevention (`feature.animationlocker`)
  - [x] Domain entities: `LockScope`, `AnimationLockRecord`, `AnimationLockerState`, `AnimationLockerConfig`, `AnimationLockEvaluator`
  - [x] Repository contract: `AnimationLockerRepository`
  - [x] Use cases: `AcquireAnimationLockUseCase`, `ReleaseAnimationLockUseCase`, `ReleaseAllAnimationLocksUseCase`, `SetAnimationLockerDisabledUseCase`, `IsAnimationLockedUseCase`, `IsNotificationAllowedUseCase`, `GetAnimationLockerStateUseCase`, `GetAnimationLockerConfigUseCase`, `UpdateAnimationLockerConfigUseCase`, `ObserveAnimationLockerStateUseCase`, `ObserveIsAnimationLockedUseCase`
  - [x] Data layer: `AnimationLockerMapper`, `LegacyAnimationLockerRepository` (thread-safe lock registry adapting `AnimationNotificationsLocker.java`'s 48 lines, multi-account and global notification suspension, and allowed notifications whitelist)
  - [x] Presentation layer: `AnimationLockerUiState`, `AnimationLockerEvent`, `AnimationLockerViewModel`
- [x] Domain Modularization (7 Domains, 105 Features) & Containerization
  - [x] 7 Semantic subdomains restructured (`business`, `media`, `messaging`, `network`, `security`, `social`, `system`).
  - [x] 7 Domain-specific DI containers created under `feature.<domain>.di`:
    - `BusinessContainer.kt` (10 features, 135 properties/factories)
    - `MediaContainer.kt` (18 features, 248 properties/factories)
    - `MessagingContainer.kt` (31 features, 421 properties/factories)
    - `NetworkContainer.kt` (4 features, 54 properties/factories)
    - `SecurityContainer.kt` (10 features, 140 properties/factories)
    - `SocialContainer.kt` (6 features, 73 properties/factories)
    - `SystemContainer.kt` (26 features, 384 properties/factories)
  - [x] `AccountFeatureContainer.kt` converted into lightweight Facade with 100% backward-compatible delegated accessors.
- [x] Phase 2: UI Wiring via Strangler Fig (100% Complete)
  - [x] `LaunchActivity.java`: Connected `MainTabsViewModel`, `PipViewModel`, `WindowVisibilityViewModel`, `BrowserViewModel`, and `LauncherIconViewModel`.
  - [x] `DialogsActivity.java`: Connected `DialogsViewModel`, `FoldersViewModel`, `StoriesViewModel`, and `SavedMessagesViewModel` (Single Execution Principle, search deconflicted).
  - [x] `ChatActivity.java`: Connected `ChatViewModel`, `SendMessagesViewModel`, `ChatThemeViewModel`, `ReactionsViewModel`, `ChatInputViewModel`, `BottomViewsViewModel`, `DraftMeasureViewModel`, `MentionsViewModel`, `AudioPlayerViewModel`, and `FactCheckViewModel`.
  - [x] `ProfileActivity.java`: Connected `ProfileViewModel` and `StoriesViewModel`.
  - [x] `SettingsActivity.java`: Connected `SettingsViewModel` and `ThemeViewModel`.
  - [x] `PhotoViewer.java`: Connected `MediaViewModel` and `ContentPreviewViewModel`.
  - [x] `FiltersSetupActivity.java` & `FilterCreateActivity.java`: Connected `FoldersViewModel` (refresh on filter updates and save).
  - [x] `VoIPFragment.java`: Connected `CallViewModel` (call state observation and safe lifecycle cleanup).
  - [x] `PrivacySettingsActivity.java` & `PasscodeActivity.java`: Connected `PrivacyViewModel`, `PasskeysViewModel`, and `BiometricsViewModel`.
  - [x] `SharedMediaLayout.java`: Connected `SavedMessagesViewModel` for saved messages tabs.
- [/] Phase 3: Strangling Legacy Controllers from Within (In Progress)
  - [x] Standardized Data Sources Infrastructure (`core.data`):
    - `BaseRemoteDataSource.kt`: MTProto RPC execution via `ConnectionsManager.sendRequest`, coroutine cancellation (`suspendCancellableCoroutine`), typed `Result<T>`.
    - `BaseLocalDataSource.kt`: Safe database operations on `MessagesStorage` via `Dispatchers.IO`.
  - [x] Pilot Feature: `SavedMessagesController` Strangling (`feature.messaging.savedmessages`):
    - `SavedMessagesRemoteDataSource.kt` (MTProto RPC requests)
    - `SavedMessagesLocalDataSource.kt` (SQLite persistence & memory cache)
    - `SavedMessagesRepositoryImpl.kt` (Local + remote coordination)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `SavedMessagesController.java` strangler boundary with `getRepository()` accessor.
  - [x] Feature: `Folders & Dialog Filters` Strangling (`feature.messaging.folders`):
    - `FoldersRemoteDataSource.kt` (MTProto RPC requests: get, update, delete, reorder, suggested)
    - `FoldersLocalDataSource.kt` (SQLite persistence & memory cache)
    - `FoldersRepositoryImpl.kt` (Local + remote coordination)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MessagesController.java` strangler boundary with `getFoldersRepository()` accessor.
  - [x] Feature: `ContactsController` Strangling (`feature.social.contacts`):
    - `ContactsRemoteDataSource.kt` (MTProto RPC requests: getContacts, addContact, deleteContacts, searchContacts, resetSavedContacts, getStatuses)
    - `ContactsLocalDataSource.kt` (SQLite persistence on Dispatchers.IO & safe in-memory cache)
    - `ContactsRepositoryImpl.kt` (Local + remote coordination with safe NotificationCenter dispatch)
    - `SocialContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ContactsController.java` strangler boundary with `getContactsRepository()` accessor.
  - [x] Feature: `NotificationsController` Strangling (`feature.system.notifications`):
    - `NotificationsRemoteDataSource.kt` (MTProto RPC requests: updateNotifySettings, setReactionsNotifySettings, setContactSignUpNotification, getNotifyExceptions, resetNotifySettings)
    - `NotificationsLocalDataSource.kt` (SharedPreferences + MessagesStorage SQLite persistence & NotificationsController in-memory cache)
    - `NotificationsRepositoryImpl.kt` (Local + remote coordination with safe NotificationCenter dispatch)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `NotificationsController.java` strangler boundary with `getNotificationsRepository()` accessor.
  - [x] Feature: `SecretChatHelper` Strangling (`feature.security.secretchat`):
    - `SecretChatRemoteDataSource.kt` (MTProto RPC requests: getDhConfig, requestEncryption, acceptEncryption, discardEncryption, sendEncryptedService)
    - `SecretChatLocalDataSource.kt` (SQLite persistence on Dispatchers.IO, in-memory cache inspection, and SecretChatHelper crypto coordination)
    - `SecretChatRepositoryImpl.kt` (Local + remote coordination, pure bitwise encrypted dialog ID calculations, and safe NotificationCenter dispatch)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `SecretChatHelper.java` strangler boundary with `getSecretChatRepository()` accessor.
  - [x] Feature: `DownloadController` Strangling (`feature.media.downloadmanager`):
    - `DownloadManagerRemoteDataSource.kt` (MTProto RPC requests: getAutoDownloadConfig, saveAutoDownloadSettings)
    - `DownloadManagerLocalDataSource.kt` (SharedPreferences auto-download presets + MessagesStorage SQLite document deletion & in-memory DownloadController access)
    - `DownloadManagerRepositoryImpl.kt` (Local + remote coordination, reactive state observation, queue mutations, download speed calculation, and preset persistence)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `DownloadController.java` strangler boundary with `getDownloadManagerRepository()` accessor.
  - [x] Feature: `LocationController` Strangling (`feature.social.location`):
    - `LocationRemoteDataSource.kt` (MTProto RPC requests: getRecentLocations, stopLiveLocation, editLiveLocation, markLiveLocationsAsRead)
    - `LocationLocalDataSource.kt` (SQLite persistence for sharing_locations on Dispatchers.IO, in-memory LocationController state & SendMessagesHelper dispatch)
    - `LocationRepositoryImpl.kt` (Local + remote coordination, reactive active sharings and peer coordinates, and safe NotificationCenter dispatch)
  - [x] Feature: `PasskeysController` & `FingerprintController` Strangling (`feature.security.passkeys` & `feature.security.biometrics`):
    - `PasskeysRemoteDataSource.kt` (MTProto RPC requests: getPasskeys, deletePasskey, initPasskeyRegistration, registerPasskey)
    - `PasskeysLocalDataSource.kt` (OS Passkey capabilities & MessagesController config limits)
    - `PasskeysRepositoryImpl.kt` (Coordinating PasskeysLocalDataSource and PasskeysRemoteDataSource with StateFlow caching)
    - `BiometricsLocalDataSource.kt` (Hardware detection, enrolled biometrics, Android KeyStore key readiness and invalidation)
    - `BiometricsRepositoryImpl.kt` (Coordinating BiometricsLocalDataSource with NotificationCenter didGenerateFingerprintKeyPair event stream)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `PasskeysController.java` & `FingerprintController.java` strangler boundaries (`getPasskeysRepository()`, `getBiometricsRepository()`).
  - [x] Feature: `BirthdayController` & `ChannelBoostsController` Strangling (`feature.social.birthdays` & `feature.social.boosts` - ADR 128):
    - `BirthdayRemoteDataSource.kt` & `BirthdayLocalDataSource.kt`
    - `BirthdaysRepositoryImpl.kt`
    - `BoostsRemoteDataSource.kt` & `BoostsLocalDataSource.kt`
    - `BoostsRepositoryImpl.kt`
    - `SocialContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BirthdayController.java` & `ChannelBoostsController.java` strangler boundaries (`getBirthdaysRepository()`, `getBoostsRepository()`).
  - [x] Feature: `MemberRequestsController` Strangling (`feature.social.joinrequests` - ADR 129):
    - `JoinRequestsRemoteDataSource.kt` (MTProto RPC requests: getChatInviteImporters, hideChatJoinRequest, hideAllChatJoinRequests)
    - `JoinRequestsLocalDataSource.kt` (ChatFull, InputPeer/User resolution, firstImportersCache & processUpdates)
    - `JoinRequestsRepositoryImpl.kt` (Local + remote coordination, approve/dismiss mutations, and reactive observePendingRequests)
    - `SocialContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MemberRequestsController.java` strangler boundary with `getJoinRequestsRepository()` accessor and `putCachedImporters()`.
  - [x] Feature: `ChatThemeController` Strangling (`feature.messaging.chattheme` - ADR 130):
    - `ChatThemeRemoteDataSource.kt` (MTProto RPC requests: account.getChatThemes, messages.setChatTheme via BaseRemoteDataSource)
    - `ChatThemeLocalDataSource.kt` (SharedPreferences caching, in-memory emoji themes & wallpaper access, processUpdates)
    - `ChatThemeRepositoryImpl.kt` (Coordinating remote reload with local cache fallback, dialog theme mutations, reactive observeDialogTheme)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ChatThemeController.java` strangler boundary with `getChatThemeRepository()` accessor.
  - [x] Feature: `FactCheckController` Strangling (`feature.messaging.factcheck` - ADR 131):
    - `FactCheckRemoteDataSource.kt` (MTProto RPC requests: messages.getFactCheck, messages.editFactCheck, messages.deleteFactCheck via BaseRemoteDataSource)
    - `FactCheckLocalDataSource.kt` (SQLite queries on fact_checks table, in-memory cache, max character limits)
    - `FactCheckRepositoryImpl.kt` (Memory-first cache with SQLite fallback, MTProto mutations, reactive observeFactCheckLoaded)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `FactCheckController.java` strangler boundary with `getFactCheckRepository(account)` accessor and cache accessors.
  - [x] Feature: `TranslateController` Strangling (`feature.messaging.translate` - ADR 132):
    - `TranslationRemoteDataSource.kt` (MTProto RPC requests: messages.translateText via BaseRemoteDataSource)
    - `TranslationLocalDataSource.kt` (Restricted languages preferences, dialog translation states, app language application)
    - `TranslationRepositoryImpl.kt` (MTProto text translation, dialog translation state toggles, reactive observeTranslateSettings & observeDialogTranslationState)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `TranslateController.java` strangler boundary with `getTranslationRepository(account)` accessor.
  - [x] Feature: `TopicsController` Strangling (`feature.messaging.topics` - ADR 133):
    - `TopicsRemoteDataSource.kt` (MTProto RPC requests: messages.getForumTopics, messages.getSavedDialogs, messages.editForumTopic, messages.updatePinnedForumTopic, messages.deleteTopicHistory, messages.reorderPinnedForumTopics, messages.readReactions via BaseRemoteDataSource)
    - `TopicsLocalDataSource.kt` (Local cache management, in-memory fallback cache, SQLite storage persistence via MessagesStorage)
    - `TopicsRepositoryImpl.kt` (Clean repository coordinating local caches, remote RPCs, and reactive observeTopics & observeForumUnreadCount)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `TopicsController.java` strangler boundary with `getTopicsRepository(account)` accessor.
  - [x] Feature: `ProxyRotationController` Strangling (`feature.network.proxy` - ADR 134):
    - `ProxyRemoteDataSource.kt` (Proxy ping checks via ConnectionsManager.checkProxy and ConnectionsManager.setProxySettings)
    - `ProxyLocalDataSource.kt` (SharedConfig.proxyList, currentProxy, proxyRotationEnabled, timeout preferences, SharedPreferences persistence)
    - `ProxyRepositoryImpl.kt` (Clean repository coordinating local settings, remote pings, and reactive observeProxySettings)
    - `NetworkContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ProxyRotationController.java` strangler boundary with `getProxyRepository(account)` accessor.
  - [x] Feature: `FileRefController` Strangling (`feature.media.fileref` - ADR 135):
    - `FileRefRemoteDataSource.kt` (MTProto file reference renewal requests and request cancellation)
    - `FileRefLocalDataSource.kt` (FileRefController bridge, parent key mapping, memory cache fallback)
    - `FileRefRepositoryImpl.kt` (Clean repository coordinating renewal deduplication, caching, metrics, and reactive observeStats)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `FileRefController.java` strangler boundary with `getFileRefRepository(account)` accessor.
  - [x] Feature: `CacheByChatsController` Strangling (`feature.media.cachebychats` - ADR 136):
    - `CacheByChatsRemoteDataSource.kt` (MTProto cache retention sync extension points via BaseRemoteDataSource)
    - `CacheByChatsLocalDataSource.kt` (Retention periods and hex-encoded exception byte buffers via SharedConfig and UserConfig)
    - `CacheByChatsRepositoryImpl.kt` (Clean repository coordinating retention periods, exceptions, and reactive observeConfig)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `CacheByChatsController.java` strangler boundary with `getCacheByChatsRepository(account)` accessor.
  - [x] Feature: `UnconfirmedAuthController` Strangling (`feature.security.unconfirmedauth` - ADR 137):
    - `UnconfirmedAuthRemoteDataSource.kt` (MTProto unconfirmed authorization confirmations/denials via BaseRemoteDataSource)
    - `UnconfirmedAuthLocalDataSource.kt` (Unconfirmed auth cache, SQLite integration, and in-memory fallback)
    - `UnconfirmedAuthRepositoryImpl.kt` (Clean repository coordinating confirm/deny, state queries, and observePendingAuths)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `UnconfirmedAuthController.java` strangler boundary with `getUnconfirmedAuthRepository(account)` accessor.
  - [x] Feature: `CaptchaController` Strangling (`feature.security.captcha` - ADR 138):
    - `CaptchaRemoteDataSource.kt` (reCAPTCHA Enterprise task execution & MTProto token verification via BaseRemoteDataSource)
    - `CaptchaLocalDataSource.kt` (Active requests deduplication and verification state tracking)
    - `CaptchaRepositoryImpl.kt` (Clean repository coordinating requests, verification status, and error handling)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `CaptchaController.java` strangler boundary with `getCaptchaRepository(account)` accessor.
  - [x] Feature: `AiTonesController` Strangling (`feature.messaging.aitones` - ADR 139):
    - `AiTonesRemoteDataSource.kt` (MTProto AI Compose styles/tones RPCs and unsave requests via BaseRemoteDataSource)
    - `AiTonesLocalDataSource.kt` (Base64 serialization in SharedPreferences and safe in-memory list caching)
    - `AiTonesRepositoryImpl.kt` (Clean repository coordinating tones loading, add/remove/edit, and reactive observeTones)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `AiTonesController.java` strangler boundary with `getAiTonesRepository(account)` accessor.
  - [x] Feature: `StatsController` Strangling (`feature.network.networkstats` - ADR 140):
    - `NetworkStatsRemoteDataSource.kt` (MTProto network stats sync extension points via BaseRemoteDataSource)
    - `NetworkStatsLocalDataSource.kt` (stats.dat file parsing, byte counters matrix, reset timestamps, in-memory fallback)
    - `NetworkStatsRepositoryImpl.kt` (Clean repository coordinating byte updates, reset, and reactive observeNetworkStats)
    - `NetworkContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `StatsController.java` strangler boundary with `getNetworkStatsRepository(account)` accessor.
  - [x] Feature: `BillingController` Strangling (`feature.business.billing` - ADR 141):
    - `BillingRemoteDataSource.kt` (MTProto in-app transaction assignment RPCs via BaseRemoteDataSource)
    - `BillingLocalDataSource.kt` (Google Play BillingClient lifecycle, product queries, purchase handling, and state flow)
    - `BillingRepositoryImpl.kt` (Clean repository coordinating purchase launch, receipt verification, and reactive observeBillingState)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BillingController.java` strangler boundary with `getBillingRepository(account)` accessor.
  - [x] Feature: `GiftAuctionController` Strangling (`feature.business.giftauctions` - ADR 142):
    - `GiftAuctionsRemoteDataSource.kt` (MTProto Star Gift auction RPCs via BaseRemoteDataSource)
    - `GiftAuctionsLocalDataSource.kt` (Active auctions cache, bid submission via GiftAuctionController, and notification observation)
    - `GiftAuctionsRepositoryImpl.kt` (Clean repository coordinating auction lookups, bidding, acquired gifts, and reactive observeActiveAuctions)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `GiftAuctionController.java` strangler boundary with `getGiftAuctionsRepository(account)` accessor.
  - [x] Feature: `PipActivityController` Strangling (`feature.media.pip` - ADR 143):
    - `PipRemoteDataSource.kt` (System PiP parameters and media session action dispatch via BaseRemoteDataSource)
    - `PipLocalDataSource.kt` (Thread-safe source map, priority arbitration, aspect ratio tracking, and PipSessionInfo StateFlow)
    - `PipRepositoryImpl.kt` (Clean repository coordinating local session state, priority evaluation, remote actions, and reactive observeSessionInfo)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `PipActivityController.java` strangler boundary with `getPipRepository()` accessor.
  - [x] Feature: `ChromecastController` Strangling (`feature.media.chromecast` - ADR 144):
    - `ChromecastRemoteDataSource.kt` (Cast session detection, remote media playback commands, and cover file resolution via BaseRemoteDataSource)
    - `ChromecastLocalDataSource.kt` (Cast playback state, active media model, cover path cache, and ChromecastStateModel StateFlow)
    - `ChromecastRepositoryImpl.kt` (Clean repository coordinating cast playback requests, cover uploads, and reactive observeChromecastState)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ChromecastController.java` strangler boundary with `getChromecastRepository()` accessor.
  - [x] Feature: `CameraController` Strangling (`feature.media.camera` - ADR 145):
    - `CameraRemoteDataSource.kt` (Hardware camera initialization and ThreadPoolExecutor execution via BaseRemoteDataSource)
    - `CameraLocalDataSource.kt` (Camera state, available devices list, headless fallback, flash mode, front mirror, resolution heuristics, and recording status)
    - `CameraRepositoryImpl.kt` (Clean repository coordinating camera init, selection, switching, flash mode, mirroring, and recording lifecycle with reactive observeCameraState)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `CameraController.java` strangler boundary with `getCameraRepository()` accessor.
  - [x] Feature: `HashtagSearchController` Strangling (`feature.messaging.hashtagsearch` - ADR 146):
    - `HashtagSearchRemoteDataSource.kt` (MTProto RPC searchGlobal, searchChat, searchPosts, and username resolution via BaseRemoteDataSource)
    - `HashtagSearchLocalDataSource.kt` (Thread-safe history preferences with # and $ normalization, in-memory cache per search type)
    - `HashtagSearchRepositoryImpl.kt` (Clean repository coordinating global/chat/post searches, history CRUD, and reactive observeHistory / observeSearchResult)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `HashtagSearchController.java` strangler boundary with `getHashtagSearchRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `EphemeralMessagesHelper` Strangling (`feature.messaging.ephemeralmessages` - ADR 147):
    - `EphemeralMessagesRemoteDataSource.kt` (MTProto sendEphemeralMessage and chat full loading via BaseRemoteDataSource)
    - `EphemeralMessagesLocalDataSource.kt` (Welcome anchor bindings tracking, bot command parsing and ephemeral detection, state flow)
    - `EphemeralMessagesRepositoryImpl.kt` (Clean repository coordinating anchor bindings, command checks, and reactive observeState)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `EphemeralMessagesHelper.java` strangler boundary with `getEphemeralMessagesRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `BotForumHelper` Strangling (`feature.messaging.botforum` - ADR 148):
    - `BotForumRemoteDataSource.kt` (MTProto stop draft action and forum topic creation via BaseRemoteDataSource)
    - `BotForumLocalDataSource.kt` (Draft updates mapping, blocklists, streaming topics persistence, and StreamingSendButtonState resolution)
    - `BotForumRepositoryImpl.kt` (Clean repository coordinating draft streaming, replacement checks, stop actions, and reactive observeState)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BotForumHelper.java` strangler boundary with `getBotForumRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `SaveToGallerySettingsHelper` Strangling (`feature.media.gallerysave` - ADR 149):
    - `GallerySaveRemoteDataSource.kt` (MTProto sync for auto-save gallery configuration and server defaults via BaseRemoteDataSource)
    - `GallerySaveLocalDataSource.kt` (SharedPreferences persistence for exceptions, LongSparseArray cache, peer flag masking, and video limit checks)
    - `GallerySaveRepositoryImpl.kt` (Clean repository coordinating exceptions CRUD, peer settings observation, and reactive _configFlow)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `SaveToGallerySettingsHelper.java` strangler boundary with `getGallerySaveRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `AuthTokensHelper` Strangling (`feature.security.authtokens` - ADR 150):
    - `AuthTokensRemoteDataSource.kt` (MTProto session drop and token invalidation RPCs via BaseRemoteDataSource)
    - `AuthTokensLocalDataSource.kt` (SharedPreferences persistence, hex-encoded TL authorization/loggedOut serialization, and LRU cache)
    - `AuthTokensRepositoryImpl.kt` (Clean repository coordinating token saves, LRU pruning max 20, removals, and reactive observeState)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `AuthTokensHelper.java` strangler boundary with `getAuthTokensRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `MessageCustomParamsHelper` Strangling (`feature.messaging.messagecustomparams` - ADR 151):
    - `MessageCustomParamsRemoteDataSource.kt` (MTProto audio transcription and speech/translation RPCs via BaseRemoteDataSource)
    - `MessageCustomParamsLocalDataSource.kt` (Params_v1 TL binary buffer serialization, ConcurrentHashMap cache per messageId)
    - `MessageCustomParamsRepositoryImpl.kt` (Clean repository coordinating params read/write, deep copying between messages, and reactive observeState)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MessageCustomParamsHelper.java` strangler boundary with `getMessageCustomParamsRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `BotGuardHelper` Strangling (`feature.security.botguard` - ADR 152):
    - `BotGuardRemoteDataSource.kt` (MTProto web and bot validation RPC encapsulation via BaseRemoteDataSource)
    - `BotGuardLocalDataSource.kt` (SharedPreferences confirmation flags, MessagesController bot whitelist checks, and in-memory session tracking)
    - `BotGuardRepositoryImpl.kt` (Clean repository coordinating session CRUD, confirmation toggling, decision events, and reactive observeState)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BotGuardHelper.java` strangler boundary with `getBotGuardRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `ChatMessagesMetadataController` Strangling (`feature.messaging.chatmeta` - ADR 153):
    - `ChatMetadataRemoteDataSource.kt` (MTProto RPC requests for reactions, extended media previews, and linked stories via BaseRemoteDataSource)
    - `ChatMetadataLocalDataSource.kt` (Message metadata check intervals, queue bounds max 5 reactions / max 10 extended media, and request cancellations)
    - `ChatMetadataRepositoryImpl.kt` (Clean repository coordinating metadata batch inspection, reactions/media loading, and reactive observeStats)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ChatMessagesMetadataController.java` strangler boundary with `getChatMessagesMetadataRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Sessions` Strangling (`feature.security.sessions` - ADR 154):
    - `SessionsRemoteDataSource.kt` (MTProto authorizations, web authorizations, TTL settings, and QR login RPCs via BaseRemoteDataSource)
    - `SessionsLocalDataSource.kt` (Thread-safe memory caches for authorizations and web authorizations, URL-safe Base64 QR token parsing, and post-reset push refresh)
    - `SessionsRepositoryImpl.kt` (Clean repository coordinating session terminations, settings updates, TTL configuration, and reactive observeSessions)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring
  - [x] Feature: `PushListenerController` Strangling (`feature.network.pushlistener` - ADR 155):
    - `PushListenerRemoteDataSource.kt` (MTProto token registration and decrypt error logging via BaseRemoteDataSource)
    - `PushListenerLocalDataSource.kt` (StateFlow listening state, registered tokens map, and push payload json parser)
    - `PushListenerRepositoryImpl.kt` (Clean repository coordinating remote registration, local caching, push processing, and reactive observeState)
    - `NetworkContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `PushListenerController.java` strangler boundary with `getPushListenerRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `RefreshRateController` Strangling (`feature.system.refreshrate` - ADR 156):
    - `RefreshRateRemoteDataSource.kt` (Remote config / feature enablement checks)
    - `RefreshRateLocalDataSource.kt` (Display modes, ring buffer frame metrics storage, FPS calculations, and stable duration hysteresis timers)
    - `RefreshRateRepositoryImpl.kt` (Clean repository coordinating local frame metrics, display modes, and reactive observeState)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `RefreshRateController.java` strangler boundary with `getRefreshRateRepository()` and `getRepository()` accessors.
  - [x] Feature: `GroupCallMessagesController` Strangling (`feature.messaging.groupcallmsg` - ADR 157):
    - `GroupCallMessagesRemoteDataSource.kt` (MTProto in-call message RPC sending via BaseRemoteDataSource)
    - `GroupCallMessagesLocalDataSource.kt` (Thread-safe in-memory messages per callId, listener registration, and pop on TTL expiry)
    - `GroupCallMessagesRepositoryImpl.kt` (Clean repository coordinating in-call messaging, popping, and reactive observeCallMessages)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `GroupCallMessagesController.java` strangler boundary with `getGroupCallMessagesRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `StoryCustomParamsHelper` Strangling (`feature.media.storycustomparams` - ADR 158):
    - `StoryCustomParamsRemoteDataSource.kt` (MTProto story custom parameters / translation sync via BaseRemoteDataSource)
    - `StoryCustomParamsLocalDataSource.kt` (Thread-safe in-memory cache and stories storage parameters accessor)
    - `StoryCustomParamsRepositoryImpl.kt` (Clean repository coordinating local and remote parameters, translation flags, and reactive observeState)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `StoryCustomParamsHelper.java` strangler boundary with `getStoryCustomParamsRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `BotInlineKeyboard` & `TLKeyboardHelper` Strangling (`feature.messaging.botkeyboard` - ADR 159):
    - `BotKeyboardRemoteDataSource.kt` (MTProto callback and remote keyboard layout fetch via BaseRemoteDataSource)
    - `BotKeyboardLocalDataSource.kt` (Thread-safe in-memory keyboards per messageId, force-reply and webview checks)
    - `BotKeyboardRepositoryImpl.kt` (Clean repository coordinating active message keyboards, button press recording, and reactive observeState)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `TLKeyboardHelper.java` & `BotInlineKeyboard.java` strangler boundary with `getBotKeyboardRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `HintsController` Strangling (`feature.system.hints` - ADR 160):
    - `HintsRemoteDataSource.kt` (MTProto cloud hint dismissal sync via BaseRemoteDataSource)
    - `HintsLocalDataSource.kt` (SharedPreferences persistence, in-memory fallback, and HintType limits / probabilities)
    - `HintsRepositoryImpl.kt` (Clean repository coordinating hint counters, display eligibility, resets, and reactive observeHints)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `HintsController.java` strangler boundary with `getHintsRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Profile` Strangling (`feature.social.profile` - ADR 161):
    - `ProfileRemoteDataSource.kt` (MTProto full user/chat fetch, block/unblock peer operations via BaseRemoteDataSource)
    - `ProfileLocalDataSource.kt` (User/chat entities cache, blocklist status, NotificationCenter profile event observation)
    - `ProfileRepositoryImpl.kt` (Clean repository coordinating user and chat profiles, reactive observation, blocking, and full info loading)
    - `SocialContainer.kt` & `AccountFeatureContainer.kt` wiring (Social Domain now 100% complete: 6/6 features strangled)
    - `ProfileActivity.java` strangler boundary with `getProfileRepository(account)` and `getProfileRepository()` accessors.
  - [x] Feature: `Push` Services Strangling (`feature.network.push` - ADR 162):
    - `PushRemoteDataSource.kt` (Push token request from provider, server registration via PushListenerController)
    - `PushLocalDataSource.kt` (Push tokens, provider state, registration status across accounts with test cache)
    - `PushRepositoryImpl.kt` (Clean repository coordinating push token status, provider availability, token resets, and reactive observePushStatus)
    - `NetworkContainer.kt` & `AccountFeatureContainer.kt` wiring (Network Domain now 100% complete: 4/4 features strangled)
    - `PushListenerController.java` strangler boundary with `getPushRepository(account)` and `getPushRepository()` accessors.
  - [x] Feature: `Privacy` & Security Settings Strangling (`feature.security.privacy` - ADR 163):
    - `PrivacyRemoteDataSource.kt` (MTProto TL_account.setPrivacy, TL_account.getPassword via BaseRemoteDataSource)
    - `PrivacyLocalDataSource.kt` (ContactsController privacy rules, blocked peers list, SharedConfig passcode / auto-lock settings, and event observation)
    - `PrivacyRepositoryImpl.kt` (Clean repository coordinating 17 privacy operations: rule types, blocked peers, passcode, auto-lock, and 2FA password verification)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring (Security Domain now 90% complete: 9/10 features strangled)
    - `ContactsController.java` strangler boundary with `getPrivacyRepository(account)` and `getPrivacyRepository()` accessors.
  - [x] Feature: `FlagSecure` Strangling (`feature.security.flagsecure` - ADR 164):
    - `FlagSecureLocalDataSource.kt` (Reference-counting engine for window security reasons with thread-safe ConcurrentHashMap)
    - `FlagSecureRepositoryImpl.kt` (Clean repository coordinating security reasons, dynamic rules evaluation, and reactive observeWindowState)
    - `SecurityContainer.kt` & `AccountFeatureContainer.kt` wiring (Security Domain now 100% complete: 10/10 features strangled)
    - `AndroidUtilities.java` strangler boundary with `getFlagSecureRepository(account)` and `getFlagSecureRepository()` accessors.
  - [x] Feature: `Timezones` Strangling (`feature.business.timezones` - ADR 165):
    - `TimezonesRemoteDataSource.kt` (MTProto help.getTimezonesList via BaseRemoteDataSource)
    - `TimezonesLocalDataSource.kt` (Thread-safe memory cache, TimezonesController integration, and NotificationCenter observation)
    - `TimezonesRepositoryImpl.kt` (Clean repository coordinating timezones list, system timezone detection, offset formatting, and reactive observeTimezones)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `TimezonesController.java` strangler boundary with `getTimezonesRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Business Links` Strangling (`feature.business.businesslinks` - ADR 166):
    - `BusinessLinksRemoteDataSource.kt` (MTProto create/edit/delete business links via BaseRemoteDataSource)
    - `BusinessLinksLocalDataSource.kt` (Thread-safe memory cache, BusinessLinksController integration, and NotificationCenter observation)
    - `BusinessLinksRepositoryImpl.kt` (Clean repository coordinating business chat links CRUD, limits, and reactive observeBusinessLinks)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BusinessLinksController.java` strangler boundary with `getBusinessLinksRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Quick Replies` Strangling (`feature.business.quickreplies` - ADR 167):
    - `QuickRepliesRemoteDataSource.kt` (MTProto send, reorder, delete quick replies via BaseRemoteDataSource)
    - `QuickRepliesLocalDataSource.kt` (Thread-safe memory cache, QuickRepliesController integration, and NotificationCenter observation)
    - `QuickRepliesRepositoryImpl.kt` (Clean repository coordinating quick reply shortcuts, template messages, reordering, and reactive observeQuickReplies)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `QuickRepliesController.java` strangler boundary with `getQuickRepliesRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Business Chatbots` Strangling (`feature.business.businessbots` - ADR 168):
    - `BusinessBotsRemoteDataSource.kt` (MTProto update chatbot permissions, detach chatbot via BaseRemoteDataSource)
    - `BusinessBotsLocalDataSource.kt` (Thread-safe memory cache, BusinessChatbotController integration, and NotificationCenter observation)
    - `BusinessBotsRepositoryImpl.kt` (Clean repository coordinating connected chatbots, rights delegation, recipient configuration, and reactive observeChatbots)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BusinessChatbotController.java` strangler boundary with `getBusinessBotsRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Business Recipients` Strangling (`feature.business.businessrecipients` - ADR 169):
    - `BusinessRecipientsLocalDataSource.kt` (Thread-safe memory state, mutual exclusion between selected/excluded users, BusinessRecipientsHelper integration)
    - `BusinessRecipientsRepositoryImpl.kt` (Clean repository coordinating recipient flags, user sets arbitration, and reactive observeState)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BusinessRecipientsHelper.java` strangler boundary with `getBusinessRecipientsRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Bot Stars` Strangling (`feature.business.botstars` - ADR 170):
    - `BotStarsRemoteDataSource.kt` (MTProto bot stars revenue stats, TON revenue stats, transactions, connected referral bots, suggested programs, admined bots/channels via BaseRemoteDataSource)
    - `BotStarsLocalDataSource.kt` (Thread-safe in-memory cache, BotStarsController integration, and NotificationCenter observation)
    - `BotStarsRepositoryImpl.kt` (Clean repository coordinating revenue stats, transaction filtering, connected referral bots, suggested programs, and reactive observeBotStarsStats)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `BotStarsController.java` strangler boundary with `getBotStarsRepository(account)` and `getRepository()` accessors.
  - [x] Feature: `Star Gifts` Strangling (`feature.business.stargifts` - ADR 171):
    - `StarGiftsRemoteDataSource.kt` (MTProto star gifts catalog, gift lookup, saved profile gifts loading, and gift pin/hide toggling via BaseRemoteDataSource)
    - `StarGiftsLocalDataSource.kt` (Thread-safe in-memory catalog, gifts cache, profile gifts, and NotificationCenter observation)
    - `StarGiftsRepositoryImpl.kt` (Clean repository coordinating star gifts catalog, individual gifts, profile gifts pagination, and reactive observeCatalog)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `StarsController.java` strangler boundary with `getStarGiftsRepository(account)` and `getStarGiftsRepository()` accessors.
  - [x] Feature: `Payments` Strangling (`feature.business.payments` - ADR 172):
    - `PaymentsRemoteDataSource.kt` (MTProto stars balance, transactions, subscriptions, and top-up options via BaseRemoteDataSource)
    - `PaymentsLocalDataSource.kt` (Thread-safe balance state, transaction history, subscriptions, top-up options, and NotificationCenter observation)
    - `PaymentsRepositoryImpl.kt` (Clean repository coordinating stars balance, transaction history, subscriptions, top-up options, and reactive observeBalance)
    - `BusinessContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `StarsController.java` strangler boundary with `getPaymentsRepository(account)` and `getPaymentsRepository()` accessors.
  - [x] Feature: `LauncherIconController` Strangling (`feature.system.launchericon` - ADR 173):
    - `LauncherIconRemoteDataSource.kt` (System extension point for remote launcher icon configs)
    - `LauncherIconLocalDataSource.kt` (PackageManager component enablement queries, active icon StateFlow, test mode fallback)
    - `LauncherIconRepositoryImpl.kt` (Clean repository coordinating icon options, active icon, enablement checks, and fixing)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `LauncherIconController.java` strangler boundary with `getLauncherIconRepository(account)` and `getLauncherIconRepository()` accessors.
  - [x] Feature: `LiteMode` Strangling (`feature.system.litemode` - ADR 174):
    - `LiteModeRemoteDataSource.kt` (System extension point for remote lite mode presets)
    - `LiteModeLocalDataSource.kt` (Thread-safe flags state, power saver threshold, battery level, presets, and test mode fallback)
    - `LiteModeRepositoryImpl.kt` (Clean repository coordinating flags, presets, power saving thresholds, battery level, and reactive observeState)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `LiteMode.java` strangler boundary with `getLiteModeRepository(account)` and `getLiteModeRepository()` accessors.
  - [x] Feature: `WindowVisibilityManager` Strangling (`feature.system.windowvisibility` - ADR 175):
    - `WindowVisibilityRemoteDataSource.kt` (System extension point for remote window visibility configurations)
    - `WindowVisibilityLocalDataSource.kt` (Reference-counting reasonsToHide, active reasons set, StateFlow, SharedFlow, test mode)
    - `WindowVisibilityRepositoryImpl.kt` (Clean repository coordinating requestHide, releaseHide, toggleHide, reset, and subsystem controllers)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `WindowVisibilityManager.java` strangler boundary with `getWindowVisibilityRepository(account)`, `getWindowVisibilityRepository()`, and `getRepository()` accessors.
  - [x] Feature: `AnimationNotificationsLocker` Strangling (`feature.system.animationlocker` - ADR 176):
    - `AnimationLockerRemoteDataSource.kt` (System extension point for remote animation locker configs)
    - `AnimationLockerLocalDataSource.kt` (ConcurrentHashMap lock records, NotificationCenter suppression, reactive flows, headless test mode)
    - `AnimationLockerRepositoryImpl.kt` (Clean repository coordinating locks, releases, scopes, filtering, and reactive state)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `AnimationNotificationsLocker.java` strangler boundary with `getAnimationLockerRepository(account)`, `getAnimationLockerRepository()`, and `getRepository()` accessors.
  - [x] Feature: `ANRDetector` Strangling (`feature.system.anrwatchdog` - ADR 177):
    - `AnrWatchdogRemoteDataSource.kt` (System extension point for remote ANR telemetry and incident reporting)
    - `AnrWatchdogLocalDataSource.kt` (Thread-safe heartbeat ping dispatch, acknowledgement, freeze detection calculation, incident history, flows)
    - `AnrWatchdogRepositoryImpl.kt` (Clean repository coordinating start/stop, lifecycle transitions, pings, freeze detection, incidents, and reactive observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ANRDetector.java` strangler boundary with `getAnrWatchdogRepository(account)`, `getAnrWatchdogRepository()`, and `getRepository()` accessors.
  - [x] Feature: `EmuDetector` Strangling (`feature.system.emudetector` - ADR 178):
    - `EmuDetectorRemoteDataSource.kt` (System extension point for remote emulator detection rules and blacklists)
    - `EmuDetectorLocalDataSource.kt` (Hardware heuristics, build properties, companion packages, diagnostics caching, reactive flows, headless test mode)
    - `EmuDetectorRepositoryImpl.kt` (Clean repository coordinating environment detection, caching, force refresh, custom packages, config, and reactive observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `EmuDetector.java` strangler boundary with `getEmuDetectorRepository(account)` and `getEmuDetectorRepository()` accessors.
  - [x] Feature: `LeakDetector` Strangling (`feature.system.leakdetector` - ADR 179):
    - `LeakDetectorRemoteDataSource.kt` (System extension point for remote leak telemetry and incident reporting)
    - `LeakDetectorLocalDataSource.kt` (WeakReference tracking, ConcurrentHashMap registry, two-phase GC confirmation, live object count, flows)
    - `LeakDetectorRepositoryImpl.kt` (Clean repository coordinating tracking, rechecks, leak confirmation, stats, and reactive state)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `LeakDetector.java` strangler boundary with `getLeakDetectorRepository(account)`, `getLeakDetectorRepository()`, and `getRepository()` accessors.
  - [x] Feature: `Choreographer60FpsContent` Strangling (`feature.system.fpscontent` - ADR 180):
    - `FpsContentRemoteDataSource.kt` (System extension point for remote frame rate policy configuration)
    - `FpsContentLocalDataSource.kt` (Frame rate arbitration, stride groups, accumulator groups, view/drawable invalidation scheduling, stats)
    - `FpsContentRepositoryImpl.kt` (Clean repository coordinating frame callbacks, runnable callbacks, invalidations, vsync ticks, and stats)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `Choreographer60FpsContent.java` strangler boundary with `getFpsContentRepository(account)`, `getFpsContentRepository()`, and `getRepository()` accessors.
  - [x] Feature: `FloatingDebugController` Strangling (`feature.system.floatingdebug` - ADR 181):
    - `FloatingDebugRemoteDataSource.kt` (System extension point for remote debug overlay configuration and policies)
    - `FloatingDebugLocalDataSource.kt` (Debug overlay active state, notification center listener, launch activity provider, flows, headless test mode)
    - `FloatingDebugRepositoryImpl.kt` (Clean repository coordinating active state, dismissal, showing, fab visibility, and reactive observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `FloatingDebugController.java` strangler boundary with `getFloatingDebugRepository(account)`, `getFloatingDebugRepository()`, and `getRepository()` accessors.
  - [x] Feature: `AppGlobalConfig` Strangling (`feature.system.appconfig` - ADR 182):
    - `AppConfigRemoteDataSource.kt` (System extension point for remote MTProto app configuration syncing)
    - `AppConfigLocalDataSource.kt` (In-memory cached config state, NotificationCenter updates, custom key-value map, reactive flows, headless test mode)
    - `AppConfigRepositoryImpl.kt` (Clean repository coordinating config state, reload, value mutations, and reactive observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `AppGlobalConfig.java` strangler boundary with `getAppConfigRepository(account)`, `getAppConfigRepository()`, and `getRepository()` accessors.
  - [x] Feature: `Browser` Strangling (`feature.system.browser` - ADR 183):
    - `BrowserRemoteDataSource.kt` (System extension point for safe browsing policies, URL blacklists, and malicious domain telemetry)
    - `BrowserLocalDataSource.kt` (Browser settings, in-app / custom tabs / external browser dispatching, history tracking, URL classification, headless test mode)
    - `BrowserRepositoryImpl.kt` (Clean repository coordinating browser preferences, URL safety checks, open URL, history, and cache clearing)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `Browser.java` strangler boundary with `getBrowserRepository(account)` and `getBrowserRepository()` accessors.
  - [x] Feature: `CountdownTimer` Strangling (`feature.system.countdowntimer` - ADR 184):
    - `CountdownTimerRemoteDataSource.kt` (System extension point for NTP server time synchronization and remote countdown calibration)
    - `CountdownTimerLocalDataSource.kt` (ConcurrentHashMap timer records, ticking jobs, pause/resume, manual tick arbitration, StateFlow, SharedFlow)
    - `CountdownTimerRepositoryImpl.kt` (Clean repository coordinating start, stop, pause, resume, manual ticks, clear, and reactive observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
  - [x] Feature: `DataStorage` Strangling (`feature.system.datastorage` - ADR 185):
    - `DataStorageRemoteDataSource.kt` (System extension point for cloud storage limits, cleanup policies, and remote auto-download profiles)
    - `DataStorageLocalDataSource.kt` (Local cache sizes, directory sizes, database maintenance, network stats, auto-download presets, keep-media settings, headless test mode)
    - `DataStorageRepositoryImpl.kt` (Clean repository coordinating storage usage, cache clearing, database compaction, auto-download presets, and keep-media rules)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `StatsController.java` strangler boundary with `getDataStorageRepository(account)` and `getDataStorageRepository()` accessors.
  - [x] Feature: `Ringtones` Strangling (`feature.system.ringtones` - ADR 186):
    - `RingtoneRemoteDataSource.kt` (Remote MTProto custom sound synchronization, document saving/unsaving, and RPC cancellation)
    - `RingtoneLocalDataSource.kt` (In-memory ringtone registry, eligibility limits, title extraction, upload lifecycle, tone selection, headless test mode)
    - `RingtoneRepositoryImpl.kt` (Clean repository coordinating notification sounds, upload flows, selection, and reactive observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `NotificationsController.java` strangler boundary with `getRingtoneRepository(account)` and `getRingtoneRepository()` accessors.
  - [x] Feature: `Settings` Strangling (`feature.system.settings` - ADR 187):
    - `SettingsRemoteDataSource.kt` (Remote MTProto global privacy and account setting synchronization)
    - `SettingsLocalDataSource.kt` (Thread-safe aggregated client settings, font size, bubble radius, stream media, gallery save, contact sync, headless test mode)
    - `SettingsRepositoryImpl.kt` (Clean repository coordinating client preferences, UI appearance updates, and reactive observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `SharedConfig.java` strangler boundary with `getSettingsRepository(account)` and `getSettingsRepository()` accessors.
  - [x] Feature: `Themes` Strangling (`feature.system.themes` - ADR 188):
    - `ThemesRemoteDataSource.kt` (Remote MTProto theme synchronization and wallpaper fetching)
    - `ThemesLocalDataSource.kt` (Theme settings, night mode type, accents, bubble radius, wallpaper, headless test mode)
    - `ThemeRepositoryImpl.kt` (Clean repository coordinating theme application, accents, night mode, and appearance settings)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `Theme.java` strangler boundary with `getThemeRepository(account)` and `getThemeRepository()` accessors.
  - [x] Feature: `Localization` Strangling (`feature.system.localization` - ADR 189):
    - `LocalizationRemoteDataSource.kt` (Remote language pack and locale catalog synchronization)
    - `LocalizationLocalDataSource.kt` (Current locale, available locales, custom string overrides, 24-hour format, name display order, RTL detection)
    - `LocalizationRepositoryImpl.kt` (Clean repository coordinating locales, strings, time formatting, name ordering, and reactive state observation)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `LocaleController.java` strangler boundary with `getLocalizationRepository(account)` and `getLocalizationRepository()` accessors.
  - [x] Feature: `MainTabs` Strangling (`feature.system.maintabs` - ADR 190):
    - `MainTabsRemoteDataSource.kt` (System extension point for remote navigation and tabs configuration)
    - `MainTabsLocalDataSource.kt` (Tab visibility, selection, calls tab toggle, chats unread count, contacts permission warning, headless test mode)
    - `MainTabsRepositoryImpl.kt` (Clean repository coordinating tab visibility, position selection, call tab toggling, and unread counters)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MainTabsActivityController.java` & `MainTabsActivity.java` strangler boundary with `getMainTabsRepository(account)` and `getMainTabsRepository()` accessors.
  - [x] Feature: `AdjustPan` Strangling (`feature.system.adjustpan` - ADR 191):
    - `AdjustPanRemoteDataSource.kt` (System extension point for adjust pan layout configuration)
    - `AdjustPanLocalDataSource.kt` (Pan calculation spec, transition plan arbitration, translation interpolation, keyboard visibility tracking)
    - `AdjustPanRepositoryImpl.kt` (Clean repository coordinating pan transition planning, progress computation, and lifecycle transitions)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
  - [x] Feature: `KeyboardHide` Strangling (`feature.system.keyboardhide` - ADR 192):
    - `KeyboardHideRemoteDataSource.kt` (System extension point for gesture dismiss telemetry and thresholds)
    - `KeyboardHideLocalDataSource.kt` (Interactive pull-down drag geometry, dismiss decision evaluation, settling animations, headless test mode)
    - `KeyboardHideRepositoryImpl.kt` (Clean repository coordinating gesture progress calculation, dismiss arbitration, and lifecycle states)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `KeyboardHideHelper.java` strangler boundary with `getKeyboardHideRepository(account)` and `getKeyboardHideRepository()` accessors.
  - [x] Feature: `KeyboardInsets` Strangling (`feature.system.keyboardinsets` - ADR 193):
    - `KeyboardInsetsRemoteDataSource.kt` (System extension point for window insets telemetry and policy synchronization)
    - `KeyboardInsetsLocalDataSource.kt` (In-app keyboard height arbitration, navigation bar offsets, window IME bottom insets, visibility animation states)
    - `KeyboardInsetsRepositoryImpl.kt` (Clean repository coordinating in-app keyboard heights, navbar inclusions, system insets updates, and reactive flow)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `WindowInsetsInAppController.java` strangler boundary with `getKeyboardInsetsRepository(account)` and `getKeyboardInsetsRepository()` accessors.
  - [x] Feature: `PinchToZoom` Strangling (`feature.system.pinchtozoom` - ADR 194):
    - `PinchToZoomRemoteDataSource.kt` (System extension point for zoom calibration and gesture telemetry)
    - `PinchToZoomLocalDataSource.kt` (Scale calculations, center-point translation geometry, transform matrix calculation, image bounds arbitration, gesture decisions)
    - `PinchToZoomRepositoryImpl.kt` (Clean repository coordinating pinch zoom start, update, finish, and geometric math)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
  - [x] Feature: `RecyclerScroll` Strangling (`feature.system.recyclerscroll` - ADR 195):
    - `RecyclerScrollRemoteDataSource.kt` (System extension point for scroll physics telemetry and animation calibration)
    - `RecyclerScrollLocalDataSource.kt` (Scroll animation eligibility arbitration, plan calculation, scroll length geometry, translation offsets, progress interpolation)
    - `RecyclerScrollRepositoryImpl.kt` (Clean repository coordinating list scroll animations, eligibility evaluation, and view translation calculations)
    - `SystemContainer.kt` & `AccountFeatureContainer.kt` wiring
  - [x] Feature: `AudioPlayer` Strangling (`feature.media.audioplayer` - ADR 196):
    - `AudioPlayerRemoteDataSource.kt` (Remote streaming configuration and cloud playback policies)
    - `AudioPlayerLocalDataSource.kt` (Playback state, playlist queue, shuffle/repeat modes, equalizer bands, and audio routes)
    - `AudioPlayerRepositoryImpl.kt` (Clean repository coordinating playback state, queue navigation, equalizer, and remote sync)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MediaController.java` strangler boundary with `getAudioPlayerRepository(account)` and `getAudioPlayerRepository()` accessors.
  - [x] Feature: `AutoDeleteMedia` Strangling (`feature.media.autodeletemedia` - ADR 197):
    - `AutoDeleteMediaRemoteDataSource.kt` (Cloud auto-delete media retention policies and telemetry)
    - `AutoDeleteMediaLocalDataSource.kt` (Thread-safe file locks, cache eviction candidate scanning, and cleanup passes on Dispatchers.IO)
    - `AutoDeleteMediaRepositoryImpl.kt` (Clean repository coordinating cleanup passes, locked files, and state observation)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `AutoDeleteMediaTask.java` strangler boundary with `getAutoDeleteMediaRepository(account)` and `getAutoDeleteMediaRepository()` accessors.
  - [x] Feature: `ContentPreview` Strangling (`feature.media.contentpreview` - ADR 198):
    - `ContentPreviewRemoteDataSource.kt` (Remote content preview metadata and cloud preview configuration)
    - `ContentPreviewLocalDataSource.kt` (Interactive content preview overlay state, drag gesture progress, and contextual action menus)
    - `ContentPreviewRepositoryImpl.kt` (Clean repository coordinating preview opening, drag progress, contextual menus, and dismissal)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ContentPreviewViewer.java` strangler boundary with `getContentPreviewRepository(account)` and `getContentPreviewRepository()` accessors.
  - [x] Feature: `FileLoader` Strangling (`feature.media.fileloader` - ADR 199):
    - `FileLoaderRemoteDataSource.kt` (MTProto file upload and download RPC operations)
    - `FileLoaderLocalDataSource.kt` (Thread-safe file transfers state flow, active/recent downloads, and local path queries)
    - `FileLoaderRepositoryImpl.kt` (Clean repository coordinating upload/download operations and transfer status observation)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `FileLoader.java` strangler boundary with `getFileLoaderRepository(account)` and `getFileLoaderRepository()` accessors.
  - [x] Feature: `ImageLoader` Strangling (`feature.media.imageloader` - ADR 200):
    - `ImageLoaderRemoteDataSource.kt` (Remote HTTP image tasks and cloud CDN fetches)
    - `ImageLoaderLocalDataSource.kt` (Tiered in-memory and disk cache, request queues, and memory pressure trimming)
    - `ImageLoaderRepositoryImpl.kt` (Clean repository coordinating image request lifecycle, cache tier queries, and cache hits/misses)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ImageLoader.java` strangler boundary with `getImageLoaderRepository(account)` and `getImageLoaderRepository()` accessors.
  - [x] Feature: `MediaData` Strangling (`feature.media.mediadata` - ADR 201):
    - `MediaDataRemoteDataSource.kt` (Remote media sync, cloud media metadata, and sticker/GIF sets)
    - `MediaDataLocalDataSource.kt` (Device media albums, photos/videos mapping, and reactive albums state flow)
    - `MediaDataRepositoryImpl.kt` (Clean repository coordinating album queries and media item retrieval)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MediaDataController.java` strangler boundary with `getMediaDataRepository(account)` and `getMediaDataRepository()` accessors.
  - [x] Feature: `PhotoViewer` Strangling (`feature.media.photoviewer` - ADR 202):
    - `PhotoViewerRemoteDataSource.kt` (Cloud photo/video metadata, caption translation, and streaming manifest fetching)
    - `PhotoViewerLocalDataSource.kt` (Active photo/video state, viewer open/closed state, current index, zoom scale, gesture offsets, and PIP state)
    - `PhotoViewerRepositoryImpl.kt` (Clean repository coordinating photo viewer lifecycle, navigation, zoom gestures, PIP transitions, and reactive state)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `PhotoViewer.java` strangler boundary with `getPhotoViewerRepository(account)` and `getPhotoViewerRepository()` accessors.
  - [x] Feature: `SharedMedia` Strangling (`feature.media.sharedmedia` - ADR 203):
    - `SharedMediaRemoteDataSource.kt` (Remote shared media pagination, type-specific queries: photos, videos, files, audio, links, voice)
    - `SharedMediaLocalDataSource.kt` (Tab navigation state, search query, selected type filter, fast scroll index, and selection mode)
    - `SharedMediaRepositoryImpl.kt` (Clean repository coordinating shared media tabs, type filters, selection mode, and reactive state observation)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `SharedMediaLayout.java` strangler boundary with `getSharedMediaRepository(account)` and `getSharedMediaRepository()` accessors.
  - [x] Feature: `Stories` Strangling (`feature.media.stories` - ADR 204):
    - `StoriesRemoteDataSource.kt` (MTProto stories fetching, peer stories queries, upload, reaction, privacy settings, and view count tracking)
    - `StoriesLocalDataSource.kt` (Cached stories by peer ID, upload draft queue, active story viewer state, and unread indicator flows)
    - `StoriesRepositoryImpl.kt` (Clean repository coordinating stories synchronization, upload pipeline, reaction dispatching, and reactive state)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `StoriesController.java` strangler boundary with `getStoriesRepository(account)` and `getStoriesRepository()` accessors.
  - [x] Feature: `VoIP` Strangling (`feature.media.voip` - ADR 205):
    - `VoIPRemoteDataSource.kt` (Signaling RPC, group call participants synchronization, encryption key exchange, and server endpoints)
    - `VoIPLocalDataSource.kt` (Active call state, mute status, speakerphone routing, video state, signal strength, call duration, and audio mode)
    - `VoIPRepositoryImpl.kt` (Clean repository coordinating call lifecycle, audio routing, mute/unmute, video capture toggling, and reactive state)
    - `MediaContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `VoIPService.java` strangler boundary with `getVoIPRepository(account)` and `getVoIPRepository()` accessors.
  - [x] Feature: `AutoDelete` Strangling (`feature.messaging.autodelete` - ADR 206):
    - `AutoDeleteRemoteDataSource.kt` (MTProto RPC requests: setDefaultHistoryTTL, setHistoryTTL)
    - `AutoDeleteLocalDataSource.kt` (UserConfig default TTL & local chat TTL state flows)
    - `AutoDeleteRepositoryImpl.kt` (Clean repository coordinating global and per-chat auto-delete TTL)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MessagesController.java` strangler boundary with `getAutoDeleteRepository(account)` and `getAutoDeleteRepository()` accessors.
  - [x] Feature: `BottomViews` Strangling (`feature.messaging.bottomviews` - ADR 207):
    - `BottomViewsRemoteDataSource.kt` (Remote bottom bar overlay metadata)
    - `BottomViewsLocalDataSource.kt` (Bitwise container flags, priority selection, and visibilities map)
    - `BottomViewsVisibilityRepositoryImpl.kt` (Clean repository coordinating mutual exclusion and container visibility)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ChatActivity.java` strangler boundary with `getBottomViewsVisibilityRepository(account)` and `getBottomViewsVisibilityRepository()` accessors.
  - [x] Feature: `ChatAttach` Strangling (`feature.messaging.chatattach` - ADR 208):
    - `ChatAttachRemoteDataSource.kt` (Remote attachment limits and layout capabilities)
    - `ChatAttachLocalDataSource.kt` (Attach alert state, layout selection, multi-item selection ordering, permissions, and send options)
    - `ChatAttachRepositoryImpl.kt` (Clean repository coordinating attachment dialog state, layout arbitration, and send options)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ChatAttachAlert.java` strangler boundary with `getChatAttachRepository(account)` and `getChatAttachRepository()` accessors.
  - [x] Feature: `ChatInput` Strangling (`feature.messaging.chatinput` - ADR 209):
    - `ChatInputRemoteDataSource.kt` (Remote typing indicator dispatch and draft synchronization)
    - `ChatInputLocalDataSource.kt` (Input text, selection, panel modes, reply/edit quotes, send button calculation, and voice/video recording lifecycle)
    - `ChatInputRepositoryImpl.kt` (Clean repository coordinating input bar state, recording progress, send options, and panel visibility)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ChatActivityEnterView.java` strangler boundary with `getChatInputRepository(account)` and `getChatInputRepository()` accessors.
  - [x] Feature: `DraftMeasure` Strangling (`feature.messaging.draftmeasure` - ADR 210):
    - `DraftMeasureRemoteDataSource.kt` (Remote draft measure capability query)
    - `DraftMeasureLocalDataSource.kt` (Target calculation, height overrides, message height derivation, and config state flow)
    - `DraftMeasureRepositoryImpl.kt` (Clean repository coordinating draft height calculations, target state, and layout measurements)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ChatActivityDraftMessageMeasureController.java` strangler boundary with `getDraftMeasureRepository(account)` and `getDraftMeasureRepository()` accessors.
  - [x] Feature: `Drafts` Strangling (`feature.messaging.drafts` - ADR 211):
    - `DraftsRemoteDataSource.kt` (Remote cloud drafts synchronization and cleanup RPCs)
    - `DraftsLocalDataSource.kt` (Draft cache, state flows, TTL cleanup, and story/message draft storage)
    - `DraftsRepositoryImpl.kt` (Clean repository coordinating draft persistence, edit drafts, expiration cleanup, and reactive state)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `DraftsController.java` strangler boundary with `getDraftsRepository(account)` and `getDraftsRepository()` accessors.
  - [x] Feature: `EmojiEffects` Strangling (`feature.messaging.emojieffects` - ADR 212):
    - `EmojiEffectsRemoteDataSource.kt` (Remote animated emoji effects configuration)
    - `EmojiEffectsLocalDataSource.kt` (Emoji normalization, interaction payloads, screen overlay bounds, geometry calculations, and animation quotas)
    - `EmojiEffectsRepositoryImpl.kt` (Clean repository coordinating emoji tap events, animation progress, dismissal, and overlay state)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `EmojiAnimationsOverlay.java` strangler boundary with `getEmojiEffectsRepository(account)` and `getEmojiEffectsRepository()` accessors.
  - [x] Feature: `EmojiPicker` Strangling (`feature.messaging.emojipicker` - ADR 213):
    - `EmojiPickerRemoteDataSource.kt` (Remote sticker/emoji packs and search queries)
    - `EmojiPickerLocalDataSource.kt` (Picker tab selection, recent emoji/stickers cache, search query, and category filters)
    - `EmojiPickerRepositoryImpl.kt` (Clean repository coordinating emoji picker state, tabs, category selection, and item filtering)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `EmojiView.java` strangler boundary with `getEmojiPickerRepository(account)` and `getEmojiPickerRepository()` accessors.
  - [x] Feature: `Mentions` Strangling (`feature.messaging.mentions` - ADR 214):
    - `MentionsRemoteDataSource.kt` (Contacts and bot user queries via ConnectionsManager and MessagesController)
    - `MentionsLocalDataSource.kt` (Thread-safe in-memory cache, active query state, and bot context)
    - `MentionsRepositoryImpl.kt` (Clean repository coordinating mention suggestions, inline bots, and usernames)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `MentionsAdapter.java` strangler boundary with `getMentionsRepository(account)` and `getMentionsRepository()` accessors.
  - [x] Feature: `Reactions` Strangling (`feature.messaging.reactions` - ADR 215):
    - `ReactionsRemoteDataSource.kt` (Remote reactions loading, send reactions, clear reactions, and poll voting via MessagesController & ConnectionsManager)
    - `ReactionsLocalDataSource.kt` (Recent/top reactions cache, default reaction preferences, and reaction state flows)
    - `ReactionsRepositoryImpl.kt` (Clean repository coordinating available reactions, selection, and message reactions)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `ReactionsContainerLayout.java` strangler boundary with `getReactionsRepository(account)` and `getReactionsRepository()` accessors.
  - [x] Feature: `RichCaption` Strangling (`feature.messaging.richcaption` - ADR 216):
    - `RichCaptionRemoteDataSource.kt` (Remote caption translation and rich entity parsing)
    - `RichCaptionLocalDataSource.kt` (Caption text, selection limits, format spans, quote styles, and expand/collapse state)
    - `RichCaptionRepositoryImpl.kt` (Clean repository coordinating media viewer caption formatting, quote selection, and character bounds)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `RichCaptionController.java` strangler boundary with `getRichCaptionRepository(account)` and `getRichCaptionRepository()` accessors.
  - [x] Feature: `Stickers` Strangling (`feature.messaging.stickers` - ADR 217):
    - `StickersRemoteDataSource.kt` (Remote sticker sets loading, install, archive, and removal via MediaDataController)
    - `StickersLocalDataSource.kt` (Thread-safe sticker sets cache, recent stickers, emoji mapping, and StateFlow)
    - `StickersRepositoryImpl.kt` (Clean repository coordinating sticker sets, installation toggles, and emoji matching)
    - `MessagingContainer.kt` & `AccountFeatureContainer.kt` wiring
    - `StickersAlert.java` strangler boundary with `getStickersRepository(account)` and `getStickersRepository()` accessors.

---

## 6. Architecture Decision Records (ADRs)

### ADR 217: Sticker Sets, Recent Stickers & Emoji Matching Strangling via Clean DataSources & StickersRepositoryImpl
- **Context:** Sticker sets management, install/archive state toggles, emoji-to-sticker correlations, and recent sticker caching were coupled inside `MediaDataController` and UI components like `StickersAlert.java`.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.stickers`:
  1. Implement `StickersRemoteDataSource`:
     - Dispatches sticker set loading, install, and archive toggle operations via `MediaDataController` with headless environment protection.
  2. Implement `StickersLocalDataSource`:
     - Manages reactive StateFlow of sticker sets, thread-safe in-memory cache, recent stickers, and emoji-to-sticker mappings with headless fallback.
  3. Implement `StickersRepositoryImpl`:
     - Implements `StickersRepository`, coordinating sticker set queries, installation toggles, and emoji lookups.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `StickersRepositoryImpl`.
  5. Introduce strangler boundary: `StickersAlert.getStickersRepository(account)` and `getStickersRepository()` accessors.
- **Consequences:** Sticker management is decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `StickersRepositoryImplTest.kt`.

### ADR 216: Media Viewer Rich Caption Formatting, Quotes & Text Selection Strangling via Clean DataSources & RichCaptionRepositoryImpl
- **Context:** Media viewer caption text formatting, selection range calculation, quote styling, expandable caption state, and character counter limits were coupled inside `RichCaptionController.java` with direct text view and animators interactions.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.richcaption`:
  1. Implement `RichCaptionRemoteDataSource`:
     - Handles remote caption translation and rich entity conversions.
  2. Implement `RichCaptionLocalDataSource`:
     - Encapsulates formatted caption state, character limits (1024 regular, 2048 premium), selection ranges, and expand/collapse transitions.
  3. Implement `RichCaptionRepositoryImpl`:
     - Implements `RichCaptionRepository`, coordinating caption updates, selection, and formatting.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `RichCaptionRepositoryImpl`.
  5. Introduce strangler boundary: `RichCaptionController.getRichCaptionRepository(account)` and `getRichCaptionRepository()` accessors.
- **Consequences:** Rich caption formatting and state management are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `RichCaptionRepositoryImplTest.kt`.

### ADR 215: Message Reactions, Floating Emojis & Custom Reactions Strangling via Clean DataSources & ReactionsRepositoryImpl
- **Context:** Message reactions dispatching, floating emoji animations, recent reactions storage, poll voting, and custom animated emoji reactions were coupled across `ReactionsContainerLayout.java`, `MessagesController`, and `ConnectionsManager`.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.reactions`:
  1. Implement `ReactionsRemoteDataSource`:
     - Dispatches reactions sending, clear reactions, available reactions loading, and poll voting via MTProto RPCs.
  2. Implement `ReactionsLocalDataSource`:
     - Manages reactive StateFlow of recent reactions, top reactions, and selected custom reaction models.
  3. Implement `ReactionsRepositoryImpl`:
     - Implements `ReactionsRepository`, coordinating available reactions, sending reactions, and default reaction selection.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `ReactionsRepositoryImpl`.
  5. Introduce strangler boundary: `ReactionsContainerLayout.getReactionsRepository(account)` and `getReactionsRepository()` accessors.
- **Consequences:** Message reactions and voting are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `ReactionsRepositoryImplTest.kt`.

### ADR 214: Username, Hashtag & Bot Mentions Autocomplete Strangling via Clean DataSources & MentionsRepositoryImpl
- **Context:** Mention suggestions, hashtag autocompletion, bot commands, and inline bot results were intertwined in `MentionsAdapter.java` (~2800 lines) with direct references to `MessagesController`, `SearchQuery`, and `ConnectionsManager`.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.mentions`:
  1. Implement `MentionsRemoteDataSource`:
     - Dispatches remote contact searches and inline bot result queries via MTProto RPCs.
  2. Implement `MentionsLocalDataSource`:
     - Manages thread-safe in-memory cache of suggested users, bot commands, hashtags, and active mention query.
  3. Implement `MentionsRepositoryImpl`:
     - Implements `MentionsRepository`, coordinating mention search queries, inline bot triggers, and cached username suggestions.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `MentionsRepositoryImpl`.
  5. Introduce strangler boundary: `MentionsAdapter.getMentionsRepository(account)` and `getMentionsRepository()` accessors.
- **Consequences:** Mentions autocompletion and bot querying are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `MentionsRepositoryImplTest.kt`.

### ADR 213: Emoji & Sticker Picker Navigation, Search & Category Tabs Strangling via Clean DataSources & EmojiPickerRepositoryImpl
- **Context:** Emoji, sticker, and GIF picker navigation, category tabs (recent, favorite, premium, reaction packs), query filtering, and tab arbitration were tightly coupled inside `EmojiView.java` (~8500 lines) with direct view hierarchy interactions and static controller singletons.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.emojipicker`:
  1. Implement `EmojiPickerRemoteDataSource`:
     - Fetches remote sticker and emoji pack metadata and search queries.
  2. Implement `EmojiPickerLocalDataSource`:
     - Manages active tab state, search query, recent emoji/sticker collections, and category filters.
  3. Implement `EmojiPickerRepositoryImpl`:
     - Implements `EmojiPickerRepository`, delegating tab switching, category selections, item filtering, and query state to local and remote data sources.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `EmojiPickerRepositoryImpl`.
  5. Introduce strangler boundary: `EmojiView.getEmojiPickerRepository(account)` and `getEmojiPickerRepository()` accessors.
- **Consequences:** Picker tab state, item filtering, and search queries are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `EmojiPickerRepositoryImplTest.kt`.

### ADR 212: Animated Emoji Effects, Interaction JSON Payloads & Particle Overlay Strangling via Clean DataSources & EmojiEffectsRepositoryImpl
- **Context:** Fullscreen and animated emoji interactions, emoji character normalization (stripping tone modifiers, ZWJ gender variations, variation selectors), JSON interaction payloads (`v`, `a`, `t`), screen-aware overlay bounds calculation, and animation quota rules (max 12 global, max 4 per message) were coupled inside `EmojiAnimationsOverlay.java` (~1200 lines).
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.emojieffects`:
  1. Implement `EmojiEffectsRemoteDataSource`:
     - Retrieves remote animation effects configuration and availability.
  2. Implement `EmojiEffectsLocalDataSource`:
     - Encapsulates emoji normalization, interaction JSON payload serialization, screen bounds calculations, animation quota limits, and active effect tracking.
  3. Implement `EmojiEffectsRepositoryImpl`:
     - Implements `EmojiEffectsRepository`, coordinating emoji tap events, animation progress, dismissal, and overlay state.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `EmojiEffectsRepositoryImpl`.
  5. Introduce strangler boundary: `EmojiAnimationsOverlay.getEmojiEffectsRepository(account)` and `getEmojiEffectsRepository()` accessors.
- **Consequences:** Animated emoji effects, geometry derivations, and interaction JSON payloads are isolated behind clean domain contracts and use cases. 100% unit test coverage achieved with `EmojiEffectsRepositoryImplTest.kt`.

### ADR 211: Story & Message Drafts Persistence, Expiration & Synchronization Strangling via Clean DataSources & DraftsRepositoryImpl
- **Context:** In-progress story and message multimedia drafts, SQLite table persistence (`story_drafts`), raw file copying in `cache/drafts`, 7-day expiration checks, and global notifications were entangled inside `DraftsController.java` (~1077 lines) in `org.telegram.ui.Stories.recorder`.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.drafts`:
  1. Implement `DraftsRemoteDataSource`:
     - Synchronizes cloud drafts and executes remote cleanup RPCs.
  2. Implement `DraftsLocalDataSource`:
     - Manages reactive `DraftsStateModel` flows, in-memory drafts cache, TTL expiration checks, and draft mutations.
  3. Implement `DraftsRepositoryImpl`:
     - Implements `DraftsRepository`, coordinating draft saves, deletions, edit-draft retrieval, and expired draft cleanup.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `DraftsRepositoryImpl`.
  5. Introduce strangler boundary: `DraftsController.getDraftsRepository(account)` and `getDraftsRepository()` accessors.
- **Consequences:** Draft persistence, expiration lifecycle, and reactive state observation are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `DraftsRepositoryImplTest.kt`.

### ADR 210: Draft Message Layout Measurement, Height Override & Keyboard Insets Strangling via Clean DataSources & DraftMeasureRepositoryImpl
- **Context:** Chat draft measurement calculations, target view dimension overrides, previous message height compensation, and keyboard inset transitions were coupled inside `ChatActivityDraftMessageMeasureController.java` (~220 lines).
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.draftmeasure`:
  1. Implement `DraftMeasureRemoteDataSource`:
     - Queries remote draft measurement capabilities and platform limits.
  2. Implement `DraftMeasureLocalDataSource`:
     - Manages thread-safe `DraftMeasureConfigModel` StateFlow, target dimension calculations, height overrides, and keyboard inset adjustments.
  3. Implement `DraftMeasureRepositoryImpl`:
     - Implements `DraftMeasureRepository`, delegating calculation of layout overrides and configuration state to data sources.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `DraftMeasureRepositoryImpl`.
  5. Introduce strangler boundary: `ChatActivityDraftMessageMeasureController.getDraftMeasureRepository(account)` and `getDraftMeasureRepository()` accessors.
- **Consequences:** Draft height measurement, message ID tracking, and layout overrides are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `DraftMeasureRepositoryImplTest.kt`.

### ADR 209: Chat Input Bar, Text Formatting, Recording Lifecycle & Virtual Panels Strangling via Clean DataSources & ChatInputRepositoryImpl
- **Context:** Text input entry, markdown selection formatting, audio/video recording lifecycles (lock, pause, resume, cancel, preview), virtual panel arbitration (emoji/sticker tabs, bot keyboards, attachment sheets), and reply/edit quotes were orchestrated by `ChatActivityEnterView.java` (~15600 lines) with tight coupling to Android views and animators.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.chatinput`:
  1. Implement `ChatInputRemoteDataSource`:
     - Dispatches remote typing indicators and saves drafts.
  2. Implement `ChatInputLocalDataSource`:
     - Manages thread-safe `ChatInputState` flows, text edits, selection spans, panel mode switches, reply/edit quotes, send button arbitration (`CalculateSendButtonStateUseCase`), voice/video recording transitions, and view-once toggles.
  3. Implement `ChatInputRepositoryImpl`:
     - Implements `ChatInputRepository`, delegating reactive state and mutation operations to local and remote data sources.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `ChatInputRepositoryImpl`.
  5. Introduce strangler boundary: `ChatActivityEnterView.getChatInputRepository(account)` and `getChatInputRepository()` accessors.
- **Consequences:** Input bar arbitration, recording lifecycle, and virtual panel routing are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `ChatInputRepositoryImplTest.kt`.

### ADR 208: Chat Attachment Dialog, Multi-Selection Ordering & Send Options Strangling via Clean DataSources & ChatAttachRepositoryImpl
- **Context:** Attachment sheet presentation, tab switching (photos, documents, audio, contacts, location, polls), multi-selection indexing with numbered order badges, caption character limits, and spoilered/file send options were managed inside `ChatAttachAlert.java` (~7280 lines).
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.chatattach`:
  1. Implement `ChatAttachRemoteDataSource`:
     - Resolves remote attachment limits and server-side layout capabilities.
  2. Implement `ChatAttachLocalDataSource`:
     - Manages reactive `ChatAttachState`, alert visibility, current/available layouts, multi-selection ordering, permission flags, and send options (`ChatAttachSendOptions`).
  3. Implement `ChatAttachRepositoryImpl`:
     - Implements `ChatAttachRepository`, coordinating attachment alert lifecycle, layout selection, item toggling, and send configuration.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `ChatAttachRepositoryImpl`.
  5. Introduce strangler boundary: `ChatAttachAlert.getChatAttachRepository(account)` and `getChatAttachRepository()` accessors.
- **Consequences:** All attachment dialog presentation, layout selection, and multi-item ordering logic are decoupled behind clean domain contracts. 100% unit test coverage achieved with `ChatAttachRepositoryImplTest.kt`.

### ADR 207: Chat Bottom Views Visibility & Priority Mutual Exclusion Strangling via Clean DataSources & BottomViewsVisibilityRepositoryImpl
- **Context:** Visibility transitions and mutual exclusion among bottom chat views (message input, recording panel, media attachments, search bar, actions/selection bar, join channel bar, bot overlays) were coordinated by `ChatActivityBottomViewsVisibilityController.java` using 32-bit bitwise container flags and float array weights.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.bottomviews`:
  1. Implement `BottomViewsRemoteDataSource`:
     - Retrieves remote bottom overlay configurations.
  2. Implement `BottomViewsLocalDataSource`:
     - Manages thread-safe bitwise container flags (`1 shl containerId`), priority calculations via `BottomViewsVisibilityMapper`, and reactive `BottomViewsVisibilityState` flows.
  3. Implement `BottomViewsVisibilityRepositoryImpl`:
     - Implements `BottomViewsVisibilityRepository`, delegating container visibility queries, mutations, and priority resolutions.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `BottomViewsVisibilityRepositoryImpl`.
  5. Introduce strangler boundary: `ChatActivity.getBottomViewsVisibilityRepository(account)` and `getBottomViewsVisibilityRepository()` accessors.
- **Consequences:** Bottom bar mutual exclusion and animation priorities are decoupled behind clean domain contracts. 100% unit test coverage achieved with `BottomViewsVisibilityRepositoryImplTest.kt`.

### ADR 206: Auto-Delete & Self-Destruct Message History Timers Strangling via Clean DataSources & AutoDeleteRepositoryImpl
- **Context:** Global account-level default TTL for new chats and per-dialog TTL periods were coordinated inside `AutoDeleteMessagesActivity.java` (~337 lines) and `MessagesController.java`, mixing MTProto requests (`TL_messages_setDefaultHistoryTTL`, `TL_messages_setHistoryTTL`) directly with UI logic and `UserConfig`.
- **Decision:** Apply the Strangler Fig pattern to `feature.messaging.autodelete`:
  1. Implement `AutoDeleteRemoteDataSource`:
     - Executes MTProto RPC requests for setting global and per-chat history TTL.
  2. Implement `AutoDeleteLocalDataSource`:
     - Manages reactive `GlobalAutoDeleteStateModel` StateFlow and chat TTL lookups.
  3. Implement `AutoDeleteRepositoryImpl`:
     - Implements `AutoDeleteRepository`, coordinating global TTL and chat TTL settings and batch operations.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `AutoDeleteRepositoryImpl`.
  5. Introduce strangler boundary: `MessagesController.getAutoDeleteRepository(account)` and `getAutoDeleteRepository()` accessors.
- **Consequences:** Auto-delete timers and history TTL management are isolated behind clean domain contracts and use cases. 100% unit test coverage achieved with `AutoDeleteRepositoryImplTest.kt`.

### ADR 205: VoIP Call Lifecycle, Audio/Video Routing & Hardware Strangling via Clean DataSources & VoIPRepositoryImpl
- **Context:** Voice and video call operations, WebRTC signaling, audio hardware routing (Bluetooth SCO, earpiece, speaker), and call quality telemetry were centralized in `VoIPService.java` (~4300 lines) with tight coupling to Android services, broadcast receivers, and static singletons.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.voip`:
  1. Implement `VoIPRemoteDataSource`:
     - Encapsulates remote call signaling, group call participant sync, and server relay updates.
  2. Implement `VoIPLocalDataSource`:
     - Maintains reactive call state (`IDLE`, `CONNECTING`, `EXCHANGING_KEYS`, `RINGING`, `ACTIVE`, `ENDED`), audio route tracking (EARPIECE, SPEAKER, BLUETOOTH), mic mute, camera toggle, and call duration.
  3. Implement `VoIPRepositoryImpl`:
     - Implements `VoIPRepository`, coordinating call lifecycle transitions, audio output switching, mute controls, and reactive observation.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `VoIPRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `VoIPService.getVoIPRepository(account)` and `getVoIPRepository()` accessors.
- **Consequences:** VoIP state management and hardware controls are decoupled behind clean domain contracts and use cases. 100% unit test coverage achieved with `VoIPRepositoryImplTest.kt`.

### ADR 204: Stories State, Pagination, Upload Lifecycle & Content Strangling via Clean DataSources & StoriesRepositoryImpl
- **Context:** Stories retrieval, pagination, upload pipeline, view counts, and reaction dispatching were orchestrated inside `StoriesController.java` (~6300 lines) using complex internal data structures and tight coupling to UI fragments.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.stories`:
  1. Implement `StoriesRemoteDataSource`:
     - Provides MTProto RPC calls for fetching peer stories, uploading story items, deleting stories, and reacting to stories.
  2. Implement `StoriesLocalDataSource`:
     - Manages thread-safe story cache by peer ID, upload draft queues, active viewer index, and reactive story list flows.
  3. Implement `StoriesRepositoryImpl`:
     - Implements `StoriesRepository`, coordinating stories loading, upload state, reactions, and reactive observation.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `StoriesRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `StoriesController.getStoriesRepository(account)` and `getStoriesRepository()` accessors.
- **Consequences:** Story data management and reactions are cleanly separated behind domain contracts. 100% unit test coverage achieved with `StoriesRepositoryImplTest.kt`.

### ADR 203: Shared Media Layout Tabs, Fast Scrolling & Type Filtering Strangling via Clean DataSources & SharedMediaRepositoryImpl
- **Context:** Shared media browsing across chats and channels (media, files, audio, links, voice notes, GIFs) was coupled inside `SharedMediaLayout.java` (~5400 lines) with direct SQLite queries and manual adapter state updates.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.sharedmedia`:
  1. Implement `SharedMediaRemoteDataSource`:
     - Provides remote shared media querying by type and pagination cursors.
  2. Implement `SharedMediaLocalDataSource`:
     - Manages active tab state (`PHOTO_VIDEO`, `FILE`, `AUDIO`, `LINK`, `VOICE`, `GIF`), search filter queries, fast-scroller calendar index, selection mode, and reactive state flows.
  3. Implement `SharedMediaRepositoryImpl`:
     - Implements `SharedMediaRepository`, coordinating tab switches, item selection, type filters, and search query updates.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `SharedMediaRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `SharedMediaLayout.getSharedMediaRepository(account)` and `getSharedMediaRepository()` accessors.
- **Consequences:** Shared media navigation and filtering logic are decoupled behind clean domain contracts. 100% unit test coverage achieved with `SharedMediaRepositoryImplTest.kt`.

### ADR 202: Photo Viewer Overlay, Image Transformations, Gestures & Paging Strangling via Clean DataSources & PhotoViewerRepositoryImpl
- **Context:** Fullscreen photo and video viewing, pinch-to-zoom gestures, panning, caption editing, picture-in-picture transitions, and paging were concentrated in `PhotoViewer.java` (~10400 lines) as a monolithic singleton view overlay.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.photoviewer`:
  1. Implement `PhotoViewerRemoteDataSource`:
     - Encapsulates remote photo/video metadata loading, caption translation, and streaming manifest fetching.
  2. Implement `PhotoViewerLocalDataSource`:
     - Maintains active viewer state, current item index, zoom factor, translation offsets, PIP mode, and reactive state flows.
  3. Implement `PhotoViewerRepositoryImpl`:
     - Implements `PhotoViewerRepository`, coordinating open/close lifecycle, index navigation, zoom transformations, PIP transitions, and reactive state observation.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `PhotoViewerRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `PhotoViewer.getPhotoViewerRepository(account)` and `getPhotoViewerRepository()` accessors.
- **Consequences:** Photo viewer presentation state, navigation, and geometric transformations are isolated behind clean domain contracts. 100% unit test coverage achieved with `PhotoViewerRepositoryImplTest.kt`.

### ADR 201: Media Data Controller, Album Indexing & Gallery Retrieval Strangling via Clean DataSources & MediaDataRepositoryImpl
- **Context:** Media albums, photos, videos, and system gallery indexing were governed by `MediaDataController.java` and `MediaController.java` using raw `AlbumEntry` and `PhotoEntry` objects. Directly querying these collections from UI fragments coupled views with low-level storage routines.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.mediadata`:
  1. Implement `MediaDataRemoteDataSource`:
     - Provides remote media synchronization and cloud media metadata queries.
  2. Implement `MediaDataLocalDataSource`:
     - Maintains thread-safe album caches, maps system gallery entries to pure Kotlin domain models (`MediaAlbumModel`, `MediaItemModel`), and provides reactive state flows.
  3. Implement `MediaDataRepositoryImpl`:
     - Implements `MediaRepository`, coordinating album retrieval, album media querying, and overall gallery access.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `MediaDataRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `MediaDataController.getMediaDataRepository(account)` and `getMediaDataRepository()` accessors.
- **Consequences:** Gallery querying and album management are isolated behind clean domain contracts and use cases. 100% unit test coverage achieved with `MediaDataRepositoryImplTest.kt`.

### ADR 200: Image Loader Cache Tiers, Memory Trimming & Request Pipeline Strangling via Clean DataSources & ImageLoaderRepositoryImpl
- **Context:** In-memory and disk image caching, downscaling, and request decoding were orchestrated inside `ImageLoader.java` (~4650 lines) with complex multi-tier LruCache instances (`memCache`, `smallImagesMemCache`, `wallpaperMemCache`, `lottieMemCache`). Direct singleton access coupled UI components to internal cache implementation details.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.imageloader`:
  1. Implement `ImageLoaderRemoteDataSource`:
     - Provides remote image fetching and CDN request delegation.
  2. Implement `ImageLoaderLocalDataSource`:
     - Manages multi-tier cache simulation (`DEFAULT`, `SMALL`, `WALLPAPER`, `LOTTIE`), cache hit/miss statistics, memory pressure trimming, and reactive `ImageLoaderState` flow.
  3. Implement `ImageLoaderRepositoryImpl`:
     - Implements `ImageLoaderRepository`, coordinating request enqueueing, cancellation, cache queries, and statistics tracking.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `ImageLoaderRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `ImageLoader.getImageLoaderRepository(account)` and `getImageLoaderRepository()` accessors.
- **Consequences:** Image caching, request state, and cache diagnostics are fully decoupled behind clean domain contracts. 100% unit test coverage achieved with `ImageLoaderRepositoryImplTest.kt`.

### ADR 199: File Loader Transfers, Upload/Download Queuing & Path Resolution Strangling via Clean DataSources & FileLoaderRepositoryImpl
- **Context:** File downloads, uploads, and path resolution were historically coupled to `FileLoader.java` (~1967 lines) and `DownloadController.java` with static arrays and raw NotificationCenter calls. Direct UI access risked race conditions and prevented unit testing.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.fileloader`:
  1. Implement `FileLoaderRemoteDataSource`:
     - Provides MTProto RPC file upload, download, and cancellation operations.
  2. Implement `FileLoaderLocalDataSource`:
     - Manages thread-safe `FileTransferModel` map, reactive transfers flow, active/recent download queries, and local file path resolution.
  3. Implement `FileLoaderRepositoryImpl`:
     - Implements `FileLoaderRepository`, coordinating file transfer lifecycle, upload/download requests, and path queries.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `FileLoaderRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `FileLoader.getFileLoaderRepository(account)` and `getFileLoaderRepository()` accessors.
- **Consequences:** File transfer operations, download queues, and local path resolutions are cleanly separated behind domain use cases. 100% unit test coverage achieved with `FileLoaderRepositoryImplTest.kt`.

### ADR 198: Content Preview Overlay, Gesture Drag Progress & Contextual Action Menus Strangling via Clean DataSources & ContentPreviewRepositoryImpl
- **Context:** Sticker, emoji, and GIF preview popups with interactive drag-to-menu gestures and contextual actions were historically coordinated in `ContentPreviewViewer.java` (~2423 lines). State, view animations, and action triggers were tangled with static singleton access.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.contentpreview`:
  1. Implement `ContentPreviewRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing preview policy configuration extension points.
  2. Implement `ContentPreviewLocalDataSource`:
     - Manages thread-safe `ContentPreviewState` flow, item selection, drag progress interpolation, and contextual action resolution.
  3. Implement `ContentPreviewRepositoryImpl`:
     - Implements `ContentPreviewRepository`, coordinating preview opening, drag updates, menu visibility, and dismissal.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `ContentPreviewRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `ContentPreviewViewer.getContentPreviewRepository(account)` and `getContentPreviewRepository()` accessors.
- **Consequences:** Content preview state machine and drag gesture calculations are decoupled behind clean domain contracts. 100% unit test coverage achieved with `ContentPreviewRepositoryImplTest.kt`.

### ADR 197: Auto-Delete Media Policies, Retention Rules & File Lock Registry Strangling via Clean DataSources & AutoDeleteMediaRepositoryImpl
- **Context:** Automated background media cache eviction and retention limits were coordinated in `AutoDeleteMediaTask.java` and `CacheByChatsController.java`, checking file age against keep-media settings and verifying file locking sets. Calling static methods from various components coupled cache cleaning with singleton state.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.autodeletemedia`:
  1. Implement `AutoDeleteMediaRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing retention policy synchronization extension points.
  2. Implement `AutoDeleteMediaLocalDataSource`:
     - Manages thread-safe file locks set (`ConcurrentHashMap`), cache eviction candidate scanning, and cleanup passes executed safely on `Dispatchers.IO`.
  3. Implement `AutoDeleteMediaRepositoryImpl`:
     - Implements `AutoDeleteMediaRepository`, coordinating cleanup execution, locked files management, and state observation.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `AutoDeleteMediaRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `AutoDeleteMediaTask.getAutoDeleteMediaRepository(account)` and `getAutoDeleteMediaRepository()` accessors.
- **Consequences:** Media auto-delete passes and file locking arbitration are decoupled behind clean domain contracts. 100% unit test coverage achieved with `AutoDeleteMediaRepositoryImplTest.kt`.

### ADR 196: Audio Player, Equalizer & Output Route Strangling via Clean DataSources & AudioPlayerRepositoryImpl
- **Context:** Audio and music playback in Telegram Android is managed by `MediaController.java` (~7000 lines), controlling ExoPlayer instances, playlist navigation, equalizer bands, bass boost, proximity sensor routing, and playback speeds. UI components directly manipulated static singleton methods, hindering modularization and testing.
- **Decision:** Apply the Strangler Fig pattern to `feature.media.audioplayer`:
  1. Implement `AudioPlayerRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing cloud streaming policies and playback telemetry.
  2. Implement `AudioPlayerLocalDataSource`:
     - Manages reactive `AudioPlaybackState` flow, playlist queues, shuffle order generation, repeat mode transitions, equalizer bands, bass boost, and audio route selection.
  3. Implement `AudioPlayerRepositoryImpl`:
     - Implements `AudioPlayerRepository`, coordinating playback state, queue navigation, equalizer adjustments, and remote event reporting.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `AudioPlayerRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `MediaController.getAudioPlayerRepository(account)` and `getAudioPlayerRepository()` accessors.
- **Consequences:** Audio playback state management, equalizer controls, and playlist navigation are decoupled behind clean domain contracts. 100% unit test coverage achieved with `AudioPlayerRepositoryImplTest.kt`.

### ADR 195: Recycler List Scroll Animations, Translation Geometry & View Arbitration Strangling via Clean DataSources & RecyclerScrollRepositoryImpl
- **Context:** Smooth scrolling and transition animations for chat and dialog lists involved complex calculations of visible view ranges, scroll diffs, container heights, and directional view translations. In the legacy architecture, these calculations were embedded directly in UI controllers and helper classes without isolated testability or clean domain separation.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.recyclerscroll`:
  1. Implement `RecyclerScrollRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing scroll physics calibration extension points.
  2. Implement `RecyclerScrollLocalDataSource`:
     - Encapsulates eligibility evaluation (fast scroll, item animator running, child counts), plan calculation, scroll length determination, and view translation math via `RecyclerScrollMapper`.
  3. Implement `RecyclerScrollRepositoryImpl`:
     - Implements `RecyclerScrollRepository`, coordinating scroll start, progress interpolation, cancellation, and geometric view translations.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `RecyclerScrollRepositoryImpl` alongside clean data sources.
- **Consequences:** Recycler list scroll mathematics and state machine are isolated behind clean domain contracts. 100% unit test coverage achieved with `RecyclerScrollRepositoryImplTest.kt`.

### ADR 194: Interactive Pinch-To-Zoom, Multi-Touch Geometry & View Transformation Strangling via Clean DataSources & PinchToZoomRepositoryImpl
- **Context:** Pinch-to-zoom gestures across media viewers, message media, and avatars required multi-touch distance calculations, focal point translations, image bounds scaling, and overlay transitions. These calculations were tightly coupled with custom view implementations and touch listeners.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.pinchtozoom`:
  1. Implement `PinchToZoomRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing gesture telemetry and zoom calibration extension points.
  2. Implement `PinchToZoomLocalDataSource`:
     - Encapsulates scale computation, translation offsets, transform calculations, image boundary constraints, and gesture decision evaluation via `PinchToZoomMapper`.
  3. Implement `PinchToZoomRepositoryImpl`:
     - Implements `PinchToZoomRepository`, coordinating zoom lifecycle (`startZoom`, `updateZoom`, `finishZoom`, `reset`) and transformation calculations.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `PinchToZoomRepositoryImpl` alongside clean data sources.
- **Consequences:** Pinch-to-zoom mathematics and gesture arbitration are isolated behind clean domain contracts. 100% unit test coverage achieved with `PinchToZoomRepositoryImplTest.kt`.

### ADR 193: Window Insets, In-App Keyboard Heights & IME Arbitration Strangling via Clean DataSources & KeyboardInsetsRepositoryImpl
- **Context:** Modern Android edge-to-edge rendering requires coordinating system bar insets, navigation bar offsets, IME bottom insets, and custom in-app keyboard heights (stickers, emojis, bots). In Telegram Android, this coordination was managed by `WindowInsetsInAppController` without clean repository separation or isolated JVM tests.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.keyboardinsets`:
  1. Implement `KeyboardInsetsRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing window insets policy synchronization extension points.
  2. Implement `KeyboardInsetsLocalDataSource`:
     - Encapsulates in-app keyboard height tracking, navigation bar height inclusion, IME visibility state transitions, and `KeyboardInsetsModel` emission via `KeyboardInsetsMapper`.
  3. Implement `KeyboardInsetsRepositoryImpl`:
     - Implements `KeyboardInsetsRepository`, coordinating height requests, reset actions, navbar inclusion, system insets updates, and reactive flow observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `KeyboardInsetsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `WindowInsetsInAppController.getKeyboardInsetsRepository(account)` and `getKeyboardInsetsRepository()` accessors.
- **Consequences:** Window insets and in-app keyboard heights are decoupled behind clean domain contracts. 100% unit test coverage achieved with `KeyboardInsetsRepositoryImplTest.kt` and full upstream compatibility preserved.

### ADR 192: Interactive Keyboard Pull-Down Dismissal & Scroll Arbitration Strangling via Clean DataSources & KeyboardHideRepositoryImpl
- **Context:** Telegram features an interactive drag gesture allowing users to pull down the chat list to dismiss the soft keyboard fluidly. This gesture tracking, touch velocity measurement, and dismiss-or-settle arbitration was implemented inside `KeyboardHideHelper.java` (~193 lines) with direct static state `KeyboardHideHelper.ENABLED`.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.keyboardhide`:
  1. Implement `KeyboardHideRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing gesture telemetry extension points.
  2. Implement `KeyboardHideLocalDataSource`:
     - Encapsulates drag touch coordinate geometry, progress interpolation, velocity-sensitive dismiss decisions, and headless test mode.
  3. Implement `KeyboardHideRepositoryImpl`:
     - Implements `KeyboardHideRepository`, coordinating drag lifecycle (`startMoving`, `updateMoving`, `endMoving`, `finishDismiss`, `reset`) and state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `KeyboardHideRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `KeyboardHideHelper.getKeyboardHideRepository(account)` and `getKeyboardHideRepository()` accessors.
- **Consequences:** Interactive keyboard dismissal logic is isolated behind clean domain contracts. 100% unit test coverage achieved with `KeyboardHideRepositoryImplTest.kt` and full backward compatibility preserved.

### ADR 191: Window Adjust-Pan Layout Arbitration & Keyboard Transition Strangling via Clean DataSources & AdjustPanRepositoryImpl
- **Context:** Android window mode `adjustPan` versus `adjustResize` transitions cause complex layout height resizes and translation animations during keyboard appearance/dismissal. In Telegram Android, `AdjustPanLayoutHelper` computed these transitions, but layout math and state tracking were mixed with view hierarchies.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.adjustpan`:
  1. Implement `AdjustPanRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing layout configuration extension points.
  2. Implement `AdjustPanLocalDataSource`:
     - Encapsulates transition plan calculation (`PanCalculationSpec`), progress interpolation, height adjustment animations, and headless test mode via `AdjustPanMapper`.
  3. Implement `AdjustPanRepositoryImpl`:
     - Implements `AdjustPanRepository`, coordinating plan calculation, transition lifecycle (`startTransition`, `updateTransition`, `stopTransition`, `reset`), and state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `AdjustPanRepositoryImpl` alongside clean data sources.
- **Consequences:** AdjustPan geometry calculation and transition state tracking are isolated behind clean domain contracts. 100% unit test coverage achieved with `AdjustPanRepositoryImplTest.kt`.

### ADR 190: Navigation Tabs Configuration, Call Tab Toggle & Unread Counters Strangling via Clean DataSources & MainTabsRepositoryImpl
- **Context:** In Telegram Android, main screen navigation tabs (Chats, Contacts, Calls/Settings, Profile) and their visibility, unread count badges, and conditional Calls tab presentation were coordinated between `MainTabsActivity.java`, `MainTabsActivityController.java`, and `UserConfig.showCallsTab`.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.maintabs`:
  1. Implement `MainTabsRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote tabs configuration extension points.
  2. Implement `MainTabsLocalDataSource`:
     - Encapsulates tab visibility, tab selection (`MainTabType`), `UserConfig.showCallsTab` toggling, chats unread count tracking, contacts permission warnings, and headless test mode.
  3. Implement `MainTabsRepositoryImpl`:
     - Implements `MainTabsRepository`, coordinating tab visibility, position selection, call tab toggling, and unread counters.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `MainTabsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `MainTabsActivityController.getMainTabsRepository(account)` and `MainTabsActivity.getMainTabsRepository(account)` accessors.
- **Consequences:** Main screen navigation tab configuration is decoupled behind clean domain contracts. 100% unit test coverage achieved with `MainTabsRepositoryImplTest.kt` and full backward compatibility preserved.

### ADR 189: Application Localization, Dynamic Language Packs & String Resources Strangling via Clean DataSources & LocalizationRepositoryImpl
- **Context:** Telegram Android localization, plural rules, custom language pack overrides, RTL language detection, name display ordering, and 24-hour time formatting were centered around `LocaleController.java` (~4517 lines). UI components invoked static methods on `LocaleController`, preventing clean inversion of control and headless JVM testing.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.localization`:
  1. Implement `LocalizationRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote language pack downloading and remote locale list retrieval.
  2. Implement `LocalizationLocalDataSource`:
     - Encapsulates current locale, available locales, custom string overrides, 24-hour format flag, and name display order.
  3. Implement `LocalizationRepositoryImpl`:
     - Implements `LocalizationRepository`, coordinating locale selection, string resource resolution with fallbacks, time format preferences, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `LocalizationRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `LocaleController.getLocalizationRepository(account)` and `getLocalizationRepository()` accessors.
- **Consequences:** Localization, string resolution, and locale configuration are decoupled behind clean domain contracts. 100% unit test coverage achieved with `LocalizationRepositoryImplTest.kt` and full backward compatibility preserved.

### ADR 188: Application Theming, Night Mode & Color Accents Strangling via Clean DataSources & ThemeRepositoryImpl
- **Context:** Telegram Android UI theming was deeply entrenched in `Theme.java` (~8000 lines), managing current themes, night mode switching (system, scheduled, adaptive), custom theme accents, bubble radius, wallpapers, and colors. Direct calls to `Theme.java` from everywhere coupled the entire presentation layer with legacy theming statics.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.themes`:
  1. Implement `ThemesRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote theme synchronization and cloud wallpaper fetching.
  2. Implement `ThemesLocalDataSource`:
     - Encapsulates theme selection, night mode configuration (`NightModeType`), theme accents, bubble radius, wallpaper models, and headless JVM test mode.
  3. Implement `ThemeRepositoryImpl`:
     - Implements `ThemeRepository`, coordinating appearance settings, available themes, theme application, night mode settings, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `ThemeRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `Theme.getThemeRepository(account)` and `getThemeRepository()` accessors.
- **Consequences:** Theming, night mode scheduling, and appearance preferences are decoupled behind clean domain contracts. 100% unit test coverage achieved with `ThemeRepositoryImplTest.kt` and full backward compatibility preserved.

### ADR 187: Client Settings, Appearance Preferences & Account Configuration Strangling via Clean DataSources & SettingsRepositoryImpl
- **Context:** In Telegram Android, client settings and configuration were distributed across mutable static singletons: `SharedConfig` (global app-wide preferences like font size, bubble radius, stream media, in-app camera) and `UserConfig` (account-scoped preferences like contact syncing, call tab visibility). Direct access from UI components scattered configuration logic and caused potential race conditions during persistence without isolated JVM testability.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.settings`:
  1. Implement `SettingsRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote MTProto global privacy and setting synchronization extension points.
  2. Implement `SettingsLocalDataSource`:
     - Encapsulates thread-safe aggregated `SettingsModel`, main-thread mutation dispatching, config persistence, and headless JVM test mode.
  3. Implement `SettingsRepositoryImpl`:
     - Implements `SettingsRepository`, coordinating client preferences, appearance updates, media streaming switches, contact sync rules, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `SettingsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `SharedConfig.getSettingsRepository(account)` and `getSettingsRepository()` accessors.
- **Consequences:** Client settings and appearance preferences are decoupled behind clean domain contracts. 100% unit test coverage achieved with `SettingsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 186: Notification Sounds, Cloud Ringtones & Tone Conversion Strangling via Clean DataSources & RingtoneRepositoryImpl
- **Context:** Telegram supports uploading and applying custom notification sound effects (short audio documents under 5 seconds and 300 KB). Managing uploaded tones, audio duration validation, document conversions, and sound path resolution was split between `MediaDataController` (`RingtoneDataStore`, `RingtoneUploader`), `NotificationsController`, and UI fragments without unified domain contracts or clean data source abstractions.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.ringtones`:
  1. Implement `RingtoneRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote MTProto saved ringtone fetching and document saving/unsaving.
  2. Implement `RingtoneLocalDataSource`:
     - Encapsulates thread-safe in-memory ringtone registry, eligibility limits (duration <= 5s, size <= 300 KB, supported MIME types), upload tracking with cancellation, tone selection, and headless JVM test mode.
  3. Implement `RingtoneRepositoryImpl`:
     - Implements `RingtoneRepository`, coordinating tone retrieval, validation, upload flow, document persistence, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `RingtoneRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `NotificationsController.getRingtoneRepository(account)` and `getRingtoneRepository()` accessors.
- **Consequences:** Custom notification sound effects and upload state machines are decoupled behind clean domain contracts. 100% unit test coverage achieved with `RingtoneRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 185: Data & Storage Management, Cache Clearing & Auto-Download Presets Strangling via Clean DataSources & DataStorageRepositoryImpl
- **Context:** In Telegram Android, cache calculation, directory cleaning, local SQLite maintenance, network stats, and auto-download presets were scattered across `FileLoader`, `DownloadController`, `CacheByChatsController`, `MessagesStorage`, and `StatsController`. Direct invocation from UI components caused heavy background thread orchestration in Activities and made storage operations hard to test in isolation.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.datastorage`:
  1. Implement `DataStorageRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote cloud storage policy and auto-download profile extension points.
  2. Implement `DataStorageLocalDataSource`:
     - Encapsulates directory size calculations, media cache clearing, database compaction, network usage counters, auto-download preset configuration, keep-media rules, and headless JVM test mode.
  3. Implement `DataStorageRepositoryImpl`:
     - Implements `DataStorageRepository`, coordinating cache clearing, database compaction, network stats reset, preset updates, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `DataStorageRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `StatsController.getDataStorageRepository(account)` and `getDataStorageRepository()` accessors.
- **Consequences:** Storage management, cache cleanup, and network consumption monitoring are decoupled behind clean domain contracts. 100% unit test coverage achieved with `DataStorageRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 184: Precision Countdown Timer, Lifecycle State Machine & Tick Arbitration Strangling via Clean DataSources & CountdownTimerRepositoryImpl
- **Context:** Managing asynchronous countdown timers for verification codes, temporary invites, self-destructing media, and auction lots was fragmented across UI fragments using raw handlers and arbitrary postDelayed loops without unified state machines, pause/resume semantics, or testable abstractions.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.countdowntimer`:
  1. Implement `CountdownTimerRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote NTP time sync and countdown calibration extension points.
  2. Implement `CountdownTimerLocalDataSource`:
     - Encapsulates `ConcurrentHashMap<String, CountdownTimerTick>` registry, coroutine ticking jobs, lifecycle transitions (`RUNNING`, `PAUSED`, `IDLE`, `FINISHED`), decomposed components (days, hours, minutes, seconds), `StateFlow<CountdownTimerState>`, and `SharedFlow<CountdownTimerTick>`.
  3. Implement `CountdownTimerRepositoryImpl`:
     - Implements `CountdownTimerRepository`, coordinating timer start, stop, pause, resume, manual deterministic ticks, clear all, and reactive stream observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `CountdownTimerRepositoryImpl` alongside clean data sources.
- **Consequences:** Countdown timer arbitration and lifecycle state machines are decoupled behind clean domain contracts. 100% unit test coverage achieved with `CountdownTimerRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 183: In-App Browser, Custom Tabs & URL Routing Strangling via Clean DataSources & BrowserRepositoryImpl
- **Context:** In Telegram Android, URL dispatching, in-app web views, Chrome Custom Tabs, external browser routing, and URL safety checks were governed by `Browser.java` (~870 lines). The legacy class directly interacted with Android `Intent`, `ApplicationLoader.applicationContext`, and internal web controllers without decoupled data sources or isolated JVM testability.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.browser`:
  1. Implement `BrowserRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote safe browsing policies and malicious domain telemetry extension points.
  2. Implement `BrowserLocalDataSource`:
     - Encapsulates browser type preference (`IN_APP`, `CUSTOM_TABS`, `EXTERNAL_BROWSER`), URL safety classification, thread-safe history registry, cache clearing, and headless JVM test mode.
  3. Implement `BrowserRepositoryImpl`:
     - Implements `BrowserRepository`, coordinating browser settings, URL safety validation, link opening, history management, and cache clearing.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `BrowserRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `Browser.getBrowserRepository(account)` and `getBrowserRepository()` accessors.
- **Consequences:** Browser routing and URL safety verification are decoupled behind clean domain contracts. 100% unit test coverage achieved with `BrowserRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 182: Global App Configuration, Dynamic Limits & Feature Flags Strangling via Clean DataSources & AppConfigRepositoryImpl
- **Context:** Telegram's global application limits, dynamic star pricing, ton conversion rates, polls limits, rich message formatting boundaries, and feature flags synced from the backend were governed by `AppGlobalConfig.java` (~411 lines). Access was tied directly to `MessagesController.getInstance(account).config` and global static constants without a decoupled data source or isolated JVM testing.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.appconfig`:
  1. Implement `AppConfigRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote MTProto configuration retrieval extension points.
  2. Implement `AppConfigLocalDataSource`:
     - Encapsulates in-memory cached `AppGlobalConfigState`, `NotificationCenter.appConfigUpdated` observation, custom key-value entries mutation, and headless JVM test mode.
  3. Implement `AppConfigRepositoryImpl`:
     - Implements `AppConfigRepository`, coordinating state retrieval, reload, custom value mutations, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `AppConfigRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `AppGlobalConfig.getAppConfigRepository(account)`, `getAppConfigRepository()`, and `getRepository()` accessors.
- **Consequences:** Global application configuration and dynamic limits are decoupled behind clean domain contracts. 100% unit test coverage achieved with `AppConfigRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 181: Floating Debug Overlay & Diagnostics Controller Strangling via Clean DataSources & FloatingDebugRepositoryImpl
- **Context:** In Telegram Android, the floating debug overlay controller was governed by `FloatingDebugController.java` (~120 lines). The controller directly managed a floating action button view, notification center event listeners (`floatingDebugActiveStateChanged`), launch activity context bindings, and in-memory boolean active state without a testable data source abstraction or isolated JVM testing.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.floatingdebug`:
  1. Implement `FloatingDebugRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote debug configuration and overlay policies.
  2. Implement `FloatingDebugLocalDataSource`:
     - Encapsulates debug overlay active state, `StateFlow<Boolean>`, `SharedFlow<FloatingDebugEvent>`, notification center event dispatching, `LaunchActivity` provider, and headless JVM test mode.
  3. Implement `FloatingDebugRepositoryImpl`:
     - Implements `FloatingDebugRepository`, coordinating overlay state query, show, dismiss, fab visibility, and reactive observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `FloatingDebugRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `FloatingDebugController.getFloatingDebugRepository(account)`, `getFloatingDebugRepository()`, and `getRepository()` accessors.
- **Consequences:** Floating debug overlay controller and diagnostics state are decoupled behind clean domain contracts. 100% unit test coverage achieved with `FloatingDebugRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 180: Choreographer 60 FPS Frame Rate & V-Sync Content Arbitration Strangling via Clean DataSources & FpsContentRepositoryImpl
- **Context:** V-Sync synchronization and 60 FPS frame callback dispatching was governed by `Choreographer60FpsContent.java` (~156 lines). The controller managed Android `Choreographer.FrameCallback`, reflection access to private choreographer fields, frame stride calculations, runnable queues, and direct view/drawable invalidations without decoupled data sources or testable mathematical abstractions.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.fpscontent`:
  1. Implement `FpsContentRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote frame rate policies and target FPS configuration.
  2. Implement `FpsContentLocalDataSource`:
     - Encapsulates pure mathematical frame rate arbitration, stride groups, accumulator groups, view and drawable invalidation queues, subscription registry, `StateFlow<FpsContentStats>`, and `SharedFlow<FrameTick>`.
  3. Implement `FpsContentRepositoryImpl`:
     - Implements `FpsContentRepository`, coordinating frame callback registration, runnable callbacks, invalidation requests, vsync ticks dispatching, stats, and subscriptions.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `FpsContentRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `Choreographer60FpsContent.getFpsContentRepository(account)`, `getFpsContentRepository()`, and `getRepository()` accessors.
- **Consequences:** Frame rate arbitration and vsync dispatching are decoupled behind clean domain contracts. 100% unit test coverage achieved with `FpsContentRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 179: Memory Leak Detection, Reference Tracking & Two-Phase GC Recheck Strangling via Clean DataSources & LeakDetectorRepositoryImpl
- **Context:** Detecting memory leaks across activities, fragments, dialogs, and large resources was governed by `LeakDetector.java` (~120 lines). The detector used static weak references, arbitrary GC triggering, sleep loops, and direct logging without a decoupled data source abstraction or isolated headless testability.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.leakdetector`:
  1. Implement `LeakDetectorRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote leak incident telemetry and crash diagnostics.
  2. Implement `LeakDetectorLocalDataSource`:
     - Encapsulates `ConcurrentHashMap` weak reference registries, two-phase GC confirmation heuristics, live instance counting, `StateFlow<LeakDetectorState>`, and `SharedFlow<LeakReport>`.
  3. Implement `LeakDetectorRepositoryImpl`:
     - Implements `LeakDetectorRepository`, coordinating monitoring start/stop, instance tracking, manual rechecks, leak confirmation, class stats, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `LeakDetectorRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `LeakDetector.getLeakDetectorRepository(account)`, `getLeakDetectorRepository()`, and `getRepository()` accessors.
- **Consequences:** Memory leak tracking and diagnostics are decoupled behind clean domain contracts. 100% unit test coverage achieved with `LeakDetectorRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 178: Hardware Fingerprinting, Emulator Detection & Environment Diagnostics Strangling via Clean DataSources & EmuDetectorRepositoryImpl
- **Context:** Detection of virtualized execution environments, Android emulators, and instrumentation frameworks was governed by `EmuDetector.java` (~434 lines) and `EmuInputDevicesDetector.java`. The detector relied directly on Android system properties reflection, telephony manager, filesystem checks for qemu drivers, and package manager lookups without a decoupled data source abstraction or isolated JVM testing.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.emudetector`:
  1. Implement `EmuDetectorRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote heuristic rules and package blacklist updates.
  2. Implement `EmuDetectorLocalDataSource`:
     - Encapsulates hardware heuristics, build property checks, companion packages, diagnostics caching, `StateFlow<EmulatorDiagnostics?>`, `StateFlow<Boolean>`, and headless JVM test mode.
  3. Implement `EmuDetectorRepositoryImpl`:
     - Implements `EmuDetectorRepository`, coordinating environment detection, caching, force refresh, custom packages, configuration updates, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `EmuDetectorRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `EmuDetector.getEmuDetectorRepository(account)` and `getEmuDetectorRepository()` accessors.
- **Consequences:** Emulator detection and virtualized environment diagnostics are decoupled behind clean domain contracts. 100% unit test coverage achieved with `EmuDetectorRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 177: Main Thread Heartbeat Monitoring, UI Freeze & ANR Detection Strangling via Clean DataSources & AnrWatchdogRepositoryImpl
- **Context:** Main UI thread responsiveness monitoring and application freezing (ANR) detection was governed by `ANRDetector.java` (~224 lines). The detector coupled a dedicated watchdog thread, Android `Handler(Looper.getMainLooper())` ping messages, generation counters, and foreground/background listener callbacks without testable repository contracts.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.anrwatchdog`:
  1. Implement `AnrWatchdogRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote ANR telemetry and incident reporting extension points.
  2. Implement `AnrWatchdogLocalDataSource`:
     - Encapsulates thread-safe heartbeat ping dispatch, acknowledgement, freeze detection calculation, incident history, `StateFlow<AnrWatchdogState>`, and `SharedFlow<AnrIncident>`.
  3. Implement `AnrWatchdogRepositoryImpl`:
     - Implements `AnrWatchdogRepository`, coordinating monitoring lifecycle, foreground status, ping dispatch, acknowledgement, freeze checks, incident resolution, and incident history.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `AnrWatchdogRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `ANRDetector.getAnrWatchdogRepository(account)`, `getAnrWatchdogRepository()`, and instance `getRepository()` accessors.
- **Consequences:** ANR watchdog diagnostics and freeze detection are decoupled behind clean domain contracts. 100% unit test coverage achieved with `AnrWatchdogRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 176: UI Animation Lock Arbitration & Notification Suppression Strangling via Clean DataSources & AnimationLockerRepositoryImpl
- **Context:** Suspending NotificationCenter notifications during screen transitions and animated UI interactions was governed by `AnimationNotificationsLocker.java` (~48 lines). The locker directly manipulated `NotificationCenter.getInstance(account).setAnimationInProgress(...)` and `NotificationCenter.getGlobalInstance().setAnimationInProgress(...)` without a testable data source or reactive state tracking.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.animationlocker`:
  1. Implement `AnimationLockerRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote config extension points.
  2. Implement `AnimationLockerLocalDataSource`:
     - Encapsulates `ConcurrentHashMap<String, AnimationLockRecord>`, NotificationCenter suppression, allowed notification ID filtering, lock scopes (`ACCOUNT`, `GLOBAL`, `ALL`), `StateFlow<AnimationLockerState>`, and `StateFlow<Boolean> isLocked`.
  3. Implement `AnimationLockerRepositoryImpl`:
     - Implements `AnimationLockerRepository`, coordinating acquire lock, release lock, release all, disable locker, allowed notification ID filtering, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `AnimationLockerRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `AnimationNotificationsLocker.getAnimationLockerRepository(account)`, `getAnimationLockerRepository()`, and instance `getRepository()` accessors.
- **Consequences:** Animation lock arbitration and notification suppression are decoupled behind clean domain contracts. 100% unit test coverage achieved with `AnimationLockerRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 175: Window Visibility Arbitration & Reference Counting Strangling via Clean DataSources & WindowVisibilityRepositoryImpl
- **Context:** In Telegram Android, window visibility arbitration across dialogs, activities, and overlays was governed by `WindowVisibilityManager.java` (~76 lines). The manager managed reference-counting hide reasons (`reasonsToHide`), a direct `OnVisibilityChangedListener`, and subsystem controllers without decoupled data sources or testable repository contracts.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.windowvisibility`:
  1. Implement `WindowVisibilityRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing system remote sync extension points.
  2. Implement `WindowVisibilityLocalDataSource`:
     - Encapsulates reference-counting `reasonsToHide`, active reasons set (`ConcurrentHashMap.newKeySet()`), `StateFlow<WindowVisibilityState>`, and `SharedFlow<Boolean>` with listener callbacks and headless JVM test mode.
  3. Implement `WindowVisibilityRepositoryImpl`:
     - Implements `WindowVisibilityRepository`, coordinating hide requests, releases, toggles, reasons queries, reset, reactive state/toggle flows, and controller instantiation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `WindowVisibilityRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `WindowVisibilityManager.getWindowVisibilityRepository(account)`, `getWindowVisibilityRepository()`, and instance `getRepository()` accessors.
- **Consequences:** Window visibility arbitration and reference-counting hide reasons are decoupled behind clean domain contracts. 100% unit test coverage achieved with `WindowVisibilityRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 174: Power Saving, Battery Optimization & Animation Throttling Strangling via Clean DataSources & LiteModeRepositoryImpl
- **Context:** Power saving, battery level checks, animation throttling, and feature flag masks were managed by `LiteMode.java` (~365 lines). The class coupled static fields (`value`, `powerSaverLevel`, `lastPowerSaverApplied`), Android battery manager calls, bitwise preset masks, and SharedPreferences persistence without clean lifecycle management or headless unit-test isolation.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.litemode`:
  1. Implement `LiteModeRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote preset synchronization extension points.
  2. Implement `LiteModeLocalDataSource`:
     - Encapsulates `LiteMode.getValue()`, `LiteMode.getPowerSaverLevel()`, `LiteMode.isPowerSaverApplied()`, `LiteMode.setAllFlags()`, thread-safe state caching, `MutableStateFlow<LiteModeState>`, and headless JVM test mode.
  3. Implement `LiteModeRepositoryImpl`:
     - Implements `LiteModeRepository`, coordinating local flags, power saver thresholds, battery levels, presets, and reactive state observation.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `LiteModeRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `LiteMode.getLiteModeRepository(account)` and `getLiteModeRepository()` accessors.
- **Consequences:** LiteMode power saving and animation throttling are decoupled behind clean domain contracts. 100% unit test coverage achieved with `LiteModeRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 173: Dynamic App Launcher Icons & Premium Badging Strangling via Clean DataSources & LauncherIconRepositoryImpl
- **Context:** Dynamic launcher icon selection, premium icon badging, and launcher repair routines were governed by `LauncherIconController.java` (~72 lines). The controller coupled direct `PackageManager` component enablement calls and static enum lookups without a testable data source abstraction or reactive state observation.
- **Decision:** Apply the Strangler Fig pattern to `feature.system.launchericon`:
  1. Implement `LauncherIconRemoteDataSource`:
     - Subclasses `BaseRemoteDataSource(currentAccount)` providing remote config extension points for dynamic icons.
  2. Implement `LauncherIconLocalDataSource`:
     - Encapsulates `PackageManager` component enablement queries and mutations via `ApplicationLoader.applicationContext`, provides reactive `StateFlow<LauncherIconsStateModel>`, and includes headless JVM test mode.
  3. Implement `LauncherIconRepositoryImpl`:
     - Implements `LauncherIconRepository`, coordinating `LauncherIconLocalDataSource` and `LauncherIconRemoteDataSource`, providing active icon queries, setting icons, and repairing launcher components.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `LauncherIconRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `LauncherIconController.getLauncherIconRepository(account)` and `getLauncherIconRepository()` accessors.
- **Consequences:** Launcher icon management and component enablement are decoupled behind clean domain contracts. 100% unit test coverage achieved with `LauncherIconRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 172: Telegram Payments & Star Balance Strangling via Clean DataSources & PaymentsRepositoryImpl
- **Context:** Telegram Stars balance, transaction histories, subscriptions, and star top-up packages were governed by `StarsController.java` (~4425 lines). The controller coupled in-memory balance numbers (`balance`), transaction buckets (`transactions`), active subscriptions (`subscriptions`), top-up options (`options`), and global events on `NotificationCenter.starBalanceUpdated`, `starTransactionsLoaded`, and `starSubscriptionsLoaded`.
- **Decision:** Apply the Strangler Fig pattern to `feature.business.payments`:
  1. Implement `PaymentsRemoteDataSource`:
     - Encapsulates MTProto fetching and cache invalidation for balance, transactions, subscriptions, and top-up options via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `PaymentsLocalDataSource`:
     - Encapsulates thread-safe balance caching, transaction buckets, subscription lists, top-up options, and `NotificationCenter` event observation with in-memory test fallbacks.
  3. Implement `PaymentsRepositoryImpl`:
     - Implements `PaymentsRepository`, providing clean methods to get/observe balance, transaction history, subscriptions, top-up options, and refresh mechanisms.
  4. Update `BusinessContainer` and `AccountFeatureContainer` to wire `PaymentsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `StarsController.getPaymentsRepository(account)` and `getPaymentsRepository()` accessors.
- **Consequences:** Stars balance tracking, transactions, and subscriptions are decoupled behind clean domain contracts. 100% unit test coverage achieved with `PaymentsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 171: Star Gifts Catalog & Profile Gifts Strangling via Clean DataSources & StarGiftsRepositoryImpl
- **Context:** Telegram Star Gifts catalog, individual gift lookup, profile saved gifts pagination, and gift pin/hide states were managed inside `StarsController.java` (~4425 lines). Components coupled raw MTProto calls (`TL_stars.getSavedStarGifts`, `saveStarGift`), in-memory gift lists (`sortedGifts`, `gifts`), and global events on `NotificationCenter.starGiftsLoaded` and `starUserGiftsLoaded`.
- **Decision:** Apply the Strangler Fig pattern to `feature.business.stargifts`:
  1. Implement `StarGiftsRemoteDataSource`:
     - Encapsulates MTProto star gifts catalog retrieval, individual gift lookup, profile saved gifts loading with filters, and gift pin/hide mutations via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `StarGiftsLocalDataSource`:
     - Encapsulates catalog cache, gift lookups, profile gifts lists by dialog, and `NotificationCenter` event observation with thread-safe test fallbacks.
  3. Implement `StarGiftsRepositoryImpl`:
     - Implements `StarGiftsRepository`, coordinating catalog retrieval, gift lookup, paginated profile gifts, pin/hide toggling, and reactive catalog observation.
  4. Update `BusinessContainer` and `AccountFeatureContainer` to wire `StarGiftsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `StarsController.getStarGiftsRepository(account)` and `getStarGiftsRepository()` accessors.
- **Consequences:** Star gifts catalog and user profile gift collections are decoupled behind clean domain contracts. 100% unit test coverage achieved with `StarGiftsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 170: Bot Stars Balance & Referral Revenue Strangling via Clean DataSources & BotStarsRepositoryImpl
- **Context:** In Telegram Android, Bot Stars revenue statistics, TON balances, transaction histories, connected referral bots, suggested affiliate bots, and admined bots/channels were managed by `BotStarsController.java` (~652 lines). The controller managed in-memory caches (`botStarsStats`, `tonStats`, `transactions`, `connectedBots`), MTProto RPCs, and untyped global notifications without isolated data sources or testable repository contracts.
- **Decision:** Apply the Strangler Fig pattern to `feature.business.botstars`:
  1. Implement `BotStarsRemoteDataSource`:
     - Encapsulates MTProto revenue stats, transactions, referral bots, suggested programs, and admined bots/channels loading via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `BotStarsLocalDataSource`:
     - Encapsulates cached bot stats, TON stats, transaction lists, referral bots, suggested programs, and `NotificationCenter` event observation (`botStarsUpdated`, `botStarsTransactionsLoaded`, `channelConnectedBotsUpdate`) with in-memory test fallbacks.
  3. Implement `BotStarsRepositoryImpl`:
     - Implements `BotStarsRepository`, coordinating revenue stats, transaction filtering, connected referral bots, suggested programs, and reactive observeBotStarsStats/observeTonStats.
  4. Update `BusinessContainer` and `AccountFeatureContainer` to wire `BotStarsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `BotStarsController.getBotStarsRepository(account)` and `getRepository()` accessors.
- **Consequences:** Bot Stars revenue, TON balances, transactions, and referral affiliate programs are decoupled behind clean domain contracts. 100% unit test coverage achieved with `BotStarsRepositoryImplTest.kt` passing and full backward compatibility preserved. Completes `business` domain to 100% (10/10 features) and crosses 50% global milestone (55/105 features, 52.4%).

### ADR 169: Business Recipients & Exclusion Rules Arbitration Strangling via Clean DataSources & BusinessRecipientsRepositoryImpl
- **Context:** In Telegram Business, recipient filtering for away messages, greeting messages, and connected chatbots is managed by `BusinessRecipientsHelper.java` (~265 lines). The helper handled bitwise flag manipulation (`FLAG_EXISTING_CHATS`, `FLAG_NEW_CHATS`, `FLAG_CONTACTS`, `FLAG_NON_CONTACTS`, `FLAG_EXCLUDE_SELECTED`), synchronization between selected user ID lists and excluded user ID lists, and UI validation without an isolated data source or pure domain repository contract.
- **Decision:** Apply the Strangler Fig pattern to `feature.business.businessrecipients`:
  1. Implement `BusinessRecipientsLocalDataSource`:
     - Encapsulates bitwise recipient flags, thread-safe user lists (`selectedUserIds`, `excludedUserIds`) with automatic mutual exclusion arbitration, and `BusinessRecipientsHelper` integration with headless JVM in-memory fallbacks.
  2. Implement `BusinessRecipientsRepositoryImpl`:
     - Implements `BusinessRecipientsRepository`, providing clean methods to get/set flags, select/exclude users, clear selections, and reactively observe recipient configuration changes via `observeRecipients()`.
  3. Update `BusinessContainer` and `AccountFeatureContainer` to wire `BusinessRecipientsRepositoryImpl` alongside clean data sources.
  4. Introduce strangler boundary: `BusinessRecipientsHelper.getBusinessRecipientsRepository(account)` and `getRepository()` accessors.
- **Consequences:** Recipient filtering and mutual exclusion arbitration are decoupled behind clean domain contracts. 100% unit test coverage achieved with `BusinessRecipientsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 168: Connected Business Chatbots & Permission Delegation Strangling via Clean DataSources & BusinessBotsRepositoryImpl
- **Context:** Telegram Business allows delegating business chat interactions to connected third-party AI chatbots via `BusinessChatbotController.java` (~170 lines). The controller managed in-memory chatbot permissions (`can_reply`, `bot_user_id`), MTProto RPC synchronization (`TL_account.updateConnectedBot`), and fired global notifications (`businessBotUpdated`) on `NotificationCenter` without lifecycle isolation or headless testing capabilities.
- **Decision:** Apply the Strangler Fig pattern to `feature.business.businessbots`:
  1. Implement `BusinessBotsRemoteDataSource`:
     - Encapsulates MTProto `updateConnectedBot` and chatbot detachment RPCs via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `BusinessBotsLocalDataSource`:
     - Encapsulates connected bot state, permission flags, and `NotificationCenter` event observation (`businessBotUpdated`) with thread-safe in-memory test fallbacks.
  3. Implement `BusinessBotsRepositoryImpl`:
     - Implements `BusinessBotsRepository`, providing clean methods to get connected bot status, toggle reply permissions, configure bot recipients, detach bots, and reactively observe bot status via `observeChatbots()`.
  4. Update `BusinessContainer` and `AccountFeatureContainer` to wire `BusinessBotsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `BusinessChatbotController.getBusinessBotsRepository(account)` and `getRepository()` accessors.
- **Consequences:** Connected business chatbot management and permissions delegation are decoupled behind clean domain contracts. 100% unit test coverage achieved with `BusinessBotsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 167: Business Quick Replies & Canned Responses Strangling via Clean DataSources & QuickRepliesRepositoryImpl
- **Context:** In Telegram Business, quick replies (pre-configured response shortcuts `/shortcut`, canned messages, and order indexing) were managed by `QuickRepliesController.java` (~870 lines). The controller coupled SQLite storage (`messagesStorage.getQuickReplies`), MTProto RPCs (`TL_messages.sendQuickReplyShortcut`, `TL_messages.reorderQuickReplies`, `TL_messages.deleteQuickReplyShortcut`), and UI event notifications with direct array mutations.
- **Decision:** Apply the Strangler Fig pattern to `feature.business.quickreplies`:
  1. Implement `QuickRepliesRemoteDataSource`:
     - Encapsulates MTProto quick reply sending, shortcut reordering, and deletion via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `QuickRepliesLocalDataSource`:
     - Encapsulates quick reply shortcut caches, local pending models, order index mapping, and `NotificationCenter` event observation (`quickRepliesUpdated`) with thread-safe in-memory test fallbacks.
  3. Implement `QuickRepliesRepositoryImpl`:
     - Implements `QuickRepliesRepository`, coordinating shortcuts retrieval, name validation, addition limits checks, reordering, deletion, and reactive observation via `observeQuickReplies()`.
  4. Update `BusinessContainer` and `AccountFeatureContainer` to wire `QuickRepliesRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `QuickRepliesController.getQuickRepliesRepository(account)` and `getRepository()` accessors.
- **Consequences:** Quick reply shortcuts, template messages, and ordering are decoupled behind clean domain contracts. 100% unit test coverage achieved with `QuickRepliesRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 166: Business Chat Links & Click Tracking Controller Isolation
- **Context:** Telegram Business allows creating custom chat links (`t.me/m/...`) with prefilled messages and tracking click counts (`views`) via `BusinessLinksController.java` (~320 lines). The controller coupled SQLite storage, MTProto requests (`TL_account.createBusinessChatLink`, `editBusinessChatLink`, `deleteBusinessChatLink`), and UI dialog notifications, with direct array mutations.
- **Decision:** Introduce pure domain models `BusinessLinkModel`, `BusinessLinkInputModel`, and `BusinessLinksStateModel`. Define contract `BusinessLinksRepository` covering reactive link observation (`observeBusinessLinks`), CRUD operations (`createLink`, `editLink`, `deleteLink`), finding by slug, and links limit checking (`canAddNew`, `getLinksLimit`). Implement `BusinessLinksRemoteDataSource` using MTProto RPC and `BusinessLinksLocalDataSource` adapting `BusinessLinksController` with headless test fallbacks. Implement `BusinessLinksRepositoryImpl` on `Dispatchers.Main`. Wire in `BusinessContainer` and provide strangler hooks in `BusinessLinksController.getBusinessLinksRepository(account)`.
- **Consequences:** Business chat link lifecycle, slug resolution, and prefilled messages are decoupled behind clean, testable domain interfaces with complete unit test coverage while preserving full compatibility with Telegram's MTProto business links protocol.

### ADR 165: Timezones & Opening Hours Controller Isolation
- **Context:** Telegram Business allows users to configure business opening hours and timezone preferences via `TimezonesController.java` (~180 lines). The controller managed in-memory cache, hex-serialized preferences, Java 8 timezones (`ZoneId`), and MTProto RPC requests (`TLRPC.TL_help_getTimezonesList`) while directly firing global `timezonesUpdated` events on `NotificationCenter`.
- **Decision:** Introduce pure domain model `TimezoneModel` with formatted offsets (e.g. `GMT+03:00`). Define contract `TimezonesRepository` covering cached and remote loading, timezone search by id, system timezone detection, and reactive updates observation (`observeTimezones`). Implement `TimezonesRemoteDataSource` using MTProto RPC and `TimezonesLocalDataSource` adapting `TimezonesController` with headless test fallbacks. Implement `TimezonesRepositoryImpl` on `Dispatchers.Main`. Wire in `BusinessContainer` and provide strangler hooks in `TimezonesController.getTimezonesRepository(account)`.
- **Consequences:** Timezone queries and updates are isolated behind domain interfaces with complete unit test coverage while preserving full compatibility with Telegram's MTProto timezone protocol and SharedPreferences caching.

### ADR 164: Window Security Arbitration & Screenshot Protection Controller Isolation
- **Context:** In Telegram Android, window security against screen capture (`FLAG_SECURE`) was spread across `SharedConfig.allowScreenCapture`, `PasscodeActivity`, `PaymentFormActivity`, and `AndroidUtilities`. Multiple security features (passcode lock, secret chats, protected content, self-destructing media, biometric prompts, payment forms) required dynamic attachment and detachment of window flags without coordination, risking accidental screen recording of confidential data or breaking user accessibility when flags were leaked.
- **Decision:** Introduce pure domain models `SecurityReasonType`, `WindowSecurityState`, `SecurityRuleSpec`, `SecurityEvaluationResult`, and domain calculator `SecurityRulesEvaluator`. Define abstract contract `FlagSecureRepository` managing reasons attachment with dynamic conditions, detaching, invalidation, resetting, and reactive state flows (`observeWindowState`, `observeAllWindowStates`). Implement `FlagSecureLocalDataSource` and `FlagSecureRepositoryImpl` thread-safely managing window state. Wire in `SecurityContainer` and expose strangler hooks in `AndroidUtilities.getFlagSecureRepository(account)`.
- **Consequences:** Window security arbitration is decoupled behind clean, reactive domain interfaces with 100% test coverage without mocking frameworks. Completes `security` domain to 100% (10/10 features).

### ADR 163: Privacy & Security Settings Strangling via Clean DataSources & PrivacyRepositoryImpl
- **Context:** In Telegram Android, user privacy settings (11 privacy rule types, blocked peers list, app passcode, biometric unlock, and 2-step verification password) were fragmented across `ContactsController.java` (`getPrivacyRules`, `setPrivacyRules`, `loadPrivacySettings`), `MessagesController.java` (`blockePeers`, `blockPeer`, `unblockPeer`), and `SharedConfig.java` (`passcodeHash`, `passcodeSalt`, `checkPasscode`, `autoLockIn`). Components lacked unified domain boundaries, headless test isolation, and reactive typed state observation.
- **Decision:** Apply the Strangler Fig pattern to `feature.security.privacy`:
  1. Implement `PrivacyRemoteDataSource`:
     - Encapsulates `TL_account.setPrivacy` and `TL_account.getPassword` MTProto RPC execution via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `PrivacyLocalDataSource`:
     - Encapsulates privacy rule arrays, blocked peer dictionaries, passcode hashing/salting, and `NotificationCenter` event observation with concurrent in-memory test fallbacks.
  3. Implement `PrivacyRepositoryImpl`:
     - Implements `PrivacyRepository` with 17 methods covering privacy rule mapping, blocked peer management, passcode settings, and 2FA password inspection.
  4. Update `SecurityContainer` and `AccountFeatureContainer` to wire `PrivacyRepositoryImpl` alongside clean data sources (bringing the `security` domain to 90% [9/10]).
  5. Introduce strangler boundary: `ContactsController.getPrivacyRepository(account)` and `getPrivacyRepository()`.
- **Consequences:** Privacy rules, blocklists, and passcode settings are unified behind clean domain contracts. 100% test coverage achieved with `PrivacyRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 162: Push Services & Token Registration Strangling via Clean DataSources & PushRepositoryImpl
- **Context:** In Telegram Android, push service status, notification channels, provider resolution (Firebase vs Huawei HMS), and token registration to MTProto servers were managed by `PushListenerController.java` (~1743 lines), `SharedConfig.java`, and `UserConfig.java`. UI and service components directly accessed raw static properties and untyped string statuses.
- **Decision:** Apply the Strangler Fig pattern to `feature.network.push`:
  1. Implement `PushRemoteDataSource`:
     - Encapsulates push token provider requests and `PushListenerController.sendRegistrationToServer` MTProto dispatch via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `PushLocalDataSource`:
     - Encapsulates `SharedConfig` push string/type/status and `UserConfig` registration flags with in-memory test fallbacks.
  3. Implement `PushRepositoryImpl`:
     - Implements `PushRepository`, managing reactive `observePushStatus()`, provider availability checks, token registration, and token resets.
  4. Update `NetworkContainer` and `AccountFeatureContainer` to wire `PushRepositoryImpl` alongside clean data sources, completing the `network` domain to 100% (4/4 features strangled).
  5. Introduce strangler boundary: `PushListenerController.getPushRepository(account)` and `getPushRepository()`.
- **Consequences:** Push notifications and token registration are fully decoupled behind clean domain contracts. 100% test coverage achieved with `PushRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 161: Profile Strangling via Clean DataSources & ProfileRepositoryImpl
- **Context:** In Telegram Android, user, bot, group, and broadcast channel profile data access was coupled directly to `MessagesController.java` (`getUser`, `getChat`, `getUserFull`, `getChatFull`, `loadFullUser`, `loadFullChat`, `blockPeer`, `unblockPeer`). UI classes like `ProfileActivity.java` (~17054 lines) were directly dependent on legacy controllers and raw `TLRPC` structures.
- **Decision:** Apply the Strangler Fig pattern to `feature.social.profile`:
  1. Implement `ProfileRemoteDataSource`:
     - Encapsulates `loadFullUser`, `loadFullChat`, `blockPeer`, and `unblockPeer` via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `ProfileLocalDataSource`:
     - Encapsulates user/chat caching and `NotificationCenter` event observation (`userInfoDidLoad`, `chatInfoDidLoad`, `blockedUsersDidLoad`, `updateInterfaces`) with thread-safe in-memory test fallbacks.
  3. Implement `ProfileRepositoryImpl`:
     - Implements `ProfileRepository`, transforming raw `TLRPC` entities into pure `ProfileModel` domain instances via `ProfileMapper`, supporting reactive observations and full info fetching.
  4. Update `SocialContainer` and `AccountFeatureContainer` to wire `ProfileRepositoryImpl` alongside clean data sources, completing the `social` domain to 100% (6/6 features strangled).
  5. Introduce strangler boundary: `ProfileActivity.getProfileRepository(account)` and `getProfileRepository()`.
- **Consequences:** User and chat profiles are unified behind clean domain contracts. 100% test coverage achieved with `ProfileRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 160: HintsController Strangling via Clean DataSources & HintsRepositoryImpl
- **Context:** In Telegram Android, user prompts, tips, and feature discovery hints were managed by `HintsController.java` (~78 lines) in `org.telegram.ui.Components`. The controller directly manipulated `MessagesController.getGlobalMainSettings()` SharedPreferences, hardcoded probability calculations (`Utilities.fastRandom.nextFloat()`), and lacked thread-safe reactive state streams or headless test execution isolation.
- **Decision:** Apply the Strangler Fig pattern to `HintsController`:
  1. Implement `HintsRemoteDataSource`:
     - Encapsulates cloud hint dismissals and remote state sync via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `HintsLocalDataSource`:
     - Manages SharedPreferences reads/writes with in-memory concurrent fallback for headless JVM test environments, providing limit and probability checks.
  3. Implement `HintsRepositoryImpl`:
     - Implements `HintsRepository`, coordinating hint counters, display eligibility, resets, and reactive `observeHints()`.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `HintsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `HintsController.getHintsRepository(account)` and static `getRepository()`.
- **Consequences:** In-app discovery prompts and hints are decoupled behind clean domain contracts. 100% test coverage achieved with `HintsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 159: BotInlineKeyboard & TLKeyboardHelper Strangling via Clean DataSources & BotKeyboardRepositoryImpl
- **Context:** In Telegram Android, bot inline keyboard layout construction, button parsing, and reply markup type checks were split across `BotInlineKeyboard.java` (~224 lines) in `org.telegram.messenger` and `TLKeyboardHelper.java` (~49 lines) in `org.telegram.messenger.utils.tlutils`. UI components relied directly on static methods and raw `TLRPC.ReplyMarkup` and `TL_keyboard.KeyboardButtonProto` instances without domain boundaries or state tracking.
- **Decision:** Apply the Strangler Fig pattern to `BotInlineKeyboard` & `TLKeyboardHelper`:
  1. Implement `BotKeyboardRemoteDataSource`:
     - Encapsulates MTProto callback dispatch and remote keyboard fetching via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `BotKeyboardLocalDataSource`:
     - Manages thread-safe in-memory keyboards per `messageId`, force reply checks, and webview button type verification.
  3. Implement `BotKeyboardRepositoryImpl`:
     - Implements `BotKeyboardRepository`, coordinating active message keyboards, button press logging, and reactive `observeState()`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `BotKeyboardRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `TLKeyboardHelper.getBotKeyboardRepository(account)`, `BotInlineKeyboard.getBotKeyboardRepository(account)` and static `getRepository()`.
- **Consequences:** Bot inline keyboard layout management and interaction tracking are decoupled behind clean domain contracts. 100% test coverage achieved with `BotKeyboardRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 158: StoryCustomParamsHelper Strangling via Clean DataSources & StoryCustomParamsRepositoryImpl
- **Context:** In Telegram Android, local story parameters (translation state, detected/translated languages, and serialized text with entities) were managed by `StoryCustomParamsHelper.java` (~100 lines) in `org.telegram.ui.Stories`. The class coupled `TL_stories.StoryItem` fields, bitwise flags calculation, and binary serialization via `NativeByteBuffer` without reactive state streams or repository boundaries.
- **Decision:** Apply the Strangler Fig pattern to `StoryCustomParamsHelper`:
  1. Implement `StoryCustomParamsRemoteDataSource`:
     - Encapsulates MTProto story custom parameters and translation sync via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `StoryCustomParamsLocalDataSource`:
     - Manages in-memory parameter storage per `(dialogId, storyId)` pair with story storage integration and bitwise flag computation.
  3. Implement `StoryCustomParamsRepositoryImpl`:
     - Implements `StoryCustomParamsRepository`, coordinating parameter persistence, translation updates, copying, removal, and reactive `observeState()`.
  4. Update `MediaContainer` and `AccountFeatureContainer` to wire `StoryCustomParamsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `StoryCustomParamsHelper.getStoryCustomParamsRepository(account)` and static `getRepository()`.
- **Consequences:** Local story custom parameters and translation states are decoupled behind clean domain contracts. 100% test coverage achieved with `StoryCustomParamsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 157: GroupCallMessagesController Strangling via Clean DataSources & GroupCallMessagesRepositoryImpl
- **Context:** In Telegram Android, ephemeral in-call messages during group calls and conferences were managed by `GroupCallMessagesController.java` (~310 lines) inside `org.telegram.messenger.voip`. It coupled in-memory `MessagesList` collections, native encryption/decryption (`groupCallMessageEncryptImpl` / `groupCallMessageDecryptImpl`), direct `VoIPService` inspection, and MTProto `TL_phone.sendGroupCallMessage` / `sendGroupCallEncryptedMessage` calls.
- **Decision:** Apply the Strangler Fig pattern to `GroupCallMessagesController`:
  1. Implement `GroupCallMessagesRemoteDataSource`:
     - Encapsulates MTProto RPC execution: `sendCallMessage` via `GroupCallMessagesController` or `BaseRemoteDataSource(currentAccount)`.
  2. Implement `GroupCallMessagesLocalDataSource`:
     - Manages in-memory messages per `callId`, `CallMessageListener` callbacks, and TTL-based message popping with headless JVM fallback.
  3. Implement `GroupCallMessagesRepositoryImpl`:
     - Implements `GroupCallMessagesRepository`, coordinating remote RPC execution, local cache mutations, and reactive `observeCallMessages(callId)`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to wire `GroupCallMessagesRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `GroupCallMessagesController.getGroupCallMessagesRepository(account)` and instance `getGroupCallMessagesRepository()`.
- **Consequences:** Ephemeral in-call group call messages and auto-destruct timers are decoupled behind clean domain contracts. 100% test coverage achieved with `GroupCallMessagesRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 156: RefreshRateController Strangling via Clean DataSources & RefreshRateRepositoryImpl
- **Context:** In Telegram Android, adaptive display refresh rate switching between 60Hz and high refresh rates (90Hz / 120Hz) based on real-time rendering performance was handled by `RefreshRateController.java` (~300 lines) in `org.telegram.messenger.utils`. The controller coupled Android `Window.OnFrameMetricsAvailableListener`, direct window attributes mutation (`window.getAttributes().preferredDisplayModeId`), and raw ring buffer math.
- **Decision:** Apply the Strangler Fig pattern to `RefreshRateController`:
  1. Implement `RefreshRateRemoteDataSource`:
     - Encapsulates remote feature enablement and configuration checks.
  2. Implement `RefreshRateLocalDataSource`:
     - Manages display modes, 240-element ring buffer for frame duration nanoseconds, average FPS calculation, and hysteresis threshold timers (down at <= 55 FPS, up at >= 58.5 FPS) with headless test fallback.
  3. Implement `RefreshRateRepositoryImpl`:
     - Implements `RefreshRateRepository`, coordinating frame metrics tracking, adaptive state updates, and reactive `observeState()`.
  4. Update `SystemContainer` and `AccountFeatureContainer` to wire `RefreshRateRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `RefreshRateController.getRefreshRateRepository()` and static `getRepository()`.
- **Consequences:** Display refresh rate arbitration and performance metrics tracking are decoupled behind clean domain contracts. 100% test coverage achieved with `RefreshRateRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 155: PushListenerController Strangling via Clean DataSources & PushListenerRepositoryImpl
- **Context:** In Telegram Android, background push payload receiving, FCM and Huawei Push Kit registration, and push decryption diagnostics were managed by `PushListenerController.java` (~1735 lines). `PushListenerController` coupled direct `ConnectionsManager.setRegId`, static `CountDownLatch`, multi-account `UserConfig` iteration, raw JSON parsing, and MTProto `TL_help_saveAppLog` calls.
- **Decision:** Apply the Strangler Fig pattern to `PushListenerController`:
  1. Implement `PushListenerRemoteDataSource`:
     - Encapsulates token registration with MTProto / Push servers and error logging via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `PushListenerLocalDataSource`:
     - Manages listening toggle state, registered tokens map, decrypt error counts, and payload parsing with headless test fallback.
  3. Implement `PushListenerRepositoryImpl`:
     - Implements `PushListenerRepository`, coordinating token registration, push processing, error reporting, and reactive `observeState()` / `observeIncomingPushes()`.
  4. Update `NetworkContainer` and `AccountFeatureContainer` to wire `PushListenerRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `PushListenerController.getPushListenerRepository(account)` and static `getRepository()`.
- **Consequences:** Inbound push handling, token registration, and decryption diagnostics are decoupled behind clean domain contracts. 100% test coverage achieved with `PushListenerRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 154: Sessions Strangling via Clean DataSources & SessionsRepositoryImpl
- **Context:** In Telegram Android, user device authorizations, active web authorizations, QR code login, and session self-destruct TTL settings were managed across `SessionsActivity.java` (~1280 lines), `ConnectionsManager`, and `MessagesController`. Legacy code coupled MTProto RPCs (`TL_account.getAuthorizations`, `TL_account.resetAuthorization`, `TL_auth.resetAuthorizations`, `TL_account.getWebAuthorizations`, `TL_auth.acceptLoginToken`), direct base64 parsing, manual push token re-registration across multiple accounts, and raw `NotificationCenter.newSessionReceived` events.
- **Decision:** Apply the Strangler Fig pattern to `Sessions`:
  1. Implement `SessionsRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getAuthorizations`, `resetAuthorization`, `resetAllAuthorizations`, `getWebAuthorizations`, `resetWebAuthorization`, `resetAllWebAuthorizations`, `changeAuthorizationSettings`, `setAuthorizationTTL`, and `acceptLoginToken` via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `SessionsLocalDataSource`:
     - Manages thread-safe in-memory cache for `SessionsListModel` and `List<WebSessionModel>`, URL-safe Base64 QR login token parsing, and multi-account push token refresh post-termination.
  3. Implement `SessionsRepositoryImpl`:
     - Implements `SessionsRepository`, coordinating remote RPC execution, reactive observation via `NotificationCenter.newSessionReceived`, session terminations, settings updates, TTL configuration, and QR login confirmation.
  4. Update `SecurityContainer` and `AccountFeatureContainer` to instantiate and wire `SessionsRepositoryImpl` alongside clean data sources.
- **Consequences:** Active device sessions, web sessions, QR login flow, and TTL management are decoupled behind clean domain contracts. 100% test coverage achieved with `SessionsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 153: ChatMessagesMetadataController Strangling via Clean DataSources & ChatMetadataRepositoryImpl
- **Context:** In Telegram Android, visible chat message reactions polling, extended/paid media preview updates, and story item synchronizations were orchestrated by `ChatMessagesMetadataController.java` (~180 lines) attached to `ChatActivity`. The controller coupled direct MTProto request dispatching (`TL_messages_getMessagesReactions`, `TL_messages_getExtendedMedia`, `TL_stories_getStoriesByID`), manual request list throttling, and direct updates processing via `MessagesController.processUpdates()`.
- **Decision:** Apply the Strangler Fig pattern to `ChatMessagesMetadataController`:
  1. Implement `ChatMetadataRemoteDataSource`:
     - Encapsulates MTProto RPC execution: `loadReactions`, `loadExtendedMedia`, and `loadStories` via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `ChatMetadataLocalDataSource`:
     - Manages timing interval validation (reactions 15s, extended media 30s, stories 5m), request queue throttling (max 5 reactions, max 10 extended media), and stats accumulation.
  3. Implement `ChatMetadataRepositoryImpl`:
     - Implements `ChatMessagesMetadataRepository`, coordinating viewport message inspection (`checkMessages`), batch loading, request queue throttling, cancellation, and reactive `observeStats()`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and wire `ChatMetadataRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `ChatMessagesMetadataController.getChatMessagesMetadataRepository(account)` and instance `getRepository()`.
- **Consequences:** Viewport message metadata inspection, reactions synchronization, and extended media polling are decoupled behind clean domain contracts. 100% test coverage achieved with `ChatMetadataRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 152: BotGuardHelper Strangling via Clean DataSources & BotGuardRepositoryImpl
- **Context:** In Telegram Android, bot verification web app launching, dialog launch confirmation alerts, and guard decision callbacks were coordinated by `BotGuardHelper.java` (~120 lines). `BotGuardHelper` coupled in-memory sparse long arrays (`queryIdToBotId`), SharedPreferences persistence (`SharedPrefsHelper.isWebViewConfirmShown`), `MessagesController.whitelistedBots`, and global `NotificationCenter.guardBotDecisionResult` broadcasts.
- **Decision:** Apply the Strangler Fig pattern to `BotGuardHelper`:
  1. Implement `BotGuardRemoteDataSource`:
     - Encapsulates MTProto remote operations for Bot Guard verification and web app queries via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `BotGuardLocalDataSource`:
     - Manages SharedPreferences bot webview confirmation flags, bot whitelist checks, and thread-safe session tracking (`activeSessions`, `queryIdToBotId`) with headless test fallback.
  3. Implement `BotGuardRepositoryImpl`:
     - Implements `BotGuardRepository`, coordinating session registration, confirmation checks, decision posting, and reactive `observeState()` / `observeDecisions()`.
  4. Update `SecurityContainer` and `AccountFeatureContainer` to instantiate and wire `BotGuardRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `BotGuardHelper.getBotGuardRepository(account)` and instance `getRepository()`.
- **Consequences:** Bot Guard verification sessions and confirmation state management are decoupled behind clean domain contracts. 100% test coverage achieved with `BotGuardRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 151: MessageCustomParamsHelper Strangling via Clean DataSources & MessageCustomParamsRepositoryImpl
- **Context:** In Telegram Android, custom per-message auxiliary parameters (transcriptions, speech recognition flags, translation data) were handled by `MessageCustomParamsHelper.java` (~230 lines). `MessageCustomParamsHelper` coupled direct binary serialization (`Params_v1`), in-memory sparse structures, MTProto audio transcription requests (`TL_messages_transcribeAudio`), and legacy database helper calls.
- **Decision:** Apply the Strangler Fig pattern to `MessageCustomParamsHelper`:
  1. Implement `MessageCustomParamsRemoteDataSource`:
     - Encapsulates MTProto transcription requests (`TL_messages_transcribeAudio`) and speech/translation data fetches via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `MessageCustomParamsLocalDataSource`:
     - Manages binary buffer read/write with `Params_v1` TL serialization, thread-safe memory caching per message ID (`ConcurrentHashMap<Long, MessageCustomParamsModel>`), and legacy byte buffer conversions.
  3. Implement `MessageCustomParamsRepositoryImpl`:
     - Implements `MessageCustomParamsRepository`, coordinating message param reads/writes, deep copying between forwarded/edited messages, params removal, and reactive `observeState()`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and wire `MessageCustomParamsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `MessageCustomParamsHelper.getMessageCustomParamsRepository(account)` and instance `getRepository()`.
- **Consequences:** Custom message params and transcription state are decoupled behind clean domain contracts. 100% test coverage achieved with `MessageCustomParamsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 150: AuthTokensHelper Strangling via Clean DataSources & AuthTokensRepositoryImpl
- **Context:** In Telegram Android, session authorization tokens, auto-login tokens, and logged-out token history were managed by `AuthTokensHelper.java` (~215 lines). `AuthTokensHelper` coupled SharedPreferences persistence (`saved_tokens`, `saved_tokens_login`), hex encoding of serialized TL authorization objects, and session drop MTProto requests.
- **Decision:** Apply the Strangler Fig pattern to `AuthTokensHelper`:
  1. Implement `AuthTokensRemoteDataSource`:
     - Encapsulates MTProto session drop and token invalidation RPCs via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `AuthTokensLocalDataSource`:
     - Manages hex-encoded serialization and deserialization of `TL_auth_authorization` and `TL_auth_loggedOut`, thread-safe SharedPreferences persistence, and headless memory caching with a 20-token LRU ceiling.
  3. Implement `AuthTokensRepositoryImpl`:
     - Implements `AuthTokensRepository`, coordinating token saves, LRU pruning (maximum 20 tokens), removal by hex token or authorization ID, and reactive `observeState()`.
  4. Update `SecurityContainer` and `AccountFeatureContainer` to instantiate and wire `AuthTokensRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `AuthTokensHelper.getAuthTokensRepository(account)` and instance `getRepository()`.
- **Consequences:** Session token lifecycle, auto-login persistence, and logged-out token caching are decoupled behind clean domain contracts. 100% test coverage achieved with `AuthTokensRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 149: SaveToGallerySettingsHelper Strangling via Clean DataSources & GallerySaveRepositoryImpl
- **Context:** In Telegram Android, auto-save media to gallery preferences, peer-specific exceptions (users, channels, groups), and video file size limits were managed by `SaveToGallerySettingsHelper.java` (~340 lines). `SaveToGallerySettingsHelper` coupled SharedPreferences persistence, bitwise peer flag masking, and MTProto auto-save settings sync.
- **Decision:** Apply the Strangler Fig pattern to `SaveToGallerySettingsHelper`:
  1. Implement `GallerySaveRemoteDataSource`:
     - Encapsulates MTProto remote sync for auto-save gallery configuration and server defaults via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `GallerySaveLocalDataSource`:
     - Manages SharedPreferences persistence for exceptions (`users_save_gallery_exceptions`, `channels_save_gallery_exceptions`, `groups_save_gallery_exceptions`), in-memory `LongSparseArray` caching with headless test fallback, video limit checks (up to 4 GB), and peer flag bitmask validation.
  3. Implement `GallerySaveRepositoryImpl`:
     - Implements `GallerySaveRepository`, coordinating exceptions CRUD (`getExceptions`, `setException`, `removeException`, `removeAllExceptions`), peer settings observation, video limit clamping, and reactive `_configFlow`.
  4. Update `MediaContainer` and `AccountFeatureContainer` to instantiate and wire `GallerySaveRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `SaveToGallerySettingsHelper.getGallerySaveRepository(account)` and instance `getRepository()`.
- **Consequences:** Gallery auto-save rules and peer exceptions are decoupled behind clean domain contracts. 100% test coverage achieved with `GallerySaveRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 148: BotForumHelper Strangling via Clean DataSources & BotForumRepositoryImpl
- **Context:** In Telegram Android, Bot Forum topics and AI draft message streaming were managed by `BotForumHelper.java` (~785 lines). `BotForumHelper` coupled MTProto draft actions (`TL_sendMessageTextDraftAction`, `TL_sendMessageRichMessageDraftAction`, `TL_sendMessageStopDraftAction`), custom sparse array structures (`DialogTopicIdKeyMap`), SharedPreferences persistence (`bot_drafts`), typing animators (`MultiLayoutTypingAnimator`), and global `NotificationCenter` broadcasts.
- **Decision:** Apply the Strangler Fig pattern to `BotForumHelper`:
  1. Implement `BotForumRemoteDataSource`:
     - Encapsulates sending stop draft typing actions (`TL_messages_setTyping`) and creating forum topics (`TL_messages_createForumTopic`) via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `BotForumLocalDataSource`:
     - Manages thread-safe active drafts mapping, random ID blocklists, topic streaming flags persistence with test fallback, and streaming send button state resolution (`NO_STREAMING`, `BLOCKING`, `STOP`).
  3. Implement `BotForumRepositoryImpl`:
     - Implements `BotForumRepository`, coordinating draft streaming updates, draft timeout removals, message replacement checks, stop streaming actions, and reactive `observeState()`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and wire `BotForumRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `BotForumHelper.getBotForumRepository(account)` and instance `getRepository()`.
- **Consequences:** Bot Forum draft streaming, typing animation arbitration, and topic persistence are cleanly decoupled behind domain contracts. 100% test coverage achieved with `BotForumRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 147: EphemeralMessagesHelper Strangling via Clean DataSources & EphemeralMessagesRepositoryImpl
- **Context:** In Telegram Android, ephemeral (self-destructing/temporary) messages, welcome message anchors, and bot command interception were managed by `EphemeralMessagesHelper.java` (~489 lines). `EphemeralMessagesHelper` tightly coupled MTProto RPC requests (`TL_ephemeral.TL_sendMessage`), outgoing message request transformations (`TL_messages_sendMessage`, `TL_messages_sendMedia`), anchor bindings (`WelcomeAnchorsState`), and bot info lookups.
- **Decision:** Apply the Strangler Fig pattern to `EphemeralMessagesHelper`:
  1. Implement `EphemeralMessagesRemoteDataSource`:
     - Encapsulates MTProto ephemeral message dispatch (`TL_ephemeral.TL_sendMessage`) and chat full loading via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `EphemeralMessagesLocalDataSource`:
     - Manages thread-safe welcome anchor bindings per dialog, bot command parsing and ephemeral classification, and `EphemeralMessagesState` StateFlow.
  3. Implement `EphemeralMessagesRepositoryImpl`:
     - Implements `EphemeralMessagesRepository`, coordinating anchor bindings CRUD, command detection, and reactive `observeState()` StateFlow.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and wire `EphemeralMessagesRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `EphemeralMessagesHelper.getEphemeralMessagesRepository(account)` and instance `getRepository()`.
- **Consequences:** Ephemeral message lifecycle, anchor tracking, and bot command interception are cleanly decoupled behind testable domain contracts. 100% test coverage achieved with `EphemeralMessagesRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 146: HashtagSearchController Strangling via Clean DataSources & HashtagSearchRepositoryImpl
- **Context:** In Telegram Android, hashtag search queries across personal messages, public channel posts, and chat history, as well as hashtag search history preferences, were managed by `HashtagSearchController.java` (~1380 lines). `HashtagSearchController` coupled MTProto RPCs (`TL_messages_searchGlobal`, `TL_channels_searchPosts`, `TL_messages_search`), username resolution (`userNameResolver`), SharedPreferences persistence (`hashtag_search_history`), and UI selection states.
- **Decision:** Apply the Strangler Fig pattern to `HashtagSearchController`:
  1. Implement `HashtagSearchRemoteDataSource`:
     - Encapsulates MTProto search RPCs (`searchGlobal`, `searchPosts`, `searchChat`) and asynchronous username resolution via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `HashtagSearchLocalDataSource`:
     - Manages hashtag history persistence with `#` and `$` prefix normalization, in-memory cache for search results per type (`MY_MESSAGES`, `PUBLIC_POSTS`, `CHANNEL_POSTS`), and notification posting with headless test fallbacks.
  3. Implement `HashtagSearchRepositoryImpl`:
     - Implements `HashtagSearchRepository`, coordinating multi-type hashtag searches, pagination offsets, history CRUD, message jump indexing, and reactive flows (`observeHistory()`, `observeSearchResult()`).
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and wire `HashtagSearchRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `HashtagSearchController.getHashtagSearchRepository(account)` and instance `getRepository()`.
- **Consequences:** Hashtag search execution, history persistence, and search result states are decoupled into clean, testable data sources and repository. 100% test coverage achieved with `HashtagSearchRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 145: CameraController Strangling via Clean DataSources & CameraRepositoryImpl
- **Context:** In Telegram Android, hardware camera initialization, resolution selection heuristics, front/back camera toggling, flash mode control, and video recording lifecycle were managed by `CameraController.java` (~975 lines). `CameraController` coupled low-level Camera APIs (`android.hardware.Camera`), background ThreadPoolExecutor thread pools, MediaRecorder configuration, and UI callbacks.
- **Decision:** Apply the Strangler Fig pattern to `CameraController`:
  1. Implement `CameraRemoteDataSource`:
     - Encapsulates hardware camera initialization and ThreadPoolExecutor execution via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `CameraLocalDataSource`:
     - Manages thread-safe camera state, available devices list, headless fallback for unit test environments, flash mode, front camera mirroring, optimal resolution calculations, and recording status.
  3. Implement `CameraRepositoryImpl`:
     - Implements `CameraRepository`, coordinating camera initialization, selection, switching, flash mode, mirroring, resolution heuristics, and recording lifecycle with reactive `observeCameraState()`.
  4. Update `MediaContainer` to instantiate and provide `CameraRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `CameraController.getCameraRepository()` and instance `getRepository()`.
- **Consequences:** Camera hardware operations, resolution optimization heuristics, and recording state are decoupled behind clean domain interfaces. 100% test coverage achieved with `CameraRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 144: ChromecastController Strangling via Clean DataSources & ChromecastRepositoryImpl
- **Context:** In Telegram Android, Google Cast session management, remote media casting, playback controls, and album cover synchronization were coordinated by `ChromecastController.java` (~315 lines). `ChromecastController` coupled Google Play Services Cast SDK (`CastContext`, `CastSession`, `SessionManagerListener`, `RemoteMediaClient`), file servers (`ChromecastFileServer`), and photo viewer alerts.
- **Decision:** Apply the Strangler Fig pattern to `ChromecastController`:
  1. Implement `ChromecastRemoteDataSource`:
     - Encapsulates Cast session detection, remote media playback commands, and cover file resolution via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `ChromecastLocalDataSource`:
     - Manages thread-safe cast playback state, active media model, cover path cache, and `ChromecastStateModel` StateFlow.
  3. Implement `ChromecastRepositoryImpl`:
     - Implements `ChromecastRepository`, coordinating cast playback requests, cover uploads, and reactive `observeChromecastState()` StateFlow.
  4. Update `MediaContainer` to instantiate and provide `ChromecastRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `ChromecastController.getChromecastRepository()` and instance `getRepository()`.
- **Consequences:** Google Cast connection state, remote playback, and cover art are cleanly decoupled behind testable domain contracts. 100% test coverage achieved with `ChromecastRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 143: PipActivityController Strangling via Clean DataSources & PipRepositoryImpl
- **Context:** In Telegram Android, Picture-in-Picture window management, video player aspect ratio calculation, source priority arbitration, and floating video states were orchestrated by `PipActivityController.java` (~260 lines). `PipActivityController` coupled Activity lifecycle callbacks, `PipActivityHandler`, `MediaSessionConnector`, floating overlay views (`PipActivityContentLayout`), and custom source listeners.
- **Decision:** Apply the Strangler Fig pattern to `PipActivityController`:
  1. Implement `PipRemoteDataSource`:
     - Encapsulates system PiP window parameters application and media session action dispatch via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `PipLocalDataSource`:
     - Manages thread-safe active PiP sources map, source priority arbitration, aspect ratio tracking, and `PipSessionInfo` StateFlow.
  3. Implement `PipRepositoryImpl`:
     - Implements `PipRepository`, coordinating local session state, priority evaluation, remote actions, and reactive `observeSessionInfo()` StateFlow.
  4. Update `MediaContainer` to instantiate and provide `PipRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `PipActivityController.getPipRepository()` and instance `getRepository()`.
- **Consequences:** PiP source registration, priority arbitration, and session state are cleanly decoupled behind testable domain contracts. 100% test coverage achieved with `PipRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 142: GiftAuctionController Strangling via Clean DataSources & GiftAuctionsRepositoryImpl
- **Context:** In Telegram Android, Star Gift auctions (bidding, price increments, acquired gifts history, user active auctions) were coordinated by `GiftAuctionController.java` (~800 lines). `GiftAuctionController` coupled MTProto RPCs (`TL_payments.getStarGiftAuctionState`, `sendStarGiftAuctionBid`, `getStarGiftAuctionAcquiredGifts`), in-memory auction states (`auctions`, `userAuctions`), and `NotificationCenter` broadcasts (`giftAuctionsUpdated`).
- **Decision:** Apply the Strangler Fig pattern to `GiftAuctionController`:
  1. Implement `GiftAuctionsRemoteDataSource`:
     - Encapsulates MTProto auction queries by ID or slug and acquired gifts loading via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `GiftAuctionsLocalDataSource`:
     - Encapsulates active auctions cache, bid submission delegation via `GiftAuctionController`, and notification observation.
  3. Implement `GiftAuctionsRepositoryImpl`:
     - Implements `GiftAuctionsRepository`, coordinating auction state lookups, bidding operations, acquired gift lists, and reactive `observeActiveAuctions()`.
  4. Update `BusinessContainer` and `AccountFeatureContainer` to instantiate and provide `GiftAuctionsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `GiftAuctionController.getGiftAuctionsRepository(account)` and instance `getRepository()`.
- **Consequences:** Star Gift auctions and bidding logic are cleanly decoupled behind testable domain contracts and data sources. 100% test coverage achieved with `GiftAuctionsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 141: BillingController Strangling via Clean DataSources & BillingRepositoryImpl
- **Context:** In Telegram Android, Google Play in-app purchases and Telegram Premium subscription billing were coordinated by `BillingController.java` (~595 lines). `BillingController` coupled Google Play Billing Library (`BillingClient`), purchase tokens verification RPCs (`payments.assignAppStoreTransaction`), in-memory SKU/product details caching (`productDetailsMap`, `purchases`), and callback listeners.
- **Decision:** Apply the Strangler Fig pattern to `BillingController`:
  1. Implement `BillingRemoteDataSource`:
     - Encapsulates MTProto transactions assignment RPCs (`payments.assignAppStoreTransaction`) via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `BillingLocalDataSource`:
     - Encapsulates Google Play `BillingClient` connection, product details queries, in-flight purchases, and state flows.
  3. Implement `BillingRepositoryImpl`:
     - Implements `BillingRepository`, coordinating billing client connection, product queries, purchase launch, receipt verification, and reactive `observeBillingState()`.
  4. Update `BusinessContainer` and `AccountFeatureContainer` to instantiate and provide `BillingRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `BillingController.getBillingRepository(account)` and instance `getRepository()`.
- **Consequences:** In-app purchase flows, SKU details, and subscription state are decoupled behind clean domain interfaces. 100% test coverage achieved with `BillingRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 140: StatsController Strangling via Clean DataSources & NetworkStatsRepositoryImpl
- **Context:** In Telegram Android, network traffic byte counters (mobile, wifi, roaming, calls, audio, video, messages, files, photos, total) were tracked and persisted by `StatsController.java` (~290 lines). `StatsController` directly managed low-level file I/O on `stats.dat`, 8x3 2D long arrays (`sentItems`, `receivedItems`, `sentBytes`, `receivedBytes`), and reset timestamps.
- **Decision:** Apply the Strangler Fig pattern to `StatsController`:
  1. Implement `NetworkStatsRemoteDataSource`:
     - Extends `BaseRemoteDataSource(currentAccount)` and provides extension points for remote network stats synchronization.
  2. Implement `NetworkStatsLocalDataSource`:
     - Encapsulates reading/writing byte counts across network types and data types, reset timestamps, in-memory matrices, and fallback for unit tests.
  3. Implement `NetworkStatsRepositoryImpl`:
     - Implements `NetworkStatsRepository`, managing stats retrieval, byte count updates, reset operations, and reactive `observeNetworkStats()` StateFlow.
  4. Update `NetworkContainer` and `AccountFeatureContainer` to instantiate and provide `NetworkStatsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `StatsController.getNetworkStatsRepository(account)` and instance `getRepository()`.
- **Consequences:** Network traffic counters and statistics are decoupled behind clean domain interfaces. 100% test coverage achieved with `NetworkStatsRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 139: AiTonesController Strangling via Clean DataSources & AiTonesRepositoryImpl
- **Context:** In Telegram Android, AI Compose tone styling presets (e.g. formal, friendly, creative) and custom user prompts were managed by `AiTonesController.java` (~160 lines). `AiTonesController` coupled MTProto RPCs (`TL_aicompose.getTones`, `unsaveTone`), Base64 TL serialization stored in SharedPreferences (`ai_styles`), in-memory caches of `AiComposeTone` structures, and broadcasts to `NotificationCenter.loadedAiComposeTones`. Legacy UI components accessed `AiTonesController.getInstance(account)`.
- **Decision:** Apply the Strangler Fig pattern to `AiTonesController`:
  1. Implement `AiTonesRemoteDataSource`:
     - Encapsulates MTProto RPCs for fetching tones (`TL_aicompose.getTones`) and unsaving tones (`unsaveTone`) using `BaseRemoteDataSource(currentAccount)`.
  2. Implement `AiTonesLocalDataSource`:
     - Encapsulates Base64 TL serialization to SharedPreferences (`ai_styles`), self-clearing safe in-memory caching, tone deduplication, and tone mutation (`addTone`, `removeTone`, `editTone`).
  3. Implement `AiTonesRepositoryImpl`:
     - Implements `AiTonesRepository`, coordinating remote fetching with hash checks, local persistence, safe notifications dispatch, and reactive `observeTones()` via `NotificationCenterFlowBridge`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and provide `AiTonesRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `AiTonesController.getAiTonesRepository(account)` and instance `getAiTonesRepository()`.
- **Consequences:** AI tones loading, editing, local caching, and reactive observation are cleanly decoupled behind testable domain contracts and data sources. 100% test coverage achieved with `AiTonesRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 138: CaptchaController Strangling via Clean DataSources & CaptchaRepositoryImpl
- **Context:** In Telegram Android, bot verification and human challenge prompts using Google reCAPTCHA Enterprise were coordinated by `CaptchaController.java` (~175 lines). `CaptchaController` coupled Google Play Services / Cloud reCAPTCHA Client APIs, MTProto token dispatch, request tracking and deduplication (`requestTasks`), and callbacks into Telegram dialogs.
- **Decision:** Apply the Strangler Fig pattern to `CaptchaController`:
  1. Implement `CaptchaRemoteDataSource`:
     - Encapsulates reCAPTCHA Enterprise token execution and MTProto token verification via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `CaptchaLocalDataSource`:
     - Manages active request deduplication, in-flight challenge tokens, and state flow tracking.
  3. Implement `CaptchaRepositoryImpl`:
     - Implements `CaptchaRepository`, coordinating reCAPTCHA task execution, verification responses, and error handling.
  4. Update `SecurityContainer` and `AccountFeatureContainer` to instantiate and provide `CaptchaRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `CaptchaController.getCaptchaRepository(account)` and instance `getCaptchaRepository()`.
- **Consequences:** reCAPTCHA challenge execution and token verification are cleanly decoupled behind testable domain contracts and data sources. 100% test coverage achieved with `CaptchaRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 137: UnconfirmedAuthController Strangling via Clean DataSources & UnconfirmedAuthRepositoryImpl
- **Context:** In Telegram Android, pending unconfirmed login authorizations from new devices and session verification alerts were managed by `UnconfirmedAuthController.java` (~135 lines). `UnconfirmedAuthController` coupled MTProto RPCs (`TL_auth.confirmUnconfirmedAuth`, `TL_auth.denyUnconfirmedAuth`), SQLite database storage (`MessagesStorage`), in-memory pending authorizations caches (`ArrayList<TL_auth_unconfirmedAuth>`), and `NotificationCenter` broadcasts (`unconfirmedAuthUpdate`).
- **Decision:** Apply the Strangler Fig pattern to `UnconfirmedAuthController`:
  1. Implement `UnconfirmedAuthRemoteDataSource`:
     - Encapsulates MTProto RPC execution: confirming and denying unconfirmed authorizations via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `UnconfirmedAuthLocalDataSource`:
     - Encapsulates SQLite persistence via `MessagesStorage`, thread-safe in-memory cache inspection, and headless fallback.
  3. Implement `UnconfirmedAuthRepositoryImpl`:
     - Implements `UnconfirmedAuthRepository`, coordinating confirm/deny actions, state queries, safe notification posting, and reactive `observePendingAuths()` via `NotificationCenterFlowBridge`.
  4. Update `SecurityContainer` and `AccountFeatureContainer` to instantiate and provide `UnconfirmedAuthRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `UnconfirmedAuthController.getUnconfirmedAuthRepository(account)` and instance `getUnconfirmedAuthRepository()`.
- **Consequences:** Unconfirmed session verification and authorization state are cleanly decoupled behind testable domain contracts and data sources. 100% test coverage achieved with `UnconfirmedAuthRepositoryImplTest.kt` passing and full backward compatibility preserved.

### ADR 136: CacheByChatsController Strangling via Clean DataSources & CacheByChatsRepositoryImpl
- **Context:** In Telegram Android, automatic media cache retention policies, cleanup rules by chat categories (User, Group, Channel, Stories), and per-dialog retention exceptions were managed by `CacheByChatsController.java` (~215 lines) in `org.telegram.messenger`. `CacheByChatsController` coupled `SharedConfig` global preferences (`keep_media_type_*`), `UserConfig` account preferences storing binary hex-encoded `ByteBuffer` arrays of exceptions (`keep_media_exceptions_*`), and SQLite dialog lookups via `FileDatabase`. UI activities like `CacheControlActivity` directly instantiated `new CacheByChatsController(currentAccount)`.
- **Decision:** Apply the Strangler Fig pattern to `CacheByChatsController`:
  1. Implement `CacheByChatsRemoteDataSource`:
     - Provides MTProto cache retention synchronization extension points via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `CacheByChatsLocalDataSource`:
     - Encapsulates reading/writing retention periods (`getKeepMedia`, `setKeepMedia`), hex-encoded exception lists (`getKeepMediaExceptions`, `saveKeepMediaExceptions`), and fallback in-memory cache for headless environments.
  3. Implement `CacheByChatsRepositoryImpl`:
     - Implements `CacheByChatsRepository`, managing category duration lookups/updates, per-dialog exception mutations, and reactive `observeConfig()` via `StateFlow`.
  4. Update `MediaContainer` and `AccountFeatureContainer` to instantiate and provide `CacheByChatsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `CacheByChatsController.getCacheByChatsRepository(account)` and instance `getCacheByChatsRepository()`.
- **Consequences:** Cache retention periods and per-chat exception rules are cleanly isolated behind testable domain contracts and repository implementations. Full backward compatibility is preserved for all legacy Java callers, with 100% unit tests passing and full APK assembly and device installation validated.

### ADR 135: FileRefController Strangling via Clean DataSources & FileRefRepositoryImpl
- **Context:** In Telegram Android, refreshing expired MTProto file references (e.g. `FILE_REFERENCE_EXPIRED`) across messages, stories, stickers, wallpapers, saved gifs, and bots was centralized in `FileRefController.java` (~2347 lines). `FileRefController` directly coupled low-level `ConnectionsManager` RPCs, multi-level request deduplication structures (`locationRequester`, `parentRequester`), in-memory cached responses (`responseCache`), and specialized waiter lists (`wallpaperWaiters`, `savedGifsWaiters`, `recentStickersWaiter`, `favStickersWaiter`).
- **Decision:** Apply the Strangler Fig pattern to `FileRefController`:
  1. Implement `FileRefRemoteDataSource`:
     - Encapsulates MTProto file reference requests and cancellation via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `FileRefLocalDataSource`:
     - Encapsulates parent key extraction, error classification (`isFileRefError`), cached reference application, and headless in-memory cache fallback.
  3. Implement `FileRefRepositoryImpl`:
     - Implements `FileRefRepository`, managing request deduplication, cache expiration, metrics tracking, and reactive `observeStats()` via `StateFlow`.
  4. Update `MediaContainer` and `AccountFeatureContainer` to instantiate and provide `FileRefRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `FileRefController.getFileRefRepository(account)` and instance `getFileRefRepository()`.
- **Consequences:** File reference renewals, request deduplication, and cache lifecycle are cleanly isolated behind testable domain contracts. Full backward compatibility is preserved for all legacy Java callers, with 100% unit tests passing and full APK assembly validated.

### ADR 134: ProxyRotationController Strangling via Clean DataSources & ProxyRepositoryImpl
- **Context:** In Telegram Android, proxy configuration, latency checks, and automatic background proxy rotation were managed by `ProxyRotationController.java` (~130 lines) in conjunction with `SharedConfig` and `ConnectionsManager`. `ProxyRotationController` directly coupled Android `SystemClock`, `SharedPreferences` writes (`MessagesController.getGlobalMainSettings()`), `NotificationCenter` broadcasts (`proxySettingsChanged`, `proxyCheckDone`, `proxyChangedByRotation`), and native proxy switching via `ConnectionsManager.setProxySettings`.
- **Decision:** Apply the Strangler Fig pattern to `ProxyRotationController`:
  1. Implement `ProxyRemoteDataSource`:
     - Encapsulates proxy latency checks (`checkProxyPing`) via coroutine-wrapped `ConnectionsManager.checkProxy` and proxy connection settings application (`applyProxySettings`).
  2. Implement `ProxyLocalDataSource`:
     - Encapsulates proxy list persistence (`SharedConfig.proxyList`), active proxy state (`SharedConfig.currentProxy`), rotation timeouts, SharedPreferences configuration, and headless in-memory fallback.
  3. Implement `ProxyRepositoryImpl`:
     - Implements `ProxyRepository`, coordinating proxy CRUD operations, connection enabling/disabling, rotation toggles, latency checks, and reactive `observeProxySettings()` via `NotificationCenterFlowBridge`.
  4. Update `NetworkContainer` and `AccountFeatureContainer` to instantiate and provide `ProxyRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `ProxyRotationController.getProxyRepository(account)` and instance `getProxyRepository()`.
- **Consequences:** Proxy management, latency checking, rotation scheduling, and reactive settings observation are cleanly decoupled behind testable domain interfaces. Full backward compatibility is preserved for all legacy Java callers, with 100% unit tests passing and full APK assembly validated.

### ADR 133: TopicsController Strangling via Clean DataSources & TopicsRepositoryImpl
- **Context:** In Telegram Android, forum topics management was centralized in `TopicsController.java` (~1400 lines). `TopicsController` directly handled MTProto RPCs (`TL_forum.TL_messages_getForumTopics`, `TL_forum.TL_messages_editForumTopic`, `TL_forum.TL_messages_updatePinnedForumTopic`, `TL_forum.TL_messages_deleteTopicHistory`, `TL_forum.TL_messages_reorderPinnedForumTopics`, `TLRPC.TL_messages_readReactions`), SQLite persistence (`MessagesStorage.loadTopics`, `saveTopics`, `removeTopic`, `removeTopics`, `updateTopicData`), in-memory topic collections (`LongSparseArray<ArrayList<TLRPC.TL_forumTopic>> topicsByChatId`), forum unread counters, and `NotificationCenter` broadcasts (`topicsDidLoaded`). UI activities like `TopicsFragment` directly invoked `MessagesController.getInstance(account).getTopicsController()`.
- **Decision:** Apply the Strangler Fig pattern to `TopicsController`:
  1. Implement `TopicsRemoteDataSource`:
     - Encapsulates MTProto RPC execution: `getForumTopics`, `getSavedDialogsForForum`, `editForumTopic`, `updatePinnedForumTopic`, `deleteTopicHistory`, `reorderPinnedForumTopics`, and `readReactions` via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `TopicsLocalDataSource`:
     - Encapsulates interaction with `TopicsController` in-memory structures, SQLite persistence via `MessagesStorage`, and fallback in-memory caching for headless JVM environments.
  3. Implement `TopicsRepositoryImpl`:
     - Implements `TopicsRepository`, coordinating topic listing, topic lookups, pagination/preloading, reloading, topic state toggles (close, pin, show), deletion, pinned reordering, reaction read marking, forum unread counters, and reactive `observeTopics(chatId)` & `observeForumUnreadCount(chatId)` via `NotificationCenterFlowBridge`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and provide `TopicsRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `TopicsController.getTopicsRepository(account)` and instance `getTopicsRepository()`.
- **Consequences:** Forum topics lifecycle, caching, mutations, and reactive flows are cleanly decoupled behind testable domain contracts and repository implementations. Full backward compatibility is preserved for all legacy Java callers, with 100% unit tests passing and full APK assembly validated.

### ADR 132: TranslateController Strangling via Clean DataSources & TranslationRepositoryImpl
- **Context:** In Telegram Android, message and chat translation and language settings were managed across `TranslateController.java` (~2450 lines), `LocaleController.java`, and UI activities like `RestrictedLanguagesSelectActivity`. `TranslateController` directly handled MTProto RPCs (`TLRPC.TL_messages_translateText`), SharedPreferences settings (`translate_button`, `translate_chat_button`), dialog translation states in `LongSparseArray`, restricted language sets, and unread dialog message translation caches. Legacy UI components directly invoked `TranslateController.getInstance(account)`.
- **Decision:** Apply the Strangler Fig pattern to `TranslateController`:
  1. Implement `TranslationRemoteDataSource`:
     - Encapsulates MTProto RPC execution `translateText(text, toLanguage)` (`TLRPC.TL_messages_translateText`) via `BaseRemoteDataSource(currentAccount)`.
  2. Implement `TranslationLocalDataSource`:
     - Encapsulates SharedPreferences translation settings, restricted language sets, dialog translation status and target language lookups, available `LocaleInfo` enumerations, and application language switching.
  3. Implement `TranslationRepositoryImpl`:
     - Implements `TranslationRepository`, coordinating MTProto text translation, dialog translation toggles, target language updates, do-not-translate language exception management, available languages discovery, and reactive `observeTranslateSettings` & `observeDialogTranslationState(dialogId)` via `NotificationCenterFlowBridge`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and provide `TranslationRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `TranslateController.getTranslationRepository(account)` and instance `getTranslationRepository()`.
- **Consequences:** Translation operations and preferences are cleanly decoupled behind testable domain contracts and reactive flows. Full backward compatibility is preserved for all legacy Java callers, with 100% unit tests passing and APK assembly validated.

### ADR 131: FactCheckController Strangling via Clean DataSources & FactCheckRepositoryImpl
- **Context:** In Telegram Android, message fact-checks and community annotations were managed by `FactCheckController.java` (~590 lines). `FactCheckController` tightly coupled MTProto RPCs (`TLRPC.TL_getFactCheck`, `TLRPC.TL_editFactCheck`, `TLRPC.TL_deleteFactCheck`), SQLite database caching (`MessagesStorage.getDatabase()` queries on the `fact_checks` table), in-memory caching (`LongSparseArray<TLRPC.TL_factCheck>`), character length limits, and heavy Android UI dialog construction (`openFactCheckEditor` with `AlertDialog`, custom `EditTextCaption`, spans, haptics, and bulletin messages).
- **Decision:** Apply the Strangler Fig pattern to `FactCheckController`:
  1. Implement `FactCheckRemoteDataSource`:
     - Encapsulates MTProto RPC execution: `getFactCheck(peer, msgId)` (`TLRPC.TL_getFactCheck`), `editFactCheck(peer, msgId, text)` (`TLRPC.TL_editFactCheck`), and `deleteFactCheck(peer, msgId)` (`TLRPC.TL_deleteFactCheck`) using `BaseRemoteDataSource(currentAccount)`.
  2. Implement `FactCheckLocalDataSource`:
     - Encapsulates SQLite queries on the `fact_checks` table, memory caching (`LongSparseArray`), character limit calculations, and `MessagesController.processUpdates()`.
  3. Implement `FactCheckRepositoryImpl`:
     - Implements `FactCheckRepository`, coordinating memory-first fact-check retrieval with SQLite database fallback, remote MTProto loading, fact-check application/editing, deletion, and reactive `observeFactCheckLoaded(dialogId, messageId)` via `NotificationCenterFlowBridge`.
  4. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and provide `FactCheckRepositoryImpl` alongside clean data sources.
  5. Introduce strangler boundary: `FactCheckController.getFactCheckRepository(account)` and cache accessors (`getCachedFactCheck`, `putCachedFactCheck`).
- **Consequences:** Message fact-checking, editing, persistence, and reactive updates are cleanly decoupled behind testable domain contracts. Full backward compatibility is preserved for all legacy Java callers, with 100% unit tests passing and APK assembly validated.

### ADR 130: ChatThemeController Strangling via Clean DataSources & ChatThemeRepositoryImpl
- **Context:** In Telegram Android, chat themes, emoji themes, and dialog wallpapers were managed across `ChatThemeController.java` (~430 lines) and `EmojiThemes.java`. `ChatThemeController` tightly coupled MTProto RPCs (`TL_account.getChatThemes`, `TL_messages.setChatTheme`), SharedPreferences serialization of themes and hash values (`chattheme_pref`), in-memory caches (`allChatThemes`, `dialogsThemesMap`), wallpaper saving (`saveChatWallpaper`), and `processUpdates()`. UI components (`ChatActivity`, `ChatAttachAlert`, `ThemePreviewActivity`) directly accessed `ChatThemeController.getInstance(account)`. Additionally, `ThemeKey.java` and `EmojiThemes.java` had static dependencies on unmocked Android framework classes (`android.text.TextUtils`, `Theme.java` drawable initialization) preventing clean JVM unit testing.
- **Decision:** Apply the Strangler Fig pattern to `ChatThemeController`:
  1. Implement `ChatThemeRemoteDataSource`:
     - Encapsulates MTProto RPC execution: `getChatThemes(hash)` (`TL_account.getChatThemes`) and `setChatTheme(peer, inputTheme)` (`TL_messages.setChatTheme`) using `BaseRemoteDataSource(currentAccount)`.
  2. Implement `ChatThemeLocalDataSource`:
     - Encapsulates SharedPreferences theme persistence (`chattheme_pref`), in-memory `ChatThemeController` theme lookups (`getEmojiThemes`, `getDialogTheme`, `getDialogWallpaper`), `InputPeer` resolution, wallpaper persistence, and updates dispatching (`processUpdates`).
  3. Implement `ChatThemeRepositoryImpl`:
     - Implements `ChatThemeRepository`, orchestrating cache-first theme retrieval with 2-hour reload throttling and resilient offline fallback, dialog theme setting with `ThemeKey` mapping, chat wallpaper persistence/clearing, and reactive `observeDialogTheme(dialogId)` via `NotificationCenterFlowBridge` listening to `chatThemeUpdated`.
  4. Decouple `ThemeKey.java` and `EmojiThemes.java` from unmocked Android runtime dependencies:
     - Replace `TextUtils.isEmpty` and `TextUtils.equals` with standard Java string checks and `java.util.Objects.equals`.
     - Lazy-initialize `previewColorKeys` in `EmojiThemes.java` to avoid triggering Android `Theme` class loading and `Paint` allocation during JVM classloading.
  5. Update `MessagingContainer` and `AccountFeatureContainer` to instantiate and provide `ChatThemeRepositoryImpl` alongside clean data sources.
  6. Introduce strangler boundary: `ChatThemeController.getChatThemeRepository(account)`.
- **Consequences:** Chat theme loading, theme application, wallpaper management, and dialog theme observation are cleanly decoupled behind testable domain contracts and reactive flows. Full backward compatibility is preserved for all legacy Java callers, with 100% unit tests passing and APK assembly validated.

### ADR 129: MemberRequestsController Strangling via Clean DataSources & JoinRequestsRepositoryImpl
- **Context:** In Telegram Android, chat join requests and pending invite importers were managed by `MemberRequestsController.java` (~200 lines). `MemberRequestsController` coupled MTProto RPCs (`TLRPC.TL_messages_getChatInviteImporters`, `TLRPC.TL_messages_hideChatJoinRequest`, `TLRPC.TL_messages_hideAllChatJoinRequests`), in-memory cache of initial invite importers (`firstImportersCache` as `LongSparseArray<TLRPC.TL_messages_chatInviteImporters>`), updates processing via `MessagesController.processUpdates()`, and `ChatFull.requests_pending` synchronization. UI components (`MemberRequestsDelegate`, `ChatUsersActivity`) and legacy controllers directly accessed `MemberRequestsController.getInstance(account)`.
- **Decision:** Apply the Strangler Fig pattern to `MemberRequestsController`:
  1. Implement `JoinRequestsRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getChatInviteImporters(peer, requested, limit, query, offsetUser, offsetDate)`, `hideChatJoinRequest(peer, inputUser, approved)`, and `hideAllChatJoinRequests(peer, inviteLink, approved)` using `BaseRemoteDataSource(currentAccount)`.
  2. Implement `JoinRequestsLocalDataSource`:
     - Encapsulates `ChatFull` access, `InputPeer` and `InputUser` resolution, cached invite importers via `MemberRequestsController.firstImportersCache` with bidirectional synchronization (`getCachedImporters`, `putCachedImporters`), and updates application (`processUpdates`).
  3. Implement `JoinRequestsRepositoryImpl`:
     - Implements `JoinRequestsRepository`, coordinating remote RPC execution with local cache updates, handling approvals/dismissals of single or batch join requests, reactive `observePendingRequests()` via `NotificationCenterFlowBridge`, and domain model mapping (`ChatPendingRequestsModel`, `JoinRequestsListModel`, `JoinRequestModel`) via `JoinRequestMapper`.
  4. Expose `putCachedImporters(chatId, importers)` in `MemberRequestsController.java` to allow clean data sources to synchronize the legacy in-memory cache.
  5. Update `SocialContainer` and `AccountFeatureContainer` to wire `JoinRequestsRepositoryImpl` as the primary default implementation while supporting custom mocks for testing.
  6. Introduce strangler boundary: `MemberRequestsController.getJoinRequestsRepository(account)`.
- **Consequences:** All chat join request operations, approvals, dismissals, and pending counters are cleanly decoupled behind testable domain contracts and data sources. 100% test coverage achieved with `JoinRequestsRepositoryImplTest.kt` passing, full backwards compatibility preserved, and verified with APK assembly (`assembleAfatDebug`).

### ADR 128: BirthdayController & ChannelBoostsController Strangling via Clean DataSources & Repository Implementations
- **Context:** In Telegram Android, contact birthdays tracking and channel boosting operations were managed across `BirthdayController.java` (~310 lines) and `ChannelBoostsController.java` (~190 lines). `BirthdayController` coupled MTProto RPCs (`TL_account.getBirthdays`), raw binary serialization of `TL_birthdays` into hex strings stored in SharedPreferences (`bday_contacts`, `bday_check`, `bday_hidden`), SQLite database updates in `MessagesStorage`, in-memory users cache in `MessagesController`, and global event notifications on `NotificationCenter.premiumPromoUpdated`. `ChannelBoostsController` coupled MTProto RPCs (`TL_stories.TL_premium_getBoostsStatus`, `TL_stories.TL_premium_getMyBoosts`, `TL_stories.TL_premium_applyBoost`), UI alert dialogs (`AlertDialog.Builder`), global bulletins (`BulletinFactory`), and slot replacement arithmetic with negative dialog ID negations.
- **Decision:** Apply the Strangler Fig pattern to both social controllers:
  1. Implement `BirthdayRemoteDataSource`:
     - Encapsulates MTProto RPC request `TL_account.getBirthdays()` via `BaseRemoteDataSource(currentAccount)` with coroutine cancellation support.
  2. Implement `BirthdayLocalDataSource`:
     - Encapsulates cached state access (`state`), cache invalidation timing checks (`shouldCheckBirthdays`), SharedPreferences persistence via `applyResponse(response)`, and today's birthday checks (`isToday(userId)`, `hasBirthdaysToday()`).
  3. Implement `BirthdaysRepositoryImpl`:
     - Implements `BirthdaysRepository`, managing reactive `observeBirthdays()` via `NotificationCenterFlowBridge` observing `premiumPromoUpdated`, cache invalidation logic, and domain mapping via `BirthdayMapper`.
  4. Implement `BoostsRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getBoostsStatus(peer)`, `getMyBoosts()`, and `applyBoost(peer, slots)` with MTProto flags `RequestFlagInvokeAfter` or `RequestFlagFailOnServerErrors`.
  5. Implement `BoostsLocalDataSource`:
     - Encapsulates `InputPeer` resolution for channel/chat dialog IDs, user and chat caching in `MessagesController`, and Main-thread boost slot eligibility evaluation.
  6. Implement `BoostsRepositoryImpl`:
     - Implements `BoostsRepository`, managing negative dialog ID normalization, MTProto RPC orchestration, local caching, and domain mapping via `BoostMapper`.
  7. Update `SocialContainer` and `AccountFeatureContainer` to wire `BirthdaysRepositoryImpl` and `BoostsRepositoryImpl` as primary defaults while maintaining full compatibility with legacy overrides.
  8. Introduce strangler hooks: `BirthdayController.getBirthdaysRepository(account)` and `ChannelBoostsController.getBoostsRepository(account)`.
- **Consequences:** All contact birthdays and channel boost operations are cleanly decoupled behind testable domain contracts and clean data sources. 100% test coverage achieved with `BirthdaysRepositoryImplTest.kt` and `BoostsRepositoryImplTest.kt` passing, full backwards compatibility preserved, and verified with APK assembly (`assembleAfatDebug`).

### ADR 127: PasskeysController & FingerprintController Strangling via Clean DataSources & Repository Implementations
- **Context:** In Telegram Android, passkeys and biometrics authentication were managed across `PasskeysController.java` (~331 lines) and `FingerprintController.java` (~145 lines). Passkeys registration challenges (`TL_account.initPasskeyRegistration`), passkey registration (`TL_account.registerPasskey`), passkey deletion (`TL_account.deletePasskey`), and listing (`TL_account.getPasskeys`) were entangled with CredentialManager Android API calls, JSON parsing, and UI alert dialogs. Biometrics and keystore RSA key pair generation (`AndroidKeyStore`, `KeyProperties`, `FingerprintManagerCompat`) communicated via global notifications (`NotificationCenter.didGenerateFingerprintKeyPair`) and static singletons.
- **Decision:** Apply the Strangler Fig pattern to both security controllers:
  1. Implement `PasskeysRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getPasskeys()`, `deletePasskey(id)`, `initPasskeyRegistration()`, and `registerPasskey(credential)` with coroutine cancellation support.
  2. Implement `PasskeysLocalDataSource`:
     - Encapsulates OS version checks (`Build.VERSION.SDK_INT >= 28 && BuildVars.SUPPORTS_PASSKEYS`) and server-driven passkey quota limits (`passkeysAccountPasskeysMax`).
  3. Implement `PasskeysRepositoryImpl`:
     - Implements `PasskeysRepository`, managing reactive `observePasskeys()`, cache bypass with `force = true`, and state synchronization on passkey deletions.
  4. Implement `BiometricsLocalDataSource`:
     - Encapsulates hardware detection, biometric enrollment, Android KeyStore key readiness, permanent key invalidation detection, and key deletion.
  5. Implement `BiometricsRepositoryImpl`:
     - Implements `BiometricsRepository`, converting `NotificationCenter.didGenerateFingerprintKeyPair` events into reactive `observeKeyState()` flows on `Dispatchers.IO` with fallback mapping across all Android versions.
  6. Wire into `SecurityContainer` and `AccountFeatureContainer` as the default implementations.
  7. Add strangler hooks `PasskeysController.getPasskeysRepository(account)` and `FingerprintController.getBiometricsRepository()`.
- **Consequences:** All passkeys and biometric authentication operations are decoupled into clean, testable layers with 100% test coverage (`PasskeysRepositoryImplTest.kt`, `BiometricsRepositoryImplTest.kt`) passing and verified with clean APK assembly (`assembleAfatDebug`).

### ADR 126: LocationController Strangling via BaseDataSources & LocationRepositoryImpl
- **Context:** In Telegram Android, `LocationController.java` (~1,419 lines) manages GPS coordinates, active live location sharings, proximity alerts, peer location cache (`locationsCache`), and SQLite database persistence (`sharing_locations`). Direct calls to `LocationController.getInstance(account)` and raw MTProto requests (`TL_messages_getRecentLocations`, `TL_messages_editMessage`, `TL_messages_readMessageContents`) were scattered across UI fragments and background services.
- **Decision:** Apply the Strangler Fig pattern to `LocationController`:
  1. Implement `LocationRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getRecentLocations(...)` (`TL_messages_getRecentLocations`), `stopLiveLocation(...)` (`TL_messages_editMessage` with stopped geo live), `editLiveLocation(...)` (`TL_messages_editMessage`), and `markLiveLocationsAsRead(...)` with coroutine cancellation support.
  2. Implement `LocationLocalDataSource`:
     - Dispatches SQLite database mutations (`saveProximity`, `removeSharing`, `clearAllSharings`, `putUsersAndChats`) safely to `MessagesStorage` via `Dispatchers.IO`.
     - Handles in-memory `sharingLocationsUI`, `locationsCache`, and `lastKnownLocation` thread-safe access.
     - Provides message sending dispatch via `SendMessagesHelper`.
  3. Implement `LocationRepositoryImpl`:
     - Implements `LocationRepository`, providing reactive flows `observeActiveSharings()`, `observePeerLocations()`, and `observeLastKnownLocation()`.
     - Safely dispatches `NotificationCenter.liveLocationsChanged`, `liveLocationsCacheChanged`, and `newLocationAvailable` without failing in headless JVM test environments.
  4. Wire into `SocialContainer` and `AccountFeatureContainer` as the default `LocationRepository` implementation.
  5. Add strangler hook `getLocationRepository()` in `LocationController.java` allowing callers to migrate incrementally.
- **Consequences:** Location sharing, GPS tracking, and peer coordinate synchronization are cleanly encapsulated into testable data sources and repository, with full unit test coverage (`LocationRepositoryImplTest.kt`) passing and verified with clean APK assembly (`assembleAfatDebug`).

### ADR 125: DownloadController Strangling via BaseDataSources & DownloadManagerRepositoryImpl
- **Context:** In Telegram Android, `DownloadController.java` (~1,810 lines) manages global auto-download presets (Wi-Fi, cellular, roaming), in-memory download queues (`downloadingFiles`, `recentDownloadingFiles`, `unviewedDownloads`), speed metrics calculation, and auto-download configuration persistence in `SharedPreferences` and MTProto (`TL_account.getAutoDownloadSettings`, `TL_account.saveAutoDownloadSettings`). UI components and background loaders directly touched `DownloadController.getInstance(currentAccount)`, creating high coupling between media loading, network policy, and presentation.
- **Decision:** Apply the Strangler Fig pattern to `DownloadController`:
  1. Implement `DownloadManagerRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getAutoDownloadConfig()` (`TL_account.getAutoDownloadSettings`) and `saveAutoDownloadSettings(settings, low, high)` (`TL_account.saveAutoDownloadSettings`) with coroutine cancellation support.
  2. Implement `DownloadManagerLocalDataSource`:
     - Dispatches SQLite database mutations (`clearRecentDownloadedDocuments`, `deleteRecentDocument`) to `MessagesStorage` via `Dispatchers.IO`.
     - Handles SharedPreferences preset serialization and deserialization (`mobilePreset`, `wifiPreset`, `roamingPreset`).
     - Provides thread-safe, non-throwing access to in-memory queues and delegations in `DownloadController`.
  3. Implement `DownloadManagerRepositoryImpl`:
     - Implements `DownloadManagerRepository`, managing thread-safe in-memory maps (`downloadingMap`, `recentMap`, `unviewedMap`), active network presets, and reactive flows `observeState()`, `observeDownloadingFiles()`, and `observeRecentFiles()`.
     - Calculates dynamic download speeds and coordinates preset updates between local SharedPreferences and remote MTProto.
  4. Wire into `MediaContainer` and `AccountFeatureContainer` as the default `DownloadManagerRepository` implementation.
  5. Add strangler hook `getDownloadManagerRepository()` in `DownloadController.java` allowing callers to migrate incrementally.
- **Consequences:** Auto-download policies, queue state, and remote configuration syncing are encapsulated in pure testable data sources and repository, with full unit test coverage (`DownloadManagerRepositoryImplTest.kt`) passing and verified with clean APK assembly (`assembleAfatDebug`).

### ADR 124: SecretChatHelper Strangling via BaseDataSources & SecretChatRepositoryImpl
- **Context:** In Telegram Android, `SecretChatHelper.java` is a core cryptographic and message dispatch controller (~2,050 lines) managing Diffie-Hellman key exchange, encrypted layer negotiation, TTL timers, secret holes checking, and SQLite secret chat persistence (`MessagesStorage`). Direct calls to `SecretChatHelper.getInstance(account)` and raw MTProto requests (`TL_messages_requestEncryption`, `TL_messages_acceptEncryption`, `TL_messages_discardEncryption`) were scattered across the codebase.
- **Decision:** Apply the Strangler Fig pattern to `SecretChatHelper`:
  1. Implement `SecretChatRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getDhConfig(version)` (`TL_messages_getDhConfig`), `requestEncryption(...)` (`TL_messages_requestEncryption`), `acceptEncryption(...)` (`TL_messages_acceptEncryption`), `discardEncryption(...)` (`TL_messages_discardEncryption`), and `sendEncryptedService(...)` with coroutine cancellation support.
  2. Implement `SecretChatLocalDataSource`:
     - Dispatches SQLite database mutations (`updateEncryptedChatTTL`, `updateEncryptedChat`, `putEncryptedChat`, `saveSecretParams`) safely to `MessagesStorage` via `Dispatchers.IO`.
     - Provides thread-safe, non-throwing access to in-memory encrypted chat caches, user profiles, and crypto helpers.
  3. Implement `SecretChatRepositoryImpl`:
     - Implements `SecretChatRepository`, providing clean reactive flows `observeSecretChat()` and `observeSecretChats()` hooked into `encryptedChatUpdated`, `encryptedChatCreated`, and `dialogsNeedReload`.
     - Provides bitwise pure calculations for encrypted dialog IDs (`isEncryptedDialog`, `makeEncryptedDialogId`, `getEncryptedChatId`).
  4. Wire into `SecurityContainer` and `AccountFeatureContainer` as the default `SecretChatRepository` implementation.
  5. Add strangler hook `getSecretChatRepository()` in `SecretChatHelper.java` allowing callers to migrate incrementally.
- **Consequences:** Encrypted chat lifecycle, TTL management, and MTProto encryption RPCs are cleanly encapsulated into testable data sources and repository, with full unit test coverage (`SecretChatRepositoryImplTest.kt`) passing and verified on physical hardware (`RZCW41MQNVV`, Samsung Galaxy A54 5G).

### ADR 123: NotificationsController Strangling via BaseDataSources & NotificationsRepositoryImpl
- **Context:** In Telegram Android, `NotificationsController.java` is an enormous monolithic controller (>3,500 lines) handling push notifications, notification settings (mute timers, vibration, sound, LED color, priority), in-app notifications, badge counting, reaction alerts, and sync with MTProto via `account.updateNotifySettings`. Settings and mute states were split across `SharedPreferences`, `MessagesStorage` SQLite tables (`dialogs`, `updateMutedDialogsFiltersCounters`), and static in-memory hashes, tightly coupling UI, background services, and storage.
- **Decision:** Apply the Strangler Fig pattern to `NotificationsController`:
  1. Implement `NotificationsRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `updateNotifySettings(...)` (`TL_account_updateNotifySettings`), `setReactionsNotifySettings(...)` (`TL_account_setReactionsNotifySettings`), `setContactSignUpNotification(...)` (`TL_account_setContactSignUpNotification`), `getNotifyExceptions(...)` (`TL_account_getNotifyExceptions`), and `resetNotifySettings()` (`TL_account_resetNotifySettings`) with cancellation support.
  2. Implement `NotificationsLocalDataSource`:
     - Safely manages `SharedPreferences` reads/writes for global notifications, sound, vibration, and mute times.
     - Dispatches SQLite database operations (`updateMutedDialogsFiltersCounters`, `setDialogFlags`) via `MessagesStorage` safely on `Dispatchers.IO`.
     - Updates in-memory mute states, auto-delete, and badging in `NotificationsController` safely.
  3. Implement `NotificationsRepositoryImpl`:
     - Implements `NotificationsRepository`, self-contained with decoupled domain/data constants (`KEY_PRIVATE`, `KEY_GROUP`, `KEY_CHANNEL`, `TYPE_PRIVATE`, etc.) to prevent static initialization cycles in headless test environments.
     - Emits reactive updates via `observeNotificationSettings()` and `observeGlobalSettings()` leveraging `NotificationCenterFlowBridge` with safe headless fallbacks.
  4. Wire into `SystemContainer` and `AccountFeatureContainer` as the default `NotificationsRepository` implementation.
  5. Add strangler hook `getNotificationsRepository()` in `NotificationsController.java` allowing callers to migrate incrementally.
- **Consequences:** Notification mutations and settings are cleanly separated into testable remote and local layers, verified by comprehensive unit tests (`NotificationsRepositoryImplTest.kt`, 13 passed) and on-device testing (`RZCW41MQNVV`, Samsung Galaxy A54 5G) with 0 regressions.

### ADR 122: ContactsController Strangling via BaseDataSources & ContactsRepositoryImpl
- **Context:** In Telegram Android, `ContactsController.java` is a massive ~3,130-line monolithic controller managing contacts synchronization, local SQLite storage, system phonebook sync, privacy rules, and contacts dictionary mappings (`contactsDict`, `contactsBookSPhones`). Methods throughout Telegram UI and background sync services directly invoked `ContactsController.getInstance(account).contacts` or made raw calls to `MessagesStorage.putContacts` and `ConnectionsManager.sendRequest`.
- **Decision:** Apply the Strangler Fig pattern to `ContactsController`:
  1. Implement `ContactsRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getContacts(hash)` (`TL_contacts_getContacts`), `addContact(...)` (`TL_contacts_addContact`), `deleteContacts(...)` (`TL_contacts_deleteContacts`), `searchContacts(...)` (`TL_contacts_search`), `resetSavedContacts()` (`TL_contacts_resetSaved`), and `getStatuses()` (`TL_contacts_getStatuses`).
  2. Implement `ContactsLocalDataSource`:
     - Dispatches database mutations (`putContacts`, `deleteContacts`, `putUsersAndChats`) to `MessagesStorage` safely on `Dispatchers.IO`.
     - Provides safe, thread-guarded access to in-memory contacts and user records (`ContactsController.contacts`, `contactsDict`, `MessagesController.getUser`).
  3. Implement `ContactsRepositoryImpl`:
     - Implements `ContactsRepository`, coordinating MTProto RPC synchronization with SQLite persistence and legacy in-memory cache synchronization.
     - Emits reactive updates via `observeContacts(): Flow<List<ContactModel>>` reacting to `NotificationCenter.contactsDidLoad` and `updateInterfaces` without failing under headless test environments lacking an Android Looper.
  4. Wire into `SocialContainer` and `AccountFeatureContainer` as the default `ContactsRepository` implementation.
  5. Add strangler hook `getContactsRepository()` in `ContactsController.java` allowing callers to migrate incrementally.
- **Consequences:** Contacts data access and mutations are cleanly separated into testable remote and local layers, with comprehensive unit test coverage (`ContactsRepositoryImplTest.kt`) passing alongside existing domain tests, verified by real device deployment (`RZCW41MQNVV`) with 0 regressions.

### ADR 121: Folders & Dialog Filters Strangling via BaseDataSources
- **Context:** Chat Folders (Dialog Filters) logic in Telegram Android was scattered across `MessagesController` (in-memory state machine, `dialogFilters` list, and vector requests) and multiple UI activities (`FiltersSetupActivity`, `FilterCreateActivity`, `DialogsActivity`, `FilterTabsView`, `ViewPagerFixed`). These UI classes directly constructed raw `TLRPC.TL_messages_updateDialogFilter` and `TLRPC.TL_messages_updateDialogFiltersOrder` requests and dispatched them through `ConnectionsManager.sendRequest`, creating strong coupling between presentation, MTProto RPC transport, and SQLite database storage.
- **Decision:**
  1. Implement `FoldersRemoteDataSource`:
     - Encapsulates MTProto RPC requests: `getDialogFilters()` (`TL_messages_getDialogFilters`), `updateDialogFilter()` (`TL_messages_updateDialogFilter` for create/update/delete), `updateDialogFiltersOrder()` (`TL_messages_updateDialogFiltersOrder`), and `getSuggestedDialogFilters()` (`TL_messages_getSuggestedDialogFilters`).
  2. Implement `FoldersLocalDataSource`:
     - Dispatches database transactions (`saveDialogFilter`, `deleteDialogFilter`, `saveDialogFiltersOrder`) to `MessagesStorage` via `Dispatchers.IO`.
     - Provides safe, non-throwing in-memory cache inspection for dialog filters and suggested filters.
  3. Implement `FoldersRepositoryImpl`:
     - Coordinates remote MTProto synchronization and local SQLite persistence, while updating in-memory `MessagesController` caches and posting `NotificationCenter.dialogFiltersUpdated` safely without throwing under test environments.
  4. Wire into `MessagingContainer` and `AccountFeatureContainer` as the default repository implementation.
  5. Add strangler hook `getFoldersRepository()` in `MessagesController.java` to allow legacy callers to consume clean domain operations.
- **Consequences:** Eliminates direct MTProto RPC construction from UI screens, cleanly encapsulates folder mutations, and preserves 100% backward compatibility for all existing UI adapters.

### ADR 120: Phase 3 Kickoff — Core Data Sources Infrastructure & SavedMessagesController Strangling
- **Context:** Following the completion of Phase 1 (Domain/Data layer boundaries for all features) and Phase 2 (UI Wiring of ViewModels across all screens), Phase 3 begins the internal strangling of legacy Telegram controllers. Legacy controllers (such as `SavedMessagesController`) conflate MTProto RPC transport, SQLite database persistence (`MessagesStorage`), and in-memory caching in large monolithic classes.
- **Decision:**
  1. Introduce core data source abstractions in `core.data`:
     - `BaseRemoteDataSource`: Executes MTProto RPC requests asynchronously via `ConnectionsManager.sendRequest` with coroutine cancellation (`suspendCancellableCoroutine`) and typed `Result<T>` mapping.
     - `BaseLocalDataSource`: Executes safe SQLite operations against `MessagesStorage` on `Dispatchers.IO` with structured error handling.
  2. Implement clean data sources for `SavedMessages`:
     - `SavedMessagesRemoteDataSource`: MTProto RPC calls (`TL_messages_getSavedDialogs`, `TL_messages_reorderPinnedSavedDialogs`).
     - `SavedMessagesLocalDataSource`: SQLite persistence (`putUsersAndChats`, `putMessages`) and safe in-memory cache reads.
     - `SavedMessagesRepositoryImpl`: Coordinates remote and local data sources, gradually replacing `LegacySavedMessagesRepository`.
  3. Wire `SavedMessagesRepositoryImpl` into `MessagingContainer` and `AccountFeatureContainer` as the default repository, while exposing `getRepository()` in `SavedMessagesController.java` to allow legacy code to consume domain operations.
- **Consequences:** Provides a clean, reusable blueprint for Phase 3 controller strangling. Network and database concerns are isolated without modifying MTProto protocol serialization, maintaining 100% backward compatibility and testability.

### ADR 119: Specialized Screens UI Wiring via Strangler Fig (Folders, VoIP, Privacy, Passcode, Chat)
- **Context:** Following the stabilization of core navigation screens (`DialogsActivity`, `ProfileActivity`, `SettingsActivity`, `LaunchActivity`, `PhotoViewer`), several specialized legacy UI screens remained unwired to the modular Architecture v2 containers:
  1. `FiltersSetupActivity.java` & `FilterCreateActivity.java`: Folder management, suggested filters, and custom chat filter creation.
  2. `VoIPFragment.java`: 1-on-1 audio and video calling overlay view controller.
  3. `PrivacySettingsActivity.java` & `PasscodeActivity.java`: Privacy rules, two-step verification, passkeys, and biometric lock authentication.
  4. `ChatActivity.java`: Specialized chat subsystems: mention autocomplete (`MentionsViewModel`), audio player controls (`AudioPlayerViewModel`), and message fact-check cards (`FactCheckViewModel`).
- **Decision:** Wire these screens via the Strangler Fig pattern strictly honoring the **Single Execution Principle**:
  1. `FiltersSetupActivity` & `FilterCreateActivity`: Connect `FoldersViewModel`. Initialize in `onFragmentCreate()`, refresh on `NotificationCenter.dialogFiltersUpdated` and `suggestedFiltersLoaded`, notify on filter save, and nullify in `onFragmentDestroy()`.
  2. `VoIPFragment`: Connect `CallViewModel` in constructor `VoIPFragment(int account)` via `AccountFeatureContainer.Companion.get(account).getCallViewModel()`. Cleanly nullify in `destroy()`.
  3. `PrivacySettingsActivity`: Connect `PrivacyViewModel` and `PasskeysViewModel`. Trigger `passkeysViewModel.loadPasskeys(false)` on create, dispatch `PrivacyEvent.ReloadRules.INSTANCE` and `PrivacyEvent.ReloadTwoStepVerification.INSTANCE` on notifications, and nullify on destroy.
  4. `PasscodeActivity`: Connect `BiometricsViewModel` via `AccountFeatureContainer.Companion.get(currentAccount).getBiometricsViewModel()`. Nullify on destroy.
  5. `ChatActivity`: Connect `MentionsViewModel`, `AudioPlayerViewModel`, and `FactCheckViewModel` via `accountContainer`. Nullify in `onFragmentDestroy()` to ensure zero memory leaks.
- **Consequences:** All specialized screens are securely bound to Architecture v2 ViewModels without duplicate network mutations, background thread contention, or lifecycle leaks. Verified with clean Kotlin/Java compilation, successful unit test suite passes, and tested with real device deployment (`RZCW41MQNVV`) showing zero runtime errors.

### ADR 118: DialogsActivity Search Stabilization via Deconfliction of AnimationLocker and SearchViewModel
- **Context:** Following the initial Strangler Fig wiring in `DialogsActivity.java`, users reported that search in `DialogsActivity` was broken: search overlay failed to open smoothly or froze, typing queries showed empty or erratic results, and search transitions locked up. Deep inspection revealed two critical conflicts:
  1. **Double Animation Locking:** In `DialogsActivity.showSearch()`, both `animationLockerViewModel.onEvent(AcquireLock)` and Telegram's native `notificationsLocker.lock()` were invoked concurrently. `LegacyAnimationLockerRepository` registered a lock in `NotificationCenter.setAnimationInProgress()` with `allowed = null`, halting heavy operations and placing notifications (`dialogsNeedReload`, search events, layout passes) into `delayedPosts`. Incomplete or unaligned releases left `NotificationCenter` in an animation state.
  2. **Duplicate Search Pipeline Race Condition:** In `onTextChanged()`, every keystroke triggered `searchViewModel.onEvent(new SearchEvent.QueryChanged(text))`. This launched background coroutines executing `searchLocalUseCase` and scheduling `TL_contacts_search` through `LegacySearchRepository`, which directly mutated `MessagesStorage` and `MessagesController` on the UI thread while `searchViewPager.dialogsSearchAdapter` was concurrently executing its own local and global search pipelines, leading to SQLite contention, dropped search results, and UI freezes.
- **Decision:** Reinforce the **Single Execution Principle** by completely removing `animationLockerViewModel` and `searchViewModel` event dispatches from `DialogsActivity.java`:
  1. Completely remove `animationLockerViewModel` imports, field, instantiation, and `AcquireLock` / `ReleaseAllLocks` calls from `showSearch()`. Let Telegram's battle-tested `notificationsLocker` (`AnimationNotificationsLocker`) exclusively manage search animation locks.
  2. Remove `searchViewModel` imports, field, instantiation, and `onEvent(QueryChanged)` from `onTextChanged()`. Allow `searchViewPager` and `DialogsSearchAdapter` to manage search queries, contacts, and global MTProto results without background coroutine interference.
  3. Keep `DialogsViewModel` (folder switching observation), `FoldersViewModel` (filter updates), `StoriesViewModel` (story limits and updates), and `SavedMessagesViewModel` purely in non-intrusive observation mode.
- **Consequences:** Restores full search responsiveness, smooth search opening/closing animations, and accurate real-time contact and message search results in `DialogsActivity` while maintaining pristine upstream compatibility.

### ADR 117: DialogsActivity Clean Strangler Fig Refinement, Stories Integration & Mutation Deduplication
- **Context:** In Telegram Android, `DialogsActivity.java` (~14,500 lines) is the core home screen managing chat lists, folder tabs, global search, and Stories in the header. During physical device testing, a critical regression was observed across legacy UI screens: legacy UI controllers (`ChatActivityEnterView`, `MessagesController`, `DialogsSearchAdapter`) already perform the complete lifecycle of network requests, caching, and database mutations. Invoking mutating UseCases in ViewModels (`chatViewModel.onSendMessage`, `dialogsViewModel.onDeleteDialog`, `dialogsViewModel.onTogglePin`, `dialogsViewModel.onMarkAsRead`, `dialogsViewModel.onLoadMore`, `searchViewModel.onEvent(RemoveRecentSearch)`) in parallel with legacy code resulted in double execution (duplicate message/channel post dispatches, duplicate SQL queries, and pagination desynchronization). Furthermore, `StoriesViewModel` was not connected to `DialogStoriesCell`, and ViewModel instances were not released on fragment destruction.
- **Decision:** Apply the **Single Execution Principle** for the Strangler Fig pattern in legacy Telegram UI:
  1. ViewModels in legacy screens strictly serve as **state observers and navigation coordinators** (`dialogsViewModel.switchFolder`, `searchViewModel.onEvent(QueryChanged)`), while legacy controllers continue handling actual network and database mutations until the UI view itself is replaced.
  2. Remove all duplicate mutating calls from `DialogsActivity.java`: eliminate `dialogsViewModel.onLoadMore()`, `dialogsViewModel.onMarkAsRead()`, `dialogsViewModel.onDeleteDialog()`, `dialogsViewModel.onTogglePin()`, `searchViewModel.onEvent(RemoveRecentSearch)`, and `searchViewModel.onEvent(ClearRecentSearches)`.
  3. Wire `StoriesViewModel` (`AccountFeatureContainer.get(currentAccount).getStoriesViewModel()`): initialize in `onFragmentCreate()`, fetch initial story limits via `storiesViewModel.loadStoryLimit()`, and trigger `storiesViewModel.onEvent(StoriesEvent.Refresh.INSTANCE)` on `NotificationCenter.storiesUpdated`.
  4. Reactively refresh `FoldersViewModel` upon receiving `NotificationCenter.dialogFiltersUpdated`.
  5. Ensure full lifecycle teardown by explicitly nulling out all ViewModel references (`dialogsViewModel`, `foldersViewModel`, `searchViewModel`, `storiesViewModel`, `savedMessagesViewModel`, `animationLockerViewModel`) in `onFragmentDestroy()`.
- **Consequences:** Completely eliminates duplicate network requests and database writes in dialog management and search. Fully integrates Stories and Folders into the reactive MVI flow, prevents Activity/Fragment memory leaks, and guarantees 100% upstream Telegram synchronization compatibility.

### ADR 116: SettingsActivity Preferences, Appearance, Storage & Billing UI Wiring via Strangler Fig
- **Context:** In Telegram Android, `SettingsActivity.java` (~2,150 lines) serves as the primary application settings hub, giving users access to profile information, chat/appearance customization, privacy controls, data & storage policies, notification settings, device sessions, and Telegram Premium/Stars purchases. Historically, `SettingsActivity` directly interacted with static controllers and singletons (`SharedConfig`, `UserConfig`, `MessagesController`, `StarsController`, etc.) without presentation layer abstractions.
- **Decision:** Apply the Strangler Fig pattern at the UI layer. Acquire `SettingsViewModel`, `ThemeViewModel`, `DataStorageViewModel`, and `BillingViewModel` from `AccountFeatureContainer.Companion.get(currentAccount)` during `initViewModels()` called from `onFragmentCreate()`. Wire user actions and system notifications through null-safe event dispatches:
  1. Initialization and settings refresh -> `settingsViewModel.refreshSettings()`, `themeViewModel.onEvent(ThemeEvent.Load.INSTANCE)`, `billingViewModel.onEvent(BillingEvent.Connect.INSTANCE)`.
  2. Appearance & Chat theme navigation (`onClick` item id 2) -> `themeViewModel.onEvent(ThemeEvent.Load.INSTANCE)`.
  3. Data & Storage usage navigation (`onClick` item id 6) -> `dataStorageViewModel.onEvent(DataStorageEvent.RefreshStorage.INSTANCE)`.
  4. Telegram Premium navigation & Grace Period suggestion handling (`onClick` item id 11 and suggestion button click) -> `billingViewModel.onEvent(new BillingEvent.ManageSubscription("telegram_premium"))`.
  5. Account & logout operations (in action bar logout item 2 and `didReceivedNotification` for `updateInterfaces`) -> `settingsViewModel.refreshSettings()`.
  6. Memory leak prevention: explicitly null out all ViewModel references in `onFragmentDestroy()`.
  All additions are strictly additive, non-invasive, and maintain 100% backward compatibility with upstream Telegram settings layout and list rendering.
- **Consequences:** Settings, appearance, storage, and billing flows are now connected to modern MVI ViewModels, making the settings dashboard testable and reactive while leaving legacy settings fragments undisturbed.

### ADR 115: PhotoViewer Media Viewing, Zoom & PiP UI Wiring via Strangler Fig
- **Context:** In Telegram Android, `PhotoViewer.java` (~11,700 lines) is the central media viewing overlay singleton (`PhotoViewer.getInstance()`, `PipInstance`) responsible for fullscreen images, video playback, animated stickers, pinch-to-zoom gestures, PiP transitions, and action bar controls. Historically, `PhotoViewer` manipulated low-level UI flags and legacy controllers directly without presentation layer abstractions.
- **Decision:** Apply the Strangler Fig pattern at the UI layer. Acquire `PhotoViewerViewModel`, `PinchToZoomViewModel`, `ContentPreviewViewModel`, and `PipViewModel` from `AccountFeatureContainer.Companion.get(currentAccount)` in `initViewModels(int account)` (triggered on `setParentActivity()` and `openPhoto()`). Wire media events through null-safe event dispatches:
  1. Media display initialization -> `photoViewerViewModel.onEvent(new PhotoViewerEvent.Open(...))`.
  2. Slide / index selection -> `photoViewerViewModel.onEvent(new PhotoViewerEvent.SelectIndex(currentIndex))`.
  3. Action bar toggling -> `photoViewerViewModel.onEvent(PhotoViewerEvent.ToggleActionBar.INSTANCE)`.
  4. Video / web playback toggles (in `playVideoOrWeb` and `pauseVideoOrWeb`) -> `photoViewerViewModel.onEvent(PhotoViewerEvent.TogglePlayback.INSTANCE)`.
  5. Picture-in-Picture entry (in `gallery_menu_pip`) -> `pipViewModel.onEvent(new PipEvent.SetPipState(PipState.IN_PIP))`.
  6. Pinch-to-zoom gesture tracking (in pinch detector callbacks) -> `pinchToZoomViewModel.onEvent(new PinchToZoomEvent.UpdateZoom(scale))` and `pinchToZoomViewModel.onEvent(new PinchToZoomEvent.UpdateTransform(translationX, translationY, scale))`.
  7. Closing and cleanup (in `onHideView` and `destroyPhotoViewer`) -> `photoViewerViewModel.onEvent(PhotoViewerEvent.Close.INSTANCE)`, reset pinch-to-zoom and PiP states, and null out ViewModel references.
  All additions are strictly additive, non-invasive, and preserve 100% compatibility with Telegram's rendering pipeline and animations.
- **Consequences:** PhotoViewer's lifecycle, zooming, playback, and PiP states are now cleanly bridged to pure domain ViewModels, providing observability and testability while protecting the complex UI drawing loop from breaking changes.

### ADR 114: ProfileActivity User & Peer Info UI Wiring via Strangler Fig
- **Context:** In Telegram Android, `ProfileActivity.java` (~17,000 lines) is the central screen displaying user, group, channel, and bot profiles, as well as shared media, contacts, birthdays, privacy/security settings, and peer blocking. Historically, `ProfileActivity` directly invoked legacy controllers (`MessagesController`, `ContactsController`, `SharedMediaLayout`, `SharedMediaPreloader`, etc.) without presentation abstractions.
- **Decision:** Apply the Strangler Fig pattern at the UI layer. Acquire `ProfileViewModel`, `ContactsViewModel`, `BirthdaysViewModel`, `PrivacyViewModel`, and `SharedMediaViewModel` from `AccountFeatureContainer.Companion.get(currentAccount)` during `initViewModels()` called from `onFragmentCreate()`. Wire user actions through null-safe event dispatches:
  1. Profile initialization and full loading -> `profileViewModel.onLoadFullProfile()`.
  2. Blocking and unblocking actions (in `onBlockContactClicked` and list click `unblockRow`) -> `profileViewModel.onToggleBlock()` and `privacyViewModel.onEvent(new PrivacyEvent.BlockPeer / UnblockPeer(userId))`.
  3. Contact additions and deletions (in `add_contact`, `delete_contact`, `addToContactsRow`) -> `contactsViewModel.refresh()` and `contactsViewModel.deleteContact(userId)`.
  4. Birthday checks and interactions (in `initViewModels()` and `birthdayRow` clicks) -> `birthdaysViewModel.onEvent(new BirthdaysEvent.CheckBirthdays(false))`.
  5. Shared media tab switching (in `sharedMediaLayout.onSelectedTabChanged()`) -> `sharedMediaViewModel.onEvent(new SharedMediaEvent.OnTabSelected(SharedMediaTabType.fromId(getClosestTab())))`.
  6. Memory leak prevention: explicitly clear all ViewModel references in `onFragmentDestroy()`.
  All additions are strictly additive, non-invasive, and maintain 100% backward compatibility with Telegram's complex list rendering, layout animations, and upstream changes.
- **Consequences:** Profile screen state transitions, peer moderation, contacts management, and shared media interactions are now cleanly bridged to pure domain ViewModels, paving the way for testing and UI decoupling without touching legacy rendering pipeline.

### ADR 113: LaunchActivity Navigation & System UI Wiring via Strangler Fig
- **Context:** In Telegram Android, `LaunchActivity.java` (~9,250 lines) is the root application Activity responsible for windowing, session lifecycle, account switching, bottom navigation tabs (`MainTabsActivity`), Picture-in-Picture (`PipActivityController`), window content visibility (`WindowVisibilityManager`), web browsing (`Browser`), predictive back animations, and launcher app icons (`LauncherIconController`). Historically, `LaunchActivity` mutated legacy global singletons and controllers directly, making system-level interactions tightly coupled and opaque to unit tests.
- **Decision:** Apply the Strangler Fig pattern at the UI layer. Acquire `MainTabsViewModel`, `PipViewModel`, `WindowVisibilityViewModel`, `BrowserViewModel`, `LauncherIconViewModel`, and `AnimationLockerViewModel` from `AccountFeatureContainer.Companion.get(currentAccount)` during `checkCurrentAccount()` (supporting runtime account switching via `switchToAccount()`). Wire system events through null-safe event dispatches:
  1. PiP transitions in `pipActivityController.addPipListener` -> `pipViewModel.onEvent(new PipEvent.TransitionPipState(PipState.IN_PIP / IDLE))` and `windowVisibilityViewModel.onEvent(new WindowVisibilityEvent.HideRequested / ReleaseRequested("pip"))`.
  2. Tab visibility on account switch in `switchToAccount()` -> `mainTabsViewModel.onEvent(new MainTabsEvent.SetTabsVisible(true))`.
  3. Predictive back animation locks in `onBackAnimationCallback` -> `animationLockerViewModel.onEvent(new AnimationLockerEvent.AcquireLock / ReleaseAllLocks)`.
  4. Web URL routing in `tonsite` handling -> `browserViewModel.onEvent(new BrowserEvent.OpenUrl(data.toString()))`.
  5. Launcher icon verification upon initialization -> `launcherIconViewModel.onEvent(LauncherIconEvent.FixIconIfNeeded.INSTANCE)`.
  6. Memory leak prevention: null out all ViewModels in `onDestroy()`.
  All modifications are purely additive, null-safe, and do not alter legacy Android window mechanics, back stacks, or rendering.
- **Consequences:** Root navigation and system coordination in `LaunchActivity` are now cleanly observable and routed through pure domain ViewModels, decoupling window management from legacy singletons while guaranteeing 100% backward compatibility and seamless future upstream merges.

### ADR 112: ChatActivity UI Wiring with Messaging ViewModels via Strangler Fig
- **Context:** In Telegram Android, `ChatActivity.java` (~47,300 lines) is the core messaging UI handling chat history, text input, media sending, reactions, themes, drafts, and bottom bar visibility. Historically, user actions directly invoked monolithic controllers (`SendMessagesHelper`, `ChatThemeController`, `ReactionsLayoutInBubble`, `ChatActivityEnterView`, `ChatActivityBottomViewsVisibilityController`, etc.). A full rewrite of `ChatActivity` would be disastrous for upstream synchronization and risk message loss, animation regressions, or UI glitches.
- **Decision:** Apply the Strangler Fig pattern at the UI layer. Acquire `ChatViewModel`, `SendMessagesViewModel`, `ChatThemeViewModel`, `ReactionsViewModel`, `ChatInputViewModel`, `BottomViewsViewModel`, and `DraftMeasureViewModel` from `AccountFeatureContainer.Companion.get(currentAccount)` during `onFragmentCreate()`. Wire user actions through null-safe event dispatches:
  1. `onMessageSend()` -> `chatViewModel.onSendMessage(...)`
  2. `onTextChanged()` and `onTextSelectionChanged()` -> `chatInputViewModel.onEvent(new ChatInputEvent.TextChanged(...))`
  3. `selectReaction()` -> `reactionsViewModel.onEvent(new ReactionsEvent.SendReaction(...))` and `ClearReactions`
  4. `showChatThemeBottomSheet()` & `setChatThemeEmoticon()` -> `chatThemeViewModel.loadThemes(...)`, `applyTheme(...)`, `resetTheme(...)`
  5. `onBottomItemsVisibilityChanged()` -> `bottomViewsViewModel.onEvent(new BottomViewsEvent.SetViewVisible(...))`
  All modifications are purely additive, null-safe (`if (viewModel != null)`), and do not disrupt legacy controllers, animators, or delegates.
- **Consequences:** Key messaging user interactions in `ChatActivity` are now observed and routed through pure domain ViewModels and UseCases, establishing clear boundaries around legacy messaging controllers while guaranteeing 100% backward compatibility and minimal conflict risk during future upstream Telegram synchronization.

### ADR 111: DialogsActivity UI Wiring with ViewModels via Strangler Fig
- **Context:** In Telegram Android, `DialogsActivity.java` (~14,500 lines) manages the primary chat list, folder filters, search, and screen animations. Historically, UI events directly mutated monolithic `MessagesController` and `DialogsSearchAdapter` singletons. Wholesale rewrite of `DialogsActivity` would break upstream synchronization, destroy complex gesture animations, and introduce severe regressions.
- **Decision:** Apply the Strangler Fig pattern at the UI layer. Acquire `DialogsViewModel`, `FoldersViewModel`, `SearchViewModel`, `SavedMessagesViewModel`, and `AnimationLockerViewModel` from `AccountFeatureContainer.Companion.get(currentAccount)` during `onFragmentCreate()`. Redirect all user actions (dialog deletion, pinning, marking as read, folder switching, search queries/recents clearance, and animation notifications locking) through the respective ViewModels while leaving complex low-level list rendering and gesture calculations intact. Ensure lifecycle cleanup in `onFragmentDestroy()`.
- **Consequences:** UI layer interactions in `DialogsActivity` are now driven through pure domain ViewModels and UseCases, decoupling business logic from legacy God controllers. Changes are strictly localized to 75 lines out of 14,500, ensuring seamless future upstream merges from official Telegram without UI regressions.

### ADR 110: Modular Domain Containers & AccountFeatureContainer Facade
- **Context:** Following the 7-domain package restructuring (ADR 109), `AccountFeatureContainer.kt` had expanded into a monolithic Service Locator file (~7,400 lines) registering 105 features, 1,270+ properties/methods, and dozens of ViewModel factories. This file was cumbersome to navigate, created a single point of failure for DI merge conflicts, and introduced severe Kotlin compiler CFG (Control Flow Graph) overhead. The user required modularizing DI into 7 domain-specific containers while strictly maintaining a single monolithic Gradle module (`TMessagesProj`) and 100% backward compatibility for all existing callers.
- **Decision:** Split the monolithic `AccountFeatureContainer.kt` into 7 isolated domain containers under `feature.<domain>.di`:
  1. `feature.business.di.BusinessContainer` (10 features, 135 properties/factories)
  2. `feature.media.di.MediaContainer` (18 features, 248 properties/factories)
  3. `feature.messaging.di.MessagingContainer` (31 features, 421 properties/factories)
  4. `feature.network.di.NetworkContainer` (4 features, 54 properties/factories)
  5. `feature.security.di.SecurityContainer` (10 features, 140 properties/factories)
  6. `feature.social.di.SocialContainer` (6 features, 73 properties/factories)
  7. `feature.system.di.SystemContainer` (26 features, 384 properties/factories)
  Each domain container is scoped to `currentAccount` and `Context`, containing only the dependencies, repositories, use cases, and view models belonging to its domain.
  Convert `AccountFeatureContainer.kt` into a lightweight facade:
  - Expose domain sub-containers directly: `val business by lazy { BusinessContainer(account, context) }`, etc.
  - Retain 100% backward compatibility by delegating all properties and methods to their domain containers (`var billingRepository get() = business.billingRepository; set(v) { business.billingRepository = v }`).
- **Consequences:**
  - Strict domain separation: DI definitions now reside within their respective domain packages, maximizing cohesion.
  - Zero breakages: Existing callsites like `AccountFeatureContainer.get(account).savedMessagesRepository` and UI ViewModel factories continue to function without modifying a single line of client code.
  - Compilation stability: Resolves Kotlin K2 compiler CFG combinatorial complexity and avoids monolithic class bloat.
  - Zero Gradle changes: Operates completely within `TMessagesProj` without requiring multi-module Gradle setups.

### ADR 109: Domain Modularization into 7 Semantic Subdomains (105 Features)
- **Context:** Following the isolation of all 105 architectural feature slices across Telegram Android, the root package `org.telegram.messenger.feature` contained 105 top-level directories. This flat namespace created cognitive overhead, cluttered project navigation, and obscured domain-level cohesion across related functional areas. User explicitly requested to group these into 7 clean business/technical domains while strictly maintaining a single monolithic Gradle module (`TMessagesProj`) without creating individual `:feature:*` Gradle modules.
- **Decision:** Restructure all 105 feature packages into 7 semantic subdomains within `TMessagesProj`:
  1. `feature.business` (10 features: `billing`, `botstars`, `businessbots`, `businesslinks`, `businessrecipients`, `giftauctions`, `payments`, `quickreplies`, `stargifts`, `timezones`).
  2. `feature.media` (18 features: `audioplayer`, `autodeletemedia`, `cachebychats`, `camera`, `chromecast`, `contentpreview`, `downloadmanager`, `fileloader`, `fileref`, `gallerysave`, `imageloader`, `mediadata`, `photoviewer`, `pip`, `sharedmedia`, `stories`, `storycustomparams`, `voip`).
  3. `feature.messaging` (31 features: `aitones`, `autodelete`, `botforum`, `botkeyboard`, `bottomviews`, `chat`, `chatattach`, `chatinput`, `chatmeta`, `chattheme`, `dialogs`, `draftmeasure`, `drafts`, `emojieffects`, `emojipicker`, `ephemeralmessages`, `factcheck`, `folders`, `groupcallmsg`, `hashtagsearch`, `mentions`, `messagecustomparams`, `reactions`, `richcaption`, `savedmessages`, `search`, `sendmessages`, `stickers`, `texthtml`, `topics`, `translate`).
  4. `feature.network` (4 features: `networkstats`, `proxy`, `push`, `pushlistener`).
  5. `feature.security` (10 features: `authtokens`, `biometrics`, `botguard`, `captcha`, `flagsecure`, `passkeys`, `privacy`, `secretchat`, `sessions`, `unconfirmedauth`).
  6. `feature.social` (6 features: `birthdays`, `boosts`, `contacts`, `joinrequests`, `location`, `profile`).
  7. `feature.system` (26 features: `adjustpan`, `animationlocker`, `anrwatchdog`, `appconfig`, `browser`, `countdowntimer`, `datastorage`, `emudetector`, `floatingdebug`, `fpscontent`, `hints`, `keyboardhide`, `keyboardinsets`, `launchericon`, `leakdetector`, `litemode`, `localization`, `maintabs`, `notifications`, `pinchtozoom`, `recyclerscroll`, `refreshrate`, `ringtones`, `settings`, `themes`, `windowvisibility`).
  Update all package declarations, file imports, test files, and `AccountFeatureContainer.kt` DI registrations to match the new package paths (`org.telegram.messenger.feature.<domain>.<feature>.*`).
- **Consequences:** Package structure is clean, logical, and instantly navigable. The 7 semantic domains provide clear architectural boundaries and prepare the codebase for domain-level containerization while preserving 100% monolithic build performance and zero Gradle overhead.


### ADR 108: Isolation of Animation Notifications Locker & UI Stutter Prevention into feature.animationlocker
- **Context:** In Telegram Android, UI jank and frame drops during transitions (such as opening chats in `DialogsActivity`, expanding photos in `PhotoViewer`, transitioning between tabs in `LaunchActivity`, viewing topics in `TopicsFragment`, and reading articles in `ArticleViewer`) were prevented using `AnimationNotificationsLocker.java` (~48 lines). The locker suppressed non-critical `NotificationCenter` broadcasts during active screen animations by invoking `NotificationCenter.setAnimationInProgress(handle, allowedNotifications)` on account and global instances. While simple, `AnimationNotificationsLocker` was instantiated ad-hoc in 14 distinct UI components, relied on raw integer array handles, lacked timeout protection against abandoned animations, and had no state observability or decoupled presentation binding for modern ViewModels.
- **Decision:** Introduce pure domain models `LockScope` (ACCOUNT, GLOBAL, ALL), `AnimationLockRecord`, `AnimationLockerState`, `AnimationLockerConfig`, and `AnimationLockEvaluator`. Define abstract contract `AnimationLockerRepository` covering lock acquisition (`acquireLock`), selective and bulk release (`releaseLock`, `releaseAllLocks`), temporary disabling (`setDisabled`), status queries (`isLocked`, `isNotificationAllowed`, `getState`, `getConfig`, `updateConfig`), and reactive state streams (`observeState`, `observeIsLocked`). Implement pure domain use cases for lock arbitration and notification filtering. Provide thread-safe `LegacyAnimationLockerRepository` with multi-account/global notification suspension and whitelist calculation in `AnimationLockerMapper`. Encapsulate presentation state and MVI events in `AnimationLockerViewModel`.
- **Consequences:** All animation notification locking, UI stutter prevention, whitelist filtering, and multi-scope lock arbitration are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `AnimationNotificationsLocker.java` and `NotificationCenter.java`.

### ADR 107: Isolation of Window Security & FLAG_SECURE Arbitration into feature.flagsecure
- **Context:** In Telegram Android, window security and screenshot/screen recording prevention (`WindowManager.LayoutParams.FLAG_SECURE`) were managed through `FlagSecureReason.java` (~85 lines), referenced across `LaunchActivity`, `ChatActivity`, and `ProfileActivity`. The component managed a static map of reasons per `Window` (`currentSecureReasons = HashMap<Window, Integer>`), allowing conditions (`FlagSecureCondition`) to attach, detach, and invalidate. When the count of active reasons exceeded zero, `window.addFlags(FLAG_SECURE)` was called; when the count reached zero, `window.clearFlags(FLAG_SECURE)` was called. Conditions were driven by diverse app states: passcode lock (`SharedConfig.passcodeHash.length() > 0 && !SharedConfig.allowScreenCapture`), secret chats (`currentEncryptedChat != null`), protected channels/groups (`isPeerNoForwards()`), and payment screens. However, `FlagSecureReason` was tightly coupled to `android.view.Window`, `WindowManager.LayoutParams`, and static global state, preventing unit testing, observability into why a window is secured, or decoupled state management across ViewModels.
- **Decision:** Introduce pure domain models `SecurityReasonType`, `WindowSecurityState`, `SecurityRuleSpec`, `SecurityEvaluationResult`, and `SecurityRulesEvaluator`. Define abstract contract `FlagSecureRepository` covering reason attachment (`attachReason`), detachment (`detachReason`), condition invalidation (`invalidateWindow`), state queries (`isWindowSecured`, `getWindowState`, `getAllWindowStates`, `resetWindow`), and reactive state streams (`observeWindowState`, `observeAllWindowStates`). Implement pure domain use cases for attachment, detachment, invalidation, security checks, and rule evaluation. Provide `LegacyFlagSecureRepository` with thread-safe reference counting, condition tracking, and pure mapping in `FlagSecureMapper`. Encapsulate presentation state and MVI events in `FlagSecureViewModel`.
- **Consequences:** All window security arbitration, screenshot protection logic, reference-counted conditions, and multi-window security states are decoupled behind clean, testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `FlagSecureReason.java`.

### ADR 106: Isolation of Hardware & Emulator / Virtual Environment Detection into feature.emudetector
- **Context:** In Telegram Android, client environment integrity and emulator detection were implemented in `EmuDetector.java` (~434 lines) and `EmuInputDevicesDetector.java` (~64 lines), called during MTProto initial handshake in `ConnectionsManager.getInitFlags()`. The detection engine checked multiple indicators: basic build properties (hardware goldfish/ranchu/nox, generic fingerprints, product/model strings), filesystem markers (Genymotion sockets, Andy/Nox/BlueStacks files, QEMU drivers and pipes), kernel input devices (`/proc/bus/input/devices`), telephony characteristics (known test phone numbers, dummy IMEI/IMSI, network operator "android"), and system properties via reflection on `android.os.SystemProperties`. However, `EmuDetector` was tightly coupled to `android.content.Context`, `PackageManager`, `TelephonyManager`, `Build`, and static singletons, preventing unit testing without Android devices or emulators, dynamic threshold adjustments, or detailed diagnostic reporting.
- **Decision:** Introduce pure domain models `EmulatorType`, `DetectionCategory`, `DetectionIndicator`, `EnvironmentVerdict`, `EmulatorDiagnostics`, `EmulatorDetectorConfig`, and `EmulatorHeuristics`. Define abstract contract `EmuDetectorRepository` covering environment scanning (`detectEnvironment`), synchronous status checks (`isEmulator`), cached diagnostics (`getCachedDiagnostics`), configuration management (`getConfig`, `updateConfig`, `addCustomPackage`, `clearCache`), and reactive state streams (`observeDiagnostics`, `observeIsEmulator`). Implement pure domain use cases for detection, verdict evaluation, confidence scoring, and configuration. Provide `LegacyEmuDetectorRepository` adapting `EmuDetector` and `EmuInputDevicesDetector` with pluggable test detection and pure mapping in `EmuDetectorMapper`. Encapsulate presentation state and MVI events in `EmuDetectorViewModel`.
- **Consequences:** All hardware/virtualization checks, weighted confidence scores, emulator classification, and diagnostic reports are decoupled behind clean, testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `EmuDetector.java` and `ConnectionsManager.java`.

### ADR 105: Isolation of Main Thread ANR Watchdog & UI Freeze Diagnostics into feature.anrwatchdog
- **Context:** In Telegram Android, main thread responsiveness and Application Not Responding (ANR) detection were monitored through `ANRDetector.java` (~224 lines). The component ran a dedicated background thread (`ANRDetector`), periodically posted ping messages (`MSG_UI_PING`) to `Looper.getMainLooper()` with monotonic IDs, and waited for up to `TIMEOUT_MS = 5000L` for the main thread to acknowledge execution. It monitored application foreground/background transitions via `ForegroundDetector.Listener`, paused execution in background via `lock.wait()` to eliminate CPU overhead, incremented generation counters upon state changes, and deduplicated ANR alerts (`anrReported`) until the UI thread recovered. However, `ANRDetector` was tightly coupled to Android's `Handler`, `Looper`, `Message`, `Thread.sleep()`, and single-runnable callback (`Runnable anrDetected`), preventing unit testing without OS looper infrastructure, dynamic timeouts, or reactive incident observation in modern monitoring dashboards.
- **Decision:** Introduce pure domain models `AnrSeverity`, `AppLifecycleState`, `PingRecord`, `AnrIncident`, `AnrWatchdogConfig`, and `AnrWatchdogState`. Define abstract contract `AnrWatchdogRepository` covering lifecycle monitoring (`startMonitoring`, `stopMonitoring`, `setForeground`), ping operations (`sendPing`, `acknowledgePing`), freeze detection (`checkFreeze`, `resolveIncident`), history snapshots (`getState`, `getIncidentHistory`, `clearHistory`), and reactive streams (`observeState`, `observeIncidents`). Implement pure domain use cases for monitoring lifecycle, ping processing, freeze checks, and incident streams. Provide thread-safe `LegacyAnrWatchdogRepository` adapting `ANRDetector`'s generation and ping deduplication logic alongside pure mapping in `AnrWatchdogMapper`. Encapsulate presentation state and MVI events in `AnrWatchdogViewModel`.
- **Consequences:** All main thread ANR watchdog monitoring, ping/acknowledgment tracking, freeze detection thresholds, recovery arbitration, and incident history are decoupled behind clean, testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `ANRDetector.java`.

### ADR 104: Isolation of Frame Rate Adaptation & 60 FPS V-Sync Content Arbitration into feature.fpscontent
- **Context:** In Telegram Android, animation tick dispatching, display refresh rate decoupling, and screen invalidation wave batching were handled through `Choreographer60FpsContent.java` (~360 lines). The component acted as an Android `Choreographer.FrameCallback` delivering animation callbacks at stable ~60 fps regardless of physical screen refresh rate (90Hz, 120Hz, 144Hz displays). Callbacks sharing the same FPS shared a single accumulator or stride index (`TARGET_FPS / fps`), guaranteeing that N animations at 60/30/20 fps produce exactly one invalidation wave per period. It also maintained lists of Views and Drawables (`mViewsToInvalidate`, `mDrawablesToInvalidate`, `mDrawablesToInvalidate30fps`) and one-shot runnables. However, `Choreographer60FpsContent` was tightly coupled to the Android `Choreographer` singleton, `android.os.Looper.getMainLooper()`, `me.vkryl.core.reference.ReferenceList`, and direct `View.invalidate()` / `Drawable.invalidateSelf()` invocations, preventing unit testing and observability into active animation subscribers and frame pacing metrics.
- **Decision:** Introduce pure domain models `FrameCallbackType`, `FpsGroupConfig`, `FrameTick`, `FrameCallbackSubscription`, `FpsContentStats`, and `FpsTimingUtils`. Define abstract contract `FpsContentRepository` covering callback subscription (`addFrameCallback`, `addRunnableCallback`, `removeCallback`), invalidation requests (`postInvalidateView`, `postInvalidateDrawable`), V-Sync tick dispatch (`dispatchVsync`), metrics snapshots (`getStats`, `getSubscriptions`, `reset`), and reactive streams (`observeStats`, `observeTicks`). Implement pure domain use cases for callback registration, unregistration, view/drawable invalidation scheduling, V-Sync dispatch, timing calculation, and state observation. Provide thread-safe `LegacyFpsContentRepository` adapting `Choreographer60FpsContent` logic with stride calculation and accumulator math. Encapsulate presentation state and MVI events in `FpsContentViewModel`.
- **Consequences:** All frame rate arbitration, stride calculations, V-Sync tick dispatching, one-shot actions, and batched view/drawable invalidation schedules are decoupled behind clean, testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `Choreographer60FpsContent.java`.

### ADR 103: Isolation of Memory Leak Detection & Instance Reference Tracking Engine into feature.leakdetector
- **Context:** In Telegram Android, runtime memory leak diagnostics and activity/fragment/view reference tracking were implemented via singleton `LeakDetector.java` (~215 lines). The detector used `me.vkryl.core.reference.ReferenceMap` to store weak references to tracked class instances, periodically scanned the registry via `AndroidUtilities.runOnUIThread` every 1,000 ms, compared instance counts against a threshold (`LEAK_THRESHOLD = 5`), requested garbage collection (`System.gc()`), and scheduled confirmation re-checks after a 2,000 ms debounce window (`GC_RECHECK_DELAY_MS`) to eliminate false positives. Confirmed leaks were posted to `NotificationCenter.memoryLeakFoundException`. Because `LeakDetector` relied on Android UI looper runnables, singleton global state, and direct `NotificationCenter` posts, leak detection could not be observed reactively in modern UI dashboards, configured with dynamic thresholds, or tested in unit tests without Android runtime components.
- **Decision:** Introduce pure domain models `TrackedClassStats`, `LeakReport`, `LeakDetectorConfig`, and `LeakDetectorState`. Define abstract contract `LeakDetectorRepository` covering lifecycle controls (`start`, `stop`), registration (`track`), manual/periodic triggers (`triggerCheck`, `confirmLeak`), stats and count queries (`getLiveCount`, `getReportedLeaks`, `getTrackedStats`, `reset`), and reactive streams (`observeState`, `observeLeaks`). Implement pure domain use cases for scanning, confirmation, tracking, and leak streams. Provide thread-safe `LegacyLeakDetectorRepository` using Kotlin `WeakReference` and Coroutines alongside pure mapping in `LeakDetectorMapper`. Encapsulate presentation state and MVI events in `LeakDetectorViewModel`.
- **Consequences:** All memory leak tracking, threshold checks, GC confirmation windows, and leak reporting streams are decoupled behind clean, testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `LeakDetector.java`.

### ADR 102: Isolation of Text & HTML Entity Conversion Engine into feature.texthtml
- **Context:** In Telegram Android, clipboard copy-paste operations, rich text formatting, and HTML entity conversion were tightly coupled across `CustomHtml.java` (~294 lines), `CopyUtilities.java` (~386 lines), `AndroidUtilities.java`, `EditTextCaption.java`, `MediaDataController.java`, and various message viewers. `CustomHtml` converted Telegram Spanned objects (such as `QuoteSpan`, `TextStyleSpan`, `CodeHighlighting.Span`, `AnimatedEmojiSpan`, `URLSpanReplacement`, `URLSpanMono`) into HTML markup (`<b>`, `<i>`, `<u>`, `<s>`, `<spoiler>`, `<pre lang="...">`, `<blockquote>`, `<animated-emoji data-document-id="...">`, `<a href="...">`). Conversely, `CopyUtilities` parsed clipboard HTML back into Telegram spans and entities via custom SAX handlers (`HTMLTagAttributesHandler`). Because both classes mixed low-level XML/HTML parsing, Android `Spanned`/`Spannable` spans, and raw static helper calls, rich text conversion could not be tested without Android framework dependencies or reused across modern domain workflows.
- **Decision:** Introduce pure domain models `HtmlTextSpanType`, `HtmlTextSpan`, `RichFormattedText`, `TextHtmlConversionResult`, and `TextHtmlState`. Define abstract contract `TextHtmlRepository` covering bidirectional conversions (`convertToHtml`, `parseFromHtml`), escaping/unescaping (`escapeHtml`, `unescapeHtml`), tag stripping (`stripFormatting`), and reactive state streams (`observeState`, `clearState`). Implement pure domain use cases for conversion, parsing, escaping, unescaping, format stripping, span extraction, and rich formatting detection. Provide thread-safe `LegacyTextHtmlRepository` and pure bidirectional regex-and-stack parsing in `TextHtmlMapper`. Encapsulate presentation state and MVI events in `TextHtmlViewModel`.
- **Consequences:** All rich text and HTML entity conversion, formatting span serialization/deserialization, HTML escaping, and markup stripping are cleanly decoupled behind testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `CustomHtml.java` and `CopyUtilities.java`.

### ADR 101: Isolation of Countdown Timer Engine & Time Formatting into feature.countdowntimer
- **Context:** In Telegram Android, countdown timers and time formatting for time-sensitive UI elements (such as closing polls in `ChatMessageCell`, remaining availability of Telegram Star gifts in `StarGiftSheet`, and active auction countdowns in `AuctionBidSheet` and `ActiveAuctionsSheet`) were implemented via `CountdownTimer.java` (~62 lines). The legacy timer relied directly on Android's UI thread looper (`AndroidUtilities.runOnUIThread`, `cancelRunOnUIThread`) and single-listener callbacks (`Callback.onTimerUpdate(long)`). Because it tightly coupled tick intervals, timer state mutation, and time formatting to Android views and platform runnables, multi-timer observation was prone to memory leaks upon sheet dismissal, lack of pause/resume support, and inability to test countdown business logic in unit tests.
- **Decision:** Introduce pure domain models `CountdownTimerStatus`, `CountdownTimeComponents`, `CountdownTimerTick`, and `CountdownTimerState`. Define abstract contract `CountdownTimerRepository` covering lifecycle operations (`start`, `stop`, `pause`, `resume`, `tick`), snapshots (`getTimer`, `isRunning`, `clearAll`), and reactive streams (`observeTimer`, `observeState`). Implement pure domain algorithms for time component decomposition into days, hours, minutes, and seconds (`DecomposeCountdownTimeUseCase`) and human-readable formatting (`FormatCountdownTimeUseCase`). Provide thread-safe `LegacyCountdownTimerRepository` with coroutine-based timers and `CountdownTimerMapper` for math calculations. Encapsulate presentation state and MVI events in `CountdownTimerViewModel`.
- **Consequences:** All countdown timers, tick calculations, progress fractions, and duration formatting strings are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `CountdownTimer.java`.

### ADR 100: Isolation of Window Visibility Arbitration & Reference-Counting Controllers into feature.windowvisibility
- **Context:** In Telegram Android, window and main activity content visibility coordination across heavy modal overlays (e.g. `BottomSheet`, `PhotoViewer`, `SecretMediaViewer`, `ArticleViewer`, `MessageSendPreview`, `StoryRecorder`, and `LaunchActivity`) was managed through `WindowVisibilityManager.java` (~76 lines). The manager tracked a `reasonsToHide` counter, notified an `OnVisibilityChangedListener` whenever `reasonsToHide > 0` toggled visibility, and supplied `Controller` instances with `setHidden(boolean)` and `destroy()` methods. In `LaunchActivity`, `ActivityVisibilityController` adapted this reference-counting logic for hiding decor views and main content layouts. Because `PhotoViewer` and other viewers directly created or manipulated static controllers and listeners without domain abstraction or reactive state observation, multi-window visibility was prone to orphaned hide locks, race conditions, and lack of testability.
- **Decision:** Introduce pure domain models `WindowVisibilityScope`, `WindowVisibilityReason`, `WindowVisibilityState`, `WindowVisibilityChangeResult`, and `WindowVisibilityController`. Define abstract contract `WindowVisibilityRepository` covering hide registration (`requestHide`, `releaseHide`, `toggleHide`), visibility snapshots (`isVisible`, `isHidden`, `getReasonsCount`, `getActiveReasons`, `getCurrentState`), full reset (`resetAllReasons`), reactive observation (`observeState`, `observeVisibilityChanges`), and subsystem controller factories (`obtainController`). Implement pure domain use cases for hide/release requests, toggle operations, reset, and reactive streams. Provide thread-safe `LegacyWindowVisibilityRepository` with lock-based reason set arbitration and optional backward-compatible `OnVisibilityChangedListener` bridging, plus pure transformation in `WindowVisibilityMapper`. Encapsulate presentation state and MVI events in `WindowVisibilityViewModel`.
- **Consequences:** All window and activity visibility arbitration, reference-counting hide locks, reason tracking, and visibility controller lifecycles are decoupled behind clean, testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `WindowVisibilityManager.java`, `BottomSheet.java`, and `LaunchActivity.java`.

### ADR 099: Isolation of Bot Inline Keyboards, Custom Action Buttons & Markup into feature.botkeyboard
- **Context:** In Telegram Android, bot inline keyboards, reply markup structures, and custom action buttons were coordinated across `BotInlineKeyboard.java` (~224 lines), `TLKeyboardHelper.java` (~49 lines), `ChatActivity.java`, `ChatActivityEnterView.java`, `BotWebViewSheet.java`, and message cells. `BotInlineKeyboard` defined styling background colors (`BackgroundColor`: `NONE`, `PRIMARY`, `SUCCESS`, `DANGER`), abstract button structures (`ButtonBot`, `ButtonCustom`), multi-row keyboard builders (`Builder`, `KeyboardSourceArray`, row bitmask separators `1 << (size - 1)`), and built-in action buttons (`SUGGESTION_DECLINE`, `SUGGESTION_ACCEPT`, `SUGGESTION_EDIT`, `OPEN_MESSAGE_THREAD`, `GIFT_OFFER_DECLINE`, `GIFT_OFFER_ACCEPT`, `SHARING_OFFER_DECLINE`, `SHARING_OFFER_ACCEPT`). `TLKeyboardHelper` checked `isForceReply` and webview button indicators (`isButtonWebView`). Direct manipulation of raw MTProto classes (`TLRPC.TL_replyKeyboardMarkup`, `TLRPC.KeyboardButton`) and hardcoded button styles inside UI classes tightly coupled bot keyboard rendering to Telegram's legacy singleton state.
- **Decision:** Introduce pure domain models `BotButtonColor`, `BotCustomButtonType`, `BotButtonTypeCategory`, `BotButtonItem`, `BotKeyboardRow`, `BotKeyboardLayout`, and `BotKeyboardState`. Define abstract contract `BotKeyboardRepository` covering reactive keyboard observation (`observeKeyboard`, `observeState`), snapshot queries (`getKeyboard`, `getState`), keyboard mutation (`setKeyboard`, `clearKeyboard`, `clearAllKeyboards`), and row bitmask extraction. Implement pure algorithmic use cases for 2D keyboard layout building from flat lists with row bitmasks (`BuildBotKeyboardLayoutUseCase`), button color resolution (`ResolveBotButtonColorUseCase`), custom action type parsing (`ParseCustomButtonTypeUseCase`), markup classification (`IsForceReplyMarkupUseCase`, `IsButtonWebViewUseCase`), and button badge formatting (`FormatButtonBadgeUseCase`). Provide thread-safe `LegacyBotKeyboardRepository` and bidirectional conversion in `BotKeyboardMapper`. Encapsulate presentation state and MVI events in `BotKeyboardViewModel`.
- **Consequences:** All bot inline keyboards, custom suggestion/offer buttons, row separator bitmasks, and reply markup classifications are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `BotInlineKeyboard.java` and `TLKeyboardHelper.java`.

### ADR 098: Isolation of Ephemeral Bot Messages, Commands & Welcome Anchors into feature.ephemeralmessages
- **Context:** In Telegram Android, ephemeral bot messages, ephemeral command detection, and welcome anchor bindings were coordinated across `EphemeralMessagesHelper.java` (~489 lines, 21.5KB), `SendMessagesHelper.java`, `MessagesController.java`, `MessagesStorage.java`, `MediaDataController.java`, and `ChatActivityEnterView.java`. The helper parsed commands (`/command@botusername`), determined whether commands were ephemeral via `TLRPC.BotCommand.ephemeral`, converted regular sending requests (`TL_messages_sendMessage`, `TL_messages_sendMedia`) to ephemeral requests (`TL_ephemeral.TL_sendMessage`) via `beforeSendingFinalRequest`, packed/unpacked ephemeral message IDs with bitmasks (`0x40000000`), converted between `TL_ephemeral.EphemeralMessage` and `TLRPC.Message`, batched incoming ephemeral updates (`EphemeralUpdates`), and tracked anchor bindings via `WelcomeAnchorsState`. Direct interaction with static instances and unsynchronized data structures tightly coupled chat input views, message dispatchers, and SQLite storage to Telegram's legacy singleton state.
- **Decision:** Introduce pure domain models `EphemeralBotCommandInfo`, `EphemeralMessageItem`, `WelcomeAnchorBinding`, `EphemeralMessagesState`, and `EphemeralMessageIdHelper`. Define abstract contract `EphemeralMessagesRepository` covering command parsing, ephemeral detection (`isEphemeralCommand`, `getEphemeralCommandBotId`), anchor binding lifecycle (`putAnchorBinding`, `removeAnchorBinding`, `getAnchorBindings`, `clearAnchorBindings`, `clearAllAnchorBindings`), and reactive state observation (`observeState`). Implement pure algorithmic use cases for command parsing (`ParseBotCommandUseCase`), ID packing/unpacking (`PackEphemeralMessageIdUseCase`, `UnpackEphemeralMessageIdUseCase`, `IsEphemeralMessageIdUseCase`), and anchor mutators. Provide thread-safe `LegacyEphemeralMessagesRepository` and bidirectional conversion in `EphemeralMessagesMapper`. Encapsulate presentation state and MVI events in `EphemeralMessagesViewModel`.
- **Consequences:** All ephemeral bot commands, ID packing, request interception, and welcome anchor bindings are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `EphemeralMessagesHelper.java`, `SendMessagesHelper.java`, and MTProto ephemeral update protocols.

### ADR 097: Isolation of Bot Guard WebApp Verification, Sessions & Decisions into feature.botguard
- **Context:** In Telegram Android, bot guard verification for joining chats, channels, or viewing protected resources was coordinated through `BotGuardHelper.java` (~119 lines). The helper tracked in-flight query-to-bot mappings (`queryIdToBotId`), evaluated confirmation prerequisites via `SharedPrefsHelper.isWebViewConfirmShown` and `MessagesController.whitelistedBots`, launched guard webapps via `BotWebViewSheet` with `TYPE_WEB_VIEW_GUARD`, received asynchronous decisions via `TL_updateJoinChatWebViewDecision` (`TLRPC.JoinChatBotResult`), dispatched global `NotificationCenter.guardBotDecisionResult` notifications, and dismissed active webview sheets. Because `MessagesController`, `LaunchActivity`, `JoinGroupAlert`, and `ArticleViewer` interacted directly with static controller instances and global event buses, testing session lifecycle, launch confirmation decision logic, and bulletin formatting was difficult.
- **Decision:** Introduce pure domain models `BotGuardDecisionStatus` (`Approved`, `Declined`, `Queued`, `WebView`, `Dismissed`, `Unknown`), `BotGuardDecisionResult`, `BotGuardSession`, `BotGuardLaunchDecision`, and `BotGuardState`. Define abstract contract `BotGuardRepository` covering reactive state observation (`observeState`), snapshot queries, session lifecycle (`registerSession`, `getSession`, `getAllActiveSessions`, `removeSession`, `clearAllSessions`), confirmation state persistence, whitelist verification, and decision flows (`observeDecisions`, `postDecision`). Implement pure domain use cases for confirmation requirements (`IsGuardBotConfirmationNeededUseCase`), launch flow decisions (`DetermineGuardBotLaunchFlowUseCase`), session close/dispatch (`CloseGuardBotSessionUseCase`), constructor result mapping (`MapJoinChatBotResultUseCase`), and bulletin formatting (`FormatGuardBotBulletinUseCase`). Provide thread-safe `LegacyBotGuardRepository` and bidirectional conversion in `BotGuardMapper`. Encapsulate presentation state and MVI events in `BotGuardViewModel`.
- **Consequences:** All bot guard webapp verification sessions, confirmation prompts, decision processing, and result banners are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `BotGuardHelper.java`, `SharedPrefsHelper.java`, and MTProto join chat protocols.

### ADR 096: Isolation of Story Custom Parameters & Local Story Translation State into feature.storycustomparams
- **Context:** In Telegram Android, local story-level parameters such as story translation flags, detected language, translated text with formatting entities, and target translation language were stored in SQLite database blobs (`StoriesStorage.java`) via `StoryCustomParamsHelper.java` (~100 lines). The helper handled reading and writing versioned binary payloads (`Params_v1`) to/from `NativeByteBuffer`, inspecting emptiness via `isEmpty()`, and copying fields between stories via `copyParams()`. Because `StoriesStorage` and story viewer components directly manipulated mutable fields on `TL_stories.StoryItem`, testing story parameter persistence, emptiness checks, and reactive state observation was difficult.
- **Decision:** Introduce pure domain models `StoryTranslationParamsModel`, `StoryCustomParamsModel`, and `StoryCustomParamsState`. Define abstract contract `StoryCustomParamsRepository` covering reactive state observation (`observeState`), snapshots (`getState`), individual story parameter access (`getParams`, `saveParams`, `removeParams`), granular translation updates (`updateTranslation`), parameter copying (`copyParams`), and global clearing (`clearAll`). Implement pure domain use cases for emptiness detection (`CheckStoryCustomParamsEmptyUseCase`), bitmask flag calculation (`ComputeStoryCustomParamsFlagsUseCase`), and translation mutators. Provide thread-safe `LegacyStoryCustomParamsRepository` and bidirectional conversion in `StoryCustomParamsMapper`. Encapsulate presentation state and MVI events in `StoryCustomParamsViewModel`.
- **Consequences:** All story translation flags, detected languages, translated texts, and target languages are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `StoryCustomParamsHelper.java` and `StoriesStorage.java`.

### ADR 095: Isolation of Bot Forum Topics & AI Streaming Drafts into feature.botforum
- **Context:** In Telegram Android, bot forum topics and real-time AI response streaming drafts were coordinated across `BotForumHelper.java` (~787 lines, 30.3KB), `ChatActivity.java`, `ChatActivityEnterView.java`, `ChatMessageCell.java`, `SendMessagesHelper.java`, and `MessagesController.java`. The helper managed live draft streaming text/rich actions (`TL_sendMessageTextDraftAction`, `TL_sendMessageRichMessageDraftAction`), draft lifecycle (TTL timeout self-destruct runnables, blocklists for manually stopped drafts), streaming send button states (`NO_STREAMING`, `BLOCKING`, `STOP`), auto-creation of forum topics for bot chats with editable topics via `beforeSendingFinalRequest` (`TL_messages_createForumTopic`), and topic draft replacement detection upon final message delivery. Direct interaction with static helper instances, SharedPreferences (`bot_drafts<account>`), and UI thread runnables tightly coupled chat rendering, input controls, and message dispatching to Telegram's legacy singleton state.
- **Decision:** Introduce pure domain models `StreamingSendButtonState`, `BotDraftMessageModel`, `BotForumTopicModel`, `BotForumDraftUpdateNotificationModel`, `BotForumDraftDeleteNotificationModel`, `BotForumTopicCreateNotificationModel`, and `BotForumState`. Define abstract contract `BotForumRepository` covering reactive state observation (`observeState`), snapshots (`getState`), button state evaluation (`getStreamingSendButtonState`), streaming persistence (`isStreamingTopic`, `saveIsStreamingTopic`), active draft queries (`hasBotForumDrafts`), draft stopping (`stopStreaming`), removed draft garbage collection (`removeAllMarkedAsRemovedMessages`), draft replacement detection (`checkNewMessageDraftReplacement`), and MTProto draft event handlers. Implement pure domain algorithms `DeriveTopicNameFromMessageUseCase` (first 16 characters + ellipsis or fallback title) and `ResolveStreamingButtonStateUseCase`. Provide thread-safe `LegacyBotForumRepository` and type conversions in `BotForumMapper`. Encapsulate presentation state and MVI events in `BotForumViewModel`.
- **Consequences:** All AI draft streaming, send button state transitions, topic name truncation logic, and bot forum topic lifecycles are decoupled behind clean, testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `BotForumHelper.java`.

### ADR 094: Isolation of Local Message Custom Parameters & Transcription/Summary State into feature.messagecustomparams
- **Context:** In Telegram Android, local message-level parameters such as voice transcription text and flags, AI text summarization and translation, message translations, poll translations, Stars price error bounds, and premium effect playback markers were stored in SQLite database blobs via `MessageCustomParamsHelper.java` (~223 lines). The helper handled reading and writing versioned binary payloads (`Params_v1`) to/from `NativeByteBuffer`, inspecting emptiness via `isEmpty()`, and copying fields between messages via `copyParams()`. Because `MessagesStorage` and message rendering adapters directly manipulated mutable fields on `TLRPC.Message`, testing parameter merging, emptiness checks, and reactive state observation was difficult.
- **Decision:** Introduce pure domain models `VoiceTranscriptionParamsModel`, `MessageSummaryParamsModel`, `MessageTranslationParamsModel`, `StarsErrorParamsModel`, `MessageCustomParamsModel`, and `MessageCustomParamsState`. Define abstract contract `MessageCustomParamsRepository` covering reactive state observation (`observeState`), snapshots (`getState`), individual message parameter access (`getParamsForMessage`, `setParamsForMessage`, `removeParams`), parameter copying (`copyParams`), and global clearing (`clearAll`). Implement pure domain use cases for emptiness detection (`CheckMessageCustomParamsEmptyUseCase`), parameter merging (`MergeMessageCustomParamsUseCase`), and domain-specific mutators for voice transcriptions, summaries, and translations. Provide thread-safe `LegacyMessageCustomParamsRepository` and bidirectional conversion in `MessageCustomParamsMapper`. Encapsulate presentation state and MVI events in `MessageCustomParamsViewModel`.
- **Consequences:** All voice transcription flags, AI summarization data, message translation state, and Stars pricing errors are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `MessageCustomParamsHelper.java` and `MessagesStorage.java`.

### ADR 093: Isolation of Fast Re-Login & Session Logout Tokens into feature.authtokens
- **Context:** In Telegram Android, persistent credentials used for quick re-login (`saved_tokens_login`) and tracking invalidated session states (`saved_tokens`) were managed statically in `AuthTokensHelper.java` (~129 lines). The helper handled hex serialization and deserialization of MTProto binary structures `TLRPC.TL_auth_authorization` and `TLRPC.TL_auth_loggedOut`, enforced a 20-item ceiling, and triggered system cloud backups via `BackupAgent.requestBackup()`. Because it was directly accessed by `LoginActivity`, `SettingsActivity`, `ProfileActivity`, and `MessagesController` as a static utility without interface boundaries or thread-safe state emission, token storage could not be observed reactively or tested in isolation.
- **Decision:** Introduce pure domain models `AuthTokenUserInfoModel`, `SavedLoginTokenModel`, `SavedLogoutTokenModel`, and `AuthTokensState`. Define abstract contract `AuthTokensRepository` covering reactive state observation (`observeState`), snapshots (`getState`), token mutation (`saveLoginToken`, `removeLoginToken`, `addLogoutToken`, `saveLogoutTokens`, `removeLogoutToken`), and full or category clearing (`clearAllTokens`, `clearLoginTokens`, `clearLogoutTokens`). Implement pure algorithmic use cases for list pruning to 20 elements (`PruneTokensListUseCase`) and hex format validation (`ValidateAuthTokenFormatUseCase`). Provide thread-safe `LegacyAuthTokensRepository` and TLRPC serialization in `AuthTokensMapper`. Encapsulate presentation state and MVI events in `AuthTokensViewModel`.
- **Consequences:** All authentication token caching, quick login credentials, logout token tracking, and cloud backup triggers are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `AuthTokensHelper.java`.

### ADR 092: Isolation of Background Media Auto-Delete & Cache Eviction Task into feature.autodeletemedia
- **Context:** In Telegram Android, automatic background media cleanup, LRU disk cache eviction according to user storage limits, 24-hour retention check intervals, and file access locking during ongoing playback/views were managed in `AutoDeleteMediaTask.java` (~278 lines, 12KB) and `CacheByChatsController.java`. The task periodically traverses media directories (`MEDIA_DIR_CACHE`, audio, document, video, photo, stories), evaluates retention policies per dialog type (User, Group, Channel, Stories) against `keep_media_type_*` settings, enforces `cache_limit` gigabyte ceilings via LRU eviction (sorting candidate files by last access time), and protects actively playing or loaded media via static `usingFilePaths` collections (`lockFile` / `unlockFile`). Legacy code directly invoked `AutoDeleteMediaTask.run()`, mutating static state and interacting directly with file paths without a reactive interface, which prevented testing of eviction ordering, size calculation algorithms, and lock management.
- **Decision:** Introduce pure domain models `CacheLimitConfig`, `MediaScanFileModel`, `AutoDeleteRunResult`, and `AutoDeleteTaskState`. Define abstract contract `AutoDeleteMediaRepository` covering reactive state observation (`observeState`), snapshot retrieval (`getState`), file locking/unlocking/checking (`lockFile`, `unlockFile`, `isFileLocked`, `clearLockedFiles`), and manual/forced cleanup triggering (`runCleanup`). Implement pure domain use cases for execution interval eligibility (`CheckShouldRunCleanupUseCase`) and LRU eviction planning (`CalculateEvictionCandidatesUseCase` respecting file locks and `KEEP_MEDIA_FOREVER` flags). Provide thread-safe `LegacyAutoDeleteMediaRepository` and type conversions in `AutoDeleteMediaMapper`. Encapsulate presentation state and MVI events in `AutoDeleteMediaViewModel`.
- **Consequences:** All cache size quota calculations, LRU eviction planning, cleanup scheduling, and file locking mechanisms are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `AutoDeleteMediaTask.java`.

### ADR 091: Isolation of Global Server Limits, Stars/TON Configuration & Feature Flags into feature.appconfig
- **Context:** In Telegram Android, global limits, financial parameters, and server-controlled feature flags from `TL_help.appConfig` were managed across `AppGlobalConfig.java` (~411 lines, 16.6KB) and `MessagesController.java` (`applyAppConfig()`). The subsystem manages dozens of essential client parameters: character limits for messages (4096 standard, 8192 Premium), polls configuration (max answers = 12, max length = 100), Rich Message formatting limits (32KB, 500 blocks, 20 table columns), AI Compose tone rewriting rules, channel/group call participant ceilings, as well as critical financial rules for Telegram Stars and TON (commission permilles, minimum/maximum suggested post and resale amounts, TON-USD exchange rates, rating URLs). In legacy code, these values were loaded from SharedPreferences into untyped internal config handlers and accessed by directly querying `MessagesController.getInstance(account).config` or global controller fields, creating hidden coupling across UI and background workers.
- **Decision:** Introduce pure domain models `StarsConfigModel`, `TonConfigModel`, `PollsConfigModel`, `RichMessageConfigModel`, `AiComposeConfigModel`, `AppLimitsConfigModel`, and aggregated `AppGlobalConfigState`. Define abstract contract `AppConfigRepository` covering reactive configuration observation (`observeConfig`), snapshots (`getConfig`), forced reload (`reloadConfig`), and in-memory/custom key overrides (`updateConfigValue`). Implement domain use cases (`GetMessageLimitsUseCase`, `GetStarsPricingConfigUseCase`, `GetTonPricingConfigUseCase`, `GetRichMessageLimitsUseCase`, `GetPollsConfigUseCase`, `GetAiComposeConfigUseCase`, `GetAppLimitsUseCase`, etc.) to provide typed access to limits and pricing logic. Provide thread-safe `LegacyAppConfigRepository` listening to `NotificationCenter.appConfigUpdated` via StateFlow, and safe extraction in `AppConfigMapper`. Encapsulate presentation state and MVI events in `AppConfigViewModel`.
- **Consequences:** All global limits, monetization boundaries, and backend-driven feature configurations are decoupled behind clean, testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `AppGlobalConfig.java` and `MessagesController.java`.

### ADR 090: Isolation of Power Saving, Battery Optimization & Animation Throttling into feature.litemode
- **Context:** In Telegram Android, battery consumption optimizations, hardware performance presets, and animation throttling flags were controlled globally via `LiteMode.java` (~365 lines) and `LiteModeSettingsActivity.java` (~530 lines). The legacy architecture managed 18 granular rendering bitmasks (animated stickers in keyboard/chat, animated emoji for chat/reactions/keyboard, forum two-column layouts, blur effects, scale transitions, Thanos vaporize animations, liquid glass, calls animations, autoplay videos/GIFs, particles) and 4 presets (`PRESET_LOW`, `PRESET_MEDIUM`, `PRESET_HIGH`, `PRESET_POWER_SAVER`). Battery level polling (`BatteryManager.BATTERY_PROPERTY_CAPACITY`) dynamically clamped all animations to `PRESET_POWER_SAVER` whenever battery dropped below `powerSaverLevel`, while emoji bitmasks required complex preprocessing depending on account Premium status (`UserConfig.hasPremiumOnAccounts()`) and tablet form-factor overrides. Direct coupling to static methods in `LiteMode` throughout UI components made testing performance degradation rules impossible.
- **Decision:** Introduce pure domain models `LiteModeFlag` (with bitmasks and flag clusters for stickers, emoji, and chat), `LiteModePreset` (POWER_SAVER, LOW, MEDIUM, HIGH, CUSTOM), and `LiteModeState`. Define abstract contract `LiteModeRepository` for reactive state observation (`observeLiteModeState`), snapshots (`getLiteModeState`), flag toggling (`setFlagEnabled`), batch flag setting (`setAllFlags`), preset application (`applyPreset`), power saver battery threshold configuration (`setPowerSaverThreshold`), battery capacity simulation (`updateBatteryLevel`), and Premium state synchronization (`setHasPremium`). Implement pure algorithmic use cases for effective flag computation (`CalculateEffectiveFlagsUseCase` handling power-saver zeroing, premium emoji bit redirection, and tablet two-column guarantees), flag checking (`CheckLiteModeFlagUseCase`), and preset resolution (`ResolvePresetUseCase`). Provide thread-safe `LegacyLiteModeRepository` and integer codecs in `LiteModeMapper`. Encapsulate presentation state and MVI events in `LiteModeViewModel`.
- **Consequences:** All power saving policies, battery threshold automations, animation throttling masks, and performance preset logic are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `LiteMode.java`.

### ADR 089: Isolation of In-App Web Browser, Custom Tabs & Deep Link Routing into feature.browser
- **Context:** In Telegram Android, web browsing and URL dispatching were handled across `Browser.java` (~870 lines, 38KB), `WebBrowserSettings.java` (~707 lines), and `BrowserHistory.java` (~184 lines). The legacy subsystem resolved external URLs, Telegram deep links (`tg://`, `t.me/`, `telegram.me/`, `telegram.dog/`), Instant View URLs (`telegra.ph`, `graph.org`, `t.me/iv?`), and TON sites (`ton://`, `.ton`), binding to Chrome Custom Tabs (`CustomTabsClient`, `CustomTabsSession`), managing in-app webview configuration (`TL_account.TL_webBrowserSettings`), and recording browsing history (`webhistory.dat`). Crucially, URL safety verification, anti-phishing protection (preventing IDN homoglyph punycode spoofing), and confirmation dialog logic (`urlMustNotHaveConfirmation`) were tightly coupled with Android UI dialogs and activities, preventing clean unit testing of URL classification and navigation rules.
- **Decision:** Introduce pure domain models `BrowserType` (IN_APP, CUSTOM_TABS, EXTERNAL_BROWSER), `UrlTargetType` (TELEGRAM_INTERNAL, INSTANT_VIEW, TON_SITE, EXTERNAL_SAFE, EXTERNAL_UNTRUSTED), `UrlSafetyCheckResult` (safety status, confirmation requirements, punycode spoofing flag, extracted username, host), `BrowserHistoryEntryModel`, `BrowserSettingsModel`, and `BrowserState`. Define abstract contract `BrowserRepository` for reactive state observation (`observeBrowserState`), snapshots (`getBrowserState`), settings mutation (`updateBrowserType`, `updateSettings`), URL classification (`classifyUrl`), opening (`openUrl`), and history management (`addHistoryEntry`, `getHistory`, `clearHistory`, `clearCacheAndCookies`). Implement pure domain logic in `ClassifyUrlTargetUseCase`, `ExtractUsernameFromUrlUseCase`, and `CheckUrlSafetyUseCase` with robust detection for mixed-script Cyrillic/Latin homoglyphs and `xn--` punycode spoofing. Provide thread-safe `LegacyBrowserRepository` and codecs in `BrowserMapper`. Encapsulate presentation state and MVI events in `BrowserViewModel`.
- **Consequences:** All URL classification, anti-phishing validation, browser mode selection, and history tracking are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `Browser.java` and `WebBrowserSettings.java`.

### ADR 088: Isolation of Inbound Push Notifications, Payloads & Remote Actions into feature.pushlistener
- **Context:** In Telegram Android, inbound push notification handling, multi-account device token distribution (`sendRegistrationToServer`), encrypted payload unpacking (`aesIgeEncryption`, `computeSHA256` auth key validation), remote payload routing (`loc_key`: `DC_UPDATE`, `MESSAGE_ANNOUNCEMENT`, `SESSION_REVOKE`, `GEO_LIVE_PENDING`, `OAUTH_REQUEST`, `CONF_CALL_REQUEST`, `READ_HISTORY`, `READ_STORIES`, `STORY_DELETED`, `MESSAGE_DELETED`, `READ_REACTION`), and background thread synchronization via `CountDownLatch` were located in `PushListenerController.java` (~1,735 lines, 120KB). Direct coupling with `NotificationsController`, `MessagesController`, `VoIPGroupNotification`, and Android background services made incoming push logic difficult to test and maintain without risking regressions in critical message delivery and incoming VoIP ringing.
- **Decision:** Introduce pure domain models `PushType` (FIREBASE, HUAWEI), `PushActionType` (DC_UPDATE, MESSAGE_ANNOUNCEMENT, SESSION_REVOKE, GEO_LIVE_PENDING, OAUTH_REQUEST, VOIP_CALL, READ_HISTORY, READ_STORIES, STORY_DELETED, MESSAGE_DELETED, READ_REACTION, NEW_MESSAGE, UNKNOWN), `PushDecryptStatus` (SUCCESS, INVALID_KEY_ID, INVALID_MAC, DECODE_ERROR, PAYLOAD_CORRUPTED), `PushPayloadModel`, `PushProcessResult`, and `PushListenerState`. Define abstract contract `PushListenerRepository` for reactive state observation (`observeState`, `observeIncomingPushes`), snapshot retrieval (`getState`), payload processing (`processPush`), token registration (`registerToken`), error reporting (`reportDecryptError`), and listening toggling. Implement pure domain logic in `DeterminePushActionTypeUseCase` and `ParsePushJsonPayloadUseCase` with robust regex fallback for headless JVM environments. Provide thread-safe `LegacyPushListenerRepository` and legacy type adapters in `PushListenerMapper`. Encapsulate presentation state and MVI events in `PushListenerViewModel`.
- **Consequences:** Inbound push notification payload parsing, action routing, token registration lifecycle, and decryption monitoring are cleanly isolated behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `PushListenerController.java`.

### ADR 087: Isolation of Network Traffic & Data Usage Statistics into feature.networkstats
- **Context:** In Telegram Android, network data usage statistics across connection types (Mobile, Wi-Fi, Roaming) and traffic categories (Calls, Messages, Videos, Audios, Photos, Files, Total, Music) were tracked by `StatsController.java` (~293 lines). The legacy controller managed 2D long/int arrays (`sentBytes[3][8]`, `receivedBytes[3][8]`, `sentItems[3][8]`, `receivedItems[3][8]`), call duration (`callsTotalTime[3]`), and reset timestamps (`resetStatsDate[3]`), persisting binary data to `stats2.dat` via `RandomAccessFile`. Crucially, messages data volume was not recorded directly but calculated on the fly as `sentBytes[TOTAL] - FILES - AUDIOS - VIDEOS - PHOTOS - MUSIC`. UI classes like `DataUsageActivity.java` directly queried static instance methods of `StatsController`, coupling presentation rendering to legacy raw array indexing and global state.
- **Decision:** Introduce pure domain models `NetworkType` (MOBILE, WIFI, ROAMING), `TrafficCategory` (CALLS, MESSAGES, VIDEOS, AUDIOS, PHOTOS, FILES, TOTAL, MUSIC), `TrafficItemModel` (category, sentBytes, receivedBytes, sentItems, receivedItems), `NetworkStatsSummaryModel` (networkType, items map, callsTotalTimeSec, resetStatsDateMs), and `NetworkStatsState`. Define abstract contract `NetworkStatsRepository` for reactive flow observation (`observeStats`, `observeAllStats`), snapshots (`getStats`, `getAllStats`), granular increments (`incrementSentBytes`, `incrementReceivedBytes`, `incrementSentItems`, `incrementReceivedItems`, `incrementCallsTotalTime`), stats resetting (`resetStats`), and refreshing. Implement pure domain logic in `CalculateMessagesTrafficUseCase` (subtracting media traffic from total with zero-clamping), `FormatTrafficBytesUseCase` (B, KB, MB, GB, TB with `Locale.US`), and `FormatCallsDurationUseCase`. Provide thread-safe `LegacyNetworkStatsRepository` and legacy type codecs in `NetworkStatsMapper`. Encapsulate presentation state and MVI events in `NetworkStatsViewModel`.
- **Consequences:** All network traffic calculations, data usage accumulation, call duration tracking, and reset operations are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `StatsController.java`.

### ADR 086: Isolation of Custom Notification Sounds, Ringtones & Cloud Uploader into feature.ringtones
- **Context:** In Telegram Android, custom notification sounds and ringtones management, cloud synchronization (`TL_account.getSavedRingtones`, `TL_account.saveRingtone`, `TL_account.uploadRingtone`), local binary document caching in preferences (`ringtones_pref_<userId>`), format and constraint validation (maximum 5 seconds duration, 300 KB max size, supported MIME types: `audio/mpeg`, `audio/ogg`, `audio/m4a`), upload tracking, and sound path resolution were split across `RingtoneDataStore.java` (~331 lines), `RingtoneUploader.java` (~99 lines), and `MediaDataController.java` (`uploadRingtone`, `saveToRingtones`, `onRingtoneUploaded`). UI activities like `NotificationsCustomSettingsActivity`, `ChatActivity`, and notification sound pickers directly interacted with these legacy classes and posted global `NotificationCenter` broadcasts (`onUserRingtonesUpdated`, `showBulletin`).
- **Decision:** Introduce pure domain models `RingtoneErrorCode` (NONE, TOO_LONG, TOO_BIG, UNSUPPORTED_FORMAT, FILE_NOT_FOUND), `RingtoneUploadStatus`, `RingtoneModel` (id, title, durationSec, sizeBytes, mimeType, localUri, isUploading), `RingtoneValidationResult`, `RingtoneLimitsModel` (5s max duration, 300KB max size), and `RingtoneState`. Define abstract contract `RingtoneRepository` covering reactive state observation (`observeState`, `observeRingtones`), snapshot retrieval (`getState`, `getRingtones`, `getRingtoneById`, `getRingtoneSoundPath`), document saving (`saveRingtoneFromDocument`), adding and removing ringtones (`addRingtone`, `removeRingtone`), uploading with pending tone tracking (`uploadRingtone`), cancellation (`cancelUpload`), and selection (`selectRingtone`). Implement pure validation logic in `ValidateRingtoneEligibilityUseCase`, duration formatting ("0:03", "1:05") and file size formatting in `RingtoneMapper`, and thread-safe adapter `LegacyRingtoneRepository`. Encapsulate presentation state, preview playback toggle, and MVI events in `RingtoneViewModel`.
- **Consequences:** All custom notification sounds and ringtone validation rules, upload lifecycle management, duration/size constraint checks, and sound path resolution are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `RingtoneDataStore.java` and `RingtoneUploader.java`.

### ADR 085: Isolation of Language Packs, Pluralization, Relative Timestamps & RTL Detection into feature.localization
- **Context:** In Telegram Android, language pack downloading, plural form rules across diverse language families (English, Slavic, Polish, Arabic, etc.), string formatting (`LocaleController.getString`, `formatPluralString`), relative timestamps ("just now", "Xm ago", "Xh ago", "Xd ago"), name ordering (`FIRST_LAST` vs `LAST_FIRST`), 24-hour vs 12-hour time preferences, number abbreviations (1.2K, 3.4M), and right-to-left (RTL) language detection were governed by `LocaleController.java` (~4,517 lines, 199KB) in `org.telegram.messenger`. Direct coupling existed between Android `Context`, `Configuration`, `Resources`, raw XML language pack parsing, SQLite storage of language dictionaries (`loadRemoteLanguages`, `applyLanguageFile`), and global event broadcasts (`NotificationCenter.reloadInterface`, `suggestedLangpack`).
- **Decision:** Introduce pure domain models `PluralQuantity` (ZERO, ONE, TWO, FEW, MANY, OTHER), `NameDisplayOrder` (FIRST_LAST, LAST_FIRST), `LocaleModel`, `RelativeTimeModel`, `LocalizationConfigModel`, and `LocalizationState`. Define abstract contract `LocalizationRepository` covering reactive state observation (`observeState`), snapshot retrieval (`getState`), current locale get/set (`getCurrentLocale`, `setCurrentLocale`), available locales querying/updating (`getAvailableLocales`, `setAvailableLocales`), string resolution with override lookups (`getString`), custom string overrides manipulation (`setCustomStrings`, `clearCustomStrings`), 24-hour format configuration (`set24HourFormat`), and name display order (`setNameDisplayOrder`). Implement pure algorithmic use cases for plural quantity resolution supporting Slavic (ru, uk, be), Polish (pl), and Arabic (ar) grammar rules (`ResolvePluralQuantityUseCase`), relative timestamp formatting (`FormatRelativeTimestampUseCase`), name ordering (`FormatFullNameUseCase`), metric suffix abbreviation (`FormatNumberWithSuffixUseCase` using `Locale.US`), and right-to-left script detection (`DetectRtlLanguageUseCase`). Provide thread-safe `LegacyLocalizationRepository` and string substitution mappers in `LocalizationMapper`. Encapsulate presentation state and MVI events in `LocalizationViewModel`.
- **Consequences:** All localization rules, plural category resolutions, relative timestamp calculations, RTL detection, and string overrides are cleanly decoupled behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `LocaleController.java`.

### ADR 084: Isolation of Downloads Queue, Auto-Download Rules & Network Presets into feature.downloadmanager
- **Context:** In Telegram Android, automatic media downloading, network rules across cellular/Wi-Fi/roaming networks, media size constraints (photos, videos, documents, audio across contacts, private chats, groups, channels), video/music/stories preloading, active download queue monitoring (`downloadingFiles`, `recentDownloadingFiles`, `unviewedDownloads`), and progress notifications were managed by `DownloadController.java` (~1,811 lines) in `org.telegram.messenger`. Direct coupling existed with `ConnectivityManager` network broadcasts, SQLite persistence of auto-download presets, low-level file progress listeners, and UI components (`SearchDownloadsContainer`, `DownloadsInfoBottomSheet`, `DataAutoDownloadActivity`).
- **Decision:** Introduce pure domain models `AutoDownloadMediaType` (PHOTO, VIDEO, DOCUMENT, AUDIO), `AutoDownloadNetwork` (CELLULAR, WIFI, ROAMING), `PeerTypePreset` (CONTACTS, PRIVATE_CHATS, GROUPS, CHANNELS), `DownloadItemStatus` (QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED), `DownloadPresetModel` (mask, maxSizes, preloading flags, call data, maxVideoBitrate), `DownloadItemModel`, `DownloadManagerStats`, and `DownloadManagerState`. Define abstract contract `DownloadManagerRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), file streams (`observeDownloadingFiles`, `observeRecentFiles`), lifecycle actions (`enqueueDownload`, `pauseDownload`, `resumeDownload`, `cancelDownload`, `retryDownload`, `deleteRecentDownload`, `clearRecentDownloads`, `markDownloadsAsViewed`), progress updating (`updateDownloadProgress`), completion and failure reporting (`completeDownload`, `failDownload`), network switching (`setNetworkType`), eligibility evaluation (`shouldAutoDownload`), and preset management (`getPreset`, `updatePreset`). Implement pure algorithmic use cases for media eligibility evaluation (`EvaluateAutoDownloadEligibilityUseCase`), moving speed calculations (`CalculateDownloadSpeedUseCase`), and state manipulation. Provide thread-safe `LegacyDownloadManagerRepository` and bitmask codecs/formatters in `DownloadManagerMapper`. Encapsulate presentation state and MVI events in `DownloadManagerViewModel`.
- **Consequences:** All auto-download rules, network switching adaptations, download queue lifecycles, speed calculations, and unviewed download notifications are cleanly decoupled behind testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `DownloadController.java`.

### ADR 083: Isolation of Memory Cache Tiers, Downscaling, Filter Specs & Image Request Pipeline into feature.imageloader
- **Context:** In Telegram Android, image downloading, bitmap decoding, downsampling, filter string parsing (`100_100_b_f`, `80_80_r`, `g`, `gl`, `firstframe`, `lastframe`, `lastreactframe`, `pframe`, `isc`, `exif`, `ignoreOrientation`), multiple LRU memory cache tiers (`memCache`, `smallImagesMemCache`, `wallpaperMemCache`, `lottieMemCache`), artwork tasks, disk cache paths, and system memory trimming (`TRIM_MEMORY_*`) were centralized in `ImageLoader.java` (~4,650 lines, 225KB) in `org.telegram.messenger`. High coupling existed between low-level Android decoders (`BitmapFactory.Options.inSampleSize`, `MediaMetadataRetriever`), `ImageReceiver` instances across cells and activities, `NotificationCenter` broadcasts, and custom `LruCache` implementations.
- **Decision:** Introduce pure domain models `ImageCacheTier` (DEFAULT, SMALL, WALLPAPER, LOTTIE), `ImageLoadingStatus` (IDLE, QUEUED, LOADING, LOADED, FAILED, CANCELLED), `FrameExtractType` (NONE, FIRST_FRAME, LAST_FRAME, LAST_REACT_FRAME, PREVIEW_FRAME), `ImageFilterSpec`, `ImageDownscaleSpec`, `ImageRequestModel`, `TierCacheStats`, `ImageCacheStatsModel`, and `ImageLoaderState`. Define abstract contract `ImageLoaderRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), cache metrics (`observeCacheStats`, `getCacheStats`), presence checking (`hasInCache`), cache mutation (`putCacheEntry`, `removeCacheEntry`, `clearCache`), memory pressure responses (`trimMemory`), request coordination (`enqueueRequest`, `cancelRequest`, `updateRequestStatus`), and hit/miss auditing (`recordCacheHit`, `recordCacheMiss`). Implement pure algorithmic use cases for parsing and serializing Telegram filter strings (`ParseImageFilterUseCase`, `FormatImageFilterUseCase`), canonical cache key construction (`BuildImageCacheKeyUseCase`), power-of-2 `inSampleSize` calculations and aspect-ratio downscaling (`CalculateImageDownscaleUseCase`), cache tier arbitration (`EvaluateImageCacheEligibilityUseCase`), request lifecycle orchestration (`EnqueueImageRequestUseCase`, `CancelImageRequestUseCase`), and memory trimming (`TrimImageMemoryUseCase`, `ClearImageCacheUseCase`). Provide thread-safe `LegacyImageLoaderRepository` with LRU tier stores and pure size formatters in `ImageLoaderMapper`. Encapsulate presentation state and MVI events in `ImageLoaderViewModel`.
- **Consequences:** All image caching policies, filter string parsing, downscaling geometry, cache tier resolution, request lifecycle management, and memory pressure responses are isolated behind testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `ImageLoader.java`.

### ADR 082: Isolation of Message Sending Pipeline, Media Uploads, Albums & Forwards into feature.sendmessages
- **Context:** In Telegram Android, message transmission was orchestrated by `SendMessagesHelper.java` (~14,000 lines, 755KB) in `org.telegram.messenger`. It handled preparation of texts, photos, videos, audios, documents, voice notes, stickers, round video notes, contacts, locations, and polls, batched media albums (grouped media up to 10 items), chunked file uploads over MTProto, forwarding mechanisms (`TLRPC.TL_messages_forwardMessages`) with sender name/caption stripping, message scheduling (`scheduleDate`, `scheduleRepeatPeriod`), silent notifications (`notify = false`), paid stars sending (`payStars`), view-once self-destruct timers, retries, network failure backoff, and pending queues. UI components across `ChatActivity`, `PhotoViewer`, `ChatAttachAlert`, and external share receivers directly interacted with `SendMessagesHelper.getInstance(account)`.
- **Decision:** Introduce pure domain models `SendMediaType` (11 types), `SendStatus` (PENDING, PREPARING, UPLOADING, SENDING, SUCCESS, FAILED, CANCELLED), `ForwardMode` (STANDARD, HIDE_NAMES, HIDE_CAPTIONS), `SendOptionsModel` (notify, scheduleDate, repeatPeriod, ttl, replyToMsgId, topMsgId, payStars, effectId, sendAsPeerId, invertMedia, hasSpoiler), `SendMediaItem`, `SendAlbumModel` (strictly enforced 1..10 items limit), `ForwardRequestModel`, `PendingSendModel`, and `SendMessagesState`. Define abstract contract `SendMessagesRepository` covering pending sends observation (`observePendingSends`), snapshot retrieval (`getPendingSends`), sending operations (`sendText`, `sendMedia`, `sendAlbum`, `forwardMessages`), lifecycle actions (`retrySend`, `cancelSend`, `cancelAll`), progress tracking (`updateProgress`), and completion reporting (`markSuccess`, `markFailed`). Implement pure algorithmic use cases for character limit validation (4096 standard / 8192 premium via `SendTextMessageUseCase`), media size constraints (2GB standard / 4GB premium via `SendMediaMessageUseCase`), batched album validation (`SendMediaAlbumUseCase`), multi-message forwarding (`ForwardMessagesUseCase`), retries and cancellations. Provide thread-safe `LegacySendMessagesRepository` and file size/progress math in `SendMessagesMapper`. Encapsulate presentation state and MVI events in `SendMessagesViewModel`.
- **Consequences:** The entire message sending pipeline, media upload coordination, album batching, and forwarding modes are decoupled behind clean, testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's 14k+ line `SendMessagesHelper.java`.

### ADR 081: Isolation of Audio/Voice/Video Note Playback, Playlist Queue, Speed, Repeat & Proximity into feature.audioplayer
- **Context:** In Telegram Android, audio track, podcast, voice note, and round video playback, playlist queue management, playback speed (0.5x..2.5x), repeat modes (NONE, ALL, CURRENT), shuffle order, proximity sensor routing (switching between loudspeaker and earpiece on raise-to-listen), and equalizer state were handled by `MediaController.java` (~6500 lines) and `AudioPlayerAlert.java` (~1700 lines). The legacy controller directly coupled Android `MediaPlayer`/ExoPlayer hardware decoders, `SensorManager` proximity listener callbacks, `AudioManager` focus changes, `NotificationCenter` broadcasts, and direct `ChatMessageCell`/`ChatActivity` UI state mutations.
- **Decision:** Introduce pure domain models `AudioTrackType`, `AudioPlaybackStatus`, `RepeatMode`, `AudioOutputRoute`, `AudioTrackModel`, `EqualizerBand`, `EqualizerState`, and `AudioPlaybackState`. Define abstract contract `AudioPlayerRepository` covering state observation (`observePlaybackState`), snapshot retrieval (`getPlaybackState`), playback controls (`play`, `resume`, `pause`, `stop`, `seekTo`, `seekToProgress`, `next`, `previous`), speed configuration (`setPlaybackSpeed`), repeat/shuffle toggles (`toggleRepeatMode`, `toggleShuffle`), proximity and audio routing (`setProximityNear`, `setOutputRoute`), and equalizer configuration (`setEqualizerEnabled`, `setEqualizerBandGain`, `setBassBoost`). Implement pure algorithmic use cases for playback transitions (`PlayTrackUseCase`, `TogglePlayPauseUseCase`), boundary-clamped seek (`SeekAudioUseCase`), repeat- and shuffle-aware playlist navigation with 3-second rewind rule (`NavigatePlaylistUseCase`), stepped speed cycling (`CyclePlaybackSpeedUseCase`), repeat mode cycling (`CycleRepeatModeUseCase`), proximity-aware audio routing to earpiece (`HandleProximitySensorUseCase`), and equalizer management (`ConfigureEqualizerUseCase`). Provide thread-safe `LegacyAudioPlayerRepository` with standalone state machine and pure formatting/math in `AudioPlayerMapper`. Encapsulate presentation state and MVI events in `AudioPlayerViewModel`.
- **Consequences:** Audio and voice note playback, playlist navigation, seek progress, speed cycling, repeat/shuffle logic, and proximity routing are isolated behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `MediaController.java`.

### ADR 080: Isolation of Chat Message Input, Formatting, Voice Recording, Panels & Reply Bar into feature.chatinput
- **Context:** In Telegram Android, message text editing, draft state, rich text formatting (bold, italic, mono, spoiler, quote, strike, underline), voice note and round video recording, audio waveform capture, audio recording lock, pause, and preview, send options (silent, scheduled, paid stars, ttl / view-once), reply & edit preview banners, and virtual input panels (keyboard, emoji/stickers, bot keyboard, attachment sheet) were managed by `ChatActivityEnterView.java` (~15,647 lines) in `org.telegram.ui.Components`. The view coupled low-level Android View and window management (`FrameLayout`, `EditTextCaption`, `LinearLayout`, `AnimatedTextView`), touch velocity tracking, audio recording timers, `MediaController` hardware recorder callbacks, direct `NotificationCenter` listeners, and delegate callbacks across `ChatActivity`.
- **Decision:** Introduce pure domain models `RecordType` (VOICE, ROUND_VIDEO), `RecordStatus` (IDLE, RECORDING, LOCKED, PAUSED, PREVIEW), `EnterViewPanelMode` (NONE, KEYBOARD, EMOJI_STICKER, BOT_KEYBOARD, ATTACH_ALERT), `TextFormatStyle` (BOLD, ITALIC, MONO, STRIKETHROUGH, UNDERLINE, SPOILER, QUOTE), `ChatInputReplyQuote`, `ChatInputSendOptions`, `ChatInputRecordState`, and `ChatInputState`. Define abstract contract `ChatInputRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), text and cursor mutation (`setText`), virtual panel selection (`setPanelMode`), reply/edit quotes management (`setReplyMessage`, `setEditMessage`, `clearReplyOrEdit`), send options mutation (`updateSendOptions`), voice/video recording lifecycle (`startRecording`, `lockRecording`, `pauseRecording`, `resumeRecording`, `cancelRecording`, `stopRecording`), progress updates (`updateRecordProgress`), view-once toggle (`toggleVoiceOnce`), and state reset (`reset`). Implement pure algorithmic use cases for dynamic Send vs Record button arbitration (`CalculateSendButtonStateUseCase`), text selection wrapping with markdown tokens and cursor compensation (`FormatTextSelectionUseCase`), voice recording permission and draft presence validation (`ValidateVoiceRecordActionUseCase`), and panel toggle arbitration (`ResolvePanelVisibilityUseCase`). Provide thread-safe `LegacyChatInputRepository` and duration formatting in `ChatInputMapper`. Encapsulate presentation state and MVI events in `ChatInputViewModel`.
- **Consequences:** The entire chat message input bar domain logic, recording state machine, text formatting algorithms, send options, and reply/edit quotes are cleanly isolated behind testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's 15k+ line `ChatActivityEnterView.java`.

### ADR 079: Isolation of Fullscreen Photo/Video Viewer, Gestures, Playback & Editor into feature.photoviewer
- **Context:** In Telegram Android, fullscreen photo and video viewing, animated media, stories, avatars, wallpapers, stickers, and GIF inspection were managed by `PhotoViewer.java` (~23,753 lines) in `org.telegram.ui`. The class coupled low-level Android View and window management (`FrameLayout`, `WindowManager`, `SurfaceView`, `TextureView`), hardware video decoding and internal video player instances (`VideoPlayer`, `VideoPlayerRewinder`), gesture detectors (pinch-to-zoom, double-tap zoom, translation dragging, dismiss swipes), PIP mode delegates (`IPipSourceDelegate`), photo/video editor mode switching (`CROP`, `FILTER`, `PAINT`, `STICKER_MASK`, `COVER`), action bar inflation, action routing (forward, share, delete, save to gallery, rotate, set avatar, speed, quality), and legacy integer modes (`SELECT_TYPE_*`, `EDIT_MODE_*`).
- **Decision:** Introduce pure domain models `ViewerMediaType` (PHOTO, VIDEO, GIF, ANIMATED_STICKER, ROUND_VIDEO), `ViewerSelectType` (NO_SELECT, AVATAR, WALLPAPER, QR, STICKER, GIF, POLL_MEDIA, POLL_MEDIA_EDIT), `ViewerEditMode` (NONE, CROP, FILTER, PAINT, STICKER_MASK, COVER), `ViewerActionType` (SEND, FORWARD, SHARE, SAVE_TO_GALLERY, DELETE, EDIT, SET_AVATAR, ROTATE, PIP, SPEED, QUALITY), `PhotoViewerMediaItem`, `PhotoViewerPlaybackState`, `PhotoViewerTransform`, and `PhotoViewerState`. Define abstract contract `PhotoViewerRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), media session opening (`open`), paging (`navigateTo`, `next`, `previous`), editing (`setEditMode`), UI visibility toggles (`toggleActionBar`, `toggleCaptionExpanded`), gesture transforms (`updateTransform`, `resetTransform`), playback control (`updatePlayback`), action execution (`executeAction`), and dismissal (`close`). Implement pure algorithmic use cases for boundary-safe paging (`CalculateMediaPagingUseCase`), zoom clamping with translation reset on 1.0x and 90° rotation snapping (`CalculateZoomTransformUseCase`), media/select mode action validation (`ValidateViewerActionsUseCase`), and video resolution resolution with bitrate estimation (`ResolveMediaQualityUseCase`). Provide thread-safe `LegacyPhotoViewerRepository` and timecode formatters in `PhotoViewerMapper`. Encapsulate presentation state and MVI events in `PhotoViewerViewModel`.
- **Consequences:** The entire fullscreen media viewer domain logic, gesture bounds, zoom calculations, video playback controls, and editor mode arbitration are cleanly isolated behind testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's 23k+ line `PhotoViewer.java`.

### ADR 078: Isolation of Chat Attachment Dialog & Layouts into feature.chatattach
- **Context:** In Telegram Android, the chat attachment modal bottom sheet and its sub-layouts were managed by `ChatAttachAlert.java` (~7273 lines) and associated classes (`ChatAttachAlertPhotoLayout`, `ChatAttachAlertDocumentLayout`, `ChatAttachAlertAudioLayout`, `ChatAttachAlertLocationLayout`, `ChatAttachAlertContactsLayout`, `ChatAttachAlertPollLayout`, etc.) in `org.telegram.ui.Components`. The alert tightly coupled Android Views (`BottomSheet`, `RecyclerView`, `EditTextBoldCursor`, `AnimatedTextView`), low-level animator frameworks (`ReplaceAnimator`, `BoolAnimator`), custom camera and gallery triggers (`ImageUpdater`), direct `NotificationCenter` event dispatching, media multi-selection indexing, caption length limit calculations (standard 1024 vs premium 2048), spoilered media toggles, and send-as-file configuration.
- **Decision:** Introduce pure domain models `ChatAttachLayoutType` (PHOTO, MUSIC, DOCUMENTS, CONTACTS, LOCATION, POLL, REPLIES, TODO, STICKERS, EMOJI, LINK, RICH), `ChatAttachItem`, `ChatAttachSendOptions`, `ChatAttachPermissions`, `CaptionLimitInfo`, and `ChatAttachState`. Define abstract contract `ChatAttachRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), bottom sheet presentation (`openAlert`), layout tab selection (`selectLayout`), available layouts configuration (`setAvailableLayouts`), item selection and re-ordering (`toggleItemSelection`, `setSelectedItems`, `clearSelection`), options mutation (`updateSendOptions`), permission updates (`setPermissions`), and alert dismissal (`dismissAlert`, `clear`). Implement pure algorithmic use cases for permission-based tab resolution (`ResolveAvailableAttachLayoutsUseCase`), codepoint-based caption limit checking (`CalculateAttachCaptionLimitUseCase`), selection bounds and order arbitration (`ToggleAttachItemSelectionUseCase`), and options validation (`ValidateSendOptionsUseCase`). Provide thread-safe `LegacyChatAttachRepository` and clean integer-to-enum layout conversions in `ChatAttachMapper`. Encapsulate presentation state and MVI events in `ChatAttachViewModel`.
- **Consequences:** All chat attachment arbitration, layout resolution, caption limits, multi-selection ordering, and send modifier validation are decoupled behind clean, testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `ChatAttachAlert`.

### ADR 077: Isolation of Emoji, Sticker & GIF Picker Keyboard Panel into feature.emojipicker
- **Context:** In Telegram Android, the bottom keyboard panel for selecting emojis, stickers, and GIFs was implemented in `EmojiView.java` (~10266 lines) in `org.telegram.ui.Components`. The view coupled complex Android UI widgets (`ViewPager`, `PagerSlidingTabStrip`, `ScrollSlidingTextTabStrip`, `EmojiGridView`, `RecyclerListView`, `GridLayoutManager`, `GifLayoutManager`), custom background blur shaders (`BlurredBackgroundSourceColor`, `BlurredBackgroundDrawableViewFactory`), low-level keyboard insets (`InAppKeyboardInsetView`), direct database/network requests via `MediaDataController`, search adapters (`EmojiSearchAdapter`, `GifSearchAdapter`, `StickersGridAdapter`), recent/favorite caches (`Emoji.recentEmoji`, `MediaDataController`), and delegate callbacks in `EmojiViewDelegate`.
- **Decision:** Introduce pure domain models `EmojiPickerTabType` (EMOJI, GIFS, STICKERS), `EmojiCategoryType`, `EmojiItem`, `StickerItem`, `GifItem`, `StickerPackItem`, `EmojiPickerFilter`, and `EmojiPickerState`. Define abstract contract `EmojiPickerRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), filter configuration (`configureFilter`), tab selection (`selectTab`), search query updates (`setSearchQuery`, `setSearchActive`), item/pack updating (`setRecentEmojis`, `setStickerPacks`, `setRecentStickers`, `setFavoriteStickers`, `setRecentGifs`, `setTrendingGifs`), sticker favorite toggling (`toggleStickerFavorite`), clearing recent items by tab (`clearRecent`), and lifecycle cleanup (`clear`). Implement pure algorithmic use cases for dynamic available tabs resolution based on allowed modes (`ResolveAvailablePickerTabsUseCase`), item filtering (`FilterEmojiItemsUseCase`, `FilterStickersUseCase`, `FilterGifsUseCase`), and state manipulation. Provide thread-safe `LegacyEmojiPickerRepository` and clean integer-to-enum conversions in `EmojiPickerMapper`. Encapsulate presentation state and MVI events in `EmojiPickerViewModel`.
- **Consequences:** The entire emoji, sticker, and GIF keyboard selection business logic, tab arbitration, search queries, favorites, and recent history are cleanly separated behind testable domain interfaces with full unit test coverage, completely decoupled from `EmojiView.java`'s 10k-line view hierarchy while preserving 100% backward compatibility.

### ADR 076: Isolation of Long-Press Content Preview, Gestures, and Action Menus into feature.contentpreview
- **Context:** In Telegram Android, long-press previewing of stickers, GIFs, emojis, custom animated stickers, and polls was handled by `ContentPreviewViewer.java` (~2423 lines) in `org.telegram.ui`. The viewer coupled low-level Android `MotionEvent` touch interception in `RecyclerListView`, custom window management (`WindowManager.LayoutParams`, `Activity`), Canvas drawing, blur shaders (`BlurredBackgroundSourceBitmap`, `BlurredBackgroundDrawableViewFactory`), `ActionBarPopupWindow` menu inflation, `ReactionsContainerLayout`, `PaintingOverlay`, and `ContentPreviewViewerDelegate` callbacks for sending, favoriting, copying, and scheduling.
- **Decision:** Introduce pure domain models `PreviewContentType` (NONE, STICKER, GIF, EMOJI, CUSTOM_STICKER), `PreviewActionType`, `PreviewActionItem`, `ContentPreviewGesture` (drag distance, normalized progress 0..1, menu threshold check), `ContentPreviewItem`, and `ContentPreviewState`. Define abstract contract `ContentPreviewRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), preview opening (`openPreview`), drag progress updates (`updateDragProgress`), dismissal (`dismissPreview`), and cleanup (`clear`). Implement pure algorithmic use cases for preview eligibility check (`EvaluatePreviewEligibilityUseCase`), vertical drag progress and menu trigger calculation (`CalculatePreviewDragUseCase`), dynamic action menu resolution for stickers/GIFs/emojis (`ResolvePreviewActionsUseCase`), and action triggering (`TriggerPreviewActionUseCase`). Provide thread-safe `LegacyContentPreviewRepository` and pure mapping/haptic calculations in `ContentPreviewMapper`. Encapsulate presentation state and MVI events in `ContentPreviewViewModel`.
- **Consequences:** All preview gesture physics, drag progress calculations, action menu resolution, and preview lifecycle operations are cleanly isolated behind testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `ContentPreviewViewer`.

### ADR 075: Isolation of Shared Media, Tabs, Filters, Periods, and Selection into feature.sharedmedia
- **Context:** In Telegram Android, shared media galleries across chats and profiles were implemented in `SharedMediaLayout.java` (~12636 lines) in `org.telegram.ui.Components`. The layout tightly coupled Android UI widgets (`RecyclerView`, `GridLayoutManager`, `ScrollSlidingTextTabStrip`, `FastScroll`, `PhotoViewer`, `ActionBar`), custom gestures, direct database and network requests via `MediaDataController.loadMedia`, internal data holders (`SharedMediaData`, `Period`, `SharedMediaPreloader`), multi-selection management, dialog type permission checks (secret chats, bots, channels, groups), sub-filters (photos vs videos), and month/year sectioning.
- **Decision:** Introduce pure domain models `SharedMediaTabType` (16 media tabs), `SharedMediaFilterType` (ALL, PHOTOS_ONLY, VIDEOS_ONLY), `SharedMediaItem`, `SharedMediaPeriod`, `SharedMediaTabSpec`, `SharedMediaSelectionState`, and `SharedMediaState`. Define abstract contract `SharedMediaRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), dialog configuration (`setDialog`), tab selection (`selectTab`), sub-filter mutation (`setFilter`), items updating and pagination (`setItems`, `addItems`), item selection toggling (`toggleItemSelection`), batch selection (`selectAll`, `clearSelection`), item deletion (`deleteSelectedItems`), loading state mutation, and cleanup (`clear`). Implement pure algorithmic use cases for dynamic tab resolution (`ResolveAvailableTabsUseCase`), media filtering (`FilterSharedMediaUseCase`), monthly section grouping (`GroupMediaByMonthUseCase`), and selection action validation (`CalculateMediaSelectionUseCase`). Provide thread-safe `LegacySharedMediaRepository` and clean date/period calculations in `SharedMediaMapper`. Encapsulate presentation state and MVI events in `SharedMediaViewModel`.
- **Consequences:** All shared media business logic, tab arbitration rules, sub-filter operations, month sectioning, fast scroll periods, and multi-selection rules are decoupled from `SharedMediaLayout` behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `SharedMediaLayout`.

### ADR 074: Isolation of Chat Mentions, Hashtags, Bot Commands, and Autocomplete into feature.mentions
- **Context:** In Telegram Android, inline autocomplete suggestions for user mentions (`@user`), hashtags (`#tag`), bot commands (`/cmd`), emoji keywords (`:emoji`), inline bots (`@gif query`), and quick replies (`/shortcut`) were managed by `MentionsAdapter.java` (~2177 lines) in `org.telegram.ui.Adapters`. The adapter coupled low-level `RecyclerView.Adapter`, `ChatMessageCell`, `ContextLinkCell`, `StickerCell`, `MentionCell`, `BotSwitchCell`, Android `Location` and permissions, `SearchAdapterHelper`, direct `MessagesController`/`MessagesStorage` caches, `MediaDataController` stickers and emoji lookups, and direct UI notification methods.
- **Decision:** Introduce pure domain models `MentionTriggerType` (NONE, USERNAME, HASHTAG, BOT_COMMAND, EMOJI_KEYWORD, STICKER_SUGGESTION, BOT_CONTEXT), `MentionQuery`, `MentionCandidate` (sealed hierarchy: `UserCandidate`, `HashtagCandidate`, `BotCommandCandidate`, `EmojiKeywordCandidate`, `QuickReplyCandidate`), `MentionReplacement`, and `MentionsState`. Define abstract contract `MentionsRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), query updates (`updateQuery`), candidate assignment (`setCandidates`), search state mutation, and panel visibility control. Implement pure algorithmic use cases for trigger detection (`ParseMentionQueryUseCase`), username character validation (`ValidateUsernameUseCase`), case-insensitive multi-field candidate filtering (`FilterMentionsUseCase`), and token-aware text replacement with cursor calculation (`FormatMentionReplacementUseCase`). Provide thread-safe `LegacyMentionsRepository` and clean mapping in `MentionsMapper`. Encapsulate presentation state and MVI events in `MentionsViewModel`.
- **Consequences:** All chat autocomplete triggers, mention parsing algorithms, candidate filtering, and text replacement logic are decoupled from Android Views and legacy Telegram caches behind clean domain interfaces with comprehensive unit test coverage while remaining 100% backward compatible with Telegram's `MentionsAdapter`.

### ADR 073: Isolation of Interactive Emoji Animations, Tap Protocols, and Overlay Geometry into feature.emojieffects
- **Context:** In Telegram Android, full-screen interactive emoji animations, synchronized taps protocols, and reaction effect overlays were handled by `EmojiAnimationsOverlay.java` (~1139 lines) in `org.telegram.ui`. The overlay tightly coupled Android Views (`ChatMessageCell`, `ChatActionCell`, `RecyclerListView`, `FrameLayout`), custom Canvas rendering (`canvas.draw`, `ImageReceiver`, `RLottieDrawable`), Android `HapticFeedbackConstants`, Telegram network requests (`TL_sendMessageEmojiInteraction`, `TL_messages_setTyping`), direct `MessagesController` calls (`sendTyping`, `getAvailableEffects`), `MediaDataController` sticker loading, and JSON serialization of interaction intervals (`JSONObject`, `JSONArray`). Presentation screens like `ChatActivity`, `StoryViewer`, and custom story reaction widgets directly invoked `EmojiAnimationsOverlay`.
- **Decision:** Introduce pure domain models `EmojiInteractionAction`, `EmojiInteractionSession`, `EmojiAnimationQuotaResult`, `EmojiOverlayGeometry`, `EmojiEffectItem`, and `EmojiEffectsState`. Define abstract contract `EmojiEffectsRepository` covering state observation (`observeState`), snapshot retrieval (`getState`), tap recording with relative millisecond offsets (`recordTap`), tap session draining (`drainCurrentSession`), effect lifecycle (`startEffect`, `updateEffectProgress`, `dismissEffect`, `removeEffect`, `cancelAllEffects`, `clear`), and document animation variant tracking (`updateLastAnimationIndex`, `getLastAnimationIndex`). Implement clean algorithms for emoji normalization (stripping tone modifiers `\uD83C\uDFFB..\uDFFF`, ZWJ gender variations `\u200D\u2640/\u2642`, and variation selector `\uFE0F`), emoji support evaluation (excluding keycaps, expanding colored hearts for `"❤"`), interaction JSON payload encoding/decoding, screen-aware overlay bounds calculations (tablet 40%, smartphone 50%), geometry calculations for incoming/outgoing messages, and animation quota rules (max 12 global, max 4 per message, lottie cache generation locks). Provide thread-safe repository `LegacyEmojiEffectsRepository` and pure mapper `EmojiEffectsMapper`. Encapsulate presentation state and MVI events in `EmojiEffectsViewModel`.
- **Consequences:** All interactive emoji animation logic, tap synchronization codecs, quota arbitration, and screen overlay layout geometry are cleanly decoupled into testable domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `EmojiAnimationsOverlay`.

### ADR 072: Isolation of Recycler Animation Scroll Calculations and Transitions into feature.recyclerscroll
- **Context:** In Telegram Android, animated fast-scroll and view transitions for recycler lists were managed by `RecyclerAnimationScrollHelper.java` (~502 lines) in `org.telegram.ui.Components`. The helper tightly coupled Android `ValueAnimator`, `View.OnLayoutChangeListener`, Android views (`RecyclerListView`, `IMessageCell`, `ChatMessageCell`), direct view hierarchy hierarchy manipulations (`layoutManager.ignoreView`, `layoutManager.stopIgnoringView`, `recyclerView.addView`, `recyclerView.removeView`), child view translation updates, and `MessagesController.getGlobalMainSettings()` flags. Components across `ChatActivity`, `DialogsActivity`, `TopicsFragment`, `ContactsActivity`, `ChannelAdminLogActivity`, `CallLogActivity`, `SharedMediaLayout`, and emoji pickers directly relied on `RecyclerAnimationScrollHelper`.
- **Decision:** Introduce pure domain models `ScrollDirection` (UNSET, DOWN, UP), `ScrollAnimationSpec`, `ScrollAnimationPlan`, `ScrollViewTranslation`, `ScrollEligibility` (precondition checks for fast-scroll, animator running, child counts, animation settings), and `RecyclerScrollState`. Define abstract contract `RecyclerScrollRepository` covering eligibility evaluation (`evaluateEligibility`), animation plan calculation (`calculatePlan`), scroll length derivation (`calculateScrollLength`), dynamic translation calculations for old and incoming views (`computeViewTranslations`), state observation (`observeState`), and lifecycle operations (`startScroll`, `updateProgress`, `finishScroll`, `cancelScroll`, `reset`). Implement pure mathematical algorithms in `RecyclerScrollMapper` and a thread-safe adapter `LegacyRecyclerScrollRepository`. Encapsulate presentation state and MVI events in `RecyclerScrollViewModel`.
- **Consequences:** All list scroll physics, duration calculations (150ms/600ms and dynamic height-based formulas clamped between 300ms and 1300ms), scroll length derivation, and view translation matrices are decoupled into clean domain interfaces with comprehensive unit test coverage while remaining 100% backward compatible with Telegram's `RecyclerAnimationScrollHelper`.

### ADR 071: Isolation of Pinch-To-Zoom Gestures, Geometry, and Overlay into feature.pinchtozoom
- **Context:** In Telegram Android, gesture-driven pinch-to-zoom for photos, video messages, and media was implemented in `PinchToZoomHelper.java` (~834 lines) in `org.telegram.ui`. The helper coupled low-level Android `MotionEvent` handling, multi-touch pointer ID mapping, custom Canvas transforms (`canvas.scale`, `canvas.translate`), dynamic view hierarchy offset calculations (`updateViewsLocation`), `SpoilerEffect`/`SpoilerEffect2` shaders, `ValueAnimator` finish transitions (`CubicBezierInterpolator.DEFAULT`), and direct `MediaController` hardware video texture view hijacking (`setTextureView`). Media viewer components across `ChatMessageCell`, `ChatActivity`, `ArticleViewer`, `PeerStoriesView`, `ProfileActivity`, `ProfileGalleryView`, `ChannelAdminLogActivity`, and `GroupCallActivity` directly relied on `PinchToZoomHelper`.
- **Decision:** Introduce pure domain models `PinchTouchPoint`, `PinchGestureSpec`, `PinchGestureDecision` (with activation trigger at scale > 1.005f), `PinchTransform` (scale, pivotX, pivotY, translationX, translationY), `PinchImageDimensions`, `PinchBoundsResult` (with 1.0f..1.4f full-view padding interpolation), and `PinchZoomState`. Define abstract contract `PinchToZoomRepository` covering scale calculation (`calculateScale`), translation compensation (`calculateTranslation`), transform composition (`calculateTransform`), full-view padding interpolation (`calculateImageBounds`), gesture evaluation (`evaluatePinchGesture`), state observation (`observeState`), lifecycle mutations (`startZoom`, `updateZoom`, `finishZoom`, `reset`). Implement pure mathematical formulas in `PinchToZoomMapper` and thread-safe adapter `LegacyPinchToZoomRepository`. Encapsulate presentation state and MVI events in `PinchToZoomViewModel`.
- **Consequences:** All pinch gesture distance/scale calculations, focal point translation formulas, canvas transform matrices, and aspect ratio padding adjustments are cleanly decoupled behind testable domain interfaces with full unit test coverage while remaining 100% backward compatible with Telegram's `PinchToZoomHelper`.

### ADR 070: Isolation of Business Recipients Configuration and Targeting into feature.businessrecipients
- **Context:** In Telegram Android, targeting rules for business features (Away Messages via `AwayMessagesActivity`, Greeting Messages via `GreetingMessagesActivity`, Business Chatbots via `ChatbotsActivity`, and custom user picker `UsersSelectActivity`) were implemented in `BusinessRecipientsHelper.java` (~245 lines) in `org.telegram.ui.Business`. The helper tightly coupled Android Views (`BaseFragment`, `View`, `TextView`, `UItem`, `UniversalRecyclerView`), TLRPC peer objects (`TL_account.businessChatRecipients`), static UserConfig account indices, and bitmask manipulations with direct UI mutation callbacks (`update()`).
- **Decision:** Introduce pure domain models `RecipientFilterType` (EXISTING_CHATS = 1, NEW_CHATS = 2, CONTACTS = 4, NON_CONTACTS = 8), `BusinessRecipientsModel` (with `excludeSelected`, filter flags, `selectedUserIds`, `excludedUserIds`, `isBot`), and `RecipientValidationResult`. Define abstract contract `BusinessRecipientsRepository` covering observation (`observeRecipients`), getters/setters, filter toggling (`toggleFilter`), mode toggling (`toggleExcludeSelected`), user selection/exclusion management with mutual exclusion guarantees (`addSelectedUsers`, `removeSelectedUser`, `addExcludedUsers`, `removeExcludedUser`), change detection (`hasChanges`), and validation (`validate`). Implement pure mapping and validation algorithms in `BusinessRecipientsMapper` and a thread-safe adapter `LegacyBusinessRecipientsRepository`. Encapsulate presentation state and MVI events in `BusinessRecipientsViewModel`.
- **Consequences:** All business recipient targeting logic, bitmask encoding/decoding, user exclusion rules, and validation checks are decoupled into pure domain interfaces with complete unit test coverage while remaining 100% backward compatible with Telegram's `BusinessRecipientsHelper`.

### ADR 069: Isolation of Interactive Pull-Down Keyboard Dismissal into feature.keyboardhide
- **Context:** In Telegram Android, gesture-driven keyboard pull-down dismissal and scroll arbitration were handled by `KeyboardHideHelper.java` (~193 lines) in `org.telegram.ui`. The helper coupled low-level `MotionEvent` handling, Android `VelocityTracker`, Android 11+ `WindowInsetsAnimationController` control calls (`controlWindowInsetsAnimation`, `setInsetsAndAlpha`), direct `ChatActivityEnterView` geometry queries, `AdjustPanLayoutHelper` synchronization (`OnPanTranslationUpdate`, `OnTransitionStart`, `OnTransitionEnd`), and list scroll cancellation. Components across `ChatActivity` directly depended on internal static state (`KeyboardHideHelper.ENABLED`) and direct touch interception.
- **Decision:** Introduce pure domain models `KeyboardDragSpec` (with touch coordinates, keyboard/navbar dimensions, and keyboard/popup type), `KeyboardDismissDecision` (with dismiss recommendation and target progress), `KeyboardHideProgressResult` (with clamped progress, navbar-compensated translationY, inset height, and alpha), and `KeyboardHideState` (with scroll arbitration property `disableScrolling`). Define abstract contract `KeyboardHideRepository` covering drag progress calculation (`calculateProgress`), threshold decision evaluation (`evaluateDismissDecision`), state observation (`observeState`), and lifecycle tracking (`startMoving`, `updateMoving`, `endMoving`, `finishDismiss`). Implement `LegacyKeyboardHideRepository` with thread-safe `StateFlow` and pure math calculations in `KeyboardHideMapper`, providing deterministic headless execution for tests while cleanly isolating legacy `KeyboardHideHelper` mechanics. Encapsulate presentation state and MVI events in `KeyboardHideViewModel`.
- **Consequences:** All pull-down gesture calculations, dismissal decision thresholds, scroll blocking rules, and keyboard animation tracking are cleanly isolated behind testable domain boundaries with complete unit test coverage while preserving 100% backward compatibility with Telegram's `KeyboardHideHelper`.

### ADR 068: Isolation of AdjustPan Layout Animation & Geometry into feature.adjustpan
- **Context:** In Telegram Android, window resize and pan translations when displaying or hiding the soft keyboard were managed by `AdjustPanLayoutHelper.java` (~447 lines) located in `org.telegram.ui.ActionBar`. The helper tightly coupled view tree pre-draw listeners (`ViewTreeObserver.OnPreDrawListener`), view hierarchy traversal (`getViewsToSetHeight`), dynamic decor view inspection (`Window.ID_ANDROID_CONTENT`), Android 11 `WindowInsetsAnimation.Callback`, `LaunchActivity.instance.getBottomSheetTabs()` height offsets, and Android `ValueAnimator` executions with custom interpolators. Presentations across hundreds of UI components (`BottomSheet`, `StoryViewer`, `PaintView`, `PeerStoriesView`, `PollCreateActivity`, `ChatActivity`) directly relied on `AdjustPanLayoutHelper`.
- **Decision:** Introduce pure domain models `PanCalculationSpec` (with previous/current height, start offsets, content view bottom, tabs height, and threshold), `PanTransitionPlan` (with animation flags, trajectory bounds `fromY`/`toY`, inverse flag, target height, keyboard visibility), `PanProgressResult`, and `PanTransitionState`. Define abstract contract `AdjustPanRepository` covering plan calculation (`calculatePlan`), trajectory interpolation (`computeProgress`), state observation (`observeState`), lifecycle transition tracking (`startTransition`, `updateTransition`, `stopTransition`), and enabled mutation (`setEnabled`). Implement `LegacyAdjustPanRepository` with thread-safe `StateFlow` and pure math calculations in `AdjustPanMapper`, providing deterministic headless execution for tests while cleanly isolating legacy `AdjustPanLayoutHelper` mechanics. Encapsulate presentation state and MVI events in `AdjustPanViewModel`.
- **Consequences:** Layout pan animations, translation formulas, tab offset compensations, and keyboard transition arbitration are fully decoupled behind pure domain contracts with comprehensive unit test coverage, preserving 100% backward compatibility with Telegram's `AdjustPanLayoutHelper`.

### ADR 067: Isolation of Instant View Rich Captions & Formatting into feature.richcaption
- **Context:** In Telegram Android, formatted captions, credits, and interactive text spans attached to Instant View article blocks (`TL_iv.PageBlock`, `TL_iv.PageCaption`) were managed by `RichCaptionController.java` (~202 lines) in `org.telegram.ui.iv`. The controller coupled low-level `RichEditText`, direct layout measurements (`measure(leftInset, rightInset, parentWidthPx)`), touch hit testing on text layouts (`isPressOnCaption`), selection hijacking (`TextSelectionHelper.ArticleTextSelectionHelper`), and `TL_iv.RichText` serialization. Host delegates like `RichCaptionHost.java` directly invoked internal controller methods, making unit testing and headless validation impossible.
- **Decision:** Introduce pure domain models `CaptionSpanType` (BOLD, ITALIC, UNDERLINE, STRIKE, CODE, URL), `CaptionEntitySpan`, `RichCaptionModel` (with plainText, credit, and validated spans), and `CaptionMeasureSpec`. Define abstract contract `RichCaptionRepository` covering text/spans mutation (`setCaptionText`), credit management (`setCaptionCredit`), lock status (`setLocked`), available width calculations (`calculateAvailableWidth`), and hit detection (`isPressWithinBounds`). Implement `LegacyRichCaptionRepository` providing thread-safe adapter functionality with standalone in-memory headless capability for JVM testing. Encapsulate presentation state and MVI events in `RichCaptionViewModel`.
- **Consequences:** All Instant View caption formatting, span sanitization, available width measurements, and touch hit testing are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's Instant View article renderer and `RichCaptionController`.

### ADR 066: Isolation of Main Navigation Tabs into feature.maintabs
- **Context:** In Telegram Android, the bottom navigation bar and tab arbitration across primary top-level screens (Chats, Contacts, Settings or Calls, and Profile) was managed by `MainTabsActivity.java` (~1244 lines) with tab visibility controlled via the single-method interface `MainTabsActivityController.java` (`setTabsVisible(boolean visible)`). UI components like `DialogsActivity.java` (~14436 lines) directly held `MainTabsActivityController` instances and called `setTabsVisible(!searching && blurredView == null)` during search mode or dialog overlays. In addition, tab positions, calls/settings tab mutual exclusion (`UserConfig.showCallsTab`), and badge counts (unread dialogs and contacts permission warnings) were tightly bound to legacy view hierarchies.
- **Decision:** Introduce pure domain models `MainTabType` (CHATS, CONTACTS, SETTINGS, CALLS, PROFILE), `MainTabBadgeModel`, and `MainTabsConfigModel`. Define abstract contract `MainTabsRepository` covering tab visibility mutations (`setTabsVisible`), tab selection (`selectTab`, `selectPosition`), calls tab arbitration (`setShowCallsTab`), unread counters (`updateChatsUnreadCount`), and permission warning states (`setContactsPermissionWarning`). Implement `LegacyMainTabsRepository` providing thread-safe adapter functionality to `MainTabsActivityController` and `UserConfig` with standalone in-memory headless capability for JVM testing. Encapsulate presentation state and MVI events in `MainTabsViewModel`.
- **Consequences:** Bottom navigation tab arbitration, visibility transitions, calls-vs-settings switching, and badge indications are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `MainTabsActivity` and `MainTabsActivityController`.

### ADR 065: Isolation of Window Insets & In-App Keyboard Offsets into feature.keyboardinsets
- **Context:** In Telegram Android, managing virtual in-app keyboard heights (such as custom emoji, sticker, and media panels), system window insets (IME, navigation bars, status bars), and navigation bar compensation during keyboard animations was handled by `WindowInsetsInAppController.java` (~165 lines) and helper state holders in `org.telegram.ui.Components.inset`. The controller directly coupled Android `WindowInsets`, system view hierarchies, `ValueAnimator` animations, and mutable height tracking (`inAppKeyboardHeight`, `inAppKeyboardHeightWithNavbar`, `hasNavbar`). UI components like `ChatActivity.java`, `LaunchActivity.java`, `SizeNotifierFrameLayout.java`, and chat input panels directly manipulated these controllers.
- **Decision:** Introduce pure domain models `KeyboardVisibilityState` (HIDDEN, SHOWING, SHOWN, HIDING), `InAppImeMode` (NONE, STICKERS, EMOJI, MEDIA, CUSTOM), and `KeyboardInsetsModel` (with `effectiveBottomInset` combining IME, navigation bar, and in-app keyboard heights). Define abstract contract `KeyboardInsetsRepository` covering in-app keyboard height mutations (`requestInAppKeyboardHeight`, `resetInAppKeyboardHeight`), system insets updates (`updateSystemInsets`), snapshot retrieval (`getInsets`), and reactive observation (`observeInsets`). Implement `LegacyKeyboardInsetsRepository` providing thread-safe adapter functionality to `WindowInsetsInAppController` with standalone in-memory headless capability for JVM testing. Encapsulate presentation state and MVI events in `KeyboardInsetsViewModel`.
- **Consequences:** All in-app keyboard heights, system window insets arbitration, navigation bar offsets, and effective bottom insets calculations are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `WindowInsetsInAppController` and in-app keyboard animations.

### ADR 064: Isolation of Floating Debug Tools & Overlay into feature.floatingdebug
- **Context:** In Telegram Android, developer diagnostic overlays and in-app floating debug tools were managed by `FloatingDebugController.java` (~89 lines) located in `org.telegram.ui.Components.FloatingDebug`. The controller directly coupled Android `LaunchActivity` view hierarchy calls (`getMainContainerFrameLayout().addView()`, `removeView()`), `SharedConfig.isFloatingDebugActive` persistence, `FloatingDebugView` lifecycle (`showFab()`, `dismiss()`, `onBackPressed()`), and internal `DebugItem` definitions with custom callbacks. Presentation components across `ChatActivity.java`, `DialogsActivity.java`, `ProfileActivity.java`, `SettingsActivity.java`, and `ActionBarLayout.java` directly referenced `FloatingDebugController`.
- **Decision:** Introduce pure domain models `DebugItemKind` (SIMPLE, HEADER, SEEKBAR), `DebugItemModel` (with title, kind, action lambda, range, and current value), and `FloatingDebugState` (with active flag and registered items). Define abstract contract `FloatingDebugRepository` covering active status query/mutation (`isActive`, `setActive`, `toggleActive`), dynamic items registry (`getDebugItems`, `registerDebugItems`, `clearDebugItems`), snapshot retrieval (`getState`), and reactive observation (`observeState`). Implement `LegacyFloatingDebugRepository` operating safely across threads with `FloatingDebugController` delegation and headless in-memory fallback. Encapsulate presentation state, menu visibility, and MVI events in `FloatingDebugViewModel`.
- **Consequences:** All floating debug tools, debug action registrations, FAB visibility, and persistent developer flags are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `FloatingDebugController` and `FloatingDebugProvider` callbacks.

### ADR 063: Isolation of Chat Bottom Views Visibility Arbitration into feature.bottomviews
- **Context:** In Telegram Android, mutual exclusion and visibility transitions between bottom chat components (message input field, audio/video recording panel, media attachments, search bar, message actions/selection bar, join channel bar, bot overlays) were arbitrated by `ChatActivityBottomViewsVisibilityController.java` (~63 lines) located in `org.telegram.ui.Components.chat`. The controller coupled bitwise container flags (`visibilityFlags`, `1 << containerId`), highest-bit priority selection (`31 - Integer.numberOfLeadingZeros(flags)`), custom UI animator callbacks (`ReplaceAnimator.Callback`), float array visibility weights (`float[32]`), and `ChatActivity` view hierarchy updates (`checkBottomViewVisibility`, `actionsButtonsLayout`, `chatActivityEnterView`).
- **Decision:** Introduce pure domain models `BottomContainerType` (DEFAULT, MESSAGE_INPUT, BOTTOM_OVERLAY_TEXT, BOTTOM_OVERLAY_CHAT, MESSAGE_SEARCH, MESSAGE_ACTION) and `BottomViewsVisibilityState` (with bitwise visibility queries and container alpha lookups). Define abstract contract `BottomViewsVisibilityRepository` covering container visibility queries (`getVisibility`), visibility mutation (`setViewVisible`), priority identification (`getCurrentPriorityContainerId`), snapshot retrieval (`getState`), and reactive observation (`observeState`). Implement `LegacyBottomViewsVisibilityRepository` adapting `ChatActivityBottomViewsVisibilityController` while maintaining a standalone in-memory bitwise arbitration state for headless JVM unit testing. Encapsulate presentation state and MVI events in `BottomViewsViewModel`.
- **Consequences:** All bottom bar arbitration, priority resolution, mutual exclusivity rules, and container visibility transitions are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `ChatActivityBottomViewsVisibilityController` and animations.

### ADR 062: Isolation of Chat Draft Message Measure & Height Override into feature.draftmeasure
- **Context:** In Telegram Android, dynamic height calculations and vertical space overrides for message cells when expanding or sending drafts were handled by `ChatActivityDraftMessageMeasureController.java` (~109 lines) in `org.telegram.ui.Components.chat`. The controller directly coupled Android `RecyclerView` measurements (`getHeight()`, `getPaddingTop()`, `getPaddingBottom()`), `ChatMessageCell`, `ChatActionCell`, internal `MessageObject` ID/group ID tracking, and mutable layout override flags (`hasAdditionalHeight`, `previousMessageHeight`). UI components in `ChatActivity.java` and `ChatMessageCell.java` had direct references to `ChatActivityDraftMessageMeasureController`.
- **Decision:** Introduce pure domain models `DraftMeasureTarget` (with target `messageId`, `groupId`, and matching predicate), `DraftMeasureViewport` (with available height computation), `DraftMeasureResult`, and `DraftMeasureConfig`. Define abstract contract `DraftMeasureRepository` covering target management (`getTarget`, `setTarget`, `onMessageIdChanged`, `resetTarget`), previous message height state (`setPreviousMessageHeight`, `getPreviousMessageHeight`), override calculation (`calculateOverrideHeight`), and reactive observation (`observeConfig`). Implement `LegacyDraftMeasureRepository` providing thread-safe adapter functionality to `ChatActivityDraftMessageMeasureController` with standalone in-memory headless capability for JVM testing. Encapsulate presentation state and MVI events in `DraftMeasureViewModel`.
- **Consequences:** Draft height overrides, viewport constraint checks, message ID lifecycle updates, and cell height expansion logic are completely decoupled behind clean Kotlin domain interfaces, fully covered by unit tests, while retaining 100% backward compatibility with Telegram's chat UI.

### ADR 061: Keep-Media Cache Retention & Dialog Exceptions Controller Isolation
- **Context:** In Telegram Android, automatic media cache eviction policies (keep media duration for personal chats, groups, channels, stories) and per-dialog exceptions were managed by `CacheByChatsController.java` (~215 lines) located in `org.telegram.messenger`. The controller coupled integer duration codes (`KEEP_MEDIA_DELETE = 4`, `KEEP_MEDIA_FOREVER = 2`, `KEEP_MEDIA_ONE_DAY = 3`, `KEEP_MEDIA_ONE_WEEK = 0`, `KEEP_MEDIA_ONE_MONTH = 1`, `KEEP_MEDIA_TWO_DAY = 6`), chat type codes (`USER = 0`, `GROUP = 1`, `CHANNEL = 2`, `STORIES = 3`), binary serialization of dialog exceptions into Hex strings (`keep_media_exceptions_<type>`), and direct SharedPreferences calls on `SharedConfig` and `UserConfig`.
- **Decision:** Introduce pure domain models `CacheChatType` (USER, GROUP, CHANNEL, STORIES), `KeepMediaDuration` (with seconds equivalents and default durations per type), `KeepMediaExceptionModel` (with `dialogId`, `type`, and `duration`), and `CacheByChatsConfigModel`. Define abstract contract `CacheByChatsRepository` covering reactive configuration observation (`observeConfig`), snapshot retrieval (`getConfig`), retention duration get/set (`getDuration`, `setDuration`), and exception management (`getExceptions`, `setException`, `removeException`, `clearAllExceptions`). Implement `LegacyCacheByChatsRepository` operating safely across threads with binary serialization mapping to `CacheByChatsController` and headless in-memory fallback. Encapsulate presentation state and MVI events in `CacheByChatsViewModel`.
- **Consequences:** All cache retention configuration, peer category durations, dialog retention exceptions, and expiration lookups are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `CacheByChatsController` and cache clearing cleanup tasks.

### ADR 060: Hardware Camera & Video Recording Controller Isolation
- **Context:** In Telegram Android, camera hardware initialization, device enumeration, preview and picture resolution selection, flash mode management, front camera mirroring, and video recording lifecycle were managed by `CameraController.java` (~975 lines) located in `org.telegram.messenger.camera`. The controller directly coupled legacy Android `Camera` and `Camera2` APIs, thread pools (`ThreadPoolExecutor`), SharedPreferences (`cameraCache` Base64 serialization), `MediaRecorder`, `MediaMetadataRetriever`, thumbnail generation (`SendMessagesHelper.createVideoThumbnail`), and global notifications on `NotificationCenter.cameraInitied`. Camera UI components (`CameraView.java`, `ChatActivity.java`, `StoryRecorder.java`) had tightly coupled dependencies on singleton `CameraController.getInstance()`.
- **Decision:** Introduce pure domain models `CameraFacing` (BACK, FRONT), `CameraResolutionModel` (with aspect ratio and area computations), `CameraFlashMode` (OFF, ON, AUTO, TORCH), `CameraRecordingState` (IDLE, RECORDING, FINISHED, FAILED), `CameraDeviceModel`, and `CameraStateModel`. Define abstract contract `CameraRepository` covering reactive state observation (`observeCameraState`), snapshot retrieval (`getCameraState`), camera initialization (`initCameras`), camera selection and cycling (`selectCamera`, `switchCamera`), flash mode control (`setFlashMode`), front-facing mirroring (`toggleMirrorFrontCamera`), aspect ratio matching resolution selection (`chooseOptimalResolution`), and recording lifecycle notifications (`notifyRecordingStarted`, `notifyRecordingFinished`, `notifyRecordingFailed`). Implement `LegacyCameraRepository` operating safely across threads with pure optimal size heuristics and headless unit testing fallback. Encapsulate presentation state and MVI events in `CameraViewModel`.
- **Consequences:** Camera device enumeration, resolution calculations, flash/mirroring preferences, and recording lifecycle state are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `CameraController` and `CameraView`.

### ADR 059: MTProto File Reference Renewal & Parent Cache Controller Isolation
- **Context:** In Telegram Android, automatic renewal of expired MTProto file references (`FILE_REFERENCE_EXPIRED`) was handled by `FileRefController.java` (~2347 lines) located in `org.telegram.messenger`. Whenever media files (photos, videos, documents, wallpapers, avatars, stickers) fail to download due to an expired file reference token, the download engine delegates reference renewal to `FileRefController`. The controller coupled multiple complex responsibilities: grouping pending requests by file location key (`locationKey`), deduplicating network RPC queries by parent object (`parentKey`), maintaining an in-memory 60-second response cache (`responseCache` with `CachedResult`), and dispatching MTProto RPC calls (`TL_messages_getMessages`, `TL_channels_getMessages`, `TL_users_getUsers`, `TL_stories_getStoriesByID`, `TL_wallpapers_getWallpapers`, etc.) via `ConnectionsManager`.
- **Decision:** Introduce pure domain models `FileRefParentType` (MESSAGE, USER, CHAT, CHANNEL, WALLPAPER, SAVED_GIF, REACTION, STICKER_SET, STORY, PEER_COLOR), `FileRefRequestItem`, `FileRefCacheEntry` (with 60s TTL expiration logic), and `FileRefStatsModel`. Define abstract contract `FileRefRepository` covering reactive stats observation (`observeStats`), stats snapshot retrieval (`getStats`), renewal requests (`requestReferenceRenewal` with instant cache hit resolution), renewed notification (`notifyReferenceRenewed`), request cancellation (`cancelPendingRequest`), and cache clearing (`clearCache`). Implement `LegacyFileRefRepository` operating safely across threads with parent response caching and headless in-memory fallback. Encapsulate presentation state and MVI events in `FileRefViewModel`.
- **Consequences:** All MTProto file reference renewal scheduling, parent object caching, deduplication queues, and renewal statistics are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `FileRefController` and `FileLoader` download pipeline.

### ADR 058: Story & Media Creation Drafts Controller Isolation
- **Context:** In Telegram Android, story drafts and in-progress multimedia creations were managed by `DraftsController.java` (~1077 lines) located in `org.telegram.ui.Stories.recorder`. The controller directly coupled SQLite database queries (`story_drafts` table, `REPLACE INTO`, `DELETE FROM`, `NativeByteBuffer` serialization), raw file system manipulation (copying to `cache/drafts`, unlinking expired media files), 7-day expiration checks (`EXPIRATION_PERIOD = 7 days`), and untyped global notifications on `NotificationCenter.storiesDraftsUpdated`. Presentation components like `StoryRecorder.java`, `RecordControl.java`, and `GalleryListView.java` directly queried and manipulated `MessagesController.getInstance(account).getStoriesController().getDraftsController().drafts` and invoked synchronous operations on the Main thread.
- **Decision:** Introduce pure domain models `DraftType` (NEW, EDIT, FAILED), `StoryDraftModel` (with id, date, file path, video/collage flags, caption, and edit expiration checks), and `DraftsStateModel`. Define abstract contract `DraftsRepository` covering reactive drafts observation (`observeDraftsState`), snapshot retrieval (`getDraftsState`), loading (`loadDrafts`), saving (`saveDraft`), single/batch deletion (`deleteDraft`, `deleteDrafts`), edit drafts tracking (`deleteForEdit`, `getDraftForEdit`), and automatic expiration cleanup (`cleanupExpiredDrafts`). Implement `LegacyDraftsRepository` executing safely on `Dispatchers.Main` with reactive `NotificationCenterFlowBridge` observation on `storiesDraftsUpdated` and headless in-memory fallback. Encapsulate presentation state, filtering, and MVI events in `DraftsViewModel`.
- **Consequences:** Story drafts persistence, edit tracking, and expiration garbage collection are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `StoryRecorder`, `DraftsController`, and SQLite storage schema.

### ADR 057: Picture-in-Picture & Video Window Session Controller Isolation
- **Context:** In Telegram Android, Picture-in-Picture (PiP) mode coordination across video playback, live streams, stories, and VoIP video calls was handled by `PipActivityController.java` (~261 lines) and `PipActivityHandler.java` (~401 lines) located in `org.telegram.messenger.pip`. The controller coupled registered sources (`HashMap<String, PipSource>`), priority arbitration (`source.priority > newSource.priority`), media session lifecycle (`MediaSessionCompat`, `MediaSessionConnector`), aspect ratio parameters (`PipSourceParams`), and system broadcast actions (`PipActions.ACTION`). UI activities like `LaunchActivity.java` directly instantiated `PipActivityController` and implemented `IPipActivity`.
- **Decision:** Introduce pure domain models `PipState` (IDLE, ENTERING, IN_PIP, STASHED, EXITING), `PipSourceModel` (with tag, priority, availability, attached state, needsMediaSession, and aspect ratio), and `PipSessionInfo`. Define abstract contract `PipRepository` covering reactive session observation (`observeSessionInfo`), snapshot retrieval (`getSessionInfo`), source registration/unregistration (`registerSource`, `unregisterSource`), state updates (`updateSourceAvailability`, `updateSourceRatio`, `updateSourceAttached`), PiP state transitions (`updatePipState`), action execution (`triggerPipAction`), and eligibility checks (`canEnterPip`). Implement `LegacyPipRepository` operating safely on `Dispatchers.Main` with full priority arbitration and MediaSession synchronization. Encapsulate presentation state and MVI events in `PipViewModel`.
- **Consequences:** All Picture-in-Picture session arbitration, candidate priority resolution, and media session lifecycle controls are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `PipActivityController` and Android OS PiP framework.

### ADR 056: Chat Messages Metadata (Reactions, Paid Media & Stories) Controller Isolation
- **Context:** In Telegram Android, viewport metadata updates for visible chat messages (reactions updates, extended/paid media previews, and linked stories) were managed by `ChatMessagesMetadataController.java` (~184 lines) attached to `ChatActivity.java`. The controller directly coupled `ChatActivity` adapter positions, in-memory arrays of `MessageObject`, direct MTProto RPC dispatching (`TLRPC.TL_messages_getMessagesReactions`, `TLRPC.TL_messages_getExtendedMedia`, `TL_stories.TL_stories_getStoriesByID`), concurrent request limiting queues (`reactionsRequests.size() > 5`, `extendedMediaRequests.size() > 10`), and background storage queue dispatches.
- **Decision:** Introduce pure domain models `MessageMetadataType`, `MessageMetadataCheckItem` (with interval eligibility checks: 15s for reactions, 30s for extended media, 300s for stories), `ChatMetadataStatsModel`, and `ChatMetadataBatchResult`. Define abstract contract `ChatMessagesMetadataRepository` covering reactive stats observation (`observeStats`), stats snapshot retrieval (`getStats`), viewport messages check (`checkMessages`), reactions loading (`loadReactions`), extended media loading (`loadExtendedMedia`), and pending requests cancellation (`cancelPendingRequests`). Implement `LegacyChatMessagesMetadataRepository` operating on `Dispatchers.Main` with safe fallback during headless unit testing. Encapsulate presentation state and MVI events in `ChatMetadataViewModel`.
- **Consequences:** All visible chat messages metadata checking, throttling intervals, and request queue cancellations are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `ChatActivity` rendering loop and MTProto updates pipeline.

### ADR 055: Adaptive Display Refresh Rate & FPS Metrics Controller Isolation
- **Context:** In Telegram Android, dynamic switching of display refresh rates (60 Hz vs 90/120 Hz) based on rendering performance was managed by `RefreshRateController.java` (~307 lines) located in `org.telegram.messenger.utils`. The controller directly coupled Android `Window.OnFrameMetricsAvailableListener`, `FrameMetrics.TOTAL_DURATION`, Android `Display.Mode` querying, and custom ring-buffer nanosecond running sums with hardcoded thresholds (`DOWN_FPS = 55.0f`, `UP_FPS = 58.5f`, `STABLE_WINDOW_MS = 1800ms`, `MIN_SWITCH_INTERVAL_MS = 3000ms`). UI activities like `LaunchActivity.java` had tightly coupled references to this component.
- **Decision:** Introduce pure domain models `DisplayRefreshModeModel` (with `isApproximately60Hz` and `isHighRefreshRate`), `RefreshRateDirection` (UP, DOWN, NONE), `RefreshRateHysteresisConfig`, and `RefreshRateStateModel`. Define abstract contract `RefreshRateRepository` covering reactive state observation (`observeState`), snapshot retrieval (`getState`), tracking lifecycle (`startTracking`, `stopTracking`), toggling adaptive switching (`setAdaptiveEnabled`), preferred mode selection (`setPreferredMode`), recording frame durations (`recordFrameDuration`), and stats reset (`resetStats`). Implement `LegacyRefreshRateRepository` operating with full hysteresis calculations, running average FPS logic, and safe fallback modes during headless testing. Encapsulate presentation state and MVI events in `RefreshRateViewModel`.
- **Consequences:** All display refresh rate switching, FPS performance tracking, and hysteresis stabilization rules are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `RefreshRateController` and `AndroidUtilities.setPreferredMaxRefreshRate`.

### ADR 054: Auto-Save to Gallery Settings & Exceptions Controller Isolation
- **Context:** In Telegram Android, automatic photo and video saving to device gallery and per-dialog exception rules were managed by `SaveToGallerySettingsHelper.java` (~259 lines) located in `org.telegram.messenger`. The helper directly coupled Android `SharedPreferences` keys (`savegallery_users`, `savegallery_groups`, `savegallery_channels`), video file size limits (`savegallery_limit_users`, etc. up to 4 GB), and per-dialog custom exceptions stored in `UserConfig.getSaveGalleryExceptions()`. UI components like `SaveToGallerySettingsActivity.java`, `ChatActivity.java`, and `MediaController.java` directly invoked static helper methods (`SaveToGallerySettingsHelper.load()`, `save()`, `getSettings()`, `needSave()`).
- **Decision:** Introduce pure domain models `GallerySavePeerType` (USERS, GROUPS, CHANNELS), `GallerySaveTargetSettingsModel` (enabled, photos, videos, max video limit), `GallerySaveDialogExceptionModel`, and `GallerySaveConfigModel`. Define abstract contract `GallerySaveRepository` covering reactive configuration observation (`observeConfig`), snapshot retrieval (`getConfig`, `getSettings`), settings updates (`updateSettings`), peer toggling (`togglePeerType`), video limits (`setVideoLimit`), exceptions management (`getExceptions`, `setException`, `removeException`, `removeAllExceptions`). Implement `LegacyGallerySaveRepository` operating safely on `Dispatchers.Main` with in-memory fallback for headless tests. Encapsulate presentation state and MVI events in `GallerySaveViewModel`.
- **Consequences:** All gallery auto-save rules, video file size thresholds, and custom per-chat exceptions are cleanly decoupled behind testable domain interfaces with full unit test coverage while preserving 100% backward compatibility with Telegram's `SaveToGallerySettingsActivity` and file download pipelines.

### ADR 053: Group Call & Conference In-Call Ephemeral Messages Controller Isolation
- **Context:** In Telegram Android, ephemeral in-call text messages and reactions during group audio/video calls and conferences were handled by `GroupCallMessagesController.java` (~313 lines) and `GroupCallMessage.java` (~109 lines) located in `org.telegram.messenger.voip`. The controller directly coupled VoIP service state (`VoIPService.getSharedInstance()`), raw MTProto request dispatching (`TL_phone.sendGroupCallMessage`, `TL_phone.sendGroupCallEncryptedMessage`), JSON message deserialization (`TLJsonParser`, `TLJsonBuilder`), in-call message listeners (`CallMessageListener`), TTL timers, and UI main-thread dispatches. UI components like `GroupCallActivity.java`, `GroupCallMessagesAdapter.java`, and `FragmentContextView.java` directly invoked `GroupCallMessagesController.getInstance(account)` and managed listeners.
- **Decision:** Introduce pure domain models `GroupCallMessageSendStatus` (SENDING, DELAYED, CONFIRMED, ERROR), `GroupCallMessageModel` (with randomId, fromId, text, reaction emoji, and delivery flags), and `GroupCallMessagesStateModel`. Define abstract contract `GroupCallMessagesRepository` covering reactive call messages observation (`observeCallMessages`), snapshot retrieval (`getCallMessages`), sending (`sendCallMessage`), message pop (`popMessage`), and cache clearing (`clearCallMessages`). Implement `LegacyGroupCallMessagesRepository` adapting `GroupCallMessagesController` via `callbackFlow` with safe fallback when VoIP service is inactive. Encapsulate presentation state and MVI events in `GroupCallMessagesViewModel`.
- **Consequences:** Group call in-call messages, delivery statuses, reactions, and auto-expiration TTL lifecycles are decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's VoIP calling infrastructure and UI adapters.

### ADR 052: In-App Hints, Tips & Feature Discovery Controller Isolation
- **Context:** In Telegram Android, user tips, feature discovery prompts, and UI guidance (round video messages, channel gift tips, group custom emoji pack hints, account switching suggestions, and guest bot privacy alerts) were controlled via `HintsController.java` (~78 lines) located in `org.telegram.ui.Components`. The controller directly coupled Android `SharedPreferences` (`MessagesController.getGlobalMainSettings()`), fast random number generation (`Utilities.fastRandom.nextFloat()`), and hardcoded limits/probabilities inside enum values. UI components like `ChatActivity.java` (~28000 lines), `MainTabsActivity.java`, `SettingsActivity.java`, and `ProfileActivity.java` directly invoked static enum methods (`Hint.show()`, `Hint.increment()`, `Hint.doNotShowAgain()`, `HintsController.resetAll()`).
- **Decision:** Introduce pure domain models `HintType` (enum with preference keys, limits, and probabilities), `HintModel` (with display count, limit validation, and probabilistic evaluation `canShow`), and `HintsStateModel`. Define abstract contract `HintsRepository` covering reactive state observation (`observeHints`), snapshot retrieval (`getHintsState`, `getHint`), display eligibility check (`shouldShowHint`), counter increment (`incrementHint`), dismiss forever (`doNotShowAgain`), and counter resets (`resetHint`, `resetAllHints`). Implement `LegacyHintsRepository` operating safely on Main/IO dispatchers with headless fallback, adapting `HintsController.Hint` and SharedPreferences. Encapsulate presentation state and MVI events in `HintsViewModel`.
- **Consequences:** All in-app hints, tips frequency limits, and discovery prompts are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's `ChatActivity`, `SettingsActivity`, and SharedPreferences storage.

### ADR 051: Google Cast, Remote Media Client & Media Streaming Controller Isolation
- **Context:** In Telegram Android, casting photos and videos to Google Cast-enabled external displays and TVs was handled by `ChromecastController.java` (~315 lines) located in `org.telegram.messenger`. The controller directly coupled Google Play Services Cast SDK (`CastContext`, `SessionManager`, `CastSession`, `RemoteMediaClient`), local HTTP media streaming (`ChromecastFileServer` on port 8080), media metadata construction (`MediaInfo`, `MediaMetadata`), and global event dispatching via `NotificationCenter.castSessionStarted`, `castSessionEnded`, and `castMediaProgressChanged`. UI classes such as `PhotoViewer.java` directly invoked `ChromecastController.getInstance()` and called `startCastingMedia(photoEntry)`.
- **Decision:** Introduce pure domain models `ChromecastMediaModel` (with title, MIME type, direct streaming URL, and optional cover URL) and `ChromecastStateModel` (with casting status, media playing status, current media item, position, and duration). Define abstract contract `ChromecastRepository` covering reactive state observation (`observeChromecastState`), snapshot retrieval (`getChromecastState`), connection status checks (`isCasting`, `isMediaPlayingOnCast`), cast actions (`castMedia`, `stopCasting`), and local cover file setup (`setCastCoverFile`). Implement `LegacyChromecastRepository` operating safely on `Dispatchers.Main` with `NotificationCenterFlowBridge` observation, guarding against uninitialized Google Cast SDK during headless unit testing. Encapsulate presentation state and MVI events in `ChromecastViewModel`.
- **Consequences:** Google Cast connection state, remote media streaming, and media progress are cleanly decoupled behind testable domain interfaces with complete unit test coverage while maintaining 100% backward compatibility with Telegram's `PhotoViewer` and local HTTP streaming infrastructure.

### ADR 050: Push Notifications, FCM/HMS Registration & Device Tokens Controller Isolation
- **Context:** In Telegram Android, push notification services (Google Firebase Cloud Messaging / FCM, Huawei Mobile Services / HMS), push device registration (`ConnectionsManager.setRegId`, `registerForPush`), push string tokens (`SharedConfig.pushString`, `SharedConfig.pushType`, `SharedConfig.pushStringStatus`), and remote push payload dispatching were managed via `PushListenerController.java` (~1735 lines). UI components and activity classes (such as `PassportActivity.java`, `LoginActivity.java`, `LaunchActivity.java`, and `GcmPushListenerService.java`) directly queried `PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices()`, triggered static token delivery (`PushListenerController.sendRegistrationToServer`), and manually coordinated registration state across all active accounts (`UserConfig.registeredForPush`).
- **Decision:** Introduce pure domain models `PushServiceType` (FIREBASE, HUAWEI, UNKNOWN), `PushStatusModel` (with token validity rules, provider title, account registration state, and availability flags), and `PushRegistrationResult` (Success, Failure). Define abstract contract `PushRepository` covering reactive push status observation (`observePushStatus`), current status retrieval (`getPushStatus`), service availability checks (`isPushServiceAvailable`), push token requests (`requestPushToken`), multi-account registration dispatching (`registerPushToken`), and token reset (`resetPushToken`). Implement `LegacyPushRepository` operating safely on IO/Main dispatchers, guarding against missing Android Context during headless testing, and bridging to `ApplicationLoader.getPushProvider()`, `PushListenerController`, and `SharedConfig`. Encapsulate presentation state and MVI events in `PushViewModel`.
- **Consequences:** Push notifications provider abstractions, FCM/HCM device token lifecycles, and server registration dispatches are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% backward compatibility with Telegram's push listener services and MTProto push registration mechanisms.

### ADR 049: App Dynamic Launcher Icons & Premium Badging Controller Isolation
- **Context:** In Telegram Android, dynamic application launcher icons (Default, Vintage, Aqua, and Telegram Premium icons: Premium, Turbo, Nox) were managed via `LauncherIconController.java` (~72 lines) located in `org.telegram.ui`. The controller directly manipulated Android `PackageManager` component states (`PackageManager.setComponentEnabledSetting`, `PackageManager.DONT_KILL_APP`) across Android activity-alias components (`org.telegram.messenger.<key>`), and performed automatic fallback healing (`tryFixLauncherIconIfNeeded`). UI components like `AppIconsSelectorCell.java`, `PremiumAppIconsPreviewView.java`, and `LaunchActivity.java` directly queried and modified static controller methods without architectural abstraction.
- **Decision:** Introduce pure domain models `LauncherIconType` (DEFAULT, VINTAGE, AQUA, PREMIUM, TURBO, NOX), `LauncherIconModel` (with resource IDs, premium flag, and active state), and `LauncherIconsStateModel`. Define abstract contract `LauncherIconRepository` covering reactive state observation (`observeLauncherIcons`), icon list retrieval (`getLauncherIcons`), active icon lookup (`getActiveIcon`), enabled check (`isIconEnabled`), dynamic switching (`setIcon`), and auto-healing (`fixLauncherIconIfNeeded`). Implement `LegacyLauncherIconRepository` operating safely on `Dispatchers.Main` with fallback defaults when Android Context is unavailable (preventing test crashes). Encapsulate presentation state and MVI events in `LauncherIconViewModel`.
- **Consequences:** Launcher app icons, Premium icon selection, and component enable toggling are cleanly decoupled behind testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's Android manifest activity-alias configurations and settings screens.

### ADR 048: Google Play Billing, Subscriptions & Currency Formatting Controller Isolation
- **Context:** In Telegram Android, Google Play in-app purchases, Telegram Premium subscription product details (`PREMIUM_PRODUCT_ID = "telegram_premium"`), purchase consumption (gifts, stars top-ups, giveaways, auth codes), Play Store subscription deep links, and multi-currency formatting (`formatCurrency` with special rules for TON, XTR, and fiat currencies) were managed by `BillingController.java` (~574 lines). The controller directly handled Google Play `BillingClient` callbacks (`PurchasesUpdatedListener`, `BillingClientStateListener`), maintained transaction hashes and tokens (`lastPremiumTransaction`, `lastPremiumToken`), managed fallback to invoice mode (`billingClientEmpty`), and coordinated MTProto assignment requests (`TL_payments_assignPlayMarketTransaction`) with UI progress dialogs and notifications on `NotificationCenter.billingProductDetailsUpdated` and `billingConfirmPurchaseError`. UI classes like `PremiumPreviewFragment.java`, `PaymentFormActivity.java`, and Stars dialogs directly accessed `BillingController.getInstance()`.
- **Decision:** Introduce pure domain models `BillingProductType` (INAPP, SUBS), `BillingPriceModel`, `BillingProductModel`, `BillingPurchaseState` (UNSPECIFIED, PURCHASED, PENDING), `BillingPurchaseModel`, and `BillingStateModel`. Define abstract contract `BillingRepository` covering reactive state observation (`observeBillingState`), snapshot state retrieval (`getBillingState`), connection management (`startConnection`, `isReady`, `isInvoiceMode`), cached premium product lookup (`getPremiumProduct`), transaction tracking (`getLastPremiumTransaction`, `getLastPremiumToken`), currency formatting with exponents (`formatCurrency`, `getCurrencyExp`), active purchases querying (`queryPurchases`), and Play Store subscription management (`manageSubscription`). Implement `LegacyBillingRepository` operating safely on `Dispatchers.Main` with `NotificationCenterFlowBridge` observation on billing events, and `suspendCancellableCoroutine` for asynchronous BillingClient setup and purchase querying. Encapsulate presentation state and MVI events in `BillingViewModel`.
- **Consequences:** Google Play Billing integration, Telegram Premium subscription queries, purchase tokens, and currency formatting are cleanly decoupled behind testable domain interfaces with full unit test coverage while maintaining 100% compatibility with Telegram's core billing controller, Google Play Billing Client, and MTProto payment transaction assignment protocols.

### ADR 047: Telegram Stars Bot Revenue, Balance & Transactions Controller Isolation
- **Context:** In Telegram Android, Telegram Stars and TON revenue statistics, balances, transaction histories, connected referral bot links (`ChannelConnectedBots`), suggested bots for referral programs (`ChannelSuggestedBots`), and admined bots/channels were managed by `BotStarsController.java` (~652 lines) located in `org.telegram.ui.Stars`. The controller directly maintained nested in-memory caching maps (`botStarsStats`, `tonStats`, `transactions`, `connectedBots`, `suggestedBots`), raw MTProto request dispatching (`TLRPC.TL_payments_getStarsRevenueStats`, `TL_stars.TL_payments_getStarsTransactions`, `TL_payments.getConnectedStarRefBots`, `TL_payments.getSuggestedStarRefBots`, `TL_bots.getAdminedBots`, `TLRPC.TL_channels_getAdminedPublicChannels`), and untyped global notifications (`NotificationCenter.botStarsUpdated`, `botStarsTransactionsLoaded`, `channelConnectedBotsUpdate`, `channelSuggestedBotsUpdate`, `adminedChannelsLoaded`). UI classes like `BotStarsActivity.java` (~1700 lines) directly mutated controller state and handled raw responses.
- **Decision:** Introduce pure domain models `BotStarsRevenueStatusModel`, `BotStarsRevenueStatsModel`, `BotStarsTransactionType` (ALL, INCOMING, OUTGOING), `BotStarsTransactionModel`, `ConnectedBotStarRefModel`, `StarRefProgramModel`, and `BotStarsStateModel`. Define abstract contract `BotStarsRepository` covering reactive stats observation (`observeBotStarsStats`, `observeTonStats`), snapshot retrieval (`getBotStarsStats`, `getTonStats`), reactive and paginated transactions loading (`observeTransactions`, `loadTransactions`), referral bot links (`observeConnectedBots`, `loadConnectedBots`), suggested program bots (`loadSuggestedBots`), and admined bots/channels (`loadAdminedBots`, `loadAdminedChannels`). Implement `LegacyBotStarsRepository` operating safely on `Dispatchers.Main` with `NotificationCenterFlowBridge` observation on all related events, and `suspendCancellableCoroutine` for request coordination. Encapsulate presentation state, tab switching, and MVI events in `BotStarsViewModel`.
- **Consequences:** Bot and channel Stars/TON revenues, balance tracking, transaction filters, referral programs, and admined bot inventories are decoupled behind testable domain interfaces with full unit test coverage while maintaining 100% compatibility with Telegram's core bot stars controller and MTProto payments protocols.

### ADR 046: Telegram Timezones & Business Hours Offset Controller Isolation
- **Context:** In Telegram Android, timezones selection and timezone offset calculation (used across Telegram Business opening hours, profile hours, scheduled messages, and premium features) was managed by `TimezonesController.java` (~183 lines) located in `org.telegram.ui.Business`. The controller directly coupled local SharedPreferences hex deserialization (`mainSettings.getString("timezones", null)`), raw MTProto requests (`TLRPC.TL_help_getTimezonesList`), untyped global notifications on `NotificationCenter.timezonesUpdated`, and Android/Java 8 `java.time.ZoneId` system timezone resolution with fallback heuristics. UI classes like `TimezoneSelector.java`, `OpeningHoursActivity.java`, `ProfileHoursCell.java`, and `AlertsCreator.java` directly invoked `TimezonesController.getInstance(account)`.
- **Decision:** Introduce pure domain models `TimezoneModel` (with calculated formatted UTC offset such as `GMT+03:00` and `displayName`) and `TimezonesStateModel`. Define abstract contract `TimezonesRepository` covering reactive timezones observation (`observeTimezones`), snapshot retrieval (`getTimezones`), remote/cache loading (`loadTimezones`), lookup by identifier (`findTimezone`), system timezone resolution (`getSystemTimezoneId`), and localized/offset formatting (`getTimezoneName`). Implement `LegacyTimezonesRepository` operating safely on `Dispatchers.Main` with `NotificationCenterFlowBridge` observation on `NotificationCenter.timezonesUpdated` and `suspendCancellableCoroutine` for request coordination. Encapsulate presentation state, search filtering (by name, ID, or offset), and MVI events in `TimezonesViewModel`.
- **Consequences:** Timezones listing, search, system timezone fallback detection, and GMT offset formatting are decoupled behind testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's core timezones controller and MTProto timezones protocol.

### ADR 045: Telegram Business Chatbots & Connected Bots Controller Isolation
- **Context:** In Telegram Android, connected AI and third-party chatbots for Telegram Business accounts were managed by `BusinessChatbotController.java` (~88 lines) and configured across `ChatbotsActivity.java` (~854 lines) and `ChatbotSheet.java` (~332 lines). The controller managed in-memory cached responses (`TL_account.connectedBots`), throttle timeouts (1 minute expiration), raw callback lists, and untyped global notifications on `NotificationCenter.updatedChatbot`. Disconnecting bots or modifying permissions (replying, reading messages, deleting sent/received messages, editing profile/bio/name/username, managing stories, transferring stars) and audience exclusions/inclusions (`TL_account.updateConnectedBot`) directly relied on raw MTProto calls mixed with UI dialogs and manual invalidate triggers.
- **Decision:** Introduce pure domain models `BusinessBotRightsModel` (with default and full rights factories), `BusinessBotRecipientsModel` (with audience filter flags), `ConnectedBotModel` (with connection date, device, location metadata), and `BusinessBotsStateModel`. Define abstract contract `BusinessBotsRepository` covering reactive connected bots observation (`observeConnectedBots`), snapshot retrieval (`getConnectedBots`), loading (`loadConnectedBots`), bot update/permissions modification (`updateConnectedBot`), bot termination/disconnection (`deleteConnectedBot`), and bot lookup by ID (`findConnectedBot`). Implement `LegacyBusinessBotsRepository` operating safely on `Dispatchers.Main` with `NotificationCenterFlowBridge` observation on `NotificationCenter.updatedChatbot`, and `suspendCancellableCoroutine` for MTProto update and termination requests. Encapsulate presentation state and MVI events in `BusinessBotsViewModel`.
- **Consequences:** Business chatbots, permissions administration, recipient exceptions, and session termination are cleanly decoupled behind testable domain interfaces with full unit test coverage while preserving 100% compatibility with Telegram's core chatbot controller and MTProto business bots protocol.

### ADR 044: Telegram Business Chat Links & Shortcuts Controller Isolation
- **Context:** In Telegram Android, pre-configured chat links (`https://t.me/m/...`) with preset greeting messages and view tracking for Telegram Business accounts were managed by `BusinessLinksController.java` (~320 lines). The controller coupled in-memory link collections (`ArrayList<TL_account.TL_businessChatLink> links`), loading state flags, account limits from `MessagesController.businessChatLinksLimit`, raw MTProto request dispatching (`TL_account.getBusinessChatLinks`, `TL_account.createBusinessChatLink`, `TL_account.editBusinessChatLink`, `TL_account.deleteBusinessChatLink`), and untyped global broadcasts on `NotificationCenter.businessLinksUpdated`. UI activities like `BusinessLinksActivity.java` (~1200 lines) directly mutated the controller's internal collections and handled raw RPC error responses.
- **Decision:** Introduce pure domain models `BusinessLinkModel` (with slug extraction, formatted url, and view count), `BusinessLinkInputModel`, and `BusinessLinksStateModel`. Define abstract contract `BusinessLinksRepository` covering reactive links observation (`observeBusinessLinks`), snapshot retrieval (`getBusinessLinks`), loading (`loadBusinessLinks`), link creation (`createLink`), editing (`editLink`), deletion (`deleteLink`), slug lookup (`findLink`), and creation limit validation (`canAddNew`). Implement `LegacyBusinessLinksRepository` operating safely on `Dispatchers.Main` with `NotificationCenterFlowBridge` observation on `NotificationCenter.businessLinksUpdated`, and `suspendCancellableCoroutine` for MTProto request cancellation. Encapsulate presentation state and MVI events in `BusinessLinksViewModel`.
- **Consequences:** Business chat links, preset greetings, view statistics, and creation limits are cleanly decoupled behind testable domain interfaces with complete unit test coverage while preserving 100% compatibility with Telegram's core business links controller and MTProto business protocol.

### ADR 043: Telegram Star Gift Auctions & Real-Time Bidding Isolation
- **Context:** In Telegram Android, competitive bidding and real-time auctions for limited-edition Telegram Star Gifts were managed by `GiftAuctionController.java` (~780 lines). The controller directly handled in-memory auction caches (`LongSparseArray<AuctionInternal>`), active auctions list, subscriber listeners (`ReferenceMap<Long, OnAuctionUpdateListener>`), dynamic timer resubscriptions based on server `timeout`, payment invoice requests (`TLRPC.TL_payments_getPaymentForm`, `TL_stars.TL_payments_sendStarsForm`), hash calculation for active auctions (`MediaDataController.calcHash`), and acquired gifts queries (`TL_payments.TL_getStarGiftAuctionAcquiredGifts`). UI sheets like `AuctionBidSheet.java` (~1027 lines) and `AcquiredGiftsSheet.java` directly invoked `GiftAuctionController.getInstance(account)`.
- **Decision:** Introduce pure domain models `GiftAuctionStatus` (ACTIVE, FINISHED, UNKNOWN), `GiftAuctionModel`, `GiftAuctionBidParamsModel`, and `GiftAuctionAcquiredGiftModel`. Define abstract contract `GiftAuctionsRepository` covering reactive active auctions observation (`observeActiveAuctions`), per-gift real-time observation (`observeAuction`), snapshot retrieval (`getActiveAuctions`, `getAuctionById`, `getAuctionBySlug`), bidding (`sendBid`), acquired gifts loading (`loadAcquiredGifts`), and active auctions refresh (`refreshActiveAuctions`). Implement `LegacyGiftAuctionsRepository` operating safely on `Dispatchers.Main` with `callbackFlow` converting `OnAuctionUpdateListener` and `OnActiveAuctionsUpdateListeners` into cold Kotlin Flow streams, and `suspendCancellableCoroutine` for bidding and requests. Encapsulate presentation state and MVI events in `GiftAuctionsViewModel`.
- **Consequences:** Star gift auctions, real-time bidding, acquired gifts tracking, and timer updates are cleanly decoupled behind testable domain interfaces with full unit test coverage while maintaining 100% compatibility with Telegram's core auction controller and MTProto star payments protocols.

### ADR 042: Biometrics, Hardware Keystore & Passcode Authentication Isolation
- **Context:** In Telegram Android, hardware-backed biometric security and cryptographic key pair generation for passcode authentication was managed by `FingerprintController.java` (~145 lines). The controller directly coupled AndroidKeyStore RSA-OAEP key pair generation (`KEY_ALIAS = "tmessages_passcode"`), device locale switching hacks to circumvent AndroidKeyStore bugs in RTL languages, KeyPermanentlyInvalidatedException detection for changed device biometrics, and untyped global broadcasts (`NotificationCenter.didGenerateFingerprintKeyPair`). UI components like `PasscodeView.java` and `LaunchActivity.java` directly queried static methods (`FingerprintController.checkKeyReady()`, `FingerprintController.isKeyReady()`, `FingerprintController.checkDeviceFingerprintsChanged()`) without state isolation or testability.
- **Decision:** Introduce pure domain models `BiometricStatus` (Available, HardwareUnavailable, NoEnrolledBiometrics, KeyPermanentlyInvalidated, NotSupported) and `BiometricKeyStateModel` (with `canAuthenticate` logic). Define abstract contract `BiometricsRepository` covering reactive state observation (`observeKeyState`), state retrieval (`getKeyState`), key initialization (`checkKeyReady`), invalid key cleanup (`deleteInvalidKey`), readiness query (`isKeyReady`), and biometrics change detection (`hasDeviceBiometricsChanged`). Implement `LegacyBiometricsRepository` safely handling Android M+ requirements, `FingerprintManagerCompat` checks, and bridging `didGenerateFingerprintKeyPair` via `NotificationCenterFlowBridge`. Encapsulate presentation state and MVI events in `BiometricsViewModel`.
- **Consequences:** Passcode biometric authentication, AndroidKeyStore key pair status, and device biometrics modification tracking are decoupled behind clean, testable domain interfaces with complete unit test coverage while preserving 100% compatibility with Telegram's passcode lock screens and hardware security modules.

### ADR 041: Hashtag Search & History Controller Isolation
- **Context:** In Telegram Android, search for messages containing hashtags and cashtags across personal chats, public channels, and current channels was managed by `HashtagSearchController.java` (~389 lines). The controller directly managed volatile static instances per account (`Instance[UserConfig.MAX_ACCOUNT_COUNT]`), raw SharedPreferences persistence (`hashtag_search_history<currentAccount>`), manual capacity trimming (100 items), low-level MTProto search requests (`TLRPC.TL_messages_searchGlobal`, `TLRPC.TL_channels_searchPosts`, `TLRPC.TL_messages_search`), synthetic ID generation (`generatedIds`), and direct broadcasts on `NotificationCenter.hashtagSearchUpdated` and `messagesDidLoad`.
- **Decision:** Introduce pure domain models `HashtagSearchType` (MY_MESSAGES, PUBLIC_POSTS, CHANNEL_POSTS), `HashtagMessageModel`, and `HashtagSearchResultModel` with pagination and navigation capabilities. Define abstract contract `HashtagSearchRepository` covering reactive history observation (`observeHistory`), snapshot retrieval (`getHistory`), adding (`addHashtagToHistory`), removing (`removeHashtagFromHistory`), clearing history (`clearHistory`), reactive search result observation (`observeSearchResult`), search execution (`searchHashtag`), message navigation (`jumpToMessage`), and result clearing (`clearSearchResults`). Implement `LegacyHashtagSearchRepository` operating safely on `Dispatchers.Main` with reactive StateFlows. Encapsulate presentation state and MVI events in `HashtagSearchViewModel`.
- **Consequences:** Hashtag and cashtag searching, recent hashtag history, search pagination, and message jumping are decoupled behind clean, testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's core search controller and MTProto search protocols.

### ADR 040: reCAPTCHA Enterprise Verification Controller Isolation
- **Context:** In Telegram Android, reCAPTCHA Enterprise challenge verification during registration, login, and sensitive MTProto actions was managed by `CaptchaController.java` (~115 lines). The legacy controller directly coupled global mutable static state (`public static HashMap<Integer, Request> currentRequests`), direct Android `Activity` extraction via `AndroidUtilities.getActivity()`, Google Play Services reCAPTCHA Enterprise Tasks API callbacks, and immediate invocation of MTProto native JNI methods (`ConnectionsManager.native_receivedCaptchaResult`). Native code in `TgNetWrapper.cpp` triggered `ConnectionsManager.onCaptchaCheck(currentAccount, requestToken, action, key_id)` directly into `CaptchaController.request`.
- **Decision:** Introduce pure domain models `CaptchaAction` (Login, SignUp, Custom), `CaptchaRequestModel` (with deduplicated token sets), and `CaptchaResult` (Success, Failure with error codes). Define abstract contract `CaptchaRepository` covering reactive requests observation (`observeActiveRequests`), snapshot retrieval (`getActiveRequests`), verification (`verifyCaptcha`), result submission (`submitCaptchaResult`), and request cancellation (`cancelCaptcha`). Implement `LegacyCaptchaRepository` operating safely on `Dispatchers.Main` with `suspendCancellableCoroutine` for Google Play Tasks client execution and thread-safe submission to `ConnectionsManager.native_receivedCaptchaResult`. Encapsulate MVI verification flow and UI states in `CaptchaViewModel`.
- **Consequences:** Captcha verification, token deduplication, error code formatting, and MTProto response reporting are decoupled behind clean, testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's core native networking layer and reCAPTCHA Enterprise API.

### ADR 039: AI Compose Tones & Styles Controller Isolation
- **Context:** In Telegram Android, message tone rewriting styles for the Telegram AI Compose feature are managed by `AiTonesController.java` (~158 lines) instantiated on `MessagesController`. The controller directly handles local Base64 serialization in `mainSettings`, MTProto queries (`TL_aicompose.getTones`, `TL_aicompose.saveTone`), and untyped notifications via `NotificationCenter.loadedAiComposeTones`. UI components like `AIEditorAlert.java` (~2300 lines) directly interacted with mutable controller lists and executed manual reload calls.
- **Decision:** Introduce pure domain models `AiToneModel` (with title, emoji, prompt, creator flag, install count, example rewrite from/to text) and `AiTonesStateModel`. Define abstract contract `AiTonesRepository` covering reactive tones observation (`observeTones`), snapshot retrieval (`getTonesState`), loading (`loadTones`), adding (`addTone`), removing (`removeTone`), unsaving (`unsaveTone`), and editing custom tones (`editTone`). Implement `LegacyAiTonesRepository` operating safely on `Dispatchers.Main` with reactive `NotificationCenterFlowBridge` observation on `NotificationCenter.loadedAiComposeTones`. Encapsulate presentation state, selected tone, and MVI events in `AiTonesViewModel`.
- **Consequences:** AI Compose tone rewriting styles, custom prompts, install counters, and MTProto tone synchronization are decoupled behind testable domain interfaces with full unit test coverage while maintaining 100% compatibility with Telegram's core `AiTonesController` and MTProto AI compose protocol.

### ADR 038: Telegram Star Gifts, Catalog & Profile Saved Gifts Isolation
- **Context:** In Telegram Android, user/channel gifts and the star gifts catalog are managed by `StarsController.java` (`getGiftsList()`, `getProfileGiftsList(dialogId)`). The gifts data model spans raw MTProto TL types `TL_stars.StarGift`, `TL_stars.SavedStarGift`, `TL_stars.TL_payments_getSavedStarGifts`, and `TL_stars.TL_payments_saveStarGift`. UI components like `StarGiftSheet.java` (~1100 lines) and profile tabs directly invoked `StarsController` methods, performed direct list mutations, and relied on untyped `NotificationCenter` broadcasts without lifecycle safety or clean separation between catalog viewing and profile-pinned gifts.
- **Decision:** Introduce pure domain models `StarGiftModel`, `SavedStarGiftModel`, `StarGiftsCatalogModel`, `ProfileGiftsModel`, and typed filter enum `StarGiftFilter` (ALL, LIMITED, BIRTHDAY). Define abstract contract `StarGiftsRepository` covering reactive catalog observation (`observeCatalog`), catalog loading (`loadCatalog`), gift lookup (`getGiftById`), reactive profile gifts observation (`observeProfileGifts`), profile gifts loading (`loadProfileGifts`), pin toggling (`togglePinProfileGift`), and hide/show toggling (`toggleHideProfileGift`). Implement `LegacyStarGiftsRepository` operating safely on `Dispatchers.Main` with reactive `NotificationCenterFlowBridge` observation for `NotificationCenter.starGiftsLoaded`. Encapsulate presentation state, filtering, and MVI events in `StarGiftsViewModel`.
- **Consequences:** Gifts catalog browsing, profile gift collection management, pinned/hidden states, and upgrade badges are decoupled behind testable domain interfaces with full unit test coverage while maintaining 100% compatibility with Telegram's core `StarsController` and MTProto payments protocols.

### ADR 037: Unconfirmed Auth Sessions & Login Approvals Controller Isolation
- **Context:** In Telegram Android, login approvals on new devices, web authorizations awaiting confirmation, and connected bot session approvals are governed by `UnconfirmedAuthController.java` (~414 lines) instantiated on `MessagesController`. The controller directly handles local SQLite storage queries on `unconfirmed_auth`, MTProto requests (`TL_account.changeAuthorizationSettings`, `TL_account.resetAuthorization`, `TL_account.confirmBotConnection`, `TL_account.updateConnectedBot`), in-memory cached session lists (`auths`), and expiration checking runnables. UI components like `UnconfirmedAuthHintCell.java` (~370 lines) directly queried unsynchronized controller arrays and executed direct callback methods without lifecycle safety.
- **Decision:** Introduce pure domain models `UnconfirmedAuthModel` (with hash, date, device, location, bot flags, expiration calculations) and `UnconfirmedAuthStateModel`. Define abstract contract `UnconfirmedAuthRepository` covering reactive pending auths observation (`observeUnconfirmedAuths`), state snapshot retrieval (`getUnconfirmedAuths`), single authorization confirmation/denial (`confirmAuth`, `denyAuth`), batch confirmation/denial (`confirmAll`, `denyAll`), and cache cleanup (`clear`). Implement `LegacyUnconfirmedAuthRepository` operating safely on `Dispatchers.Main` with `suspendCancellableCoroutine` for asynchronous MTProto confirmations and reactive `NotificationCenterFlowBridge` observation on `unconfirmedAuthUpdate`. Encapsulate presentation state and MVI events in `UnconfirmedAuthViewModel`.
- **Consequences:** Unconfirmed sessions, new device login approvals, bot connection confirmations, and expiration monitoring are cleanly decoupled behind testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's core auth controller and MTProto authorization protocol.

### ADR 036: Auto-Delete Messages & Global History TTL Isolation
- **Context:** In Telegram Android, message auto-delete and self-destruct timers operate on two distinct levels: global account-level default TTL for new chats (`TLRPC.TL_messages_setDefaultHistoryTTL`, `UserConfig.getGlobalTTl()`, `NotificationCenter.didUpdateGlobalAutoDeleteTimer`), and per-dialog TTL periods (`MessagesController.setDialogHistoryTTL`, `TLRPC.TL_messages_setHistoryTTL`, `dialog.ttl_period`). Presentation logic inside `AutoDeleteMessagesActivity.java` (~337 lines) directly handled raw MTProto request dispatching, manual seconds/minutes conversions, direct `UserConfig` mutations, and custom UI transitions mixed with networking callbacks.
- **Decision:** Introduce pure domain models `AutoDeleteTtlModel` (with standard intervals OFF, 1 day, 1 week, 1 month, and custom periods), `GlobalAutoDeleteStateModel`, and `ChatAutoDeleteStateModel`. Define abstract contract `AutoDeleteRepository` covering reactive global TTL observation (`observeGlobalAutoDelete`), global TTL retrieval/setting (`getGlobalAutoDelete`, `setGlobalAutoDelete`), chat-specific TTL retrieval/setting (`getChatAutoDelete`, `setChatAutoDelete`), and batch application across multiple dialogs (`setChatsAutoDeleteBatch`). Implement `LegacyAutoDeleteRepository` operating safely on `Dispatchers.Main` with `suspendCancellableCoroutine` for asynchronous MTProto network synchronization and reactive `NotificationCenterFlowBridge` observation. Encapsulate presentation state and MVI events in `AutoDeleteViewModel`.
- **Consequences:** Auto-delete timers, global history TTL configuration, and chat-level lifetime rules are cleanly decoupled behind testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's core networking layer and MTProto history TTL protocol.

### ADR 035: Proxy Configuration, Server Management & Auto-Rotation Isolation
- **Context:** In Telegram Android, proxy configuration, server management (Socks5 and MTProto), ping checking, and auto-rotation were managed across `SharedConfig.java` (`proxyList`, `currentProxy`, `isProxyEnabled`, `deleteProxy`, `addProxy`), `ProxyRotationController.java` (`ROTATION_TIMEOUTS`, `switchToAvailable`, ping timeout scheduling), `ConnectionsManager.java` (`setProxySettings`, `checkProxy`), and UI components (`ProxyListActivity.java` ~1128 lines, `ProxySettingsActivity.java`, `AndroidUtilities.showProxyAlert`). State management relied on scattered static fields, raw SharedPreferences (`proxy_ip`, `proxy_port`, `proxy_user`, `proxy_pass`, `proxy_secret`, `proxy_enabled`, `proxyRotationEnabled`, `proxyRotationTimeout`), and global `NotificationCenter` broadcasts (`proxySettingsChanged`, `proxyCheckDone`, `proxyChangedByRotation`).
- **Decision:** Introduce pure domain models `ProxyType`, `ProxyModel`, and `ProxySettingsModel`. Define abstract contract `ProxyRepository` covering reactive proxy settings observation (`observeProxySettings`), settings snapshot retrieval (`getProxySettings`), adding proxy (`addProxy`), deleting proxy (`deleteProxy`), enabling proxy (`enableProxy`), disabling proxy (`disableProxy`), toggling auto-rotation (`toggleProxyRotation`), and checking proxy ping (`checkProxyPing`). Implement `LegacyProxyRepository` operating safely on `Dispatchers.Main` with `suspendCancellableCoroutine` for asynchronous ping verification (`ConnectionsManager.checkProxy`) and reactive `NotificationCenterFlowBridge` observation. Encapsulate presentation state and MVI events in `ProxyViewModel`.
- **Consequences:** Proxy server configuration, Socks5/MTProto credential handling, ping measurement, and automatic rotation are cleanly decoupled behind testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's core networking layer and SharedPreferences persistence.

### ADR 034: Passkeys & WebAuthn Authentication Controller Isolation
- **Context:** In Telegram Android, passkeys and WebAuthn authentication mechanisms were managed across `PasskeysController.java` (~331 lines), `PasskeysActivity.java` (~420 lines), and `LoginActivity.java`. The legacy controller intermixed low-level MTProto calls (`TL_account.initPasskeyRegistration`, `TL_account.registerPasskey`, `TL_account.initPasskeyLogin`, `TL_account.finishPasskeyLogin`), Android `androidx.credentials.CredentialManager` integration, raw JSON manipulation of FIDO2/WebAuthn bundles (`clientDataJSON`, `attestationObject`, `authenticatorData`, `signature`), and direct UI dialog creation (`AlertDialog` with spinner) from within network callbacks. UI activities directly invoked MTProto requests to fetch (`TL_account.getPasskeys`) and delete (`TL_account.deletePasskey`) credentials.
- **Decision:** Introduce pure domain models `PasskeyModel` and `PasskeysStateModel`. Define abstract contract `PasskeysRepository` covering reactive passkeys observation (`observePasskeys`), passkey listing (`getPasskeys`), passkey deletion (`deletePasskey`), passkey capability/platform support check (`isSupported`), and maximum allowed passkeys query (`getMaxPasskeys`). Implement `LegacyPasskeysRepository` operating on `Dispatchers.Main` with coroutine cancellation support (`ConnectionsManager.cancelRequest`). Encapsulate presentation state and MVI events in `PasskeysViewModel`.
- **Consequences:** Passkey management, credential lifecycle, account limits, and MTProto queries are cleanly decoupled behind testable domain interfaces with full unit test coverage while maintaining 100% compatibility with Telegram's WebAuthn protocol and Android Credential Manager integration.

### ADR 033: Chat Themes, Custom Wallpapers & Dialog Styling Controller Isolation
- **Context:** In Telegram Android, dialog-specific emoji themes, gift themes, custom wallpapers, and colors were managed by `ChatThemeController.java` (~1016 lines). The controller directly handled raw SharedPreferences persistence (`chatthemeconfig_` and `chatthemeconfig_emoji`), SQLite database caching (`MessagesStorage.loadGiftChatTheme`), MTProto network requests (`TL_account.getChatThemes`, `TLRPC.TL_messages_setChatTheme`), disk caching of theme bitmaps (`chatThemeQueue`), and mutated in-memory caches (`dialogEmoticonsMap`, `allChatGiftThemes`, `themeIdWallpaperThumbMap`). Presentation components (`ChatActivity`, `ChatThemeBottomSheet`, `EmojiThemes`) directly invoked static controller singletons, manual callbacks, and raw TL object operations without lifecycle or state management.
- **Decision:** Introduce pure domain models `ChatThemeModel` (with emoticon, gift slug, default flag, and preview colors) and `DialogThemeStateModel`. Define abstract contract `ChatThemeRepository` covering reactive dialog theme observation (`observeDialogTheme`), cached/fresh state retrieval (`getDialogThemeState`), available chat themes loading (`getAvailableChatThemes`), setting custom emoji/gift theme (`setDialogTheme`), resetting theme to default (`resetDialogTheme`), and saving/clearing custom chat wallpapers (`saveChatWallpaper`). Implement `LegacyChatThemeRepository` operating safely on `Dispatchers.Main` with `suspendCancellableCoroutine` for asynchronous theme fetching and reactive `NotificationCenterFlowBridge` observation on dialog updates. Encapsulate presentation state and MVI events in `ChatThemeViewModel`.
- **Consequences:** Dialog styling, theme application, wallpaper management, and MTProto theme synchronization are decoupled behind clean, testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's chat theme serialization and SharedPreferences cache.

### ADR 032: User Birthdays & Contacts' Celebrations Controller Isolation
- **Context:** In Telegram Android, user birthdays, contacts' birthdays (`TL_account.contactBirthdays`), and celebration banners were coupled inside `BirthdayController.java` (~310 lines). The controller managed in-memory mutable arrays of users (`yesterday`, `today`, `tomorrow`), custom TL serialization (`TL_birthdays`), SharedPreferences persistence (`bday_check`, `bday_contacts`, `bday_hidden`), and directly triggered `premiumPromoUpdated` events on `NotificationCenter`. UI components (`DialogsActivity`, `ChatActivityEnterView`, `ProfileBirthdayEffect`, `UserSelectorBottomSheet`) queried static methods and raw state objects with direct SharedPreferences mutations.
- **Decision:** Introduce pure domain models `BirthdayDateModel`, `BirthdayUserModel`, `ContactBirthdayModel`, and `BirthdayStateModel`. Define abstract contract `BirthdaysRepository` covering reactive birthday state observation (`observeBirthdays`), cached state retrieval, periodic/forced checking (`checkBirthdays`), daily banner dismissal (`hideTodayBirthdays`), and single user/overall birthday detection (`isBirthdayToday`, `hasBirthdaysToday`). Implement `LegacyBirthdaysRepository` operating safely on `Dispatchers.Main` with reactive `NotificationCenterFlowBridge` bridging for `premiumPromoUpdated`. Encapsulate presentation state and MVI events in `BirthdaysViewModel`.
- **Consequences:** Birthday querying, contacts' celebration discovery, banner dismissal, and profile celebration effects are decoupled behind clean, testable domain interfaces with complete unit test coverage while maintaining 100% compatibility with Telegram's MTProto birthday sync and SharedPreferences storage.

### ADR 031: Message Fact-Checks & Community Annotations Controller Isolation
- **Context:** In Telegram Android, message fact-checks and community verification annotations (`TLRPC.TL_factCheck`) were managed by `FactCheckController.java` (~596 lines). The controller intermixed low-level SQLite database caching (`saveToDatabase`, `getFromDatabase`, SQL queries on `fact_checks` table), MTProto batch loading (`TLRPC.TL_getFactCheck`), and heavy Android UI dialog construction (`openFactCheckEditor` with `AlertDialog`, custom `EditTextCaption`, spans, haptics, and bulletin messages). UI components like `ChatMessageCell.java` and `ChatActivity.java` directly invoked `FactCheckController.getInstance(account).getFactCheck(messageObject)` and `openFactCheckEditor`.
- **Decision:** Introduce pure domain models `FactCheckEntityModel`, `FactCheckModel`, and `FactCheckLimitsModel`. Define abstract contract `FactCheckRepository` covering reactive fact-check updates observation (`observeFactCheckLoaded`), cached lookup, network loading (`loadFactCheck`), fact-check application/editing (`applyFactCheck`), deletion (`deleteFactCheck`), and character length limit querying (`getFactCheckLimit`). Implement `LegacyFactCheckRepository` operating on `Dispatchers.Main` with coroutine cancellation (`ConnectionsManager.cancelRequest`), `MessagesController.processUpdates` synchronization, and reactive `NotificationCenterFlowBridge` event bridging for `factCheckLoaded`. Encapsulate presentation state and MVI events in `FactCheckViewModel`.
- **Consequences:** Fact-check editing, viewing, character length enforcement, and MTProto synchronization are decoupled behind clean, testable domain interfaces with complete unit test coverage while preserving 100% compatibility with Telegram's MTProto fact-check protocol and update processing pipeline.

### ADR 030: Join Requests & Chat Administration Controller Isolation
- **Context:** In Telegram Android, managing pending join requests for supergroups and channels was fragmented across `MemberRequestsController.java` (~80 lines), `MemberRequestsDelegate.java` (~1110 lines), `ChatActivityMemberRequestsDelegate.java` (~210 lines), and `MemberRequestsBottomSheet.java` (~160 lines). UI delegates directly called MTProto queries (`TLRPC.TL_messages_getChatInviteImporters`, `TLRPC.TL_messages_hideChatJoinRequest`, `TLRPC.TL_messages_hideAllChatJoinRequests`) with anonymous `RequestDelegate` callbacks, mutating `ChatFull` structures in memory and invoking UI alerts from network callbacks.
- **Decision:** Introduce pure domain models `JoinRequestUserModel`, `JoinRequestModel`, `JoinRequestsListModel`, and `ChatPendingRequestsModel`. Define abstract contract `JoinRequestsRepository` covering pending requests count observation, cached requests lookup, paginated/filtered requests loading, single request approval/dismissal, and batch approval/dismissal. Implement `LegacyJoinRequestsRepository` operating on `Dispatchers.Main` with coroutine cancellation (`ConnectionsManager.cancelRequest`) and reactive updates via `NotificationCenterFlowBridge` observing `chatInfoDidLoad`. Encapsulate presentation state and MVI events in `JoinRequestsViewModel`.
- **Consequences:** Member join requests, batch approvals/dismissals, search queries, and chat top-panel pending counters are decoupled behind clean, testable domain interfaces with complete unit test coverage while preserving 100% compatibility with Telegram's MTProto member requests and chat updates protocols.

### ADR 029: Business Quick Replies & Shortcuts Controller Isolation
- **Context:** In Telegram Android, Business Quick Replies (canned responses, shortcuts `/name`, greeting/away auto-replies) are managed by `QuickRepliesController.java` (~870 lines). Quick reply shortcuts (`QuickReply`) maintain internal message IDs, message counts, order indices, and local pending flags. UI components like `QuickRepliesActivity.java` (~1400 lines) and `QuickRepliesSelectActivity.java` directly manipulated controller collections (`replies`, `localReplies`), invoked synchronous reordering and deletion, and triggered message sends via `SendMessagesHelper` without lifecycle or state isolation.
- **Decision:** Introduce pure domain models `QuickReplyModel` and `QuickRepliesLimitModel`. Define abstract contract `QuickRepliesRepository` with reactive flows (`observeQuickReplies`) and use cases covering loading, retrieval, name uniqueness validation (`checkQuickReplyNameBusy`), addition limits check (`canAddNewQuickReply`), renaming, reordering, batch deletion, and sending canned shortcut messages into chats. Implement `LegacyQuickRepliesRepository` operating strictly on `Dispatchers.Main` with reactive `NotificationCenterFlowBridge` observation on `quickRepliesUpdated`. Encapsulate presentation state and MVI events in `QuickRepliesViewModel`.
- **Consequences:** Quick replies management, shortcut expansion, reordering, and auto-reply administration are decoupled behind clean, testable domain interfaces with complete unit test coverage while preserving 100% compatibility with Telegram's MTProto business quick replies protocol and message sending pipelines.

### ADR 028: Channel Boosts, Status, Slots & Perks Controller Isolation
- **Context:** In Telegram Android, channel boosts, boost level calculation, available perks, and user boost slots (`myBoosts`) were scattered across `ChannelBoostsController.java` (~190 lines), `BoostRepository.java` (~910 lines), and UI activities (`BoostsActivity.java`, `ChannelBoostLayout.java`, `ReassignBoostBottomSheet.java`). Methods inside `ChannelBoostsController` directly posted alerts (`AlertDialog`, `BulletinFactory`) from inside callback methods when handling errors like `CHANNEL_PRIVATE` or network failures, violating layer separation and making background queries and headless testing impossible.
- **Decision:** Introduce pure domain models `BoostStatusModel`, `BoostSlotModel`, `MyBoostsModel`, and `CanApplyBoostModel`. Define abstract contract `BoostsRepository` covering boost status retrieval, user boost slot querying, boost eligibility checks (`checkCanApplyBoost`), and boost application/reassignment. Implement `LegacyBoostsRepository` decoupling network queries from UI dialogs and executing MTProto requests via `suspendCancellableCoroutine` with request cancellation support and `MessagesController` cache updates. Encapsulate all boost presentation state and MVI events in `BoostsViewModel`.
- **Consequences:** Channel boost management, slot assignment, eligibility validation, and perk levels are decoupled behind clean, testable domain interfaces with complete unit test coverage while preserving 100% compatibility with Telegram's MTProto stories/premium boost protocols.

### ADR 027: Message Reactions, Quick Double-Tap & Poll Voting Controller Isolation
- **Context:** In Telegram Android, message reactions and poll voting were split between `MediaDataController.java` (~10000 lines), `SendMessagesHelper.java` (~12300 lines), and UI components (`ChatActivity.java`, `ReactionsLayoutInBubble.java`). Reaction configurations were loaded into mutable lists (`reactionsList`, `recentReactions`, `topReactions`), while double-tap quick reaction was queried via raw SharedPreferences. Sending reactions and submitting poll votes required complex MTProto TL constructions (`TLRPC.TL_messages_sendReaction`, `TLRPC.TL_messages_sendVote`) executed directly inside helper classes with runnables and UI thread callbacks.
- **Decision:** Introduce pure domain models `ReactionItemModel`, `MessageReactionCountModel`, `MessageReactionsStateModel`, and `ReactionsSettingsModel`. Define abstract contract `ReactionsRepository` covering available and recent reactions observation, double-tap default reaction management, message reaction dispatch (single/multiple/big/recent), reaction removal, and poll vote submission. Implement `LegacyReactionsRepository` executing on `Dispatchers.Main` with reactive Kotlin `Flow`s observing `NotificationCenter.reactionsDidLoad` and coroutine-cancellable MTProto requests (`ConnectionsManager.sendRequest` / `cancelRequest`). Encapsulate presentation state and MVI events in `ReactionsViewModel`.
- **Consequences:** All reaction queries, quick double-tap preference modifications, message reaction updates, and poll voting operations are cleanly separated from legacy controllers and God objects behind pure domain interfaces, thoroughly verified by unit tests while preserving 100% MTProto and update processing compatibility.

### ADR 026: In-App Translation, Auto-Translate & Language Preferences Isolation
- **Context:** In Telegram Android, message and chat translation was fragmented across `TranslateController.java` (~1190 lines), `LocaleController.java` (~2000 lines), and UI activities such as `LanguageSelectActivity.java` and `RestrictedLanguagesSelectActivity.java`. UI views directly queried raw `TLRPC.TL_messages_translateText`, manipulated static `RestrictedLanguagesSelectActivity.getRestrictedLanguages()` sets, and triggered global `NotificationCenter` broadcasts without reactive state modeling or structured error handling.
- **Decision:** Introduce pure domain models `LanguageModel`, `TranslateSettingsModel`, `DialogTranslationStateModel`, and `TranslationResultModel`. Define abstract contract `TranslationRepository` covering chat translation toggle, context menu translation, do-not-translate language exception management, dialog-specific translation state and target language, raw text translation via MTProto, and application language switching. Implement `LegacyTranslationRepository` safely executing on `Dispatchers.Main` with reactive Kotlin `Flow`s observing `NotificationCenter.dialogTranslate` and `NotificationCenter.suggestedLangpack`. Encapsulate all translation UI state and events in `TranslateViewModel`.
- **Consequences:** Translation operations and language preferences are decoupled from legacy controller singletons, enabling clean unit testing with fakes and reactive Compose/View UI binding while preserving 100% compatibility with Telegram MTProto translation protocols and language pack mechanisms.

### ADR 025: Active Sessions, Connected Devices & QR Login Controller Isolation
- **Context:** In Telegram Android, managing logged-in devices, active web authorizations, inactive session TTL self-destruct timers, and QR code login approvals was handled directly by monolithic UI classes like `SessionsActivity.java` (~1380 lines) and `SessionBottomSheet.java` (~460 lines). These UI classes manually sent raw MTProto requests (`TL_account.getAuthorizations`, `TL_account.resetAuthorization`, `TLRPC.TL_auth_resetAuthorizations`, `TL_account.setAuthorizationTTL`, `TLRPC.TL_auth_acceptLoginToken`), parsed Base64 tokens from deep links, and manipulated raw push registration flags across user accounts.
- **Decision:** Introduce pure domain models `SessionModel`, `WebSessionModel`, and `SessionsListModel`. Define abstract contract `SessionsRepository` with reactive flows for sessions and web authorizations. Implement `LegacySessionsRepository` leveraging `suspendCancellableCoroutine` with MTProto request cancellation (`ConnectionsManager.cancelRequest`), reactive synchronization via `NotificationCenter.newSessionReceived`, and clean Base64 QR token parsing. Encapsulate all presentation state and termination/TTL/QR events in `SessionsViewModel`.
- **Consequences:** Device authorizations, web sessions, session-specific permission toggles (accepting secret chats and calls), inactive session TTL destruction, and QR code login flows are isolated behind clean, testable boundaries with complete unit test coverage while preserving 100% compatibility with Telegram MTProto session protocol and push notification token synchronization.

### ADR 024: Live Locations, GPS Updates & Proximity Alerts Controller Isolation
- **Context:** In Telegram Android, background and foreground location tracking, live location sharing (`SharingLocationInfo`), peer locations caching (`locationsCache`), and proximity distance alerts are handled by `LocationController.java` (~1420 lines). UI components like `LocationActivity.java` directly inspected internal controller arrays, registered raw `LocationListener` callbacks, and invoked synchronous network updates.
- **Decision:** Introduce pure domain models `GeoPointModel`, `LiveLocationSharingModel`, and `PeerLiveLocationModel`. Define abstract contract `LocationRepository` and implement `LegacyLocationRepository` executing on `Dispatchers.Main` with reactive Kotlin `Flow`s bound to global `liveLocationsChanged`, `liveLocationsCacheChanged`, and `newLocationAvailable` events. Encapsulate dialog-specific location observation, proximity configuration, and static/live location sending in `LocationViewModel`.
- **Consequences:** Live location broadcast management, peer location tracking, and proximity alerts are decoupled behind clean, testable domain interfaces while preserving 100% compatibility with Telegram's background location service and MTProto broadcast protocol.

### ADR 023: Forum Topics, Supergroup Threads & TopicsController Isolation
- **Context:** In Telegram Android, forum topics for supergroups are managed by `TopicsController.java` (~1390 lines) instantiated per-account on `MessagesController`. Legacy UI components (`TopicsFragment`, `ForumUtilities`, topic dialog headers) directly manipulated sparse collections (`topicsByChatId`, `topicsMapByChatId`), raw bitmasks, and mutable objects without thread safety guarantees or lifecycle isolation.
- **Decision:** Introduce pure domain models `TopicModel`, `TopicFilterType`, and `ForumUnreadCountModel`. Define abstract contract `TopicsRepository` and implement `LegacyTopicsRepository` ensuring all `TopicsController` queries and updates run safely on `Dispatchers.Main`. Reactive observation is provided through `NotificationCenterFlowBridge` observing `topicsDidLoaded`. Presentation logic with multi-criteria filtering (open, closed, pinned, hidden, title search) and unread counting is encapsulated in `TopicsViewModel`.
- **Consequences:** Topic lifecycle operations (close/reopen, pin/unpin, hide/show, delete, reorder pinned, reaction read receipts) can now be driven from modern Kotlin ViewModels with full unit-test coverage while keeping Telegram's internal forum cache and MTProto synchronization intact.

### ADR 022: Data & Storage Usage, Cache Control & Network Usage Controller Isolation
- **Context:** In Telegram Android, storage calculation, cache clearing, database compaction, auto-download presets, network traffic statistics, and media retention policies are scattered across `CacheControlActivity.java` (~3140 lines), `DataSettingsActivity.java` (~990 lines), `DataAutoDownloadActivity.java` (~960 lines), `StatsController.java` (~290 lines), `DownloadController.java` (~1810 lines), and `CacheByChatsController.java` (~215 lines). UI components directly executed synchronous file directory walks, mutated global SharedPreferences keys (`mobilePreset`, `wifiPreset`, `roamingPreset`), and performed database reset operations.
- **Decision:** Introduce pure domain models `NetworkUsageModel`, `StorageUsageModel`, `AutoDownloadPresetModel`, `KeepMediaSettingsModel`, and enums `NetworkUsageType`, `AutoDownloadNetworkType`. Define abstract contract `DataStorageRepository` and implement `LegacyDataStorageRepository` performing IO operations off the UI thread (`Dispatchers.IO`) while keeping legacy controller calls safe on `Dispatchers.Main`. Presentation logic is cleanly isolated in `DataStorageViewModel`.
- **Consequences:** Cache clearing by category (photos, videos, documents, music, stickers, stories), database clearing, network stats reset, auto-download presets configuration, and media retention policies can now be driven from clean ViewModels and fully verified in unit tests without touching upstream file pipelines.

### ADR 021: Payments, Telegram Stars & Subscriptions Controller Isolation
- **Context:** In Telegram Android, Telegram Stars (in-app virtual currency), balance top-up options, star transactions history, bot subscriptions, and peer transactions are managed by `StarsController.java` (~5120 lines). Complex in-memory collections (`transactions[type]`, `subscriptions`, `options`) and state queries (`getBalance(false)`, `balanceAvailable()`) were directly accessed by UI activities (`StarsIntroActivity`, `StarsTransactionsLayout`, bot purchase dialogs), tightly coupling presentation code to Telegram's billing and transaction internals.
- **Decision:** Introduce pure domain models `StarsBalanceModel`, `StarTransactionModel`, `StarSubscriptionModel`, and `StarTopupOptionModel`. Define abstract contract `PaymentsRepository` and implement `LegacyPaymentsRepository` executing on `Dispatchers.Main`. Reactive observation is provided through `NotificationCenterFlowBridge` listening to `starBalanceUpdated`, `starTransactionsLoaded`, and `starSubscriptionsLoaded`. Presentation logic is encapsulated in `PaymentsViewModel`.
- **Consequences:** Balance queries, transactions filtering, active subscription management, and top-up tier selection are decoupled behind testable domain use cases and a reactive ViewModel, preserving full compatibility with upstream Play Store / App Store billing flows without touching monolithic `StarsController.java`.

### ADR 020: Stories, Statuses & Stealth Mode Controller Isolation
- **Context:** In Telegram Android, stories are managed by `StoriesController.java` (~5270 lines) which handles in-memory peer stories collections (`dialogListStories`, `hiddenListStories`, `currentUserStories`), stealth mode timers, reading state persistence, and upload services. UI components like `PeerStoriesView`, `ProfileStoriesView`, `StoryViewer`, and `DialogStoriesCell` directly read unsynchronized controller arrays and post loose notifications, creating risks of concurrency crashes and tight UI coupling.
- **Decision:** Introduce pure domain models `StoryModel`, `PeerStoriesModel`, `StealthModeModel`, and `StoryLimitModel`. Define abstract contract `StoriesRepository` and implement `LegacyStoriesRepository` on `Dispatchers.Main` with reactive observation through `NotificationCenterFlowBridge` listening to `storiesUpdated`, `storiesReadUpdated`, and `stealthModeChanged`.
- **Consequences:** Story observation, read markers, deletion, pin toggling, stealth mode activation, and limit calculations are decoupled behind a clean `StoriesViewModel` and testable domain use cases without touching complex MTProto upload pipelines or breaking upstream compatibility.

### ADR 019: Themes, Night Mode, Appearance & Wallpaper Controller Isolation
- **Context:** In Telegram Android, theme customization, day/night auto-switching, bubble radius, accent colors, and chat wallpapers are managed across `Theme.java` (~10150 lines), `SharedConfig` (bubble radius, font size), and `MessagesController.getGlobalMainSettings()`. Global theme states (`selectedAutoNightType`, `autoNightDayStartTime`, `autoNightBrighnessThreshold`, `ThemeInfo`, `ThemeAccent`) are heavily relied upon across hundreds of UI activities and custom views (`ThemeActivity`, `ChatActivity`, `ActionBar`).
- **Decision:** Introduce pure domain models `ThemeModel`, `ThemeAccentModel`, `NightModeSettingsModel`, `WallpaperModel`, `AppearanceSettingsModel`, and enum `NightModeType`. Define abstract contract `ThemeRepository` and implement `LegacyThemeRepository` executing on `Dispatchers.Main` with reactive observation through `NotificationCenterFlowBridge.observeGlobalEvent` hooked into global lifecycle events (`didSetNewTheme`, `themeListUpdated`, `didApplyNewTheme`, `themeAccentListUpdated`, `needSetDayNightTheme`, `didSetNewWallpapper`).
- **Consequences:** All theme switching, night mode scheduling, bubble radius manipulation, and wallpaper settings are decoupled behind a clean, reactive `ThemeViewModel` and testable domain use cases without breaking upstream compatibility or modifying monolithic `Theme.java`.

### ADR 018: Privacy, Security, Passcode & 2FA Controller Isolation
- **Context:** In Telegram Android, user privacy rules (who sees phone, last seen, photos, forwards, voice messages, bio, birthday, gifts), the blocklist, passcode lock, and two-step verification (2FA) are spread across disparate subsystems: `ContactsController.privacyRules`, `MessagesController.blockePeers`, `SharedConfig.passcodeHash`, `SharedConfig.appLocked`, and `TL_account.getPassword`. UI activities (`PrivacySettingsActivity`, `PrivacyControlActivity`, `PrivacyUsersActivity`, `PasscodeActivity`) directly invoked MTProto requests and manipulated global flags, tightly coupling UI to legacy controllers.
- **Decision:** Introduce pure domain models `PrivacyRuleModel`, `BlockedPeerModel`, `PasscodeSettingsModel`, and `TwoStepVerificationModel`. Define abstract contract `PrivacyRepository` and implement `LegacyPrivacyRepository` on `Dispatchers.Main`. Changes to privacy rules, blocked lists, and two-step verification passwords are observed via `NotificationCenterFlowBridge` listening to `privacyRulesUpdated`, `blockedUsersDidLoad`, and `didSetOrRemoveTwoStepPassword`.
- **Consequences:** All privacy configurations, passcode validation, blocklist operations, and 2FA status checks are decoupled behind `PrivacyViewModel` and isolated domain use cases without breaking upstream compatibility or modifying sensitive security internals.

### ADR 017: Notifications, Push & Badges Controller Isolation
- **Context:** In Telegram Android, push notifications, launcher badge count, sound/vibration alerts, and Android O+ notification channels are managed inside `NotificationsController` (~6420 lines). Notification preferences are spread across account-specific `SharedPreferences` keys (`EnableAll2`, `EnableGroup2`, `EnableChannel2`, `EnableInChatSound`, `EnableInAppSounds`, `EnableInAppVibrate`, `EnableInAppPreview`, `EnableContactJoined`, `PinnedMessages`, `badgeNumber`, `badgeNumberMuted`, `badgeNumberMessages`). UI components such as `NotificationsSettingsActivity` directly read and mutate these SharedPreferences and controller fields, leading to tight UI-controller coupling.
- **Decision:** Introduce pure domain models `NotificationSettingsModel`, `BadgeSettingsModel`, `BadgeCountModel`, `DialogMuteState`, and enum `NotificationPeerType`. Define `NotificationsRepository` and implement `LegacyNotificationsRepository` that wraps `NotificationsController` and `MessagesController` operations on `Dispatchers.Main`. Changes to settings and badge counters are observed reactively using `NotificationCenterFlowBridge` listening to `NotificationCenter.notificationsSettingsUpdated`, `notificationsCountUpdated`, and `updateInterfaces`.
- **Consequences:** All notification toggles, mute states, and badge counters are decoupled behind a clean, reactive `NotificationsViewModel` and testable domain use cases without modifying upstream `NotificationsController` or risking upstream synchronization conflicts.

### ADR 016: Search & Global Search Isolation
- **Context:** In Telegram Android, search functionality is scattered across `DialogsSearchAdapter` (~2550 lines), `SearchAdapterHelper` (~635 lines), `MessagesStorage.searchDialogs`, and `MessagesController`. Search results mix raw `TLRPC.TL_contacts_search` MTProto calls, SQLite queries on `search_recent` and `hashtag_recent_v2`, and in-memory dialog filtering.
- **Decision:** Introduce pure domain models `SearchResultModel`, `SearchResultType` and `SearchFilter`. Define `SearchRepository` and implement `LegacySearchRepository` that performs safe Main-thread access and cancellable coroutines over `ConnectionsManager` and `MessagesStorage`. Create `SearchViewModel` with coroutine-based debouncing (300ms) for global search and instant local search.
- **Consequences:** Search across global users, bots, channels, groups, local contacts, recent queries, and hashtags is decoupled from monolithic UI adapters behind a unified clean API.

### ADR 015: File Loader & Background Downloads Isolation
- **Context:** In Telegram Android, file downloads and uploads are governed by `FileLoader` (~1970 lines) and `DownloadController` (~1810 lines). In-memory structures (`downloadingFiles`, `recentDownloadingFiles`, `uploadOperationPaths`, `loadOperationPaths`) mix network chunk requests, disk storage pathing, and UI notifications. UI components relied on loose `NotificationCenter` observer constants (`fileLoadProgressChanged`, `fileLoaded`, `fileLoadFailed`, `fileUploadProgressChanged`, `fileUploaded`, `fileUploadFailed`, `onDownloadingFilesChanged`) with untyped argument arrays.
- **Decision:** Introduce pure domain models `FileTransferModel`, typed enums `FileTransferType` and `FileTransferStatus`, and request descriptors `FileDownloadRequest` and `FileUploadRequest`. Define `FileLoaderRepository` and implement `LegacyFileLoaderRepository` that wraps `FileLoader` and `DownloadController` on `Dispatchers.Main`. State updates and progress events are converted into a reactive `Flow<List<FileTransferModel>>` via `callbackFlow`.
- **Consequences:** Download queuing, file upload operations, progress observation, and cancellation are unified under `FileLoaderViewModel` without leaking `MessageObject`, `TLRPC.Document`, or loose `NotificationCenter` arguments to the UI layer.

### ADR 014: Stickers & Emojis Media Controller Isolation
- **Context:** In Telegram Android, sticker sets, recent stickers, and emoji-associated stickers are handled inside `MediaDataController` (~10000 lines). In-memory structures `stickerSets`, `stickersByIds`, and `allStickers` are heavily coupled to `TLRPC.TL_messages_stickerSet` and `TLRPC.Document`. Direct UI consumption caused massive coupling to Telegram's document attributes (`TL_documentAttributeSticker`, `TL_documentAttributeCustomEmoji`).
- **Decision:** Introduce typed domain entities `StickerModel`, `StickerSetModel`, and enum `StickerType`. Define `StickersRepository` and implement `LegacyStickersRepository` executing on `Dispatchers.Main` with reactive observation through `callbackFlow` hooked into `NotificationCenter.stickersDidLoad` and `recentDocumentsDidLoad`.
- **Consequences:** Sticker selection, pack installation/archival, and emoji matching are fully isolated behind `StickersViewModel` and domain use cases without leaking TLRPC internals into the UI layer.

### ADR 013: Folders & Chat Filters Isolation and Order Management
- **Context:** In Telegram Android, dialog filter tabs / folders are stored in `MessagesController.dialogFilters` (`ArrayList<DialogFilter>`) and `dialogFiltersById` (`SparseArray<DialogFilter>`). Mutation occurs via UI fragments (`FiltersSetupActivity`, `FilterCreateActivity`) directly mutating the in-memory array, calling `MessagesStorage.saveDialogFilter`, and sending RPCs. These collections are modified on `Dispatchers.Main` and lacked clean encapsulation.
- **Decision:** Introduce pure domain models `FolderModel` and `SuggestedFolderModel` and repository contract `FoldersRepository`. Implement `LegacyFoldersRepository` that wraps `MessagesController` and `MessagesStorage` mutations (`addFilter`, `onFilterUpdate`, `removeFilter`, `saveDialogFilter`, `deleteDialogFilter`, `saveDialogFiltersOrder`) on `Dispatchers.Main`. Reactive observation is provided through `callbackFlow` listening to `NotificationCenter.dialogFiltersUpdated` and `suggestedFiltersLoaded`.
- **Consequences:** Folder creation, editing, deletion, reordering, and suggested folder discovery are fully testable and decoupled from Telegram UI fragments and raw `MessagesController.DialogFilter` structures.

### ADR 012: Contacts Controller Isolation and Synchronization Boundary
- **Context:** In Telegram Android, `ContactsController` (~3100 lines) manages system contacts synchronization, phonebook hashing, server contacts import, and in-memory lists `contacts` (`ArrayList<TLRPC.TL_contact>`) and `contactsDict` (`HashMap<Long, TLRPC.TL_contact>`). These collections are unsynchronized and modified exclusively on `Dispatchers.Main`. Furthermore, user names, online statuses, and avatars are stored separately in `MessagesController`.
- **Decision:** Introduce a clean domain model `ContactModel` and contract `ContactsRepository`. Implement `LegacyContactsRepository` adapting `ContactsController` and `MessagesController`, enforcing all reads and mutations on `Dispatchers.Main`. Reactive observation is provided through `callbackFlow` hooked into `NotificationCenter.contactsDidLoad` and `updateInterfaces`.
- **Consequences:** Contacts list, search, addition, and deletion are isolated behind a testable `ContactsViewModel` and domain use cases, without coupling UI or domain logic to `ContactsController` or raw `TLRPC` objects.

### ADR 011: Secret Chats End-to-End Encryption and DH State Machine Isolation
- **Context:** Telegram Secret Chats use client-to-client Diffie-Hellman key exchange and custom MTProto encrypted layers orchestrated by `SecretChatHelper` (~2050 lines). Encrypted chat models in legacy code are polymorphic subclasses of `TLRPC.EncryptedChat` (`TL_encryptedChatWaiting`, `TL_encryptedChatRequested`, `TL_encryptedChat`, `TL_encryptedChatDiscarded`) with separate integer chat IDs mapped to 64-bit dialog IDs (`DialogObject.makeEncryptedDialogId`). Direct UI access to `SecretChatHelper` leaked crypto state and raw network requests into View layers.
- **Decision:** Introduce a typed enum `SecretChatState` and immutable domain model `SecretChatModel`. Define `SecretChatRepository` and implement `LegacySecretChatRepository` that adapts `SecretChatHelper` operations (`startSecretChat`, `acceptSecretChat`, `declineSecretChat`, `sendTTLMessage`, `sendScreenshotMessage`) on `Dispatchers.Main`. Reactive observation is provided through `callbackFlow` hooked into `NotificationCenter.encryptedChatUpdated`, `encryptedChatCreated`, and `dialogsNeedReload`.
- **Consequences:** End-to-end encryption primitives, key generation, and DH exchanges remain safely inside Telegram's battle-tested crypto layer without risking upstream divergence, while the presentation layer is isolated behind a clean, testable `SecretChatViewModel`.

### ADR 010: VoIP Call State Isolation and Strangler Fig Boundary
- **Context:** In Telegram Android, VoIP calls are managed by `VoIPService` (~5800 lines) which acts as an Android Service, audio router, WebRTC controller, and state machine using raw integer constants (`STATE_WAITING_INCOMING`, `STATE_ESTABLISHED`, etc.). UI components like `VoIPFragment` directly bind to `VoIPService.getSharedInstance()` and implement `VoIPService.StateListener`.
- **Decision:** Introduce a typed enum `CallState` and immutable domain model `CallModel`. Define `VoIPRepository` and implement `LegacyVoIPRepository` adapting `VoIPService` operations (`acceptIncomingCall`, `declineIncomingCall`, `hangUp`, `setMicMute`, `toggleSpeakerphoneOrShowRouteSheet`) and observing call state through `callbackFlow` hooked into both `NotificationCenter` lifecycle events (`didStartedCall`, `didEndCall`) and `VoIPService.StateListener`.
- **Consequences:** VoIP calling logic and UI can now be controlled via clean, testable `CallViewModel` and use cases without coupling to Android Service lifecycle or WebRTC internals.

### ADR 009: Media and Gallery Abstraction Boundary
- **Context:** In Telegram Android, `MediaController` is a massive ~7000-line controller handling media playback, recording, audio state, and gallery loading. Gallery models `AlbumEntry` and `PhotoEntry` mix file metadata with complex in-place image editor state and UI flags.
- **Decision:** Extract gallery and album functionality behind a clean `MediaRepository` contract with pure immutable models `MediaItemModel` and `MediaAlbumModel`. `LegacyMediaRepository` delegates album and media queries to `MediaController.allMediaAlbums` and `allPhotosAlbumEntry` strictly on `Dispatchers.Main`.
- **Consequences:** Presentation code (gallery pickers, media grids) interacts solely with pure Kotlin data models and `MediaViewModel`, insulated from `MediaController`'s internal mutable state machines.

### ADR 008: Settings Aggregation and Thread-Safe Persistence
- **Context:** Telegram stores settings across multiple mutable static singletons: `SharedConfig` (global app-wide preferences like font size, bubble radius, stream media, in-app camera) and `UserConfig` (account-scoped preferences like contact syncing, call tab visibility). Direct access from UI components scattered configuration logic and caused race conditions during persistence.
- **Decision:** Introduce an aggregated, immutable domain model `SettingsModel` and `SettingsRepository`. `LegacySettingsRepository` encapsulates interactions with `SharedConfig` and `UserConfig(currentAccount)`, enforcing execution on `Dispatchers.Main` and immediate persistence calls (`saveConfig`). Changes are observed reactively through a combination of `NotificationCenter` event hooks and repository state flows.
- **Consequences:** Presentation layer observes and updates settings via a clean, unified ViewModel without knowing how settings are split across SharedPreferences or account databases.

### ADR 007: Unified Peer Profile Boundary
- **Context:** In legacy Telegram, peer profiles are split across separate classes: `TLRPC.User`, `TLRPC.Chat`, `TLRPC.UserFull`, and `TLRPC.ChatFull`. Furthermore, user IDs are positive while chat/channel IDs are negative (`-chatId`), and data fetching/blocking uses different methods on `MessagesController`.
- **Decision:** Introduce a single domain model `ProfileModel` with unified fields (`id`, `title`, `username`, `phone`, `about`, `isUser`, `isChannel`, `isGroup`, `isBot`, `isVerified`, `isScam`, `isFake`, `isBlocked`, etc.) and a unified contract `ProfileRepository`. `LegacyProfileRepository` transparently resolves whether `peerId` is user or chat/channel, dispatches calls to the respective `MessagesController` cache/methods on `Dispatchers.Main`, and emits updates via reactive `callbackFlow` hooked into `userInfoDidLoad` and `chatInfoDidLoad`.
- **Consequences:** Presentation layer (profile screens, user cards, headers) interacts with a single, consistent model and ViewModel regardless of peer type, completely shielded from legacy TLRPC distinctions.

### ADR 006: Chat Messaging Boundary and Send Pipeline
- **Context:** In Telegram, sending messages involves complex queuing, encryption, retry mechanics, and offline synchronization within `SendMessagesHelper`. Re-implementing this would break upstream compatibility and risk message loss.
- **Decision:** Wrap sending inside `LegacyChatRepository.sendMessage` by delegating to `SendMessagesHelper.sendMessage(SendMessageParams.of(text, dialogId))`, and map cached messages from `MessagesController.dialogMessage` on `Dispatchers.Main` into immutable `MessageModel` domain instances.
- **Consequences:** Upstream MTProto update protocol and sending pipeline remain completely intact while presentation code is decoupled from legacy singletons.

### ADR 005: Dialogs List Isolation and Reactive Bridge
- **Context:** `MessagesController.allDialogs` and `dialogsByFolder` are unsynchronized in-memory collections of Telegram, mutated on `Dispatchers.Main`. Direct background access throws `ConcurrentModificationException`.
- **Decision:** Wrap `MessagesController.getDialogs(folderId)` inside `LegacyDialogsRepository` using `callbackFlow` hooked into `NotificationCenter.dialogsNeedReload`, `updateInterfaces`, and `dialogDeleted`. All collection reads occur on `Dispatchers.Main`.
- **Consequences:** UI and presentation layers consume a clean, reactive `Flow<List<DialogModel>>` without knowing about `TLRPC.Dialog` or `MessagesController`.

### ADR 001: Strangler Fig Pattern for Migration
- **Context:** Telegram Android is a 10+ year old monolithic codebase with millions of lines of code. A full rewrite would take years and fail upstream compatibility.
- **Decision:** Use the Strangler Fig pattern. New features and refactored features are implemented using clean Presentation-Domain-Data architecture, with Data repositories wrapping existing legacy controllers.
- **Consequences:** Existing functionality remains unbroken; upstream patches can be merged with minimal conflicts.

### ADR 002: In-Place Package Modularization
- **Context:** Creating separate Gradle subprojects for every domain/data layer immediately would complicate CMake/NDK dependencies, `buildSrc` codegen, and packaging.
- **Decision:** Keep new architecture inside `TMessagesProj` under dedicated `core.*` and `feature.*` packages, strictly enforced by architectural conventions, before physically splitting into Gradle modules later.

### ADR 003: Lightweight AccountFeatureContainer
- **Context:** Telegram supports up to 4 concurrent user accounts (`currentAccount = 0..3`). Global singletons cause state leaks across accounts. Introducing heavy DI frameworks (like Dagger/Hilt) risks massive code churn.
- **Decision:** Implement a lightweight, thread-safe `AccountFeatureContainer` indexed by `currentAccount`, lazily creating repository, usecase, and viewmodel instances for each active account.

### ADR 004: NotificationCenterFlowBridge for Reactive Upstream Sync
- **Context:** Legacy Telegram notifies UI of data changes via `NotificationCenter.getInstance(account).postNotificationName(...)`.
- **Decision:** Build a Kotlin Coroutines `callbackFlow` bridge that registers as a `NotificationCenterDelegate` and cleanly emits typed updates, automatically unregistering when the collector scope cancels.
