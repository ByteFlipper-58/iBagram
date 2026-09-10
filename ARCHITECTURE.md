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
    │
    └── hints/                            # In-App Hints, Tips & Feature Discovery Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── HintType.kt
        │   │   ├── HintModel.kt
        │   │   └── HintsStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── HintsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveHintsUseCase.kt
        │       ├── GetHintsStateUseCase.kt
        │       ├── GetHintUseCase.kt
        │       ├── ShouldShowHintUseCase.kt
        │       ├── IncrementHintUseCase.kt
        │       ├── DoNotShowAgainHintUseCase.kt
        │       ├── ResetHintUseCase.kt
        │       └── ResetAllHintsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy HintsController.Hint <-> Domain)
        │   │   └── HintMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyHintsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── HintsUiState.kt
            ├── HintsEvent.kt
            └── HintsViewModel.kt
    │
    └── groupcallmsg/                     # Group Call & Conference In-Call Messages Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── GroupCallMessageSendStatus.kt
        │   │   ├── GroupCallMessageModel.kt
        │   │   └── GroupCallMessagesStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── GroupCallMessagesRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveGroupCallMessagesUseCase.kt
        │       ├── GetGroupCallMessagesUseCase.kt
        │       ├── SendGroupCallMessageUseCase.kt
        │       ├── PopGroupCallMessageUseCase.kt
        │       └── ClearGroupCallMessagesUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy GroupCallMessage <-> Domain)
        │   │   └── GroupCallMessageMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyGroupCallMessagesRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── GroupCallMessagesUiState.kt
            ├── GroupCallMessagesEvent.kt
            └── GroupCallMessagesViewModel.kt
    │
    └── gallerysave/                      # Auto-Save to Gallery Settings & Exceptions Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── GallerySavePeerType.kt
        │   │   ├── GallerySaveTargetSettingsModel.kt
        │   │   ├── GallerySaveDialogExceptionModel.kt
        │   │   └── GallerySaveConfigModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── GallerySaveRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveGallerySaveConfigUseCase.kt
        │       ├── GetGallerySaveConfigUseCase.kt
        │       ├── GetGallerySaveSettingsUseCase.kt
        │       ├── UpdateGallerySaveSettingsUseCase.kt
        │       ├── ToggleGallerySavePeerTypeUseCase.kt
        │       ├── SetGallerySaveVideoLimitUseCase.kt
        │       ├── GetGallerySaveExceptionsUseCase.kt
        │       ├── SetGallerySaveExceptionUseCase.kt
        │       ├── RemoveGallerySaveExceptionUseCase.kt
        │       └── RemoveAllGallerySaveExceptionsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (SaveToGallerySettingsHelper <-> Domain)
        │   │   └── GallerySaveMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyGallerySaveRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── GallerySaveUiState.kt
            ├── GallerySaveEvent.kt
            └── GallerySaveViewModel.kt
    │
    └── refreshrate/                      # Adaptive Display Refresh Rate & FPS Metrics Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models
        │   │   ├── DisplayRefreshModeModel.kt
        │   │   ├── RefreshRateDirection.kt
        │   │   ├── RefreshRateHysteresisConfig.kt
        │   │   └── RefreshRateStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── RefreshRateRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveRefreshRateStateUseCase.kt
        │       ├── GetRefreshRateStateUseCase.kt
        │       ├── StartRefreshRateTrackingUseCase.kt
        │       ├── StopRefreshRateTrackingUseCase.kt
        │       ├── ToggleAdaptiveRefreshRateUseCase.kt
        │       ├── SetPreferredRefreshRateModeUseCase.kt
        │       ├── RecordFrameMetricUseCase.kt
        │       ├── ResetRefreshRateStatsUseCase.kt
        │       └── GetDisplayRefreshModesUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Display.Mode <-> Domain)
        │   │   └── RefreshRateMapper.kt
        │   └── repository/                # Adapter implementing repository with hysteresis
        │       └── LegacyRefreshRateRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── RefreshRateUiState.kt
            ├── RefreshRateEvent.kt
            └── RefreshRateViewModel.kt
    │
    └── chatmeta/                         # Chat Messages Metadata (Reactions, Paid Media, Stories) Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models
        │   │   ├── MessageMetadataType.kt
        │   │   ├── MessageMetadataCheckItem.kt
        │   │   ├── ChatMetadataStatsModel.kt
        │   │   └── ChatMetadataBatchResult.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ChatMessagesMetadataRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveChatMetadataStatsUseCase.kt
        │       ├── GetChatMetadataStatsUseCase.kt
        │       ├── CheckMessagesMetadataUseCase.kt
        │       ├── LoadMessagesReactionsUseCase.kt
        │       ├── LoadMessagesExtendedMediaUseCase.kt
        │       └── CancelPendingMetadataRequestsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (MessageObject <-> Domain)
        │   │   └── ChatMetadataMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyChatMessagesMetadataRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ChatMetadataUiState.kt
            ├── ChatMetadataEvent.kt
            └── ChatMetadataViewModel.kt
    │
    └── pip/                               # Picture-in-Picture & Video Window Session Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── PipState.kt
        │   │   ├── PipSourceModel.kt
        │   │   └── PipSessionInfo.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PipRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePipSessionUseCase.kt
        │       ├── GetPipSessionUseCase.kt
        │       ├── RegisterPipSourceUseCase.kt
        │       ├── UnregisterPipSourceUseCase.kt
        │       ├── UpdatePipSourceStateUseCase.kt
        │       ├── DispatchPipStateUseCase.kt
        │       ├── TriggerPipActionUseCase.kt
        │       └── EvaluatePipEligibilityUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (PipSource <-> Domain)
        │   │   └── PipMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPipRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PipUiState.kt
            ├── PipEvent.kt
            └── PipViewModel.kt
    │
    └── drafts/                            # Story & Media Creation Drafts Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── DraftType.kt
        │   │   ├── StoryDraftModel.kt
        │   │   └── DraftsStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── DraftsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveDraftsStateUseCase.kt
        │       ├── GetDraftsStateUseCase.kt
        │       ├── LoadDraftsUseCase.kt
        │       ├── SaveDraftUseCase.kt
        │       ├── DeleteDraftUseCase.kt
        │       ├── DeleteForEditUseCase.kt
        │       ├── GetDraftForEditUseCase.kt
        │       └── CleanupExpiredDraftsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (StoryEntry <-> Domain)
        │   │   └── DraftsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyDraftsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── DraftsUiState.kt
            ├── DraftsEvent.kt
            └── DraftsViewModel.kt
    │
    └── fileref/                           # MTProto File Reference Renewal & Parent Cache Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── FileRefParentType.kt
        │   │   ├── FileRefRequestItem.kt
        │   │   ├── FileRefCacheEntry.kt
        │   │   └── FileRefStatsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── FileRefRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveFileRefStatsUseCase.kt
        │       ├── GetFileRefStatsUseCase.kt
        │       ├── RequestReferenceRenewalUseCase.kt
        │       ├── NotifyReferenceRenewedUseCase.kt
        │       ├── CancelFileRefRequestUseCase.kt
        │       └── ClearFileRefCacheUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy objects <-> Domain)
        │   │   └── FileRefMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyFileRefRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── FileRefUiState.kt
            ├── FileRefEvent.kt
            └── FileRefViewModel.kt
    │
    └── camera/                            # Hardware Camera & Video Recording Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── CameraFacing.kt
        │   │   ├── CameraResolutionModel.kt
        │   │   ├── CameraFlashMode.kt
        │   │   ├── CameraRecordingState.kt
        │   │   ├── CameraDeviceModel.kt
        │   │   └── CameraStateModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── CameraRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveCameraStateUseCase.kt
        │       ├── GetCameraStateUseCase.kt
        │       ├── InitCamerasUseCase.kt
        │       ├── SelectCameraUseCase.kt
        │       ├── SwitchCameraUseCase.kt
        │       ├── SetCameraFlashModeUseCase.kt
        │       ├── ToggleMirrorFrontCameraUseCase.kt
        │       ├── ChooseOptimalResolutionUseCase.kt
        │       └── NotifyCameraRecordingUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy CameraInfo/Size <-> Domain)
        │   │   └── CameraMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyCameraRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── CameraUiState.kt
            ├── CameraEvent.kt
            └── CameraViewModel.kt
    │
    └── cachebychats/                      # Keep-Media Cache Retention & Dialog Exceptions Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain models & enums
        │   │   ├── CacheChatType.kt
        │   │   ├── KeepMediaDuration.kt
        │   │   ├── KeepMediaExceptionModel.kt
        │   │   └── CacheByChatsConfigModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── CacheByChatsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveCacheByChatsConfigUseCase.kt
        │       ├── GetCacheByChatsConfigUseCase.kt
        │       ├── SetKeepMediaDurationUseCase.kt
        │       ├── SetKeepMediaExceptionUseCase.kt
        │       ├── RemoveKeepMediaExceptionUseCase.kt
        │       └── ClearKeepMediaExceptionsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy KeepMediaException <-> Domain)
        │   │   └── CacheByChatsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyCacheByChatsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── CacheByChatsUiState.kt
            ├── CacheByChatsEvent.kt
            └── CacheByChatsViewModel.kt
    │
    └── draftmeasure/                      # Chat Draft Message Height Measurement & Override Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (DraftMeasureTarget, Viewport, Result, Config)
        │   │   └── DraftMeasureModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── DraftMeasureRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── CalculateDraftMeasureOverrideUseCase.kt
        │       ├── SetDraftMeasureTargetUseCase.kt
        │       ├── OnDraftMessageIdChangedUseCase.kt
        │       ├── SetPreviousMessageHeightUseCase.kt
        │       ├── ResetDraftMeasureTargetUseCase.kt
        │       ├── ObserveDraftMeasureConfigUseCase.kt
        │       └── GetDraftMeasureConfigUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy height calculations <-> Domain)
        │   │   └── DraftMeasureMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyDraftMeasureRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── DraftMeasureUiState.kt
            ├── DraftMeasureEvent.kt
            └── DraftMeasureViewModel.kt
    │
    └── bottomviews/                       # Chat Bottom Views Visibility Arbitration Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (BottomContainerType, BottomViewsVisibilityState)
        │   │   ├── BottomContainerType.kt
        │   │   └── BottomViewsVisibilityState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BottomViewsVisibilityRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── GetBottomViewVisibilityUseCase.kt
        │       ├── SetBottomViewVisibleUseCase.kt
        │       ├── GetPriorityBottomContainerUseCase.kt
        │       ├── GetBottomViewsStateUseCase.kt
        │       └── ObserveBottomViewsVisibilityUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Bitwise priority calculation <-> Domain state)
        │   │   └── BottomViewsVisibilityMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBottomViewsVisibilityRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BottomViewsUiState.kt
            ├── BottomViewsEvent.kt
            └── BottomViewsViewModel.kt
    │
    └── floatingdebug/                     # Floating Debug Tools & Overlay Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (DebugItemKind, DebugItemModel, FloatingDebugState)
        │   │   └── FloatingDebugModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── FloatingDebugRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── IsFloatingDebugActiveUseCase.kt
        │       ├── SetFloatingDebugActiveUseCase.kt
        │       ├── ToggleFloatingDebugActiveUseCase.kt
        │       ├── GetFloatingDebugItemsUseCase.kt
        │       ├── RegisterFloatingDebugItemsUseCase.kt
        │       ├── ClearFloatingDebugItemsUseCase.kt
        │       ├── ObserveFloatingDebugStateUseCase.kt
        │       └── GetFloatingDebugStateUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy debug item type <-> Domain)
        │   │   └── FloatingDebugMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyFloatingDebugRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── FloatingDebugUiState.kt
            ├── FloatingDebugEvent.kt
            └── FloatingDebugViewModel.kt
    │
    └── keyboardinsets/                    # Window Insets & In-App Keyboard Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (KeyboardVisibilityState, InAppImeMode, KeyboardInsetsModel)
        │   │   └── KeyboardInsetsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── KeyboardInsetsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── RequestInAppKeyboardHeightUseCase.kt
        │       ├── ResetInAppKeyboardHeightUseCase.kt
        │       ├── RequestInAppKeyboardHeightWithNavbarUseCase.kt
        │       ├── UpdateSystemInsetsUseCase.kt
        │       ├── GetKeyboardInsetsUseCase.kt
        │       └── ObserveKeyboardInsetsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy insets mapping <-> Domain)
        │   │   └── KeyboardInsetsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyKeyboardInsetsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── KeyboardInsetsUiState.kt
            ├── KeyboardInsetsEvent.kt
            └── KeyboardInsetsViewModel.kt
    │
    └── maintabs/                          # Main Navigation Tabs Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (MainTabType, MainTabBadgeModel, MainTabsConfigModel)
        │   │   └── MainTabsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── MainTabsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveMainTabsConfigUseCase.kt
        │       ├── GetMainTabsConfigUseCase.kt
        │       ├── SetMainTabsVisibleUseCase.kt
        │       ├── SelectMainTabUseCase.kt
        │       ├── SetShowCallsTabUseCase.kt
        │       ├── UpdateChatsUnreadCountUseCase.kt
        │       └── SetContactsPermissionWarningUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Legacy tab positions <-> Domain)
        │   │   └── MainTabsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyMainTabsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── MainTabsUiState.kt
            ├── MainTabsEvent.kt
            └── MainTabsViewModel.kt
    │
    └── richcaption/                       # Instant View Rich Caption Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (CaptionSpanType, CaptionEntitySpan, RichCaptionModel, CaptionMeasureSpec)
        │   │   └── RichCaptionModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── RichCaptionRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveRichCaptionUseCase.kt
        │       ├── GetRichCaptionUseCase.kt
        │       ├── SetRichCaptionTextUseCase.kt
        │       ├── SetRichCaptionCreditUseCase.kt
        │       ├── SetRichCaptionLockedUseCase.kt
        │       ├── CalculateCaptionMeasureWidthUseCase.kt
        │       ├── CheckCaptionPressHitUseCase.kt
        │       └── ClearRichCaptionUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (Span sanitization & mapping <-> Domain)
        │   │   └── RichCaptionMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyRichCaptionRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── RichCaptionUiState.kt
            ├── RichCaptionEvent.kt
            └── RichCaptionViewModel.kt
    │
    └── adjustpan/                         # AdjustPan Layout Animation & Geometry Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (PanCalculationSpec, PanTransitionPlan, PanProgressResult, PanTransitionState)
        │   │   └── AdjustPanModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── AdjustPanRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── CalculatePanTransitionPlanUseCase.kt
        │       ├── ComputePanProgressUseCase.kt
        │       ├── ObserveAdjustPanStateUseCase.kt
        │       ├── GetAdjustPanStateUseCase.kt
        │       ├── SetAdjustPanEnabledUseCase.kt
        │       ├── StartAdjustPanTransitionUseCase.kt
        │       ├── UpdateAdjustPanTransitionUseCase.kt
        │       ├── StopAdjustPanTransitionUseCase.kt
        │       └── ResetAdjustPanUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure geometry and interpolation math
        │   │   └── AdjustPanMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyAdjustPanRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── AdjustPanUiState.kt
            ├── AdjustPanEvent.kt
            └── AdjustPanViewModel.kt
    │
    └── keyboardhide/                      # Interactive Pull-Down Keyboard Hide Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (KeyboardDragSpec, KeyboardDismissDecision, KeyboardHideProgressResult, KeyboardHideState)
        │   │   └── KeyboardHideModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── KeyboardHideRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── CalculateKeyboardHideProgressUseCase.kt
        │       ├── EvaluateKeyboardDismissDecisionUseCase.kt
        │       ├── ObserveKeyboardHideStateUseCase.kt
        │       ├── GetKeyboardHideStateUseCase.kt
        │       ├── SetKeyboardHideEnabledUseCase.kt
        │       ├── StartKeyboardHideMovingUseCase.kt
        │       ├── UpdateKeyboardHideMovingUseCase.kt
        │       ├── EndKeyboardHideMovingUseCase.kt
        │       ├── FinishKeyboardHideDismissUseCase.kt
        │       └── ResetKeyboardHideUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure gesture progress and threshold math
        │   │   └── KeyboardHideMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyKeyboardHideRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── KeyboardHideUiState.kt
            ├── KeyboardHideEvent.kt
            └── KeyboardHideViewModel.kt
    │
    └── businessrecipients/                # Business Recipients Targeting & Configuration Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (RecipientFilterType, BusinessRecipientsModel, RecipientValidationResult)
        │   │   └── BusinessRecipientsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BusinessRecipientsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveBusinessRecipientsUseCase.kt
        │       ├── GetBusinessRecipientsUseCase.kt
        │       ├── SetBusinessRecipientsUseCase.kt
        │       ├── ToggleExcludeSelectedUseCase.kt
        │       ├── ToggleRecipientFilterUseCase.kt
        │       ├── AddSelectedUsersUseCase.kt
        │       ├── RemoveSelectedUserUseCase.kt
        │       ├── AddExcludedUsersUseCase.kt
        │       ├── RemoveExcludedUserUseCase.kt
        │       ├── CheckRecipientsChangesUseCase.kt
        │       ├── ValidateBusinessRecipientsUseCase.kt
        │       └── ResetBusinessRecipientsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure bitmask flags and validation rules
        │   │   └── BusinessRecipientsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBusinessRecipientsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BusinessRecipientsUiState.kt
            ├── BusinessRecipientsEvent.kt
            └── BusinessRecipientsViewModel.kt
    │
    └── pinchtozoom/                       # Interactive Pinch-To-Zoom Media Gestures & Overlay Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (PinchTouchPoint, PinchGestureSpec, PinchGestureDecision, PinchTransform, PinchImageDimensions, PinchBoundsResult, PinchZoomState)
        │   │   └── PinchToZoomModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PinchToZoomRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePinchZoomStateUseCase.kt
        │       ├── GetPinchZoomStateUseCase.kt
        │       ├── CalculatePinchScaleUseCase.kt
        │       ├── CalculatePinchTranslationUseCase.kt
        │       ├── CalculatePinchTransformUseCase.kt
        │       ├── CalculatePinchImageBoundsUseCase.kt
        │       ├── EvaluatePinchGestureUseCase.kt
        │       ├── StartPinchZoomUseCase.kt
        │       ├── UpdatePinchZoomUseCase.kt
        │       ├── FinishPinchZoomUseCase.kt
        │       └── ResetPinchZoomUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure gesture progress, 2D transform & aspect ratio interpolation
        │   │   └── PinchToZoomMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPinchToZoomRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PinchToZoomUiState.kt
            ├── PinchToZoomEvent.kt
            └── PinchToZoomViewModel.kt
    │
    └── recyclerscroll/                    # Recycler List Animated Scroll & Transitions Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (ScrollDirection, ScrollAnimationSpec, ScrollAnimationPlan, ScrollViewTranslation, ScrollEligibility, RecyclerScrollState)
        │   │   └── RecyclerScrollModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── RecyclerScrollRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveRecyclerScrollStateUseCase.kt
        │       ├── GetRecyclerScrollStateUseCase.kt
        │       ├── EvaluateScrollEligibilityUseCase.kt
        │       ├── CalculateScrollAnimationPlanUseCase.kt
        │       ├── CalculateScrollLengthUseCase.kt
        │       ├── ComputeScrollViewTranslationsUseCase.kt
        │       ├── StartRecyclerScrollUseCase.kt
        │       ├── UpdateRecyclerScrollProgressUseCase.kt
        │       ├── FinishRecyclerScrollUseCase.kt
        │       ├── CancelRecyclerScrollUseCase.kt
        │       └── ResetRecyclerScrollUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure scroll physics, duration calculation, and translation formulas
        │   │   └── RecyclerScrollMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyRecyclerScrollRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── RecyclerScrollUiState.kt
            ├── RecyclerScrollEvent.kt
            └── RecyclerScrollViewModel.kt
    │
    └── emojieffects/                      # Interactive Emoji Animations, Tap Protocols & Overlay Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (EmojiInteractionAction, EmojiInteractionSession, EmojiAnimationQuotaResult, EmojiOverlayGeometry, EmojiEffectItem, EmojiEffectsState)
        │   │   └── EmojiEffectsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── EmojiEffectsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── NormalizeEmojiUseCase.kt
        │       ├── EvaluateEmojiSupportUseCase.kt
        │       ├── RecordEmojiTapUseCase.kt
        │       ├── EncodeEmojiInteractionsJsonUseCase.kt
        │       ├── DecodeEmojiInteractionsJsonUseCase.kt
        │       ├── CalculateEmojiBoundsUseCase.kt
        │       ├── CalculateEmojiOverlayPositionUseCase.kt
        │       ├── EvaluateAnimationQuotaUseCase.kt
        │       ├── ObserveEmojiEffectsStateUseCase.kt
        │       ├── GetEmojiEffectsStateUseCase.kt
        │       ├── StartEmojiEffectUseCase.kt
        │       ├── UpdateEmojiEffectProgressUseCase.kt
        │       ├── DismissEmojiEffectUseCase.kt
        │       └── ClearEmojiEffectsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure codecs (JSON payload <-> Domain, TLRPC interaction mapper)
        │   │   └── EmojiEffectsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyEmojiEffectsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── EmojiEffectsUiState.kt
            ├── EmojiEffectsEvent.kt
            └── EmojiEffectsViewModel.kt
    │
    └── mentions/                          # Mentions, Hashtags, Bot Commands & Autocomplete Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (MentionTriggerType, MentionQuery, MentionCandidate, MentionReplacement, MentionsState)
        │   │   └── MentionsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── MentionsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ValidateUsernameUseCase.kt
        │       ├── ParseMentionQueryUseCase.kt
        │       ├── FilterMentionsUseCase.kt
        │       ├── FormatMentionReplacementUseCase.kt
        │       ├── ObserveMentionsStateUseCase.kt
        │       ├── GetMentionsStateUseCase.kt
        │       ├── UpdateMentionQueryUseCase.kt
        │       ├── SetMentionCandidatesUseCase.kt
        │       ├── DismissMentionsUseCase.kt
        │       └── ClearMentionsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers (TLRPC.User / TL_botCommand / Hashtags <-> Domain)
        │   │   └── MentionsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyMentionsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── MentionsUiState.kt
            ├── MentionsEvent.kt
            └── MentionsViewModel.kt
    │
    └── sharedmedia/                       # Shared Media, Tabs, Filters, FastScroll & Selection Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── SharedMediaTabType.kt
        │   │   ├── SharedMediaFilterType.kt
        │   │   ├── SharedMediaItem.kt
        │   │   ├── SharedMediaPeriod.kt
        │   │   ├── SharedMediaTabSpec.kt
        │   │   ├── SharedMediaSelectionState.kt
        │   │   └── SharedMediaState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── SharedMediaRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ResolveAvailableTabsUseCase.kt
        │       ├── FilterSharedMediaUseCase.kt
        │       ├── GroupMediaByMonthUseCase.kt
        │       ├── CalculateMediaSelectionUseCase.kt
        │       ├── ObserveSharedMediaStateUseCase.kt
        │       ├── GetSharedMediaStateUseCase.kt
        │       ├── SelectSharedMediaTabUseCase.kt
        │       ├── SetSharedMediaFilterUseCase.kt
        │       ├── ToggleMediaSelectionUseCase.kt
        │       └── ClearMediaSelectionUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & date period calculators
        │   │   └── SharedMediaMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacySharedMediaRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── SharedMediaUiState.kt
            ├── SharedMediaEvent.kt
            └── SharedMediaViewModel.kt
    │
    └── contentpreview/                    # Content Preview & Long-Press Gestures Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── PreviewContentType.kt
        │   │   ├── PreviewActionType.kt
        │   │   ├── PreviewActionItem.kt
        │   │   ├── ContentPreviewGesture.kt
        │   │   ├── ContentPreviewItem.kt
        │   │   └── ContentPreviewState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ContentPreviewRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── EvaluatePreviewEligibilityUseCase.kt
        │       ├── CalculatePreviewDragUseCase.kt
        │       ├── ResolvePreviewActionsUseCase.kt
        │       ├── ObserveContentPreviewStateUseCase.kt
        │       ├── GetContentPreviewStateUseCase.kt
        │       ├── OpenContentPreviewUseCase.kt
        │       ├── UpdatePreviewDragUseCase.kt
        │       ├── DismissContentPreviewUseCase.kt
        │       ├── ClearContentPreviewUseCase.kt
        │       └── TriggerPreviewActionUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & drag thresholds
        │   │   └── ContentPreviewMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyContentPreviewRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ContentPreviewUiState.kt
            ├── ContentPreviewEvent.kt
            └── ContentPreviewViewModel.kt
    │
    └── emojipicker/                       # Emoji, Sticker & GIF Picker Keyboard Panel Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── EmojiPickerTabType.kt
        │   │   ├── EmojiCategoryType.kt
        │   │   ├── EmojiItem.kt
        │   │   ├── StickerItem.kt
        │   │   ├── GifItem.kt
        │   │   ├── StickerPackItem.kt
        │   │   ├── EmojiPickerFilter.kt
        │   │   └── EmojiPickerState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── EmojiPickerRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ResolveAvailablePickerTabsUseCase.kt
        │       ├── FilterEmojiItemsUseCase.kt
        │       ├── FilterStickersUseCase.kt
        │       ├── FilterGifsUseCase.kt
        │       ├── ObserveEmojiPickerStateUseCase.kt
        │       ├── GetEmojiPickerStateUseCase.kt
        │       ├── SelectPickerTabUseCase.kt
        │       ├── UpdatePickerSearchQueryUseCase.kt
        │       ├── ToggleStickerFavoriteUseCase.kt
        │       └── ClearRecentPickerItemsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & tab conversions
        │   │   └── EmojiPickerMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyEmojiPickerRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── EmojiPickerUiState.kt
            ├── EmojiPickerEvent.kt
            └── EmojiPickerViewModel.kt
    │
    └── chatattach/                        # Chat Attachment Dialog & Layouts Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── ChatAttachLayoutType.kt
        │   │   ├── ChatAttachItem.kt
        │   │   ├── ChatAttachSendOptions.kt
        │   │   ├── ChatAttachPermissions.kt
        │   │   ├── CaptionLimitInfo.kt
        │   │   └── ChatAttachState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ChatAttachRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ResolveAvailableAttachLayoutsUseCase.kt
        │       ├── CalculateAttachCaptionLimitUseCase.kt
        │       ├── ToggleAttachItemSelectionUseCase.kt
        │       ├── ValidateSendOptionsUseCase.kt
        │       ├── ObserveChatAttachStateUseCase.kt
        │       ├── GetChatAttachStateUseCase.kt
        │       ├── SelectAttachLayoutUseCase.kt
        │       ├── UpdateAttachSendOptionsUseCase.kt
        │       ├── ClearAttachSelectionUseCase.kt
        │       └── OpenChatAttachAlertUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & layout conversions
        │   │   └── ChatAttachMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyChatAttachRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ChatAttachUiState.kt
            ├── ChatAttachEvent.kt
            └── ChatAttachViewModel.kt
    │
    └── photoviewer/                       # Fullscreen Media Viewer, Gestures, Playback & Editor Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── ViewerMediaType.kt
        │   │   ├── ViewerSelectType.kt
        │   │   ├── ViewerEditMode.kt
        │   │   ├── ViewerActionType.kt
        │   │   ├── PhotoViewerMediaItem.kt
        │   │   ├── PhotoViewerPlaybackState.kt
        │   │   ├── PhotoViewerTransform.kt
        │   │   └── PhotoViewerState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PhotoViewerRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── CalculateMediaPagingUseCase.kt
        │       ├── CalculateZoomTransformUseCase.kt
        │       ├── ValidateViewerActionsUseCase.kt
        │       ├── ResolveMediaQualityUseCase.kt
        │       ├── ObservePhotoViewerStateUseCase.kt
        │       ├── GetPhotoViewerStateUseCase.kt
        │       ├── OpenPhotoViewerUseCase.kt
        │       ├── NavigatePhotoViewerUseCase.kt
        │       ├── UpdatePlaybackStateUseCase.kt
        │       └── ClosePhotoViewerUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & timecode formatters
        │   │   └── PhotoViewerMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPhotoViewerRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PhotoViewerUiState.kt
            ├── PhotoViewerEvent.kt
            └── PhotoViewerViewModel.kt
    │
    └── chatinput/                         # Chat Message Input, Formatting, Voice Recording, Panels & Reply Bar Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── RecordType.kt
        │   │   ├── RecordStatus.kt
        │   │   ├── EnterViewPanelMode.kt
        │   │   ├── TextFormatStyle.kt
        │   │   ├── ChatInputReplyQuote.kt
        │   │   ├── ChatInputSendOptions.kt
        │   │   ├── ChatInputRecordState.kt
        │   │   └── ChatInputState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ChatInputRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── CalculateSendButtonStateUseCase.kt
        │       ├── FormatTextSelectionUseCase.kt
        │       ├── ValidateVoiceRecordActionUseCase.kt
        │       ├── ResolvePanelVisibilityUseCase.kt
        │       ├── ObserveChatInputStateUseCase.kt
        │       ├── GetChatInputStateUseCase.kt
        │       ├── SetChatInputTextUseCase.kt
        │       ├── SetChatInputPanelModeUseCase.kt
        │       ├── SetChatInputReplyUseCase.kt
        │       └── ClearChatInputReplyUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & record duration formatters
        │   │   └── ChatInputMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyChatInputRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ChatInputUiState.kt
            ├── ChatInputEvent.kt
            └── ChatInputViewModel.kt
    │
    └── audioplayer/                       # Audio & Voice/Video Playback, Playlist Queue, Speed & Proximity Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── AudioTrackType.kt
        │   │   ├── AudioPlaybackStatus.kt
        │   │   ├── RepeatMode.kt
        │   │   ├── AudioOutputRoute.kt
        │   │   ├── AudioTrackModel.kt
        │   │   ├── EqualizerBand.kt
        │   │   ├── EqualizerState.kt
        │   │   └── AudioPlaybackState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── AudioPlayerRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePlaybackStateUseCase.kt
        │       ├── GetPlaybackStateUseCase.kt
        │       ├── PlayTrackUseCase.kt
        │       ├── TogglePlayPauseUseCase.kt
        │       ├── SeekAudioUseCase.kt
        │       ├── NavigatePlaylistUseCase.kt
        │       ├── CyclePlaybackSpeedUseCase.kt
        │       ├── CycleRepeatModeUseCase.kt
        │       ├── ToggleShuffleUseCase.kt
        │       ├── HandleProximitySensorUseCase.kt
        │       └── ConfigureEqualizerUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers, timecode formatters & playlist index calculators
        │   │   └── AudioPlayerMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyAudioPlayerRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── AudioPlayerUiState.kt
            ├── AudioPlayerEvent.kt
            └── AudioPlayerViewModel.kt
    │
    └── sendmessages/                      # Message Sending Pipeline, Media Uploads, Albums & Forwards Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── SendMediaType.kt
        │   │   ├── SendStatus.kt
        │   │   ├── ForwardMode.kt
        │   │   ├── SendOptionsModel.kt
        │   │   ├── SendMediaItem.kt
        │   │   ├── SendAlbumModel.kt
        │   │   ├── ForwardRequestModel.kt
        │   │   ├── PendingSendModel.kt
        │   │   └── SendMessagesState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── SendMessagesRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── SendTextMessageUseCase.kt
        │       ├── SendMediaMessageUseCase.kt
        │       ├── SendMediaAlbumUseCase.kt
        │       ├── ForwardMessagesUseCase.kt
        │       ├── RetrySendMessageUseCase.kt
        │       ├── CancelSendMessageUseCase.kt
        │       ├── ObservePendingSendsUseCase.kt
        │       └── ValidateSendEligibilityUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers & size/progress calculators
        │   │   └── SendMessagesMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacySendMessagesRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── SendMessagesUiState.kt
            ├── SendMessagesEvent.kt
            └── SendMessagesViewModel.kt
    │
    └── imageloader/                      # Memory Cache Tiers, Downscaling, Filter Specs & Request Pipeline Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── ImageCacheTier.kt
        │   │   ├── ImageLoadingStatus.kt
        │   │   ├── FrameExtractType.kt
        │   │   ├── ImageFilterSpec.kt
        │   │   ├── ImageDownscaleSpec.kt
        │   │   ├── ImageRequestModel.kt
        │   │   ├── ImageCacheStatsModel.kt
        │   │   └── ImageLoaderState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── ImageLoaderRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ParseImageFilterUseCase.kt
        │       ├── FormatImageFilterUseCase.kt
        │       ├── BuildImageCacheKeyUseCase.kt
        │       ├── CalculateImageDownscaleUseCase.kt
        │       ├── EvaluateImageCacheEligibilityUseCase.kt
        │       ├── ObserveImageLoaderStateUseCase.kt
        │       ├── GetImageLoaderStateUseCase.kt
        │       ├── EnqueueImageRequestUseCase.kt
        │       ├── CancelImageRequestUseCase.kt
        │       ├── TrimImageMemoryUseCase.kt
        │       └── ClearImageCacheUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers, filter string parser & downscale math
        │   │   └── ImageLoaderMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyImageLoaderRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── ImageLoaderUiState.kt
            ├── ImageLoaderEvent.kt
            └── ImageLoaderViewModel.kt
    │
    └── downloadmanager/                  # Downloads Queue, Auto-Download Rules & Network Presets Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── AutoDownloadMediaType.kt
        │   │   ├── AutoDownloadNetwork.kt
        │   │   ├── PeerTypePreset.kt
        │   │   ├── DownloadItemStatus.kt
        │   │   ├── DownloadPresetModel.kt
        │   │   ├── DownloadItemModel.kt
        │   │   ├── DownloadManagerStats.kt
        │   │   └── DownloadManagerState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── DownloadManagerRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── EvaluateAutoDownloadEligibilityUseCase.kt
        │       ├── ObserveDownloadManagerStateUseCase.kt
        │       ├── GetDownloadManagerStateUseCase.kt
        │       ├── EnqueueDownloadUseCase.kt
        │       ├── PauseDownloadUseCase.kt
        │       ├── ResumeDownloadUseCase.kt
        │       ├── CancelDownloadUseCase.kt
        │       ├── RetryDownloadUseCase.kt
        │       ├── ClearRecentDownloadsUseCase.kt
        │       ├── MarkDownloadsAsViewedUseCase.kt
        │       ├── UpdateDownloadProgressUseCase.kt
        │       ├── CalculateDownloadSpeedUseCase.kt
        │       ├── SetDownloadNetworkTypeUseCase.kt
        │       └── UpdateDownloadPresetUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure mappers, bitmask encoder & speed calculators
        │   │   └── DownloadManagerMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyDownloadManagerRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── DownloadManagerUiState.kt
            ├── DownloadManagerEvent.kt
            └── DownloadManagerViewModel.kt
    │
    └── localization/                     # Language Packs, Pluralization, Relative Timestamps & RTL Detection Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── PluralQuantity.kt
        │   │   ├── NameDisplayOrder.kt
        │   │   ├── LocaleModel.kt
        │   │   ├── RelativeTimeModel.kt
        │   │   ├── LocalizationConfigModel.kt
        │   │   └── LocalizationState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── LocalizationRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ResolvePluralQuantityUseCase.kt
        │       ├── FormatRelativeTimestampUseCase.kt
        │       ├── FormatFullNameUseCase.kt
        │       ├── FormatNumberWithSuffixUseCase.kt
        │       ├── DetectRtlLanguageUseCase.kt
        │       ├── ObserveLocalizationStateUseCase.kt
        │       ├── GetLocalizationStateUseCase.kt
        │       ├── ApplyLocaleUseCase.kt
        │       ├── Toggle24HourFormatUseCase.kt
        │       └── SetNameDisplayOrderUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure string & plural formatters with Locale.US
        │   │   └── LocalizationMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyLocalizationRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── LocalizationUiState.kt
            ├── LocalizationEvent.kt
            └── LocalizationViewModel.kt
    │
    └── ringtones/                        # Custom Notification Sounds, Ringtones & Cloud Uploader Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── RingtoneErrorCode.kt
        │   │   ├── RingtoneUploadStatus.kt
        │   │   ├── RingtoneModel.kt
        │   │   ├── RingtoneValidationResult.kt
        │   │   ├── RingtoneLimitsModel.kt
        │   │   └── RingtoneState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── RingtoneRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ValidateRingtoneEligibilityUseCase.kt
        │       ├── ObserveRingtonesUseCase.kt
        │       ├── ObserveRingtoneStateUseCase.kt
        │       ├── GetRingtonesUseCase.kt
        │       ├── GetRingtoneByIdUseCase.kt
        │       ├── GetRingtoneSoundPathUseCase.kt
        │       ├── AddRingtoneUseCase.kt
        │       ├── RemoveRingtoneUseCase.kt
        │       ├── SaveRingtoneFromDocumentUseCase.kt
        │       ├── UploadRingtoneUseCase.kt
        │       ├── CancelRingtoneUploadUseCase.kt
        │       ├── RefreshRingtonesUseCase.kt
        │       └── SelectRingtoneUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure duration & file size formatters with Locale.US, MIME resolver
        │   │   └── RingtoneMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyRingtoneRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── RingtoneUiState.kt
            ├── RingtoneEvent.kt
            └── RingtoneViewModel.kt
    │
    └── networkstats/                     # Network Traffic & Data Usage Statistics Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── NetworkType.kt
        │   │   ├── TrafficCategory.kt
        │   │   ├── TrafficItemModel.kt
        │   │   ├── NetworkStatsSummaryModel.kt
        │   │   └── NetworkStatsState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── NetworkStatsRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObserveNetworkStatsUseCase.kt
        │       ├── ObserveAllNetworkStatsUseCase.kt
        │       ├── GetNetworkStatsUseCase.kt
        │       ├── GetAllNetworkStatsUseCase.kt
        │       ├── IncrementTrafficBytesUseCase.kt
        │       ├── IncrementTrafficItemsUseCase.kt
        │       ├── IncrementCallsTimeUseCase.kt
        │       ├── ResetNetworkStatsUseCase.kt
        │       ├── RefreshNetworkStatsUseCase.kt
        │       ├── CalculateMessagesTrafficUseCase.kt
        │       ├── FormatTrafficBytesUseCase.kt
        │       └── FormatCallsDurationUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure category mapping & legacy type codecs
        │   │   └── NetworkStatsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyNetworkStatsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── NetworkStatsUiState.kt
            ├── NetworkStatsEvent.kt
            └── NetworkStatsViewModel.kt
    │
    └── pushlistener/                     # Inbound Push Notifications, Payloads & Remote Actions Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin domain entities & enums
        │   │   ├── PushType.kt
        │   │   ├── PushActionType.kt
        │   │   ├── PushDecryptStatus.kt
        │   │   ├── PushPayloadModel.kt
        │   │   ├── PushProcessResult.kt
        │   │   └── PushListenerState.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── PushListenerRepository.kt
        │   └── usecase/                   # Isolated business operations
        │       ├── ObservePushListenerStateUseCase.kt
        │       ├── ObserveIncomingPushesUseCase.kt
        │       ├── GetPushListenerStateUseCase.kt
        │       ├── ProcessIncomingPushUseCase.kt
        │       ├── RegisterPushTokenUseCase.kt
        │       ├── TogglePushListeningUseCase.kt
        │       ├── DeterminePushActionTypeUseCase.kt
        │       └── ParsePushJsonPayloadUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (Legacy PUSH_TYPE_* <-> Domain)
        │   │   └── PushListenerMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyPushListenerRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── PushListenerUiState.kt
            ├── PushListenerEvent.kt
            └── PushListenerViewModel.kt
    │
    └── browser/                          # In-App Web Browser, Custom Tabs & Deep Link Routing Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (BrowserType, UrlTargetType, UrlSafetyCheckResult, BrowserSettingsModel, BrowserHistoryEntryModel, BrowserState)
        │   │   └── BrowserModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BrowserRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── ClassifyUrlTargetUseCase.kt
        │       ├── ExtractUsernameFromUrlUseCase.kt
        │       ├── CheckUrlSafetyUseCase.kt
        │       ├── ObserveBrowserStateUseCase.kt
        │       ├── GetBrowserStateUseCase.kt
        │       ├── UpdateBrowserSettingsUseCase.kt
        │       ├── OpenBrowserUrlUseCase.kt
        │       └── ManageBrowserHistoryUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (BrowserHistory.Entry <-> Domain)
        │   │   └── BrowserMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBrowserRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BrowserUiState.kt
            ├── BrowserEvent.kt
            └── BrowserViewModel.kt
    │
    └── litemode/                         # Power Saving, Battery Optimization & Animation Throttling Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (LiteModeFlag, LiteModePreset, LiteModeState)
        │   │   └── LiteModeModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── LiteModeRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── CalculateEffectiveFlagsUseCase.kt
        │       ├── CheckLiteModeFlagUseCase.kt
        │       ├── ResolvePresetUseCase.kt
        │       ├── ObserveLiteModeStateUseCase.kt
        │       ├── GetLiteModeStateUseCase.kt
        │       ├── ToggleLiteModeFlagUseCase.kt
        │       ├── SetLiteModePresetUseCase.kt
        │       └── UpdatePowerSaverThresholdUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (Legacy LiteMode.FLAG_* <-> Domain)
        │   │   └── LiteModeMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyLiteModeRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── LiteModeUiState.kt
            ├── LiteModeEvent.kt
            └── LiteModeViewModel.kt
    │
    └── appconfig/                        # Global Server Limits, Stars/TON Configuration & Feature Flags
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (StarsConfigModel, TonConfigModel, PollsConfigModel, RichMessageConfigModel, AiComposeConfigModel, AppLimitsConfigModel, AppGlobalConfigState)
        │   │   └── AppConfigModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── AppConfigRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── GetAppConfigUseCase.kt
        │       ├── ObserveAppConfigUseCase.kt
        │       ├── GetMessageLimitsUseCase.kt
        │       ├── GetStarsPricingConfigUseCase.kt
        │       ├── GetTonPricingConfigUseCase.kt
        │       ├── GetRichMessageLimitsUseCase.kt
        │       ├── GetPollsConfigUseCase.kt
        │       ├── GetAiComposeConfigUseCase.kt
        │       ├── GetAppLimitsUseCase.kt
        │       ├── ReloadAppConfigUseCase.kt
        │       └── UpdateAppConfigValueUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (AppGlobalConfig & MessagesController <-> Domain)
        │   │   └── AppConfigMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyAppConfigRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
    │
    └── autodeletemedia/                  # Background Media Cleanup & Cache Eviction Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (CacheLimitConfig, MediaScanFileModel, AutoDeleteRunResult, AutoDeleteTaskState)
        │   │   └── AutoDeleteMediaModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── AutoDeleteMediaRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── CheckShouldRunCleanupUseCase.kt
        │       ├── CalculateEvictionCandidatesUseCase.kt
        │       ├── LockFileUseCase.kt
        │       ├── UnlockFileUseCase.kt
        │       ├── IsFileLockedUseCase.kt
        │       ├── RunAutoDeleteCleanupUseCase.kt
        │       ├── ObserveAutoDeleteStateUseCase.kt
        │       └── GetAutoDeleteStateUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (CacheLimitConfig & MediaScanFileModel <-> Domain)
        │   │   └── AutoDeleteMediaMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyAutoDeleteMediaRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── AutoDeleteMediaUiState.kt
            ├── AutoDeleteMediaEvent.kt
            └── AutoDeleteMediaViewModel.kt
    │
    └── authtokens/                       # Fast Re-Login & Session Logout Tokens Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (AuthTokenUserInfoModel, SavedLoginTokenModel, SavedLogoutTokenModel, AuthTokensState)
        │   │   └── AuthTokensModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── AuthTokensRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── PruneTokensListUseCase.kt
        │       ├── ValidateAuthTokenFormatUseCase.kt
        │       ├── ObserveAuthTokensStateUseCase.kt
        │       ├── GetAuthTokensStateUseCase.kt
        │       ├── GetSavedLoginTokensUseCase.kt
        │       ├── SaveLoginTokenUseCase.kt
        │       ├── GetSavedLogoutTokensUseCase.kt
        │       ├── SaveLogoutTokensUseCase.kt
        │       ├── AddLogoutTokenUseCase.kt
        │       ├── RemoveTokenUseCase.kt
        │       ├── ClearAllTokensUseCase.kt
        │       └── RefreshAuthTokensUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (TL_auth_authorization & TL_auth_loggedOut <-> Domain)
        │   │   └── AuthTokensMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyAuthTokensRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── AuthTokensUiState.kt
            ├── AuthTokensEvent.kt
            └── AuthTokensViewModel.kt
    │
    └── messagecustomparams/              # Local Message Custom Parameters & Transcription/Summary State Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (VoiceTranscriptionParamsModel, MessageSummaryParamsModel, MessageTranslationParamsModel, StarsErrorParamsModel, MessageCustomParamsModel, MessageCustomParamsState)
        │   │   └── MessageCustomParamsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── MessageCustomParamsRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── CheckMessageCustomParamsEmptyUseCase.kt
        │       ├── MergeMessageCustomParamsUseCase.kt
        │       ├── ObserveMessageCustomParamsStateUseCase.kt
        │       ├── GetMessageCustomParamsStateUseCase.kt
        │       ├── GetMessageCustomParamsUseCase.kt
        │       ├── SetMessageCustomParamsUseCase.kt
        │       ├── UpdateVoiceTranscriptionUseCase.kt
        │       ├── UpdateMessageTranslationUseCase.kt
        │       ├── UpdateMessageSummaryUseCase.kt
        │       ├── CopyMessageCustomParamsUseCase.kt
        │       ├── RemoveMessageCustomParamsUseCase.kt
        │       └── ClearAllMessageCustomParamsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (TLRPC.Message <-> Domain)
        │   │   └── MessageCustomParamsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyMessageCustomParamsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── MessageCustomParamsUiState.kt
            ├── MessageCustomParamsEvent.kt
            └── MessageCustomParamsViewModel.kt
    │
    └── botforum/                         # Bot Forum Topics & AI Streaming Drafts Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (StreamingSendButtonState, BotDraftMessageModel, BotForumTopicModel, BotForumState, Notifications)
        │   │   └── BotForumModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BotForumRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── DeriveTopicNameFromMessageUseCase.kt
        │       ├── ResolveStreamingButtonStateUseCase.kt
        │       ├── ObserveBotForumStateUseCase.kt
        │       ├── GetBotForumStateUseCase.kt
        │       ├── GetStreamingSendButtonStateUseCase.kt
        │       ├── CheckIsStreamingTopicUseCase.kt
        │       ├── SaveIsStreamingTopicUseCase.kt
        │       ├── CheckHasBotForumDraftsUseCase.kt
        │       ├── StopStreamingDraftUseCase.kt
        │       ├── UpdateBotForumDraftUseCase.kt
        │       ├── RemoveMarkedRemovedDraftsUseCase.kt
        │       ├── CheckNewMessageDraftReplacementUseCase.kt
        │       └── CheckIsBotForumUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (Legacy BotForumHelper <-> Domain)
        │   │   └── BotForumMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBotForumRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BotForumUiState.kt
            ├── BotForumEvent.kt
            └── BotForumViewModel.kt
    │
    └── storycustomparams/                # Story Custom Parameters & Local Story Translation State Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (StoryTranslationParamsModel, StoryCustomParamsModel, StoryCustomParamsState)
        │   │   └── StoryCustomParamsModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── StoryCustomParamsRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── CheckStoryCustomParamsEmptyUseCase.kt
        │       ├── ComputeStoryCustomParamsFlagsUseCase.kt
        │       ├── ObserveStoryCustomParamsStateUseCase.kt
        │       ├── GetStoryCustomParamsStateUseCase.kt
        │       ├── GetStoryCustomParamsUseCase.kt
        │       ├── SaveStoryCustomParamsUseCase.kt
        │       ├── UpdateStoryTranslationUseCase.kt
        │       ├── CopyStoryCustomParamsUseCase.kt
        │       ├── RemoveStoryCustomParamsUseCase.kt
        │       └── ClearAllStoryCustomParamsUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (TL_stories.StoryItem <-> Domain)
        │   │   └── StoryCustomParamsMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyStoryCustomParamsRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── StoryCustomParamsUiState.kt
            ├── StoryCustomParamsEvent.kt
            └── StoryCustomParamsViewModel.kt
    │
    └── botguard/                         # Bot Guard WebApp Verification, Sessions & Decisions Feature
        ├── domain/
        │   ├── model/                     # Pure Kotlin models (BotGuardDecisionStatus, BotGuardDecisionResult, BotGuardSession, BotGuardState, etc.)
        │   │   └── BotGuardModel.kt
        │   ├── repository/                # Abstract repository contracts
        │   │   └── BotGuardRepository.kt
        │   └── usecase/                   # Business logic use cases
        │       ├── IsGuardBotConfirmationNeededUseCase.kt
        │       ├── DetermineGuardBotLaunchFlowUseCase.kt
        │       ├── RegisterGuardBotSessionUseCase.kt
        │       ├── GetGuardBotSessionUseCase.kt
        │       ├── GetAllActiveGuardBotSessionsUseCase.kt
        │       ├── CloseGuardBotSessionUseCase.kt
        │       ├── SetGuardBotConfirmationShownUseCase.kt
        │       ├── ClearAllGuardBotSessionsUseCase.kt
        │       ├── ObserveGuardBotDecisionsUseCase.kt
        │       ├── ObserveGuardBotStateUseCase.kt
        │       ├── MapJoinChatBotResultUseCase.kt
        │       └── FormatGuardBotBulletinUseCase.kt
        │
        ├── data/
        │   ├── mapper/                    # Pure type mappers (TLRPC.JoinChatBotResult <-> Domain)
        │   │   └── BotGuardMapper.kt
        │   └── repository/                # Adapter implementing repository
        │       └── LegacyBotGuardRepository.kt
        │
        └── presentation/                  # UI State & ViewModel
            ├── BotGuardUiState.kt
            ├── BotGuardEvent.kt
            └── BotGuardViewModel.kt
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
  - [x] Data layer: `BusinessRecipientsMapper`, `LegacyBusinessRecipientsRepository` (thread safe, adapting `BusinessRecipientsHelper`, bitmask flags, mutual exclusion, validation, change detection)
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

---

## 6. Architecture Decision Records (ADRs)

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
