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
    │
    └── location/                       # Live Locations, GPS & Proximity Alerts Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── GeoPointModel.kt
        │   │   ├── LiveLocationSharingModel.kt
        │   │   └── PeerLiveLocationModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── LocationRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveActiveSharingsUseCase.kt
        │       ├── ObservePeerLocationsUseCase.kt
        │       ├── ObserveLastKnownLocationUseCase.kt
        │       ├── GetActiveSharingsUseCase.kt
        │       ├── IsSharingLocationUseCase.kt
        │       ├── GetSharingInfoUseCase.kt
        │       ├── GetLastKnownLocationUseCase.kt
        │       ├── LoadPeerLiveLocationsUseCase.kt
        │       ├── StopLocationSharingUseCase.kt
        │       ├── StopAllLocationSharingsUseCase.kt
        │       ├── SetProximityAlertUseCase.kt
        │       ├── SendStaticLocationUseCase.kt
        │       ├── SendLiveLocationUseCase.kt
        │       └── MarkLiveLocationsAsReadUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (SharingLocationInfo / Message -> Domain)
        │   │   └── LocationMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyLocationRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── LocationUiState.kt
            ├── LocationEvent.kt
            └── LocationViewModel.kt
    │
    └── sessions/                       # Active Sessions, Devices & QR Login Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── SessionModel.kt
        │   │   ├── WebSessionModel.kt
        │   │   └── SessionsListModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── SessionsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveSessionsUseCase.kt
        │       ├── ObserveWebSessionsUseCase.kt
        │       ├── GetSessionsUseCase.kt
        │       ├── LoadSessionsUseCase.kt
        │       ├── GetWebSessionsUseCase.kt
        │       ├── LoadWebSessionsUseCase.kt
        │       ├── TerminateSessionUseCase.kt
        │       ├── TerminateAllOtherSessionsUseCase.kt
        │       ├── TerminateWebSessionUseCase.kt
        │       ├── TerminateAllWebSessionsUseCase.kt
        │       ├── UpdateSessionSettingsUseCase.kt
        │       ├── SetSessionsTtlUseCase.kt
        │       └── AcceptQrLoginUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_authorization / TL_webAuthorization -> Domain)
        │   │   └── SessionMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacySessionsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── SessionsUiState.kt
            ├── SessionsEvent.kt
            └── SessionsViewModel.kt
    │
    └── translate/                      # In-App Translation & Language Settings Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── LanguageModel.kt
        │   │   ├── TranslateSettingsModel.kt
        │   │   ├── DialogTranslationStateModel.kt
        │   │   └── TranslationResultModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── TranslationRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveTranslateSettingsUseCase.kt
        │       ├── GetTranslateSettingsUseCase.kt
        │       ├── SetChatTranslateEnabledUseCase.kt
        │       ├── SetContextTranslateEnabledUseCase.kt
        │       ├── SetDoNotTranslateLanguagesUseCase.kt
        │       ├── AddDoNotTranslateLanguageUseCase.kt
        │       ├── RemoveDoNotTranslateLanguageUseCase.kt
        │       ├── ObserveDialogTranslationStateUseCase.kt
        │       ├── GetDialogTranslationStateUseCase.kt
        │       ├── ToggleDialogTranslatingUseCase.kt
        │       ├── SetDialogTargetLanguageUseCase.kt
        │       ├── TranslateTextUseCase.kt
        │       ├── GetAvailableLanguagesUseCase.kt
        │       └── ApplyAppLanguageUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_messages_translateResult / LocaleInfo -> Domain)
        │   │   └── TranslationMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyTranslationRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── TranslateUiState.kt
            ├── TranslateEvent.kt
            └── TranslateViewModel.kt
    │
    └── reactions/                      # Reactions, Polls & Quick Reactions Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── ReactionItemModel.kt
        │   │   ├── MessageReactionCountModel.kt
        │   │   ├── MessageReactionsStateModel.kt
        │   │   └── ReactionsSettingsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ReactionsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveAvailableReactionsUseCase.kt
        │       ├── GetAvailableReactionsUseCase.kt
        │       ├── LoadAvailableReactionsUseCase.kt
        │       ├── ObserveRecentReactionsUseCase.kt
        │       ├── GetRecentReactionsUseCase.kt
        │       ├── GetReactionsSettingsUseCase.kt
        │       ├── GetDoubleTapReactionUseCase.kt
        │       ├── SetDoubleTapReactionUseCase.kt
        │       ├── SendReactionUseCase.kt
        │       ├── ClearReactionsUseCase.kt
        │       └── SendVoteUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC.Reaction / TL_availableReaction -> Domain)
        │   │   └── ReactionMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyReactionsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ReactionsUiState.kt
            ├── ReactionsEvent.kt
            └── ReactionsViewModel.kt
    │
    └── boosts/                         # Channel Boosts, Status, Slots & Perks Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── BoostStatusModel.kt
        │   │   ├── BoostSlotModel.kt
        │   │   ├── MyBoostsModel.kt
        │   │   └── CanApplyBoostModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BoostsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── GetBoostsStatusUseCase.kt
        │       ├── GetMyBoostsUseCase.kt
        │       ├── CheckCanApplyBoostUseCase.kt
        │       └── ApplyBoostUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_stories / CanApplyBoost -> Domain)
        │   │   └── BoostMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBoostsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BoostsUiState.kt
            ├── BoostsEvent.kt
            └── BoostsViewModel.kt
    │
    └── quickreplies/                   # Business Quick Replies & Shortcuts Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── QuickReplyModel.kt
        │   │   └── QuickRepliesLimitModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── QuickRepliesRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveQuickRepliesUseCase.kt
        │       ├── GetQuickRepliesUseCase.kt
        │       ├── LoadQuickRepliesUseCase.kt
        │       ├── FindQuickReplyUseCase.kt
        │       ├── CheckQuickReplyNameBusyUseCase.kt
        │       ├── CanAddNewQuickReplyUseCase.kt
        │       ├── RenameQuickReplyUseCase.kt
        │       ├── ReorderQuickRepliesUseCase.kt
        │       ├── DeleteQuickRepliesUseCase.kt
        │       └── SendQuickReplyUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (QuickRepliesController.QuickReply -> Domain)
        │   │   └── QuickReplyMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyQuickRepliesRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── QuickRepliesUiState.kt
            ├── QuickRepliesEvent.kt
            └── QuickRepliesViewModel.kt
    │
    └── joinrequests/                   # Join Requests & Chat Administration Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── JoinRequestUserModel.kt
        │   │   ├── JoinRequestModel.kt
        │   │   ├── JoinRequestsListModel.kt
        │   │   └── ChatPendingRequestsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── JoinRequestsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePendingRequestsUseCase.kt
        │       ├── GetPendingRequestsCountUseCase.kt
        │       ├── GetCachedJoinRequestsUseCase.kt
        │       ├── LoadJoinRequestsUseCase.kt
        │       ├── ApproveJoinRequestUseCase.kt
        │       ├── DismissJoinRequestUseCase.kt
        │       ├── ApproveAllJoinRequestsUseCase.kt
        │       └── DismissAllJoinRequestsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_chatInviteImporter -> Domain)
        │   │   └── JoinRequestMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyJoinRequestsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── JoinRequestsUiState.kt
            ├── JoinRequestsEvent.kt
            └── JoinRequestsViewModel.kt
    │
    └── factcheck/                      # Message Fact-Checks & Verification Annotations Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── FactCheckEntityModel.kt
        │   │   ├── FactCheckModel.kt
        │   │   └── FactCheckLimitsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── FactCheckRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveFactCheckLoadedUseCase.kt
        │       ├── GetFactCheckUseCase.kt
        │       ├── LoadFactCheckUseCase.kt
        │       ├── ApplyFactCheckUseCase.kt
        │       ├── DeleteFactCheckUseCase.kt
        │       └── GetFactCheckLimitUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_factCheck -> Domain)
        │   │   └── FactCheckMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyFactCheckRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── FactCheckUiState.kt
            ├── FactCheckEvent.kt
            └── FactCheckViewModel.kt
    │
    └── birthdays/                      # User Birthdays, Contacts' Birthdays & Birthday Wishes Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── BirthdayDateModel.kt
        │   │   ├── BirthdayUserModel.kt
        │   │   ├── ContactBirthdayModel.kt
        │   │   └── BirthdayStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BirthdaysRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveBirthdaysUseCase.kt
        │       ├── GetBirthdaysStateUseCase.kt
        │       ├── CheckBirthdaysUseCase.kt
        │       ├── HideTodayBirthdaysUseCase.kt
        │       ├── IsBirthdayTodayUseCase.kt
        │       └── HasBirthdaysTodayUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_account.contactBirthdays / BirthdayState -> Domain)
        │   │   └── BirthdayMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBirthdaysRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BirthdaysUiState.kt
            ├── BirthdaysEvent.kt
            └── BirthdaysViewModel.kt
    │
    └── chattheme/                      # Chat Themes, Wallpapers & Emoji Status per Dialog Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── ChatThemeModel.kt
        │   │   └── DialogThemeStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ChatThemeRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveDialogThemeUseCase.kt
        │       ├── GetDialogThemeStateUseCase.kt
        │       ├── GetAvailableChatThemesUseCase.kt
        │       ├── SetDialogThemeUseCase.kt
        │       ├── ResetDialogThemeUseCase.kt
        │       └── SaveChatWallpaperUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (EmojiThemes / TLRPC.WallPaper -> Domain)
        │   │   └── ChatThemeMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyChatThemeRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ChatThemeUiState.kt
            ├── ChatThemeEvent.kt
            └── ChatThemeViewModel.kt
    │
    └── passkeys/                       # Passkeys & WebAuthn Authentication Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── PasskeyModel.kt
        │   │   └── PasskeysStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PasskeysRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePasskeysUseCase.kt
        │       ├── GetPasskeysUseCase.kt
        │       ├── DeletePasskeyUseCase.kt
        │       ├── CheckCanAddPasskeyUseCase.kt
        │       └── IsPasskeysSupportedUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_account.Passkey -> Domain)
        │   │   └── PasskeyMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPasskeysRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PasskeysUiState.kt
            ├── PasskeysEvent.kt
            └── PasskeysViewModel.kt
    │
    └── proxy/                          # Proxy Configuration & Auto-Rotation Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── ProxyType.kt
        │   │   ├── ProxyModel.kt
        │   │   └── ProxySettingsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ProxyRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveProxySettingsUseCase.kt
        │       ├── GetProxySettingsUseCase.kt
        │       ├── AddProxyUseCase.kt
        │       ├── DeleteProxyUseCase.kt
        │       ├── EnableProxyUseCase.kt
        │       ├── DisableProxyUseCase.kt
        │       ├── ToggleProxyRotationUseCase.kt
        │       └── CheckProxyPingUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (SharedConfig.ProxyInfo <-> Domain)
        │   │   └── ProxyMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyProxyRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ProxyUiState.kt
            ├── ProxyEvent.kt
            └── ProxyViewModel.kt
    │
    └── autodelete/                     # Auto-Delete Messages & Global History TTL Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── AutoDeleteTtlModel.kt
        │   │   ├── GlobalAutoDeleteStateModel.kt
        │   │   └── ChatAutoDeleteStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── AutoDeleteRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveGlobalAutoDeleteUseCase.kt
        │       ├── GetGlobalAutoDeleteUseCase.kt
        │       ├── SetGlobalAutoDeleteUseCase.kt
        │       ├── GetChatAutoDeleteUseCase.kt
        │       ├── SetChatAutoDeleteUseCase.kt
        │       └── SetChatsAutoDeleteBatchUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC TTL <-> Domain)
        │   │   └── AutoDeleteMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyAutoDeleteRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── AutoDeleteUiState.kt
            ├── AutoDeleteEvent.kt
            └── AutoDeleteViewModel.kt
    │
    └── unconfirmedauth/                # Unconfirmed Auth Sessions & Login Approvals Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── UnconfirmedAuthModel.kt
        │   │   └── UnconfirmedAuthStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── UnconfirmedAuthRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveUnconfirmedAuthsUseCase.kt
        │       ├── GetUnconfirmedAuthsUseCase.kt
        │       ├── ConfirmAuthUseCase.kt
        │       ├── DenyAuthUseCase.kt
        │       ├── ConfirmAllAuthsUseCase.kt
        │       ├── DenyAllAuthsUseCase.kt
        │       └── ClearUnconfirmedAuthsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (UnconfirmedAuthController.UnconfirmedAuth <-> Domain)
        │   │   └── UnconfirmedAuthMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyUnconfirmedAuthRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── UnconfirmedAuthUiState.kt
            ├── UnconfirmedAuthEvent.kt
            └── UnconfirmedAuthViewModel.kt
    │
    └── stargifts/                        # Telegram Star Gifts Catalog & Profile Saved Gifts
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── StarGiftModel.kt
        │   │   ├── SavedStarGiftModel.kt
        │   │   ├── StarGiftsCatalogModel.kt
        │   │   ├── StarGiftFilter.kt
        │   │   └── ProfileGiftsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── StarGiftsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveStarGiftsCatalogUseCase.kt
        │       ├── GetStarGiftsCatalogUseCase.kt
        │       ├── GetStarGiftByIdUseCase.kt
        │       ├── ObserveProfileGiftsUseCase.kt
        │       ├── LoadProfileGiftsUseCase.kt
        │       ├── TogglePinProfileGiftUseCase.kt
        │       └── ToggleHideProfileGiftUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_stars.StarGift / SavedStarGift <-> Domain)
        │   │   └── StarGiftMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyStarGiftsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── StarGiftsUiState.kt
            ├── StarGiftsEvent.kt
            └── StarGiftsViewModel.kt
    │
    └── aitones/                          # AI Compose Tones & Styles Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities
        │   │   ├── AiToneModel.kt
        │   │   └── AiTonesStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── AiTonesRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveAiTonesUseCase.kt
        │       ├── GetAiTonesStateUseCase.kt
        │       ├── LoadAiTonesUseCase.kt
        │       ├── AddAiToneUseCase.kt
        │       ├── RemoveAiToneUseCase.kt
        │       ├── UnsaveAiToneUseCase.kt
        │       └── EditAiToneUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_aicompose.AiComposeTone <-> Domain)
        │   │   └── AiToneMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyAiTonesRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── AiTonesUiState.kt
            ├── AiTonesEvent.kt
            └── AiTonesViewModel.kt
    │
    └── captcha/                          # reCAPTCHA Enterprise Verification Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── CaptchaAction.kt
        │   │   ├── CaptchaRequestModel.kt
        │   │   └── CaptchaResult.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── CaptchaRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveActiveCaptchaRequestsUseCase.kt
        │       ├── GetActiveCaptchaRequestsUseCase.kt
        │       ├── VerifyCaptchaUseCase.kt
        │       ├── SubmitCaptchaResultUseCase.kt
        │       └── CancelCaptchaUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & error formatters
        │   │   └── CaptchaMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyCaptchaRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── CaptchaUiState.kt
            ├── CaptchaEvent.kt
            └── CaptchaViewModel.kt
    │
    └── hashtagsearch/                    # Hashtag Search & History Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── HashtagSearchType.kt
        │   │   ├── HashtagMessageModel.kt
        │   │   └── HashtagSearchResultModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── HashtagSearchRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveHashtagHistoryUseCase.kt
        │       ├── GetHashtagHistoryUseCase.kt
        │       ├── AddHashtagToHistoryUseCase.kt
        │       ├── RemoveHashtagFromHistoryUseCase.kt
        │       ├── ClearHashtagHistoryUseCase.kt
        │       ├── ObserveHashtagSearchResultUseCase.kt
        │       ├── SearchHashtagUseCase.kt
        │       ├── JumpToHashtagMessageUseCase.kt
        │       └── ClearHashtagSearchResultsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (HashtagSearchType <-> Int, MessageObject <-> Domain)
        │   │   └── HashtagMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyHashtagSearchRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── HashtagSearchUiState.kt
            ├── HashtagSearchEvent.kt
            └── HashtagSearchViewModel.kt
    │
    └── biometrics/                       # Biometrics, Hardware Keystore & Passcode Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & status enums
        │   │   ├── BiometricStatus.kt
        │   │   └── BiometricKeyStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BiometricsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveBiometricKeyStateUseCase.kt
        │       ├── GetBiometricKeyStateUseCase.kt
        │       ├── CheckBiometricKeyReadyUseCase.kt
        │       ├── DeleteInvalidBiometricKeyUseCase.kt
        │       ├── IsBiometricKeyReadyUseCase.kt
        │       └── HasDeviceBiometricsChangedUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (hardware/keystore flags <-> domain)
        │   │   └── BiometricMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBiometricsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BiometricsUiState.kt
            ├── BiometricsEvent.kt
            └── BiometricsViewModel.kt
    │
    └── giftauctions/                     # Telegram Star Gift Auctions & Real-Time Bidding Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & status enums
        │   │   ├── GiftAuctionStatus.kt
        │   │   ├── GiftAuctionModel.kt
        │   │   ├── GiftAuctionBidParamsModel.kt
        │   │   └── GiftAuctionAcquiredGiftModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── GiftAuctionsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveActiveAuctionsUseCase.kt
        │       ├── ObserveAuctionUseCase.kt
        │       ├── GetActiveAuctionsUseCase.kt
        │       ├── GetAuctionByIdUseCase.kt
        │       ├── GetAuctionBySlugUseCase.kt
        │       ├── SendAuctionBidUseCase.kt
        │       ├── LoadAuctionAcquiredGiftsUseCase.kt
        │       └── RefreshActiveAuctionsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_stars <-> Domain, bid params)
        │   │   └── GiftAuctionMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyGiftAuctionsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── GiftAuctionsUiState.kt
            ├── GiftAuctionsEvent.kt
            └── GiftAuctionsViewModel.kt
    │
    └── businesslinks/                    # Telegram Business Chat Links & Shortcuts Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models
        │   │   ├── BusinessLinkModel.kt
        │   │   ├── BusinessLinkInputModel.kt
        │   │   └── BusinessLinksStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BusinessLinksRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveBusinessLinksUseCase.kt
        │       ├── GetBusinessLinksUseCase.kt
        │       ├── LoadBusinessLinksUseCase.kt
        │       ├── CreateBusinessLinkUseCase.kt
        │       ├── EditBusinessLinkUseCase.kt
        │       ├── DeleteBusinessLinkUseCase.kt
        │       ├── FindBusinessLinkUseCase.kt
        │       └── CanAddNewBusinessLinkUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_account <-> Domain)
        │   │   └── BusinessLinkMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBusinessLinksRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BusinessLinksUiState.kt
            ├── BusinessLinksEvent.kt
            └── BusinessLinksViewModel.kt
    │
    └── businessbots/                     # Telegram Business Chatbots & Connected Bots Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models
        │   │   ├── BusinessBotRightsModel.kt
        │   │   ├── BusinessBotRecipientsModel.kt
        │   │   ├── ConnectedBotModel.kt
        │   │   └── BusinessBotsStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BusinessBotsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveConnectedBotsUseCase.kt
        │       ├── GetConnectedBotsUseCase.kt
        │       ├── LoadConnectedBotsUseCase.kt
        │       ├── UpdateConnectedBotUseCase.kt
        │       ├── DeleteConnectedBotUseCase.kt
        │       └── FindConnectedBotUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_account <-> Domain)
        │   │   └── BusinessBotMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBusinessBotsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BusinessBotsUiState.kt
            ├── BusinessBotsEvent.kt
            └── BusinessBotsViewModel.kt
    │
    └── timezones/                        # Telegram Timezones & Business Hours Offset Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models
        │   │   ├── TimezoneModel.kt
        │   │   └── TimezonesStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── TimezonesRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveTimezonesUseCase.kt
        │       ├── GetTimezonesUseCase.kt
        │       ├── LoadTimezonesUseCase.kt
        │       ├── FindTimezoneUseCase.kt
        │       ├── GetSystemTimezoneIdUseCase.kt
        │       └── GetTimezoneNameUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC.TL_timezone <-> Domain)
        │   │   └── TimezoneMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyTimezonesRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── TimezonesUiState.kt
            ├── TimezonesEvent.kt
            └── TimezonesViewModel.kt
    │
    └── botstars/                         # Telegram Stars Bot Revenue, Balance & Transactions Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── BotStarsRevenueStatusModel.kt
        │   │   ├── BotStarsRevenueStatsModel.kt
        │   │   ├── BotStarsTransactionType.kt
        │   │   ├── BotStarsTransactionModel.kt
        │   │   ├── ConnectedBotStarRefModel.kt
        │   │   ├── StarRefProgramModel.kt
        │   │   └── BotStarsStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BotStarsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveBotStarsStatsUseCase.kt
        │       ├── GetBotStarsStatsUseCase.kt
        │       ├── ObserveTonStatsUseCase.kt
        │       ├── GetTonStatsUseCase.kt
        │       ├── ObserveBotTransactionsUseCase.kt
        │       ├── LoadBotTransactionsUseCase.kt
        │       ├── ObserveConnectedStarBotsUseCase.kt
        │       ├── LoadConnectedStarBotsUseCase.kt
        │       ├── LoadSuggestedStarBotsUseCase.kt
        │       └── GetAdminedBotsAndChannelsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TL_payments / TL_stars <-> Domain)
        │   │   └── BotStarsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBotStarsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BotStarsUiState.kt
            ├── BotStarsEvent.kt
            └── BotStarsViewModel.kt
    │
    └── billing/                          # Google Play Billing, Subscriptions & In-App Purchases Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── BillingProductType.kt
        │   │   ├── BillingPriceModel.kt
        │   │   ├── BillingProductModel.kt
        │   │   ├── BillingPurchaseState.kt
        │   │   ├── BillingPurchaseModel.kt
        │   │   └── BillingStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BillingRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveBillingStateUseCase.kt
        │       ├── GetBillingStateUseCase.kt
        │       ├── StartBillingConnectionUseCase.kt
        │       ├── GetPremiumProductUseCase.kt
        │       ├── FormatCurrencyUseCase.kt
        │       ├── GetCurrencyExpUseCase.kt
        │       ├── QueryBillingPurchasesUseCase.kt
        │       └── ManageSubscriptionUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (ProductDetails / Purchase <-> Domain)
        │   │   └── BillingMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBillingRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BillingUiState.kt
            ├── BillingEvent.kt
            └── BillingViewModel.kt
    │
    └── launchericon/                     # App Dynamic Launcher Icons & Premium Badging Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── LauncherIconType.kt
        │   │   ├── LauncherIconModel.kt
        │   │   └── LauncherIconsStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── LauncherIconRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveLauncherIconsUseCase.kt
        │       ├── GetLauncherIconsUseCase.kt
        │       ├── GetActiveLauncherIconUseCase.kt
        │       ├── IsLauncherIconEnabledUseCase.kt
        │       ├── SetLauncherIconUseCase.kt
        │       └── FixLauncherIconIfNeededUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (LauncherIconController.LauncherIcon <-> Domain)
        │   │   └── LauncherIconMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyLauncherIconRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── LauncherIconUiState.kt
            ├── LauncherIconEvent.kt
            └── LauncherIconViewModel.kt
    │
    └── push/                             # Push Notifications, FCM/HMS Registration & Device Tokens Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── PushServiceType.kt
        │   │   ├── PushStatusModel.kt
        │   │   └── PushRegistrationResult.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PushRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePushStatusUseCase.kt
        │       ├── GetPushStatusUseCase.kt
        │       ├── IsPushAvailableUseCase.kt
        │       ├── RequestPushTokenUseCase.kt
        │       ├── RegisterPushTokenUseCase.kt
        │       └── ResetPushTokenUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy PushType <-> Domain)
        │   │   └── PushMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPushRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PushUiState.kt
            ├── PushEvent.kt
            └── PushViewModel.kt
    │
    └── chromecast/                       # Google Cast, Remote Media Client & Media Streaming Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models
        │   │   ├── ChromecastMediaModel.kt
        │   │   └── ChromecastStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ChromecastRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveChromecastStateUseCase.kt
        │       ├── GetChromecastStateUseCase.kt
        │       ├── IsCastingUseCase.kt
        │       ├── IsMediaPlayingOnCastUseCase.kt
        │       ├── CastMediaUseCase.kt
        │       ├── StopCastingUseCase.kt
        │       └── SetCastCoverFileUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (ChromecastMedia <-> Domain)
        │   │   └── ChromecastMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyChromecastRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ChromecastUiState.kt
            ├── ChromecastEvent.kt
            └── ChromecastViewModel.kt
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
  - [x] Data layer: `QuickReplyMapper`, `LegacyQuickRepliesRepository` (Main-thread safe, adapting `QuickRepliesController` and `SendMessagesHelper` with `NotificationCenterFlowBridge` observing `NotificationCenter.quickRepliesUpdated`)
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
  - [x] Data layer: `BusinessLinkMapper`, `LegacyBusinessLinksRepository` (Main-thread safe, adapting `BusinessLinksController`, `NotificationCenter.businessLinksUpdated`, and MTProto business chat links protocol)
  - [x] Presentation layer: `BusinessLinksUiState`, `BusinessLinksEvent`, `BusinessLinksViewModel`
