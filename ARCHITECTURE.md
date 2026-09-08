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
│       └── AccountFeatureContainer.kt     # Scoped Service Locator per currentAccount (test-overridable)
│
└── feature/                               # Migrated feature slices
    └── savedmessages/                     # Pilot Feature: Saved Messages
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── SavedDialogModel.kt
        │   │   └── SavedTagModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── SavedMessagesRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── GetSavedDialogsUseCase.kt
        │       ├── TogglePinSavedDialogUseCase.kt
        │       ├── DeleteSavedDialogUseCase.kt
        │       ├── GetSavedTagsUseCase.kt
        │       └── SearchSavedDialogsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy <-> Domain)
        │   │   └── SavedMessagesMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacySavedMessagesRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── SavedMessagesUiState.kt
            ├── SavedMessagesEvent.kt
            └── SavedMessagesViewModel.kt
    │
    └── dialogs/                           # Main Chat List Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   └── DialogModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── DialogsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── GetDialogsUseCase.kt
        │       ├── LoadMoreDialogsUseCase.kt
        │       ├── PinDialogUseCase.kt
        │       ├── DeleteDialogUseCase.kt
        │       └── MarkDialogAsReadUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC.Dialog <-> DialogModel)
        │   │   └── DialogMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyDialogsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── DialogsUiState.kt
            ├── DialogsEvent.kt
            └── DialogsViewModel.kt
    │
    └── chat/                              # Chat & Messaging Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── MessageModel.kt
        │   │   └── MessageDeliveryStatus.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ChatRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveMessagesUseCase.kt
        │       ├── GetMessagesUseCase.kt
        │       ├── LoadHistoryUseCase.kt
        │       ├── SendMessageUseCase.kt
        │       └── DeleteMessagesUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (MessageObject <-> MessageModel)
        │   │   └── ChatMessageMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyChatRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ChatUiState.kt
            ├── ChatEvent.kt
            └── ChatViewModel.kt
    │
    └── profile/                           # User & Chat Profile Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   └── ProfileModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ProfileRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveProfileUseCase.kt
        │       ├── GetProfileUseCase.kt
        │       ├── LoadFullProfileUseCase.kt
        │       ├── BlockPeerUseCase.kt
        │       └── UnblockPeerUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC.User / TLRPC.Chat <-> ProfileModel)
        │   │   └── ProfileMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyProfileRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ProfileUiState.kt
            ├── ProfileEvent.kt
            └── ProfileViewModel.kt
    │
    └── settings/                          # Settings & Preferences Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   └── SettingsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── SettingsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveSettingsUseCase.kt
        │       ├── GetSettingsUseCase.kt
        │       ├── UpdateFontSizeUseCase.kt
        │       ├── UpdateBubbleRadiusUseCase.kt
        │       ├── UpdateSaveToGalleryUseCase.kt
        │       ├── UpdateStreamMediaUseCase.kt
        │       └── UpdateSyncContactsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (SharedConfig / UserConfig -> SettingsModel)
        │   │   └── SettingsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacySettingsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── SettingsUiState.kt
            ├── SettingsEvent.kt
            └── SettingsViewModel.kt
    │
    └── media/                             # Media & Gallery Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── MediaItemModel.kt
        │   │   └── MediaAlbumModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── MediaRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveMediaAlbumsUseCase.kt
        │       ├── GetMediaAlbumsUseCase.kt
        │       ├── GetAlbumMediaUseCase.kt
        │       └── GetAllMediaUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (MediaController PhotoEntry/AlbumEntry -> Domain)
        │   │   └── MediaMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyMediaRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── MediaUiState.kt
            ├── MediaEvent.kt
            └── MediaViewModel.kt
    │
    └── voip/                              # Calls & VoIP Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── CallModel.kt
        │   │   └── CallState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── VoIPRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveCurrentCallUseCase.kt
        │       ├── GetCurrentCallUseCase.kt
        │       ├── StartCallUseCase.kt
        │       ├── AcceptCallUseCase.kt
        │       ├── DeclineCallUseCase.kt
        │       ├── HangUpCallUseCase.kt
        │       ├── ToggleMuteUseCase.kt
        │       └── ToggleSpeakerphoneUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (VoIPService state & models -> Domain)
        │   │   └── CallMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyVoIPRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── CallUiState.kt
            ├── CallEvent.kt
            └── CallViewModel.kt
    │
    └── secretchat/                        # Secret Chats & E2E Encryption Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── SecretChatModel.kt
        │   │   └── SecretChatState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── SecretChatRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveSecretChatUseCase.kt
        │       ├── ObserveSecretChatsUseCase.kt
        │       ├── GetSecretChatUseCase.kt
        │       ├── StartSecretChatUseCase.kt
        │       ├── AcceptSecretChatUseCase.kt
        │       ├── DeclineSecretChatUseCase.kt
        │       ├── SetSecretChatTtlUseCase.kt
        │       └── SendScreenshotNotificationUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC.EncryptedChat -> SecretChatModel)
        │   │   └── SecretChatMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacySecretChatRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── SecretChatUiState.kt
            ├── SecretChatEvent.kt
            └── SecretChatViewModel.kt
    │
    └── contacts/                        # Contacts & Phonebook Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   └── ContactModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ContactsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveContactsUseCase.kt
        │       ├── GetContactsUseCase.kt
        │       ├── GetContactUseCase.kt
        │       ├── AddContactUseCase.kt
        │       ├── DeleteContactUseCase.kt
        │       └── SearchContactsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC.TL_contact/User -> ContactModel)
        │   │   └── ContactMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyContactsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ContactsUiState.kt
            ├── ContactsEvent.kt
            └── ContactsViewModel.kt
    │
    └── folders/                         # Folders & Chat Filters Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── FolderModel.kt
        │   │   └── SuggestedFolderModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── FoldersRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveFoldersUseCase.kt
        │       ├── GetFoldersUseCase.kt
        │       ├── GetFolderUseCase.kt
        │       ├── CreateFolderUseCase.kt
        │       ├── UpdateFolderUseCase.kt
        │       ├── DeleteFolderUseCase.kt
        │       ├── ReorderFoldersUseCase.kt
        │       └── GetSuggestedFoldersUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (DialogFilter/Suggested -> Domain)
        │   │   └── FolderMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyFoldersRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── FoldersUiState.kt
            ├── FoldersEvent.kt
            └── FoldersViewModel.kt
    │
    └── stickers/                        # Stickers & Emojis Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── StickerModel.kt
        │   │   ├── StickerSetModel.kt
        │   │   └── StickerType.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── StickersRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveStickerSetsUseCase.kt
        │       ├── GetStickerSetsUseCase.kt
        │       ├── GetStickerSetUseCase.kt
        │       ├── GetRecentStickersUseCase.kt
        │       ├── GetStickersForEmojiUseCase.kt
        │       ├── ToggleStickerSetInstalledUseCase.kt
        │       └── ToggleStickerSetArchivedUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC sticker models -> Domain)
        │   │   └── StickerMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyStickersRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── StickersUiState.kt
            ├── StickersEvent.kt
            └── StickersViewModel.kt
    │
    └── notifications/                 # Notifications, Push & Badges Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── NotificationPeerType.kt
        │   │   ├── NotificationSettingsModel.kt
        │   │   ├── BadgeSettingsModel.kt
        │   │   ├── BadgeCountModel.kt
        │   │   └── DialogMuteState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── NotificationsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveNotificationSettingsUseCase.kt
        │       ├── GetNotificationSettingsUseCase.kt
        │       ├── ObserveBadgeUseCase.kt
        │       ├── GetBadgeUseCase.kt
        │       ├── ObserveBadgeSettingsUseCase.kt
        │       ├── GetBadgeSettingsUseCase.kt
        │       ├── TogglePeerNotificationsUseCase.kt
        │       ├── ToggleInChatSoundUseCase.kt
        │       ├── ToggleInAppSoundsUseCase.kt
        │       ├── ToggleInAppVibrateUseCase.kt
        │       ├── ToggleInAppPreviewUseCase.kt
        │       ├── ToggleContactJoinedNotificationsUseCase.kt
        │       ├── TogglePinnedMessagesNotificationsUseCase.kt
        │       ├── UpdateBadgeSettingsUseCase.kt
        │       ├── MuteDialogUseCase.kt
        │       ├── IsDialogMutedUseCase.kt
        │       └── RefreshBadgeUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (SharedPreferences/Controller -> Domain)
        │   │   └── NotificationMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyNotificationsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── NotificationsUiState.kt
            ├── NotificationsEvent.kt
            └── NotificationsViewModel.kt
    │
    └── privacy/                       # Privacy, Security, Passcode & 2FA Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── PrivacyRuleType.kt
        │   │   ├── PrivacyRuleMode.kt
        │   │   ├── PrivacyRuleModel.kt
        │   │   ├── BlockedPeerModel.kt
        │   │   ├── PasscodeSettingsModel.kt
        │   │   └── TwoStepVerificationModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PrivacyRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePrivacyRulesUseCase.kt
        │       ├── GetPrivacyRulesUseCase.kt
        │       ├── SetPrivacyRuleUseCase.kt
        │       ├── LoadPrivacyRulesUseCase.kt
        │       ├── ObserveBlockedPeersUseCase.kt
        │       ├── GetBlockedPeersUseCase.kt
        │       ├── BlockPrivacyPeerUseCase.kt
        │       ├── UnblockPrivacyPeerUseCase.kt
        │       ├── GetPasscodeSettingsUseCase.kt
        │       ├── SetPasscodeUseCase.kt
        │       ├── CheckPasscodeUseCase.kt
        │       ├── ClearPasscodeUseCase.kt
        │       ├── ObserveTwoStepVerificationUseCase.kt
        │       └── LoadTwoStepVerificationUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC/SharedConfig -> Domain)
        │   │   └── PrivacyMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPrivacyRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PrivacyUiState.kt
            ├── PrivacyEvent.kt
            └── PrivacyViewModel.kt
    │
    └── themes/                        # Themes, Night Mode, Appearance & Wallpaper Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── NightModeType.kt
        │   │   ├── ThemeAccentModel.kt
        │   │   ├── ThemeModel.kt
        │   │   ├── NightModeSettingsModel.kt
        │   │   ├── WallpaperModel.kt
        │   │   └── AppearanceSettingsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ThemeRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveAppearanceSettingsUseCase.kt
        │       ├── GetAppearanceSettingsUseCase.kt
        │       ├── ObserveAvailableThemesUseCase.kt
        │       ├── GetAvailableThemesUseCase.kt
        │       ├── ApplyThemeUseCase.kt
        │       ├── ObserveNightModeUseCase.kt
        │       ├── SetNightModeTypeUseCase.kt
        │       ├── SetNightModeSettingsUseCase.kt
        │       ├── SetThemeAccentUseCase.kt
        │       ├── SetBubbleRadiusUseCase.kt
        │       └── ResetAppearanceSettingsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (ThemeInfo/SharedConfig -> Domain)
        │   │   └── ThemeMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyThemeRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ThemeUiState.kt
            ├── ThemeEvent.kt
            └── ThemeViewModel.kt
    │
    └── stories/                       # Stories, Statuses & Stealth Mode Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── StoryModel.kt
        │   │   ├── PeerStoriesModel.kt
        │   │   ├── StealthModeModel.kt
        │   │   └── StoryLimitModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── StoriesRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveStoriesUseCase.kt
        │       ├── ObserveHiddenStoriesUseCase.kt
        │       ├── ObserveStealthModeUseCase.kt
        │       ├── ObserveSelfStoriesUseCase.kt
        │       ├── GetPeerStoriesUseCase.kt
        │       ├── MarkStoryAsReadUseCase.kt
        │       ├── DeleteStoryUseCase.kt
        │       ├── ToggleStoryPinUseCase.kt
        │       ├── ToggleStoryHiddenUseCase.kt
        │       ├── ActivateStealthModeUseCase.kt
        │       ├── GetStoryLimitUseCase.kt
        │       └── RefreshStoriesUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_stories -> Domain)
        │   │   └── StoryMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyStoriesRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── StoriesUiState.kt
            ├── StoriesEvent.kt
            └── StoriesViewModel.kt
    │
    └── payments/                      # Payments & Telegram Stars Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── StarsBalanceModel.kt
        │   │   ├── StarTransactionModel.kt
        │   │   ├── StarSubscriptionModel.kt
        │   │   └── StarTopupOptionModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PaymentsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveStarsBalanceUseCase.kt
        │       ├── ObserveStarTransactionsUseCase.kt
        │       ├── ObserveStarSubscriptionsUseCase.kt
        │       ├── GetStarsBalanceUseCase.kt
        │       ├── GetStarTransactionsUseCase.kt
        │       ├── GetStarSubscriptionsUseCase.kt
        │       ├── GetStarTopupOptionsUseCase.kt
        │       ├── RefreshStarsBalanceUseCase.kt
        │       ├── RefreshStarTransactionsUseCase.kt
        │       └── RefreshStarSubscriptionsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_stars -> Domain)
        │   │   └── PaymentMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPaymentsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PaymentsUiState.kt
            ├── PaymentsEvent.kt
            └── PaymentsViewModel.kt
    │
    └── datastorage/                   # Data, Storage Usage & Cache Control Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── NetworkUsageType.kt
        │   │   ├── NetworkUsageModel.kt
        │   │   ├── StorageUsageModel.kt
        │   │   ├── AutoDownloadNetworkType.kt
        │   │   ├── AutoDownloadPresetModel.kt
        │   │   └── KeepMediaSettingsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── DataStorageRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveNetworkUsageUseCase.kt
        │       ├── ObserveStorageUsageUseCase.kt
        │       ├── ObserveAutoDownloadPresetUseCase.kt
        │       ├── ObserveKeepMediaSettingsUseCase.kt
        │       ├── GetNetworkUsageUseCase.kt
        │       ├── ResetNetworkUsageUseCase.kt
        │       ├── GetStorageUsageUseCase.kt
        │       ├── ClearCacheUseCase.kt
        │       ├── ClearDatabaseUseCase.kt
        │       ├── GetAutoDownloadPresetUseCase.kt
        │       ├── UpdateAutoDownloadPresetUseCase.kt
        │       ├── GetKeepMediaSettingsUseCase.kt
        │       ├── UpdateKeepMediaUseCase.kt
        │       └── RefreshStorageUsageUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (StatsController/DownloadController -> Domain)
        │   │   └── DataStorageMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyDataStorageRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── DataStorageUiState.kt
            ├── DataStorageEvent.kt
            └── DataStorageViewModel.kt
    │
    └── topics/                         # Forum Topics & Supergroup Threads Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── TopicModel.kt
        │   │   ├── TopicFilterType.kt
        │   │   └── ForumUnreadCountModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── TopicsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveTopicsUseCase.kt
        │       ├── ObserveForumUnreadCountUseCase.kt
        │       ├── GetTopicsUseCase.kt
        │       ├── GetTopicUseCase.kt
        │       ├── LoadTopicsUseCase.kt
        │       ├── ReloadTopicsUseCase.kt
        │       ├── ToggleCloseTopicUseCase.kt
        │       ├── TogglePinTopicUseCase.kt
        │       ├── ToggleShowTopicUseCase.kt
        │       ├── DeleteTopicsUseCase.kt
        │       ├── ReorderPinnedTopicsUseCase.kt
        │       ├── MarkTopicReactionsAsReadUseCase.kt
        │       └── GetForumUnreadCountUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_forumTopic -> TopicModel)
        │   │   └── TopicMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyTopicsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── TopicsUiState.kt
            ├── TopicsEvent.kt
            └── TopicsViewModel.kt
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

---

## 6. Architecture Decision Records (ADRs)

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
