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

---

## 6. Architecture Decision Records (ADRs)

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