- [x] Telegram Business Chatbots & Connected Bots (`feature.businessbots`)
  - [x] Domain entities: `BusinessBotRightsModel`, `BusinessBotRecipientsModel`, `ConnectedBotModel`, `BusinessBotsStateModel`
  - [x] Repository contract: `BusinessBotsRepository`
  - [x] Use cases: `ObserveConnectedBotsUseCase`, `GetConnectedBotsUseCase`, `LoadConnectedBotsUseCase`, `UpdateConnectedBotUseCase`, `DeleteConnectedBotUseCase`, `FindConnectedBotUseCase`
  - [x] Data layer: `BusinessBotMapper`, `LegacyBusinessBotsRepository` (Main-thread safe, adapting `BusinessChatbotController`, `NotificationCenter.updatedChatbot`, and MTProto connected bots protocol)
  - [x] Presentation layer: `BusinessBotsUiState`, `BusinessBotsEvent`, `BusinessBotsViewModel`
- [x] Telegram Timezones & Business Hours Offset (`feature.timezones`)
  - [x] Domain entities: `TimezoneModel` (with formatted UTC offset and display name), `TimezonesStateModel`
  - [x] Repository contract: `TimezonesRepository`
  - [x] Use cases: `ObserveTimezonesUseCase`, `GetTimezonesUseCase`, `LoadTimezonesUseCase`, `FindTimezoneUseCase`, `GetSystemTimezoneIdUseCase`, `GetTimezoneNameUseCase`
  - [x] Data layer: `TimezoneMapper`, `LegacyTimezonesRepository` (Main-thread safe, adapting `TimezonesController`, `mainSettings` cache, and `NotificationCenter.timezonesUpdated`)
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

---

## 6. Architecture Decision Records (ADRs)

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
