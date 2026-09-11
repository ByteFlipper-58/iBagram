package org.telegram.messenger.feature.messaging.di

import java.util.concurrent.ConcurrentHashMap
import org.telegram.messenger.feature.messaging.aitones.data.repository.LegacyAiTonesRepository
import org.telegram.messenger.feature.messaging.aitones.domain.repository.AiTonesRepository
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.AddAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.EditAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.GetAiTonesStateUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.LoadAiTonesUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.ObserveAiTonesUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.RemoveAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.UnsaveAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.presentation.AiTonesViewModel
import org.telegram.messenger.feature.messaging.autodelete.data.repository.LegacyAutoDeleteRepository
import org.telegram.messenger.feature.messaging.autodelete.domain.repository.AutoDeleteRepository
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.GetChatAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.GetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.ObserveGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.SetChatAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.SetChatsAutoDeleteBatchUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.SetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.presentation.AutoDeleteViewModel
import org.telegram.messenger.feature.messaging.botforum.data.repository.LegacyBotForumRepository
import org.telegram.messenger.feature.messaging.botforum.domain.repository.BotForumRepository
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckHasBotForumDraftsUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckIsBotForumUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckIsStreamingTopicUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckNewMessageDraftReplacementUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.DeriveTopicNameFromMessageUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.GetBotForumStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.GetStreamingSendButtonStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.ObserveBotForumStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.RemoveMarkedRemovedDraftsUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.ResolveStreamingButtonStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.SaveIsStreamingTopicUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.StopStreamingDraftUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.UpdateBotForumDraftUseCase
import org.telegram.messenger.feature.messaging.botforum.presentation.BotForumViewModel
import org.telegram.messenger.feature.messaging.botkeyboard.data.repository.LegacyBotKeyboardRepository
import org.telegram.messenger.feature.messaging.botkeyboard.domain.repository.BotKeyboardRepository
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.BuildBotKeyboardLayoutUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.CheckIsButtonWebViewUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.CheckIsForceReplyUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ClearAllKeyboardsUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.GetBotKeyboardStateUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.GetKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ObserveBotKeyboardStateUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.RecordButtonPressedUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.RemoveKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ResolveCustomButtonTypeUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.SetKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.presentation.BotKeyboardViewModel
import org.telegram.messenger.feature.messaging.bottomviews.data.repository.LegacyBottomViewsVisibilityRepository
import org.telegram.messenger.feature.messaging.bottomviews.domain.repository.BottomViewsVisibilityRepository
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetBottomViewVisibilityUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetBottomViewsStateUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetPriorityBottomContainerUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.ObserveBottomViewsVisibilityUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.SetBottomViewVisibleUseCase
import org.telegram.messenger.feature.messaging.bottomviews.presentation.BottomViewsViewModel
import org.telegram.messenger.feature.messaging.chat.data.repository.LegacyChatRepository
import org.telegram.messenger.feature.messaging.chat.domain.repository.ChatRepository
import org.telegram.messenger.feature.messaging.chat.domain.usecase.DeleteMessagesUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.GetMessagesUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.LoadHistoryUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.ObserveMessagesUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.SendMessageUseCase
import org.telegram.messenger.feature.messaging.chat.presentation.ChatViewModel
import org.telegram.messenger.feature.messaging.chatattach.data.repository.LegacyChatAttachRepository
import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.CalculateAttachCaptionLimitUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ClearAttachSelectionUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.GetChatAttachStateUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ObserveChatAttachStateUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.OpenChatAttachAlertUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ResolveAvailableAttachLayoutsUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.SelectAttachLayoutUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ToggleAttachItemSelectionUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.UpdateAttachSendOptionsUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ValidateSendOptionsUseCase
import org.telegram.messenger.feature.messaging.chatattach.presentation.ChatAttachViewModel
import org.telegram.messenger.feature.messaging.chatinput.data.repository.LegacyChatInputRepository
import org.telegram.messenger.feature.messaging.chatinput.domain.repository.ChatInputRepository
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.CalculateSendButtonStateUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ClearChatInputReplyUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.FormatTextSelectionUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.GetChatInputStateUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ObserveChatInputStateUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ResolvePanelVisibilityUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.SetChatInputPanelModeUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.SetChatInputReplyUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.SetChatInputTextUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ValidateVoiceRecordActionUseCase
import org.telegram.messenger.feature.messaging.chatinput.presentation.ChatInputViewModel
import org.telegram.messenger.feature.messaging.chatmeta.data.repository.LegacyChatMessagesMetadataRepository
import org.telegram.messenger.feature.messaging.chatmeta.domain.repository.ChatMessagesMetadataRepository
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CancelPendingMetadataRequestsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CheckMessagesMetadataUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.GetChatMetadataStatsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesExtendedMediaUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesReactionsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.ObserveChatMetadataStatsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.presentation.ChatMetadataViewModel
import org.telegram.messenger.feature.messaging.chattheme.data.repository.LegacyChatThemeRepository
import org.telegram.messenger.feature.messaging.chattheme.domain.repository.ChatThemeRepository
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.GetAvailableChatThemesUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.GetDialogThemeStateUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.ObserveDialogThemeUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.ResetDialogThemeUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.SaveChatWallpaperUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.SetDialogThemeUseCase
import org.telegram.messenger.feature.messaging.chattheme.presentation.ChatThemeViewModel
import org.telegram.messenger.feature.messaging.dialogs.data.repository.LegacyDialogsRepository
import org.telegram.messenger.feature.messaging.dialogs.domain.repository.DialogsRepository
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.DeleteDialogUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.GetDialogsUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.LoadMoreDialogsUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.MarkDialogAsReadUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.PinDialogUseCase
import org.telegram.messenger.feature.messaging.dialogs.presentation.DialogsViewModel
import org.telegram.messenger.feature.messaging.draftmeasure.data.repository.LegacyDraftMeasureRepository
import org.telegram.messenger.feature.messaging.draftmeasure.domain.repository.DraftMeasureRepository
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.CalculateDraftMeasureOverrideUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.GetDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ObserveDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.OnDraftMessageIdChangedUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ResetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetPreviousMessageHeightUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.presentation.DraftMeasureViewModel
import org.telegram.messenger.feature.messaging.drafts.data.repository.LegacyDraftsRepository
import org.telegram.messenger.feature.messaging.drafts.domain.repository.DraftsRepository
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.CleanupExpiredDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteDraftUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.LoadDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.ObserveDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.SaveDraftUseCase
import org.telegram.messenger.feature.messaging.drafts.presentation.DraftsViewModel
import org.telegram.messenger.feature.messaging.emojieffects.data.repository.LegacyEmojiEffectsRepository
import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.CalculateEmojiBoundsUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.CalculateEmojiOverlayPositionUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.ClearEmojiEffectsUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.DecodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.DismissEmojiEffectUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.EncodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.EvaluateAnimationQuotaUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.EvaluateEmojiSupportUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.GetEmojiEffectsStateUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.NormalizeEmojiUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.ObserveEmojiEffectsStateUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.RecordEmojiTapUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.StartEmojiEffectUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.UpdateEmojiEffectProgressUseCase
import org.telegram.messenger.feature.messaging.emojieffects.presentation.EmojiEffectsViewModel
import org.telegram.messenger.feature.messaging.emojipicker.data.repository.LegacyEmojiPickerRepository
import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ClearRecentPickerItemsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterEmojiItemsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterGifsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterStickersUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.GetEmojiPickerStateUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ObserveEmojiPickerStateUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ResolveAvailablePickerTabsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.SelectPickerTabUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ToggleStickerFavoriteUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.UpdatePickerSearchQueryUseCase
import org.telegram.messenger.feature.messaging.emojipicker.presentation.EmojiPickerViewModel
import org.telegram.messenger.feature.messaging.ephemeralmessages.data.repository.LegacyEphemeralMessagesRepository
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.repository.EphemeralMessagesRepository
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.ClearAllWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.GetEphemeralCommandBotIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.GetEphemeralMessagesStateUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.GetWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.IsEphemeralCommandUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.IsEphemeralMessageIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.ObserveEphemeralMessagesStateUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.PackEphemeralMessageIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.ParseBotCommandUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.PutWelcomeAnchorBindingUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.RemoveWelcomeAnchorBindingUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.UnpackEphemeralMessageIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.presentation.EphemeralMessagesViewModel
import org.telegram.messenger.feature.messaging.factcheck.data.repository.LegacyFactCheckRepository
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.ApplyFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.DeleteFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.GetFactCheckLimitUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.GetFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.LoadFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.ObserveFactCheckLoadedUseCase
import org.telegram.messenger.feature.messaging.factcheck.presentation.FactCheckViewModel
import org.telegram.messenger.feature.messaging.folders.data.repository.LegacyFoldersRepository
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository
import org.telegram.messenger.feature.messaging.folders.domain.usecase.CreateFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.DeleteFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetSuggestedFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.ObserveFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.ReorderFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.UpdateFolderUseCase
import org.telegram.messenger.feature.messaging.folders.presentation.FoldersViewModel
import org.telegram.messenger.feature.messaging.groupcallmsg.data.repository.LegacyGroupCallMessagesRepository
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.repository.GroupCallMessagesRepository
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.ClearGroupCallMessagesUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.GetGroupCallMessagesUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.ObserveGroupCallMessagesUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.PopGroupCallMessageUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.SendGroupCallMessageUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.presentation.GroupCallMessagesViewModel
import org.telegram.messenger.feature.messaging.hashtagsearch.data.repository.LegacyHashtagSearchRepository
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.AddHashtagToHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ClearHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ClearHashtagSearchResultsUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.GetHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.JumpToHashtagMessageUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ObserveHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ObserveHashtagSearchResultUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.RemoveHashtagFromHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.SearchHashtagUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.presentation.HashtagSearchViewModel
import org.telegram.messenger.feature.messaging.mentions.data.repository.LegacyMentionsRepository
import org.telegram.messenger.feature.messaging.mentions.domain.repository.MentionsRepository
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ClearMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.DismissMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.FilterMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.FormatMentionReplacementUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.GetMentionsStateUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ObserveMentionsStateUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ParseMentionQueryUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.SetMentionCandidatesUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.UpdateMentionQueryUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ValidateUsernameUseCase
import org.telegram.messenger.feature.messaging.mentions.presentation.MentionsViewModel
import org.telegram.messenger.feature.messaging.messagecustomparams.data.repository.LegacyMessageCustomParamsRepository
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.repository.MessageCustomParamsRepository
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.CheckMessageCustomParamsEmptyUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ClearAllMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.CopyMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.GetMessageCustomParamsStateUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.GetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.MergeMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ObserveMessageCustomParamsStateUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.RemoveMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.SetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageSummaryUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageTranslationUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateVoiceTranscriptionUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.presentation.MessageCustomParamsViewModel
import org.telegram.messenger.feature.messaging.reactions.data.repository.LegacyReactionsRepository
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.ClearReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetAvailableReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetDoubleTapReactionUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetReactionsSettingsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetRecentReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.LoadAvailableReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.ObserveAvailableReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.ObserveRecentReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.SendReactionUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.SendVoteUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.SetDoubleTapReactionUseCase
import org.telegram.messenger.feature.messaging.reactions.presentation.ReactionsViewModel
import org.telegram.messenger.feature.messaging.richcaption.data.repository.LegacyRichCaptionRepository
import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.CalculateCaptionMeasureWidthUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.CheckCaptionPressHitUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.ClearRichCaptionUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.GetRichCaptionUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.ObserveRichCaptionUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.SetRichCaptionCreditUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.SetRichCaptionLockedUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.SetRichCaptionTextUseCase
import org.telegram.messenger.feature.messaging.richcaption.presentation.RichCaptionViewModel
import org.telegram.messenger.feature.messaging.savedmessages.data.datasource.SavedMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.savedmessages.data.datasource.SavedMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.savedmessages.data.repository.LegacySavedMessagesRepository
import org.telegram.messenger.feature.messaging.savedmessages.data.repository.SavedMessagesRepositoryImpl
import org.telegram.messenger.feature.messaging.savedmessages.domain.repository.SavedMessagesRepository
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.DeleteSavedDialogUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.GetSavedTagsUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.SearchSavedDialogsUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.TogglePinSavedDialogUseCase
import org.telegram.messenger.feature.messaging.savedmessages.presentation.SavedMessagesViewModel
import org.telegram.messenger.feature.messaging.search.data.repository.LegacySearchRepository
import org.telegram.messenger.feature.messaging.search.domain.repository.SearchRepository
import org.telegram.messenger.feature.messaging.search.domain.usecase.ClearRecentHashtagsUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.ClearRecentSearchesUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.GetRecentHashtagsUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.GetRecentSearchesUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.PutRecentHashtagUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.RemoveRecentSearchUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.SearchGlobalUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.SearchLocalUseCase
import org.telegram.messenger.feature.messaging.search.presentation.SearchViewModel
import org.telegram.messenger.feature.messaging.sendmessages.data.repository.LegacySendMessagesRepository
import org.telegram.messenger.feature.messaging.sendmessages.domain.repository.SendMessagesRepository
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.CancelSendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ForwardMessagesUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ObservePendingSendsUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.RetrySendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaAlbumUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendTextMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.presentation.SendMessagesViewModel
import org.telegram.messenger.feature.messaging.stickers.data.repository.LegacyStickersRepository
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetRecentStickersUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetStickerSetUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetStickerSetsUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetStickersForEmojiUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.ObserveStickerSetsUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.ToggleStickerSetArchivedUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.ToggleStickerSetInstalledUseCase
import org.telegram.messenger.feature.messaging.stickers.presentation.StickersViewModel
import org.telegram.messenger.feature.messaging.topics.data.repository.LegacyTopicsRepository
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository
import org.telegram.messenger.feature.messaging.topics.domain.usecase.DeleteTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.GetForumUnreadCountUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.GetTopicUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.GetTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.LoadTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.MarkTopicReactionsAsReadUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ObserveForumUnreadCountUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ObserveTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ReloadTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ReorderPinnedTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ToggleCloseTopicUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.TogglePinTopicUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ToggleShowTopicUseCase
import org.telegram.messenger.feature.messaging.topics.presentation.TopicsViewModel
import org.telegram.messenger.feature.messaging.translate.data.repository.LegacyTranslationRepository
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository
import org.telegram.messenger.feature.messaging.translate.domain.usecase.AddDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ApplyAppLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetAvailableLanguagesUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetDialogTranslationStateUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetTranslateSettingsUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ObserveDialogTranslationStateUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ObserveTranslateSettingsUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.RemoveDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetChatTranslateEnabledUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetContextTranslateEnabledUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetDialogTargetLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetDoNotTranslateLanguagesUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ToggleDialogTranslatingUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.TranslateTextUseCase
import org.telegram.messenger.feature.messaging.translate.presentation.TranslateViewModel
import org.telegram.ui.Components.chat.ChatActivityBottomViewsVisibilityController
import org.telegram.ui.Components.chat.ChatActivityDraftMessageMeasureController

class MessagingContainer(val account: Int) {

    val savedMessagesRemoteDataSource: SavedMessagesRemoteDataSource by lazy {
        SavedMessagesRemoteDataSource(account)
    }

    val savedMessagesLocalDataSource: SavedMessagesLocalDataSource by lazy {
        SavedMessagesLocalDataSource(account)
    }

    fun createSavedMessagesRepository(): SavedMessagesRepository {
        return SavedMessagesRepositoryImpl(
            account = account,
            localDataSource = savedMessagesLocalDataSource,
            remoteDataSource = savedMessagesRemoteDataSource
        )
    }

    private var customSavedMessagesRepository: SavedMessagesRepository? = null

    /**
     * Repository providing Saved Messages operations.
     * Can be replaced or mocked via custom setter for testing.
     */
    var savedMessagesRepository: SavedMessagesRepository
        get() = customSavedMessagesRepository ?: createSavedMessagesRepository()
        set(value) {
            customSavedMessagesRepository = value
        }

    val getSavedDialogsUseCase: GetSavedDialogsUseCase
        get() = GetSavedDialogsUseCase(savedMessagesRepository)

    val togglePinSavedDialogUseCase: TogglePinSavedDialogUseCase
        get() = TogglePinSavedDialogUseCase(savedMessagesRepository)

    val deleteSavedDialogUseCase: DeleteSavedDialogUseCase
        get() = DeleteSavedDialogUseCase(savedMessagesRepository)

    val getSavedTagsUseCase: GetSavedTagsUseCase
        get() = GetSavedTagsUseCase(savedMessagesRepository)

    val searchSavedDialogsUseCase: SearchSavedDialogsUseCase
        get() = SearchSavedDialogsUseCase(savedMessagesRepository)

    private var cachedSavedMessagesViewModel: SavedMessagesViewModel? = null

    fun getSavedMessagesViewModel(): SavedMessagesViewModel {
        return cachedSavedMessagesViewModel ?: createSavedMessagesViewModel().also {
            cachedSavedMessagesViewModel = it
        }
    }

    fun createSavedMessagesViewModel(): SavedMessagesViewModel {
        return SavedMessagesViewModel(
            account = account,
            getSavedDialogsUseCase = getSavedDialogsUseCase,
            togglePinSavedDialogUseCase = togglePinSavedDialogUseCase,
            deleteSavedDialogUseCase = deleteSavedDialogUseCase,
            getSavedTagsUseCase = getSavedTagsUseCase,
            searchSavedDialogsUseCase = searchSavedDialogsUseCase
        )
    }

    private var customDialogsRepository: DialogsRepository? = null

    var dialogsRepository: DialogsRepository
        get() = customDialogsRepository ?: LegacyDialogsRepository(account)
        set(value) {
            customDialogsRepository = value
        }

    val getDialogsUseCase: GetDialogsUseCase
        get() = GetDialogsUseCase(dialogsRepository)

    val loadMoreDialogsUseCase: LoadMoreDialogsUseCase
        get() = LoadMoreDialogsUseCase(dialogsRepository)

    val pinDialogUseCase: PinDialogUseCase
        get() = PinDialogUseCase(dialogsRepository)

    val deleteDialogUseCase: DeleteDialogUseCase
        get() = DeleteDialogUseCase(dialogsRepository)

    val markDialogAsReadUseCase: MarkDialogAsReadUseCase
        get() = MarkDialogAsReadUseCase(dialogsRepository)

    private var cachedDialogsViewModel: DialogsViewModel? = null

    fun getDialogsViewModel(): DialogsViewModel {
        return cachedDialogsViewModel ?: createDialogsViewModel().also {
            cachedDialogsViewModel = it
        }
    }

    fun createDialogsViewModel(): DialogsViewModel {
        return DialogsViewModel(
            account = account,
            getDialogsUseCase = getDialogsUseCase,
            loadMoreDialogsUseCase = loadMoreDialogsUseCase,
            pinDialogUseCase = pinDialogUseCase,
            deleteDialogUseCase = deleteDialogUseCase,
            markDialogAsReadUseCase = markDialogAsReadUseCase
        )
    }

    private var customChatRepository: ChatRepository? = null

    var chatRepository: ChatRepository
        get() = customChatRepository ?: LegacyChatRepository(account)
        set(value) {
            customChatRepository = value
        }

    val observeMessagesUseCase: ObserveMessagesUseCase
        get() = ObserveMessagesUseCase(chatRepository)

    val getMessagesUseCase: GetMessagesUseCase
        get() = GetMessagesUseCase(chatRepository)

    val loadHistoryUseCase: LoadHistoryUseCase
        get() = LoadHistoryUseCase(chatRepository)

    val sendMessageUseCase: SendMessageUseCase
        get() = SendMessageUseCase(chatRepository)

    val deleteMessagesUseCase: DeleteMessagesUseCase
        get() = DeleteMessagesUseCase(chatRepository)

    private val cachedChatViewModels = ConcurrentHashMap<Long, ChatViewModel>()

    fun getChatViewModel(dialogId: Long): ChatViewModel {
        return cachedChatViewModels.computeIfAbsent(dialogId) { createChatViewModel(it) }
    }

    fun createChatViewModel(dialogId: Long): ChatViewModel {
        return ChatViewModel(
            account = account,
            dialogId = dialogId,
            observeMessagesUseCase = observeMessagesUseCase,
            loadHistoryUseCase = loadHistoryUseCase,
            sendMessageUseCase = sendMessageUseCase,
            deleteMessagesUseCase = deleteMessagesUseCase
        )
    }

    private var customFoldersRepository: FoldersRepository? = null

    var foldersRepository: FoldersRepository
        get() = customFoldersRepository ?: LegacyFoldersRepository(account)
        set(value) {
            customFoldersRepository = value
        }

    val observeFoldersUseCase: ObserveFoldersUseCase
        get() = ObserveFoldersUseCase(foldersRepository)

    val getFoldersUseCase: GetFoldersUseCase
        get() = GetFoldersUseCase(foldersRepository)

    val getFolderUseCase: GetFolderUseCase
        get() = GetFolderUseCase(foldersRepository)

    val createFolderUseCase: CreateFolderUseCase
        get() = CreateFolderUseCase(foldersRepository)

    val updateFolderUseCase: UpdateFolderUseCase
        get() = UpdateFolderUseCase(foldersRepository)

    val deleteFolderUseCase: DeleteFolderUseCase
        get() = DeleteFolderUseCase(foldersRepository)

    val reorderFoldersUseCase: ReorderFoldersUseCase
        get() = ReorderFoldersUseCase(foldersRepository)

    val getSuggestedFoldersUseCase: GetSuggestedFoldersUseCase
        get() = GetSuggestedFoldersUseCase(foldersRepository)

    private var cachedFoldersViewModel: FoldersViewModel? = null

    val foldersViewModel: FoldersViewModel
        get() {
            var vm = cachedFoldersViewModel
            if (vm == null) {
                vm = createFoldersViewModel()
                cachedFoldersViewModel = vm
            }
            return vm
        }

    fun createFoldersViewModel(): FoldersViewModel {
        return FoldersViewModel(
            observeFoldersUseCase = observeFoldersUseCase,
            getFoldersUseCase = getFoldersUseCase,
            getFolderUseCase = getFolderUseCase,
            createFolderUseCase = createFolderUseCase,
            updateFolderUseCase = updateFolderUseCase,
            deleteFolderUseCase = deleteFolderUseCase,
            reorderFoldersUseCase = reorderFoldersUseCase,
            getSuggestedFoldersUseCase = getSuggestedFoldersUseCase
        )
    }

    private var customStickersRepository: StickersRepository? = null

    var stickersRepository: StickersRepository
        get() = customStickersRepository ?: LegacyStickersRepository(account)
        set(value) {
            customStickersRepository = value
        }

    val observeStickerSetsUseCase: ObserveStickerSetsUseCase
        get() = ObserveStickerSetsUseCase(stickersRepository)

    val getStickerSetsUseCase: GetStickerSetsUseCase
        get() = GetStickerSetsUseCase(stickersRepository)

    val getStickerSetUseCase: GetStickerSetUseCase
        get() = GetStickerSetUseCase(stickersRepository)

    val getRecentStickersUseCase: GetRecentStickersUseCase
        get() = GetRecentStickersUseCase(stickersRepository)

    val getStickersForEmojiUseCase: GetStickersForEmojiUseCase
        get() = GetStickersForEmojiUseCase(stickersRepository)

    val toggleStickerSetInstalledUseCase: ToggleStickerSetInstalledUseCase
        get() = ToggleStickerSetInstalledUseCase(stickersRepository)

    val toggleStickerSetArchivedUseCase: ToggleStickerSetArchivedUseCase
        get() = ToggleStickerSetArchivedUseCase(stickersRepository)

    private val cachedStickersViewModels = ConcurrentHashMap<Int, StickersViewModel>()

    val stickersViewModel: StickersViewModel
        get() = getStickersViewModel(0)

    fun getStickersViewModel(type: Int = 0): StickersViewModel {
        return cachedStickersViewModels.computeIfAbsent(type) { createStickersViewModel(it) }
    }

    fun createStickersViewModel(type: Int = 0): StickersViewModel {
        return StickersViewModel(
            observeStickerSetsUseCase = observeStickerSetsUseCase,
            getStickerSetsUseCase = getStickerSetsUseCase,
            getStickerSetUseCase = getStickerSetUseCase,
            getRecentStickersUseCase = getRecentStickersUseCase,
            getStickersForEmojiUseCase = getStickersForEmojiUseCase,
            toggleStickerSetInstalledUseCase = toggleStickerSetInstalledUseCase,
            toggleStickerSetArchivedUseCase = toggleStickerSetArchivedUseCase,
            stickerType = type
        )
    }

    private var customSearchRepository: SearchRepository? = null

    var searchRepository: SearchRepository
        get() = customSearchRepository ?: LegacySearchRepository(account)
        set(value) {
            customSearchRepository = value
        }

    val searchGlobalUseCase: SearchGlobalUseCase
        get() = SearchGlobalUseCase(searchRepository)

    val searchLocalUseCase: SearchLocalUseCase
        get() = SearchLocalUseCase(searchRepository)

    val getRecentSearchesUseCase: GetRecentSearchesUseCase
        get() = GetRecentSearchesUseCase(searchRepository)

    val clearRecentSearchesUseCase: ClearRecentSearchesUseCase
        get() = ClearRecentSearchesUseCase(searchRepository)

    val removeRecentSearchUseCase: RemoveRecentSearchUseCase
        get() = RemoveRecentSearchUseCase(searchRepository)

    val getRecentHashtagsUseCase: GetRecentHashtagsUseCase
        get() = GetRecentHashtagsUseCase(searchRepository)

    val putRecentHashtagUseCase: PutRecentHashtagUseCase
        get() = PutRecentHashtagUseCase(searchRepository)

    val clearRecentHashtagsUseCase: ClearRecentHashtagsUseCase
        get() = ClearRecentHashtagsUseCase(searchRepository)

    private var cachedSearchViewModel: SearchViewModel? = null

    val searchViewModel: SearchViewModel
        get() {
            var vm = cachedSearchViewModel
            if (vm == null) {
                vm = createSearchViewModel()
                cachedSearchViewModel = vm
            }
            return vm
        }

    fun createSearchViewModel(): SearchViewModel {
        return SearchViewModel(
            searchGlobalUseCase = searchGlobalUseCase,
            searchLocalUseCase = searchLocalUseCase,
            getRecentSearchesUseCase = getRecentSearchesUseCase,
            clearRecentSearchesUseCase = clearRecentSearchesUseCase,
            removeRecentSearchUseCase = removeRecentSearchUseCase,
            getRecentHashtagsUseCase = getRecentHashtagsUseCase,
            putRecentHashtagUseCase = putRecentHashtagUseCase,
            clearRecentHashtagsUseCase = clearRecentHashtagsUseCase
        )
    }

    private var customTopicsRepository: TopicsRepository? = null

    var topicsRepository: TopicsRepository
        get() = customTopicsRepository ?: LegacyTopicsRepository(account)
        set(value) {
            customTopicsRepository = value
        }

    val observeTopicsUseCase: ObserveTopicsUseCase
        get() = ObserveTopicsUseCase(topicsRepository)

    val observeForumUnreadCountUseCase: ObserveForumUnreadCountUseCase
        get() = ObserveForumUnreadCountUseCase(topicsRepository)

    val getTopicsUseCase: GetTopicsUseCase
        get() = GetTopicsUseCase(topicsRepository)

    val getTopicUseCase: GetTopicUseCase
        get() = GetTopicUseCase(topicsRepository)

    val loadTopicsUseCase: LoadTopicsUseCase
        get() = LoadTopicsUseCase(topicsRepository)

    val reloadTopicsUseCase: ReloadTopicsUseCase
        get() = ReloadTopicsUseCase(topicsRepository)

    val toggleCloseTopicUseCase: ToggleCloseTopicUseCase
        get() = ToggleCloseTopicUseCase(topicsRepository)

    val togglePinTopicUseCase: TogglePinTopicUseCase
        get() = TogglePinTopicUseCase(topicsRepository)

    val toggleShowTopicUseCase: ToggleShowTopicUseCase
        get() = ToggleShowTopicUseCase(topicsRepository)

    val deleteTopicsUseCase: DeleteTopicsUseCase
        get() = DeleteTopicsUseCase(topicsRepository)

    val reorderPinnedTopicsUseCase: ReorderPinnedTopicsUseCase
        get() = ReorderPinnedTopicsUseCase(topicsRepository)

    val markTopicReactionsAsReadUseCase: MarkTopicReactionsAsReadUseCase
        get() = MarkTopicReactionsAsReadUseCase(topicsRepository)

    val getForumUnreadCountUseCase: GetForumUnreadCountUseCase
        get() = GetForumUnreadCountUseCase(topicsRepository)

    private var cachedTopicsViewModel: TopicsViewModel? = null

    val topicsViewModel: TopicsViewModel
        get() {
            var vm = cachedTopicsViewModel
            if (vm == null) {
                vm = createTopicsViewModel()
                cachedTopicsViewModel = vm
            }
            return vm
        }

    fun createTopicsViewModel(): TopicsViewModel {
        return TopicsViewModel(
            observeTopicsUseCase = observeTopicsUseCase,
            observeForumUnreadCountUseCase = observeForumUnreadCountUseCase,
            getTopicsUseCase = getTopicsUseCase,
            getTopicUseCase = getTopicUseCase,
            loadTopicsUseCase = loadTopicsUseCase,
            reloadTopicsUseCase = reloadTopicsUseCase,
            toggleCloseTopicUseCase = toggleCloseTopicUseCase,
            togglePinTopicUseCase = togglePinTopicUseCase,
            toggleShowTopicUseCase = toggleShowTopicUseCase,
            deleteTopicsUseCase = deleteTopicsUseCase,
            reorderPinnedTopicsUseCase = reorderPinnedTopicsUseCase,
            markTopicReactionsAsReadUseCase = markTopicReactionsAsReadUseCase,
            getForumUnreadCountUseCase = getForumUnreadCountUseCase
        )
    }

    private var customTranslationRepository: TranslationRepository? = null

    var translationRepository: TranslationRepository
        get() = customTranslationRepository ?: LegacyTranslationRepository(account)
        set(value) {
            customTranslationRepository = value
        }

    val observeTranslateSettingsUseCase: ObserveTranslateSettingsUseCase
        get() = ObserveTranslateSettingsUseCase(translationRepository)

    val getTranslateSettingsUseCase: GetTranslateSettingsUseCase
        get() = GetTranslateSettingsUseCase(translationRepository)

    val setChatTranslateEnabledUseCase: SetChatTranslateEnabledUseCase
        get() = SetChatTranslateEnabledUseCase(translationRepository)

    val setContextTranslateEnabledUseCase: SetContextTranslateEnabledUseCase
        get() = SetContextTranslateEnabledUseCase(translationRepository)

    val setDoNotTranslateLanguagesUseCase: SetDoNotTranslateLanguagesUseCase
        get() = SetDoNotTranslateLanguagesUseCase(translationRepository)

    val addDoNotTranslateLanguageUseCase: AddDoNotTranslateLanguageUseCase
        get() = AddDoNotTranslateLanguageUseCase(translationRepository)

    val removeDoNotTranslateLanguageUseCase: RemoveDoNotTranslateLanguageUseCase
        get() = RemoveDoNotTranslateLanguageUseCase(translationRepository)

    val observeDialogTranslationStateUseCase: ObserveDialogTranslationStateUseCase
        get() = ObserveDialogTranslationStateUseCase(translationRepository)

    val getDialogTranslationStateUseCase: GetDialogTranslationStateUseCase
        get() = GetDialogTranslationStateUseCase(translationRepository)

    val toggleDialogTranslatingUseCase: ToggleDialogTranslatingUseCase
        get() = ToggleDialogTranslatingUseCase(translationRepository)

    val setDialogTargetLanguageUseCase: SetDialogTargetLanguageUseCase
        get() = SetDialogTargetLanguageUseCase(translationRepository)

    val translateTextUseCase: TranslateTextUseCase
        get() = TranslateTextUseCase(translationRepository)

    val getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase
        get() = GetAvailableLanguagesUseCase(translationRepository)

    val applyAppLanguageUseCase: ApplyAppLanguageUseCase
        get() = ApplyAppLanguageUseCase(translationRepository)

    private var cachedTranslateViewModel: TranslateViewModel? = null

    val translateViewModel: TranslateViewModel
        get() {
            var vm = cachedTranslateViewModel
            if (vm == null) {
                vm = createTranslateViewModel()
                cachedTranslateViewModel = vm
            }
            return vm
        }

    fun createTranslateViewModel(): TranslateViewModel {
        return TranslateViewModel(
            observeTranslateSettingsUseCase = observeTranslateSettingsUseCase,
            getTranslateSettingsUseCase = getTranslateSettingsUseCase,
            setChatTranslateEnabledUseCase = setChatTranslateEnabledUseCase,
            setContextTranslateEnabledUseCase = setContextTranslateEnabledUseCase,
            setDoNotTranslateLanguagesUseCase = setDoNotTranslateLanguagesUseCase,
            addDoNotTranslateLanguageUseCase = addDoNotTranslateLanguageUseCase,
            removeDoNotTranslateLanguageUseCase = removeDoNotTranslateLanguageUseCase,
            observeDialogTranslationStateUseCase = observeDialogTranslationStateUseCase,
            getDialogTranslationStateUseCase = getDialogTranslationStateUseCase,
            toggleDialogTranslatingUseCase = toggleDialogTranslatingUseCase,
            setDialogTargetLanguageUseCase = setDialogTargetLanguageUseCase,
            translateTextUseCase = translateTextUseCase,
            getAvailableLanguagesUseCase = getAvailableLanguagesUseCase,
            applyAppLanguageUseCase = applyAppLanguageUseCase
        )
    }

    private var customReactionsRepository: ReactionsRepository? = null

    var reactionsRepository: ReactionsRepository
        get() = customReactionsRepository ?: LegacyReactionsRepository(account)
        set(value) {
            customReactionsRepository = value
        }

    val observeAvailableReactionsUseCase: ObserveAvailableReactionsUseCase
        get() = ObserveAvailableReactionsUseCase(reactionsRepository)

    val getAvailableReactionsUseCase: GetAvailableReactionsUseCase
        get() = GetAvailableReactionsUseCase(reactionsRepository)

    val loadAvailableReactionsUseCase: LoadAvailableReactionsUseCase
        get() = LoadAvailableReactionsUseCase(reactionsRepository)

    val observeRecentReactionsUseCase: ObserveRecentReactionsUseCase
        get() = ObserveRecentReactionsUseCase(reactionsRepository)

    val getRecentReactionsUseCase: GetRecentReactionsUseCase
        get() = GetRecentReactionsUseCase(reactionsRepository)

    val getReactionsSettingsUseCase: GetReactionsSettingsUseCase
        get() = GetReactionsSettingsUseCase(reactionsRepository)

    val getDoubleTapReactionUseCase: GetDoubleTapReactionUseCase
        get() = GetDoubleTapReactionUseCase(reactionsRepository)

    val setDoubleTapReactionUseCase: SetDoubleTapReactionUseCase
        get() = SetDoubleTapReactionUseCase(reactionsRepository)

    val sendReactionUseCase: SendReactionUseCase
        get() = SendReactionUseCase(reactionsRepository)

    val clearReactionsUseCase: ClearReactionsUseCase
        get() = ClearReactionsUseCase(reactionsRepository)

    val sendVoteUseCase: SendVoteUseCase
        get() = SendVoteUseCase(reactionsRepository)

    private var cachedReactionsViewModel: ReactionsViewModel? = null

    val reactionsViewModel: ReactionsViewModel
        get() {
            var vm = cachedReactionsViewModel
            if (vm == null) {
                vm = createReactionsViewModel()
                cachedReactionsViewModel = vm
            }
            return vm
        }

    fun createReactionsViewModel(): ReactionsViewModel {
        return ReactionsViewModel(
            observeAvailableReactionsUseCase = observeAvailableReactionsUseCase,
            getAvailableReactionsUseCase = getAvailableReactionsUseCase,
            loadAvailableReactionsUseCase = loadAvailableReactionsUseCase,
            observeRecentReactionsUseCase = observeRecentReactionsUseCase,
            getRecentReactionsUseCase = getRecentReactionsUseCase,
            getReactionsSettingsUseCase = getReactionsSettingsUseCase,
            getDoubleTapReactionUseCase = getDoubleTapReactionUseCase,
            setDoubleTapReactionUseCase = setDoubleTapReactionUseCase,
            sendReactionUseCase = sendReactionUseCase,
            clearReactionsUseCase = clearReactionsUseCase,
            sendVoteUseCase = sendVoteUseCase
        )
    }

    private var customFactCheckRepository: FactCheckRepository? = null

    var factCheckRepository: FactCheckRepository
        get() = customFactCheckRepository ?: LegacyFactCheckRepository(account)
        set(value) {
            customFactCheckRepository = value
        }

    val observeFactCheckLoadedUseCase: ObserveFactCheckLoadedUseCase
        get() = ObserveFactCheckLoadedUseCase(factCheckRepository)

    val getFactCheckUseCase: GetFactCheckUseCase
        get() = GetFactCheckUseCase(factCheckRepository)

    val loadFactCheckUseCase: LoadFactCheckUseCase
        get() = LoadFactCheckUseCase(factCheckRepository)

    val applyFactCheckUseCase: ApplyFactCheckUseCase
        get() = ApplyFactCheckUseCase(factCheckRepository)

    val deleteFactCheckUseCase: DeleteFactCheckUseCase
        get() = DeleteFactCheckUseCase(factCheckRepository)

    val getFactCheckLimitUseCase: GetFactCheckLimitUseCase
        get() = GetFactCheckLimitUseCase(factCheckRepository)

    private var cachedFactCheckViewModel: FactCheckViewModel? = null

    val factCheckViewModel: FactCheckViewModel
        get() {
            var vm = cachedFactCheckViewModel
            if (vm == null) {
                vm = createFactCheckViewModel()
                cachedFactCheckViewModel = vm
            }
            return vm
        }

    fun createFactCheckViewModel(): FactCheckViewModel {
        return FactCheckViewModel(
            observeFactCheckLoadedUseCase = observeFactCheckLoadedUseCase,
            getFactCheckUseCase = getFactCheckUseCase,
            loadFactCheckUseCase = loadFactCheckUseCase,
            applyFactCheckUseCase = applyFactCheckUseCase,
            deleteFactCheckUseCase = deleteFactCheckUseCase,
            getFactCheckLimitUseCase = getFactCheckLimitUseCase
        )
    }

    private var customChatThemeRepository: ChatThemeRepository? = null

    var chatThemeRepository: ChatThemeRepository
        get() = customChatThemeRepository ?: LegacyChatThemeRepository(account)
        set(value) {
            customChatThemeRepository = value
        }

    val observeDialogThemeUseCase: ObserveDialogThemeUseCase
        get() = ObserveDialogThemeUseCase(chatThemeRepository)

    val getDialogThemeStateUseCase: GetDialogThemeStateUseCase
        get() = GetDialogThemeStateUseCase(chatThemeRepository)

    val getAvailableChatThemesUseCase: GetAvailableChatThemesUseCase
        get() = GetAvailableChatThemesUseCase(chatThemeRepository)

    val setDialogThemeUseCase: SetDialogThemeUseCase
        get() = SetDialogThemeUseCase(chatThemeRepository)

    val resetDialogThemeUseCase: ResetDialogThemeUseCase
        get() = ResetDialogThemeUseCase(chatThemeRepository)

    val saveChatWallpaperUseCase: SaveChatWallpaperUseCase
        get() = SaveChatWallpaperUseCase(chatThemeRepository)

    private var cachedChatThemeViewModel: ChatThemeViewModel? = null

    val chatThemeViewModel: ChatThemeViewModel
        get() {
            var vm = cachedChatThemeViewModel
            if (vm == null) {
                vm = createChatThemeViewModel()
                cachedChatThemeViewModel = vm
            }
            return vm
        }

    fun createChatThemeViewModel(): ChatThemeViewModel {
        return ChatThemeViewModel(
            observeDialogThemeUseCase = observeDialogThemeUseCase,
            getDialogThemeStateUseCase = getDialogThemeStateUseCase,
            getAvailableThemesUseCase = getAvailableChatThemesUseCase,
            setDialogThemeUseCase = setDialogThemeUseCase,
            resetDialogThemeUseCase = resetDialogThemeUseCase,
            saveChatWallpaperUseCase = saveChatWallpaperUseCase
        )
    }

    private var customAutoDeleteRepository: AutoDeleteRepository? = null

    var autoDeleteRepository: AutoDeleteRepository
        get() = customAutoDeleteRepository ?: LegacyAutoDeleteRepository(account)
        set(value) {
            customAutoDeleteRepository = value
        }

    val observeGlobalAutoDeleteUseCase: ObserveGlobalAutoDeleteUseCase
        get() = ObserveGlobalAutoDeleteUseCase(autoDeleteRepository)

    val getGlobalAutoDeleteUseCase: GetGlobalAutoDeleteUseCase
        get() = GetGlobalAutoDeleteUseCase(autoDeleteRepository)

    val setGlobalAutoDeleteUseCase: SetGlobalAutoDeleteUseCase
        get() = SetGlobalAutoDeleteUseCase(autoDeleteRepository)

    val getChatAutoDeleteUseCase: GetChatAutoDeleteUseCase
        get() = GetChatAutoDeleteUseCase(autoDeleteRepository)

    val setChatAutoDeleteUseCase: SetChatAutoDeleteUseCase
        get() = SetChatAutoDeleteUseCase(autoDeleteRepository)

    val setChatsAutoDeleteBatchUseCase: SetChatsAutoDeleteBatchUseCase
        get() = SetChatsAutoDeleteBatchUseCase(autoDeleteRepository)

    private var cachedAutoDeleteViewModel: AutoDeleteViewModel? = null

    val autoDeleteViewModel: AutoDeleteViewModel
        get() {
            var vm = cachedAutoDeleteViewModel
            if (vm == null) {
                vm = createAutoDeleteViewModel()
                cachedAutoDeleteViewModel = vm
            }
            return vm
        }

    fun createAutoDeleteViewModel(): AutoDeleteViewModel {
        return AutoDeleteViewModel(
            observeGlobalAutoDeleteUseCase = observeGlobalAutoDeleteUseCase,
            getGlobalAutoDeleteUseCase = getGlobalAutoDeleteUseCase,
            setGlobalAutoDeleteUseCase = setGlobalAutoDeleteUseCase,
            getChatAutoDeleteUseCase = getChatAutoDeleteUseCase,
            setChatAutoDeleteUseCase = setChatAutoDeleteUseCase,
            setChatsAutoDeleteBatchUseCase = setChatsAutoDeleteBatchUseCase
        )
    }

    private var customAiTonesRepository: AiTonesRepository? = null

    var aiTonesRepository: AiTonesRepository
        get() = customAiTonesRepository ?: LegacyAiTonesRepository(account)
        set(value) {
            customAiTonesRepository = value
        }

    val observeAiTonesUseCase: ObserveAiTonesUseCase
        get() = ObserveAiTonesUseCase(aiTonesRepository)

    val getAiTonesStateUseCase: GetAiTonesStateUseCase
        get() = GetAiTonesStateUseCase(aiTonesRepository)

    val loadAiTonesUseCase: LoadAiTonesUseCase
        get() = LoadAiTonesUseCase(aiTonesRepository)

    val addAiToneUseCase: AddAiToneUseCase
        get() = AddAiToneUseCase(aiTonesRepository)

    val removeAiToneUseCase: RemoveAiToneUseCase
        get() = RemoveAiToneUseCase(aiTonesRepository)

    val unsaveAiToneUseCase: UnsaveAiToneUseCase
        get() = UnsaveAiToneUseCase(aiTonesRepository)

    val editAiToneUseCase: EditAiToneUseCase
        get() = EditAiToneUseCase(aiTonesRepository)

    private var cachedAiTonesViewModel: AiTonesViewModel? = null

    val aiTonesViewModel: AiTonesViewModel
        get() {
            var vm = cachedAiTonesViewModel
            if (vm == null) {
                vm = createAiTonesViewModel()
                cachedAiTonesViewModel = vm
            }
            return vm
        }

    fun createAiTonesViewModel(): AiTonesViewModel {
        return AiTonesViewModel(
            observeAiTonesUseCase = observeAiTonesUseCase,
            getAiTonesStateUseCase = getAiTonesStateUseCase,
            loadAiTonesUseCase = loadAiTonesUseCase,
            addAiToneUseCase = addAiToneUseCase,
            removeAiToneUseCase = removeAiToneUseCase,
            unsaveAiToneUseCase = unsaveAiToneUseCase,
            editAiToneUseCase = editAiToneUseCase
        )
    }

    private var customHashtagSearchRepository: HashtagSearchRepository? = null

    var hashtagSearchRepository: HashtagSearchRepository
        get() = customHashtagSearchRepository ?: LegacyHashtagSearchRepository(account)
        set(value) {
            customHashtagSearchRepository = value
        }

    val observeHashtagHistoryUseCase: ObserveHashtagHistoryUseCase
        get() = ObserveHashtagHistoryUseCase(hashtagSearchRepository)

    val getHashtagHistoryUseCase: GetHashtagHistoryUseCase
        get() = GetHashtagHistoryUseCase(hashtagSearchRepository)

    val addHashtagToHistoryUseCase: AddHashtagToHistoryUseCase
        get() = AddHashtagToHistoryUseCase(hashtagSearchRepository)

    val removeHashtagFromHistoryUseCase: RemoveHashtagFromHistoryUseCase
        get() = RemoveHashtagFromHistoryUseCase(hashtagSearchRepository)

    val clearHashtagHistoryUseCase: ClearHashtagHistoryUseCase
        get() = ClearHashtagHistoryUseCase(hashtagSearchRepository)

    val observeHashtagSearchResultUseCase: ObserveHashtagSearchResultUseCase
        get() = ObserveHashtagSearchResultUseCase(hashtagSearchRepository)

    val searchHashtagUseCase: SearchHashtagUseCase
        get() = SearchHashtagUseCase(hashtagSearchRepository)

    val jumpToHashtagMessageUseCase: JumpToHashtagMessageUseCase
        get() = JumpToHashtagMessageUseCase(hashtagSearchRepository)

    val clearHashtagSearchResultsUseCase: ClearHashtagSearchResultsUseCase
        get() = ClearHashtagSearchResultsUseCase(hashtagSearchRepository)

    private var cachedHashtagSearchViewModel: HashtagSearchViewModel? = null

    val hashtagSearchViewModel: HashtagSearchViewModel
        get() {
            var vm = cachedHashtagSearchViewModel
            if (vm == null) {
                vm = createHashtagSearchViewModel()
                cachedHashtagSearchViewModel = vm
            }
            return vm
        }

    fun createHashtagSearchViewModel(): HashtagSearchViewModel {
        return HashtagSearchViewModel(
            observeHashtagHistoryUseCase = observeHashtagHistoryUseCase,
            getHashtagHistoryUseCase = getHashtagHistoryUseCase,
            addHashtagToHistoryUseCase = addHashtagToHistoryUseCase,
            removeHashtagFromHistoryUseCase = removeHashtagFromHistoryUseCase,
            clearHashtagHistoryUseCase = clearHashtagHistoryUseCase,
            observeHashtagSearchResultUseCase = observeHashtagSearchResultUseCase,
            searchHashtagUseCase = searchHashtagUseCase,
            jumpToHashtagMessageUseCase = jumpToHashtagMessageUseCase,
            clearHashtagSearchResultsUseCase = clearHashtagSearchResultsUseCase
        )
    }

    private var customGroupCallMessagesRepository: GroupCallMessagesRepository? = null

    var groupCallMessagesRepository: GroupCallMessagesRepository
        get() = customGroupCallMessagesRepository ?: LegacyGroupCallMessagesRepository(account)
        set(value) {
            customGroupCallMessagesRepository = value
        }

    val observeGroupCallMessagesUseCase: ObserveGroupCallMessagesUseCase
        get() = ObserveGroupCallMessagesUseCase(groupCallMessagesRepository)

    val getGroupCallMessagesUseCase: GetGroupCallMessagesUseCase
        get() = GetGroupCallMessagesUseCase(groupCallMessagesRepository)

    val sendGroupCallMessageUseCase: SendGroupCallMessageUseCase
        get() = SendGroupCallMessageUseCase(groupCallMessagesRepository)

    val popGroupCallMessageUseCase: PopGroupCallMessageUseCase
        get() = PopGroupCallMessageUseCase(groupCallMessagesRepository)

    val clearGroupCallMessagesUseCase: ClearGroupCallMessagesUseCase
        get() = ClearGroupCallMessagesUseCase(groupCallMessagesRepository)

    private val cachedGroupCallMessagesViewModels = ConcurrentHashMap<Long, GroupCallMessagesViewModel>()

    fun getGroupCallMessagesViewModel(callId: Long = 0L): GroupCallMessagesViewModel {
        return cachedGroupCallMessagesViewModels.computeIfAbsent(callId) { createGroupCallMessagesViewModel(it) }
    }

    fun createGroupCallMessagesViewModel(callId: Long = 0L): GroupCallMessagesViewModel {
        return GroupCallMessagesViewModel(
            observeGroupCallMessagesUseCase = observeGroupCallMessagesUseCase,
            getGroupCallMessagesUseCase = getGroupCallMessagesUseCase,
            sendGroupCallMessageUseCase = sendGroupCallMessageUseCase,
            popGroupCallMessageUseCase = popGroupCallMessageUseCase,
            clearGroupCallMessagesUseCase = clearGroupCallMessagesUseCase,
            initialCallId = callId
        )
    }

    private var customChatMessagesMetadataRepository: ChatMessagesMetadataRepository? = null

    var chatMessagesMetadataRepository: ChatMessagesMetadataRepository
        get() = customChatMessagesMetadataRepository ?: LegacyChatMessagesMetadataRepository(account)
        set(value) {
            customChatMessagesMetadataRepository = value
        }

    val observeChatMetadataStatsUseCase: ObserveChatMetadataStatsUseCase
        get() = ObserveChatMetadataStatsUseCase(chatMessagesMetadataRepository)

    val getChatMetadataStatsUseCase: GetChatMetadataStatsUseCase
        get() = GetChatMetadataStatsUseCase(chatMessagesMetadataRepository)

    val checkMessagesMetadataUseCase: CheckMessagesMetadataUseCase
        get() = CheckMessagesMetadataUseCase(chatMessagesMetadataRepository)

    val loadMessagesReactionsUseCase: LoadMessagesReactionsUseCase
        get() = LoadMessagesReactionsUseCase(chatMessagesMetadataRepository)

    val loadMessagesExtendedMediaUseCase: LoadMessagesExtendedMediaUseCase
        get() = LoadMessagesExtendedMediaUseCase(chatMessagesMetadataRepository)

    val cancelPendingMetadataRequestsUseCase: CancelPendingMetadataRequestsUseCase
        get() = CancelPendingMetadataRequestsUseCase(chatMessagesMetadataRepository)

    private var cachedChatMetadataViewModel: ChatMetadataViewModel? = null

    val chatMetadataViewModel: ChatMetadataViewModel
        get() {
            var vm = cachedChatMetadataViewModel
            if (vm == null) {
                vm = createChatMetadataViewModel()
                cachedChatMetadataViewModel = vm
            }
            return vm
        }

    fun createChatMetadataViewModel(): ChatMetadataViewModel {
        return ChatMetadataViewModel(
            observeChatMetadataStatsUseCase = observeChatMetadataStatsUseCase,
            getChatMetadataStatsUseCase = getChatMetadataStatsUseCase,
            checkMessagesMetadataUseCase = checkMessagesMetadataUseCase,
            loadMessagesReactionsUseCase = loadMessagesReactionsUseCase,
            loadMessagesExtendedMediaUseCase = loadMessagesExtendedMediaUseCase,
            cancelPendingMetadataRequestsUseCase = cancelPendingMetadataRequestsUseCase
        )
    }

    private var customDraftsRepository: DraftsRepository? = null

    var draftsRepository: DraftsRepository
        get() = customDraftsRepository ?: LegacyDraftsRepository(account)
        set(value) {
            customDraftsRepository = value
        }

    val observeDraftsStateUseCase: ObserveDraftsStateUseCase
        get() = ObserveDraftsStateUseCase(draftsRepository)

    val getDraftsStateUseCase: GetDraftsStateUseCase
        get() = GetDraftsStateUseCase(draftsRepository)

    val loadDraftsUseCase: LoadDraftsUseCase
        get() = LoadDraftsUseCase(draftsRepository)

    val saveDraftUseCase: SaveDraftUseCase
        get() = SaveDraftUseCase(draftsRepository)

    val deleteDraftUseCase: DeleteDraftUseCase
        get() = DeleteDraftUseCase(draftsRepository)

    val deleteForEditUseCase: DeleteForEditUseCase
        get() = DeleteForEditUseCase(draftsRepository)

    val getDraftForEditUseCase: GetDraftForEditUseCase
        get() = GetDraftForEditUseCase(draftsRepository)

    val cleanupExpiredDraftsUseCase: CleanupExpiredDraftsUseCase
        get() = CleanupExpiredDraftsUseCase(draftsRepository)

    private var cachedDraftsViewModel: DraftsViewModel? = null

    val draftsViewModel: DraftsViewModel
        get() {
            var vm = cachedDraftsViewModel
            if (vm == null) {
                vm = createDraftsViewModel()
                cachedDraftsViewModel = vm
            }
            return vm
        }

    fun createDraftsViewModel(): DraftsViewModel {
        return DraftsViewModel(
            observeDraftsStateUseCase = observeDraftsStateUseCase,
            getDraftsStateUseCase = getDraftsStateUseCase,
            loadDraftsUseCase = loadDraftsUseCase,
            saveDraftUseCase = saveDraftUseCase,
            deleteDraftUseCase = deleteDraftUseCase,
            deleteForEditUseCase = deleteForEditUseCase,
            getDraftForEditUseCase = getDraftForEditUseCase,
            cleanupExpiredDraftsUseCase = cleanupExpiredDraftsUseCase
        )
    }

    // ==========================================
    // Feature: FileRef (MTProto File Reference Renewal)
    // ==========================================

    fun createDraftMeasureRepository(legacyController: ChatActivityDraftMessageMeasureController? = null): DraftMeasureRepository {
        return LegacyDraftMeasureRepository(legacyController)
    }

    val draftMeasureRepository: DraftMeasureRepository by lazy {
        LegacyDraftMeasureRepository()
    }

    val calculateDraftMeasureOverrideUseCase: CalculateDraftMeasureOverrideUseCase
        get() = CalculateDraftMeasureOverrideUseCase(draftMeasureRepository)

    val setDraftMeasureTargetUseCase: SetDraftMeasureTargetUseCase
        get() = SetDraftMeasureTargetUseCase(draftMeasureRepository)

    val onDraftMessageIdChangedUseCase: OnDraftMessageIdChangedUseCase
        get() = OnDraftMessageIdChangedUseCase(draftMeasureRepository)

    val setPreviousMessageHeightUseCase: SetPreviousMessageHeightUseCase
        get() = SetPreviousMessageHeightUseCase(draftMeasureRepository)

    val resetDraftMeasureTargetUseCase: ResetDraftMeasureTargetUseCase
        get() = ResetDraftMeasureTargetUseCase(draftMeasureRepository)

    val observeDraftMeasureConfigUseCase: ObserveDraftMeasureConfigUseCase
        get() = ObserveDraftMeasureConfigUseCase(draftMeasureRepository)

    val getDraftMeasureConfigUseCase: GetDraftMeasureConfigUseCase
        get() = GetDraftMeasureConfigUseCase(draftMeasureRepository)

    private var cachedDraftMeasureViewModel: DraftMeasureViewModel? = null

    val draftMeasureViewModel: DraftMeasureViewModel
        get() {
            var vm = cachedDraftMeasureViewModel
            if (vm == null) {
                vm = createDraftMeasureViewModel()
                cachedDraftMeasureViewModel = vm
            }
            return vm
        }

    fun createDraftMeasureViewModel(legacyController: ChatActivityDraftMessageMeasureController? = null): DraftMeasureViewModel {
        val repo = if (legacyController != null) createDraftMeasureRepository(legacyController) else draftMeasureRepository
        return DraftMeasureViewModel(
            calculateOverrideUseCase = CalculateDraftMeasureOverrideUseCase(repo),
            setTargetUseCase = SetDraftMeasureTargetUseCase(repo),
            onMessageIdChangedUseCase = OnDraftMessageIdChangedUseCase(repo),
            setPreviousHeightUseCase = SetPreviousMessageHeightUseCase(repo),
            resetTargetUseCase = ResetDraftMeasureTargetUseCase(repo),
            observeConfigUseCase = ObserveDraftMeasureConfigUseCase(repo),
            getConfigUseCase = GetDraftMeasureConfigUseCase(repo)
        )
    }

    fun createBottomViewsRepository(legacyController: ChatActivityBottomViewsVisibilityController? = null): BottomViewsVisibilityRepository {
        return LegacyBottomViewsVisibilityRepository(legacyController)
    }

    val bottomViewsRepository: BottomViewsVisibilityRepository by lazy {
        LegacyBottomViewsVisibilityRepository()
    }

    val getBottomViewVisibilityUseCase: GetBottomViewVisibilityUseCase
        get() = GetBottomViewVisibilityUseCase(bottomViewsRepository)

    val setBottomViewVisibleUseCase: SetBottomViewVisibleUseCase
        get() = SetBottomViewVisibleUseCase(bottomViewsRepository)

    val getPriorityBottomContainerUseCase: GetPriorityBottomContainerUseCase
        get() = GetPriorityBottomContainerUseCase(bottomViewsRepository)

    val getBottomViewsStateUseCase: GetBottomViewsStateUseCase
        get() = GetBottomViewsStateUseCase(bottomViewsRepository)

    val observeBottomViewsVisibilityUseCase: ObserveBottomViewsVisibilityUseCase
        get() = ObserveBottomViewsVisibilityUseCase(bottomViewsRepository)

    private var cachedBottomViewsViewModel: BottomViewsViewModel? = null

    val bottomViewsViewModel: BottomViewsViewModel
        get() {
            var vm = cachedBottomViewsViewModel
            if (vm == null) {
                vm = createBottomViewsViewModel()
                cachedBottomViewsViewModel = vm
            }
            return vm
        }

    fun createBottomViewsViewModel(legacyController: ChatActivityBottomViewsVisibilityController? = null): BottomViewsViewModel {
        val repo = if (legacyController != null) createBottomViewsRepository(legacyController) else bottomViewsRepository
        return BottomViewsViewModel(
            observeBottomViewsVisibilityUseCase = ObserveBottomViewsVisibilityUseCase(repo),
            getBottomViewsStateUseCase = GetBottomViewsStateUseCase(repo),
            setBottomViewVisibleUseCase = SetBottomViewVisibleUseCase(repo)
        )
    }

    fun createRichCaptionRepository(): RichCaptionRepository {
        return LegacyRichCaptionRepository()
    }

    val richCaptionRepository: RichCaptionRepository by lazy {
        LegacyRichCaptionRepository()
    }

    val observeRichCaptionUseCase: ObserveRichCaptionUseCase
        get() = ObserveRichCaptionUseCase(richCaptionRepository)

    val getRichCaptionUseCase: GetRichCaptionUseCase
        get() = GetRichCaptionUseCase(richCaptionRepository)

    val setRichCaptionTextUseCase: SetRichCaptionTextUseCase
        get() = SetRichCaptionTextUseCase(richCaptionRepository)

    val setRichCaptionCreditUseCase: SetRichCaptionCreditUseCase
        get() = SetRichCaptionCreditUseCase(richCaptionRepository)

    val setRichCaptionLockedUseCase: SetRichCaptionLockedUseCase
        get() = SetRichCaptionLockedUseCase(richCaptionRepository)

    val calculateCaptionMeasureWidthUseCase: CalculateCaptionMeasureWidthUseCase
        get() = CalculateCaptionMeasureWidthUseCase(richCaptionRepository)

    val checkCaptionPressHitUseCase: CheckCaptionPressHitUseCase
        get() = CheckCaptionPressHitUseCase(richCaptionRepository)

    val clearRichCaptionUseCase: ClearRichCaptionUseCase
        get() = ClearRichCaptionUseCase(richCaptionRepository)

    private var cachedRichCaptionViewModel: RichCaptionViewModel? = null

    val richCaptionViewModel: RichCaptionViewModel
        get() {
            var vm = cachedRichCaptionViewModel
            if (vm == null) {
                vm = createRichCaptionViewModel()
                cachedRichCaptionViewModel = vm
            }
            return vm
        }

    fun createRichCaptionViewModel(): RichCaptionViewModel {
        return RichCaptionViewModel(
            observeRichCaptionUseCase = observeRichCaptionUseCase,
            getRichCaptionUseCase = getRichCaptionUseCase,
            setRichCaptionTextUseCase = setRichCaptionTextUseCase,
            setRichCaptionCreditUseCase = setRichCaptionCreditUseCase,
            setRichCaptionLockedUseCase = setRichCaptionLockedUseCase,
            clearRichCaptionUseCase = clearRichCaptionUseCase
        )
    }

    fun createEmojiEffectsRepository(): EmojiEffectsRepository {
        return LegacyEmojiEffectsRepository()
    }

    val emojiEffectsRepository: EmojiEffectsRepository by lazy {
        LegacyEmojiEffectsRepository()
    }

    val normalizeEmojiUseCase: NormalizeEmojiUseCase
        get() = NormalizeEmojiUseCase()

    val evaluateEmojiSupportUseCase: EvaluateEmojiSupportUseCase
        get() = EvaluateEmojiSupportUseCase(normalizeEmojiUseCase)

    val recordEmojiTapUseCase: RecordEmojiTapUseCase
        get() = RecordEmojiTapUseCase(emojiEffectsRepository, normalizeEmojiUseCase)

    val encodeEmojiInteractionsJsonUseCase: EncodeEmojiInteractionsJsonUseCase
        get() = EncodeEmojiInteractionsJsonUseCase()

    val decodeEmojiInteractionsJsonUseCase: DecodeEmojiInteractionsJsonUseCase
        get() = DecodeEmojiInteractionsJsonUseCase()

    val calculateEmojiBoundsUseCase: CalculateEmojiBoundsUseCase
        get() = CalculateEmojiBoundsUseCase()

    val calculateEmojiOverlayPositionUseCase: CalculateEmojiOverlayPositionUseCase
        get() = CalculateEmojiOverlayPositionUseCase()

    val evaluateAnimationQuotaUseCase: EvaluateAnimationQuotaUseCase
        get() = EvaluateAnimationQuotaUseCase()

    val observeEmojiEffectsStateUseCase: ObserveEmojiEffectsStateUseCase
        get() = ObserveEmojiEffectsStateUseCase(emojiEffectsRepository)

    val getEmojiEffectsStateUseCase: GetEmojiEffectsStateUseCase
        get() = GetEmojiEffectsStateUseCase(emojiEffectsRepository)

    val startEmojiEffectUseCase: StartEmojiEffectUseCase
        get() = StartEmojiEffectUseCase(emojiEffectsRepository, evaluateAnimationQuotaUseCase)

    val updateEmojiEffectProgressUseCase: UpdateEmojiEffectProgressUseCase
        get() = UpdateEmojiEffectProgressUseCase(emojiEffectsRepository)

    val dismissEmojiEffectUseCase: DismissEmojiEffectUseCase
        get() = DismissEmojiEffectUseCase(emojiEffectsRepository)

    val clearEmojiEffectsUseCase: ClearEmojiEffectsUseCase
        get() = ClearEmojiEffectsUseCase(emojiEffectsRepository)

    private var cachedEmojiEffectsViewModel: EmojiEffectsViewModel? = null

    val emojiEffectsViewModel: EmojiEffectsViewModel
        get() {
            var vm = cachedEmojiEffectsViewModel
            if (vm == null) {
                vm = createEmojiEffectsViewModel()
                cachedEmojiEffectsViewModel = vm
            }
            return vm
        }

    fun createEmojiEffectsViewModel(): EmojiEffectsViewModel {
        return EmojiEffectsViewModel(
            observeEmojiEffectsStateUseCase = observeEmojiEffectsStateUseCase,
            recordEmojiTapUseCase = recordEmojiTapUseCase,
            startEmojiEffectUseCase = startEmojiEffectUseCase,
            updateEmojiEffectProgressUseCase = updateEmojiEffectProgressUseCase,
            dismissEmojiEffectUseCase = dismissEmojiEffectUseCase,
            clearEmojiEffectsUseCase = clearEmojiEffectsUseCase,
            repository = emojiEffectsRepository
        )
    }

    fun createMentionsRepository(): MentionsRepository {
        return LegacyMentionsRepository()
    }

    val mentionsRepository: MentionsRepository by lazy {
        LegacyMentionsRepository()
    }

    val validateUsernameUseCase: ValidateUsernameUseCase
        get() = ValidateUsernameUseCase()

    val parseMentionQueryUseCase: ParseMentionQueryUseCase
        get() = ParseMentionQueryUseCase(validateUsernameUseCase)

    val filterMentionsUseCase: FilterMentionsUseCase
        get() = FilterMentionsUseCase()

    val formatMentionReplacementUseCase: FormatMentionReplacementUseCase
        get() = FormatMentionReplacementUseCase()

    val observeMentionsStateUseCase: ObserveMentionsStateUseCase
        get() = ObserveMentionsStateUseCase(mentionsRepository)

    val getMentionsStateUseCase: GetMentionsStateUseCase
        get() = GetMentionsStateUseCase(mentionsRepository)

    val updateMentionQueryUseCase: UpdateMentionQueryUseCase
        get() = UpdateMentionQueryUseCase(mentionsRepository, parseMentionQueryUseCase)

    val setMentionCandidatesUseCase: SetMentionCandidatesUseCase
        get() = SetMentionCandidatesUseCase(mentionsRepository, filterMentionsUseCase)

    val dismissMentionsUseCase: DismissMentionsUseCase
        get() = DismissMentionsUseCase(mentionsRepository)

    val clearMentionsUseCase: ClearMentionsUseCase
        get() = ClearMentionsUseCase(mentionsRepository)

    private var cachedMentionsViewModel: MentionsViewModel? = null

    val mentionsViewModel: MentionsViewModel
        get() {
            var vm = cachedMentionsViewModel
            if (vm == null) {
                vm = createMentionsViewModel()
                cachedMentionsViewModel = vm
            }
            return vm
        }

    fun createMentionsViewModel(): MentionsViewModel {
        return MentionsViewModel(
            observeMentionsStateUseCase = observeMentionsStateUseCase,
            updateMentionQueryUseCase = updateMentionQueryUseCase,
            setMentionCandidatesUseCase = setMentionCandidatesUseCase,
            formatMentionReplacementUseCase = formatMentionReplacementUseCase,
            dismissMentionsUseCase = dismissMentionsUseCase,
            clearMentionsUseCase = clearMentionsUseCase
        )
    }

    fun createEmojiPickerRepository(): EmojiPickerRepository {
        return LegacyEmojiPickerRepository(resolveAvailablePickerTabsUseCase)
    }

    val emojiPickerRepository: EmojiPickerRepository by lazy {
        createEmojiPickerRepository()
    }

    val resolveAvailablePickerTabsUseCase: ResolveAvailablePickerTabsUseCase
        get() = ResolveAvailablePickerTabsUseCase()

    val filterEmojiItemsUseCase: FilterEmojiItemsUseCase
        get() = FilterEmojiItemsUseCase()

    val filterStickersUseCase: FilterStickersUseCase
        get() = FilterStickersUseCase()

    val filterGifsUseCase: FilterGifsUseCase
        get() = FilterGifsUseCase()

    val observeEmojiPickerStateUseCase: ObserveEmojiPickerStateUseCase
        get() = ObserveEmojiPickerStateUseCase(emojiPickerRepository)

    val getEmojiPickerStateUseCase: GetEmojiPickerStateUseCase
        get() = GetEmojiPickerStateUseCase(emojiPickerRepository)

    val selectPickerTabUseCase: SelectPickerTabUseCase
        get() = SelectPickerTabUseCase(emojiPickerRepository)

    val updatePickerSearchQueryUseCase: UpdatePickerSearchQueryUseCase
        get() = UpdatePickerSearchQueryUseCase(emojiPickerRepository)

    val toggleStickerFavoriteUseCase: ToggleStickerFavoriteUseCase
        get() = ToggleStickerFavoriteUseCase(emojiPickerRepository)

    val clearRecentPickerItemsUseCase: ClearRecentPickerItemsUseCase
        get() = ClearRecentPickerItemsUseCase(emojiPickerRepository)

    private var cachedEmojiPickerViewModel: EmojiPickerViewModel? = null

    val emojiPickerViewModel: EmojiPickerViewModel
        get() {
            var vm = cachedEmojiPickerViewModel
            if (vm == null) {
                vm = createEmojiPickerViewModel()
                cachedEmojiPickerViewModel = vm
            }
            return vm
        }

    fun createEmojiPickerViewModel(): EmojiPickerViewModel {
        return EmojiPickerViewModel(
            observeStateUseCase = observeEmojiPickerStateUseCase,
            getStateUseCase = getEmojiPickerStateUseCase,
            selectTabUseCase = selectPickerTabUseCase,
            updateSearchQueryUseCase = updatePickerSearchQueryUseCase,
            toggleFavoriteUseCase = toggleStickerFavoriteUseCase,
            clearRecentUseCase = clearRecentPickerItemsUseCase,
            repository = emojiPickerRepository
        )
    }

    fun createChatAttachRepository(): ChatAttachRepository {
        return LegacyChatAttachRepository()
    }

    val chatAttachRepository: ChatAttachRepository by lazy {
        createChatAttachRepository()
    }

    val resolveAvailableAttachLayoutsUseCase: ResolveAvailableAttachLayoutsUseCase
        get() = ResolveAvailableAttachLayoutsUseCase()

    val calculateAttachCaptionLimitUseCase: CalculateAttachCaptionLimitUseCase
        get() = CalculateAttachCaptionLimitUseCase()

    val toggleAttachItemSelectionUseCase: ToggleAttachItemSelectionUseCase
        get() = ToggleAttachItemSelectionUseCase(chatAttachRepository)

    val validateSendOptionsUseCase: ValidateSendOptionsUseCase
        get() = ValidateSendOptionsUseCase()

    val observeChatAttachStateUseCase: ObserveChatAttachStateUseCase
        get() = ObserveChatAttachStateUseCase(chatAttachRepository)

    val getChatAttachStateUseCase: GetChatAttachStateUseCase
        get() = GetChatAttachStateUseCase(chatAttachRepository)

    val selectAttachLayoutUseCase: SelectAttachLayoutUseCase
        get() = SelectAttachLayoutUseCase(chatAttachRepository)

    val updateAttachSendOptionsUseCase: UpdateAttachSendOptionsUseCase
        get() = UpdateAttachSendOptionsUseCase(chatAttachRepository)

    val clearAttachSelectionUseCase: ClearAttachSelectionUseCase
        get() = ClearAttachSelectionUseCase(chatAttachRepository)

    val openChatAttachAlertUseCase: OpenChatAttachAlertUseCase
        get() = OpenChatAttachAlertUseCase(chatAttachRepository, resolveAvailableAttachLayoutsUseCase)

    private var cachedChatAttachViewModel: ChatAttachViewModel? = null

    val chatAttachViewModel: ChatAttachViewModel
        get() {
            var vm = cachedChatAttachViewModel
            if (vm == null) {
                vm = createChatAttachViewModel()
                cachedChatAttachViewModel = vm
            }
            return vm
        }

    fun createChatAttachViewModel(): ChatAttachViewModel {
        return ChatAttachViewModel(
            observeStateUseCase = observeChatAttachStateUseCase,
            openAlertUseCase = openChatAttachAlertUseCase,
            selectLayoutUseCase = selectAttachLayoutUseCase,
            toggleSelectionUseCase = toggleAttachItemSelectionUseCase,
            updateSendOptionsUseCase = updateAttachSendOptionsUseCase,
            clearSelectionUseCase = clearAttachSelectionUseCase,
            calculateCaptionLimitUseCase = calculateAttachCaptionLimitUseCase,
            repository = chatAttachRepository
        )
    }

    private var customChatInputRepository: ChatInputRepository? = null

    var chatInputRepository: ChatInputRepository
        get() = customChatInputRepository ?: LegacyChatInputRepository(
            sendButtonStateUseCase = calculateSendButtonStateUseCase
        )
        set(value) {
            customChatInputRepository = value
        }

    val calculateSendButtonStateUseCase: CalculateSendButtonStateUseCase
        get() = CalculateSendButtonStateUseCase()

    val formatTextSelectionUseCase: FormatTextSelectionUseCase
        get() = FormatTextSelectionUseCase()

    val validateVoiceRecordActionUseCase: ValidateVoiceRecordActionUseCase
        get() = ValidateVoiceRecordActionUseCase()

    val resolvePanelVisibilityUseCase: ResolvePanelVisibilityUseCase
        get() = ResolvePanelVisibilityUseCase()

    val observeChatInputStateUseCase: ObserveChatInputStateUseCase
        get() = ObserveChatInputStateUseCase(chatInputRepository)

    val getChatInputStateUseCase: GetChatInputStateUseCase
        get() = GetChatInputStateUseCase(chatInputRepository)

    val setChatInputTextUseCase: SetChatInputTextUseCase
        get() = SetChatInputTextUseCase(chatInputRepository)

    val setChatInputPanelModeUseCase: SetChatInputPanelModeUseCase
        get() = SetChatInputPanelModeUseCase(chatInputRepository)

    val setChatInputReplyUseCase: SetChatInputReplyUseCase
        get() = SetChatInputReplyUseCase(chatInputRepository)

    val clearChatInputReplyUseCase: ClearChatInputReplyUseCase
        get() = ClearChatInputReplyUseCase(chatInputRepository)

    private var cachedChatInputViewModel: ChatInputViewModel? = null

    val chatInputViewModel: ChatInputViewModel
        get() {
            var vm = cachedChatInputViewModel
            if (vm == null) {
                vm = createChatInputViewModel()
                cachedChatInputViewModel = vm
            }
            return vm
        }

    fun createChatInputViewModel(): ChatInputViewModel {
        return ChatInputViewModel(
            repository = chatInputRepository,
            formatUseCase = formatTextSelectionUseCase,
            resolvePanelUseCase = resolvePanelVisibilityUseCase
        )
    }

    val sendMessagesRepository: SendMessagesRepository by lazy {
        LegacySendMessagesRepository(account)
    }

    val sendTextMessageUseCase: SendTextMessageUseCase
        get() = SendTextMessageUseCase(sendMessagesRepository)

    val sendMediaMessageUseCase: SendMediaMessageUseCase
        get() = SendMediaMessageUseCase(sendMessagesRepository)

    val sendMediaAlbumUseCase: SendMediaAlbumUseCase
        get() = SendMediaAlbumUseCase(sendMessagesRepository)

    val forwardMessagesUseCase: ForwardMessagesUseCase
        get() = ForwardMessagesUseCase(sendMessagesRepository)

    val retrySendMessageUseCase: RetrySendMessageUseCase
        get() = RetrySendMessageUseCase(sendMessagesRepository)

    val cancelSendMessageUseCase: CancelSendMessageUseCase
        get() = CancelSendMessageUseCase(sendMessagesRepository)

    val observePendingSendsUseCase: ObservePendingSendsUseCase
        get() = ObservePendingSendsUseCase(sendMessagesRepository)

    private var cachedSendMessagesViewModel: SendMessagesViewModel? = null

    val sendMessagesViewModel: SendMessagesViewModel
        get() {
            var vm = cachedSendMessagesViewModel
            if (vm == null) {
                vm = createSendMessagesViewModel()
                cachedSendMessagesViewModel = vm
            }
            return vm
        }

    fun createSendMessagesViewModel(): SendMessagesViewModel {
        return SendMessagesViewModel(
            sendTextMessageUseCase = sendTextMessageUseCase,
            sendMediaMessageUseCase = sendMediaMessageUseCase,
            sendMediaAlbumUseCase = sendMediaAlbumUseCase,
            forwardMessagesUseCase = forwardMessagesUseCase,
            retrySendMessageUseCase = retrySendMessageUseCase,
            cancelSendMessageUseCase = cancelSendMessageUseCase,
            observePendingSendsUseCase = observePendingSendsUseCase
        )
    }

    val messageCustomParamsRepository: MessageCustomParamsRepository by lazy {
        LegacyMessageCustomParamsRepository(account)
    }

    val checkMessageCustomParamsEmptyUseCase: CheckMessageCustomParamsEmptyUseCase
        get() = CheckMessageCustomParamsEmptyUseCase()

    val mergeMessageCustomParamsUseCase: MergeMessageCustomParamsUseCase
        get() = MergeMessageCustomParamsUseCase()

    val observeMessageCustomParamsStateUseCase: ObserveMessageCustomParamsStateUseCase
        get() = ObserveMessageCustomParamsStateUseCase(messageCustomParamsRepository)

    val getMessageCustomParamsStateUseCase: GetMessageCustomParamsStateUseCase
        get() = GetMessageCustomParamsStateUseCase(messageCustomParamsRepository)

    val getMessageCustomParamsUseCase: GetMessageCustomParamsUseCase
        get() = GetMessageCustomParamsUseCase(messageCustomParamsRepository)

    val setMessageCustomParamsUseCase: SetMessageCustomParamsUseCase
        get() = SetMessageCustomParamsUseCase(messageCustomParamsRepository, mergeMessageCustomParamsUseCase)

    val updateVoiceTranscriptionUseCase: UpdateVoiceTranscriptionUseCase
        get() = UpdateVoiceTranscriptionUseCase(messageCustomParamsRepository)

    val updateMessageTranslationUseCase: UpdateMessageTranslationUseCase
        get() = UpdateMessageTranslationUseCase(messageCustomParamsRepository)

    val updateMessageSummaryUseCase: UpdateMessageSummaryUseCase
        get() = UpdateMessageSummaryUseCase(messageCustomParamsRepository)

    val copyMessageCustomParamsUseCase: CopyMessageCustomParamsUseCase
        get() = CopyMessageCustomParamsUseCase(messageCustomParamsRepository)

    val removeMessageCustomParamsUseCase: RemoveMessageCustomParamsUseCase
        get() = RemoveMessageCustomParamsUseCase(messageCustomParamsRepository)

    val clearAllMessageCustomParamsUseCase: ClearAllMessageCustomParamsUseCase
        get() = ClearAllMessageCustomParamsUseCase(messageCustomParamsRepository)

    private var cachedMessageCustomParamsViewModel: MessageCustomParamsViewModel? = null

    val messageCustomParamsViewModel: MessageCustomParamsViewModel
        get() {
            var vm = cachedMessageCustomParamsViewModel
            if (vm == null) {
                vm = createMessageCustomParamsViewModel()
                cachedMessageCustomParamsViewModel = vm
            }
            return vm
        }

    fun createMessageCustomParamsViewModel(): MessageCustomParamsViewModel {
        return MessageCustomParamsViewModel(
            observeState = observeMessageCustomParamsStateUseCase,
            getParams = getMessageCustomParamsUseCase,
            setParams = setMessageCustomParamsUseCase,
            updateVoice = updateVoiceTranscriptionUseCase,
            updateTranslation = updateMessageTranslationUseCase,
            updateSummary = updateMessageSummaryUseCase,
            copyParams = copyMessageCustomParamsUseCase,
            removeParams = removeMessageCustomParamsUseCase,
            clearAll = clearAllMessageCustomParamsUseCase,
            repository = messageCustomParamsRepository
        )
    }

    private var customBotForumRepository: BotForumRepository? = null

    var botForumRepository: BotForumRepository
        get() = customBotForumRepository ?: LegacyBotForumRepository(account)
        set(value) {
            customBotForumRepository = value
        }

    val deriveTopicNameFromMessageUseCase: DeriveTopicNameFromMessageUseCase
        get() = DeriveTopicNameFromMessageUseCase()

    val resolveStreamingButtonStateUseCase: ResolveStreamingButtonStateUseCase
        get() = ResolveStreamingButtonStateUseCase()

    val observeBotForumStateUseCase: ObserveBotForumStateUseCase
        get() = ObserveBotForumStateUseCase(botForumRepository)

    val getBotForumStateUseCase: GetBotForumStateUseCase
        get() = GetBotForumStateUseCase(botForumRepository)

    val getStreamingSendButtonStateUseCase: GetStreamingSendButtonStateUseCase
        get() = GetStreamingSendButtonStateUseCase(botForumRepository)

    val checkIsStreamingTopicUseCase: CheckIsStreamingTopicUseCase
        get() = CheckIsStreamingTopicUseCase(botForumRepository)

    val saveIsStreamingTopicUseCase: SaveIsStreamingTopicUseCase
        get() = SaveIsStreamingTopicUseCase(botForumRepository)

    val checkHasBotForumDraftsUseCase: CheckHasBotForumDraftsUseCase
        get() = CheckHasBotForumDraftsUseCase(botForumRepository)

    val stopStreamingDraftUseCase: StopStreamingDraftUseCase
        get() = StopStreamingDraftUseCase(botForumRepository)

    val updateBotForumDraftUseCase: UpdateBotForumDraftUseCase
        get() = UpdateBotForumDraftUseCase(botForumRepository)

    val removeMarkedRemovedDraftsUseCase: RemoveMarkedRemovedDraftsUseCase
        get() = RemoveMarkedRemovedDraftsUseCase(botForumRepository)

    val checkNewMessageDraftReplacementUseCase: CheckNewMessageDraftReplacementUseCase
        get() = CheckNewMessageDraftReplacementUseCase(botForumRepository)

    val checkIsBotForumUseCase: CheckIsBotForumUseCase
        get() = CheckIsBotForumUseCase(botForumRepository)

    private var cachedBotForumViewModel: BotForumViewModel? = null

    val botForumViewModel: BotForumViewModel
        get() {
            var vm = cachedBotForumViewModel
            if (vm == null) {
                vm = createBotForumViewModel()
                cachedBotForumViewModel = vm
            }
            return vm
        }

    fun createBotForumViewModel(): BotForumViewModel {
        return BotForumViewModel(
            observeBotForumStateUseCase = observeBotForumStateUseCase,
            getStreamingSendButtonStateUseCase = getStreamingSendButtonStateUseCase,
            checkIsStreamingTopicUseCase = checkIsStreamingTopicUseCase,
            saveIsStreamingTopicUseCase = saveIsStreamingTopicUseCase,
            stopStreamingDraftUseCase = stopStreamingDraftUseCase,
            updateBotForumDraftUseCase = updateBotForumDraftUseCase,
            removeMarkedRemovedDraftsUseCase = removeMarkedRemovedDraftsUseCase,
            checkNewMessageDraftReplacementUseCase = checkNewMessageDraftReplacementUseCase,
            checkHasBotForumDraftsUseCase = checkHasBotForumDraftsUseCase
        )
    }

    private var customEphemeralMessagesRepository: EphemeralMessagesRepository? = null

    var ephemeralMessagesRepository: EphemeralMessagesRepository
        get() = customEphemeralMessagesRepository ?: LegacyEphemeralMessagesRepository(account)
        set(value) {
            customEphemeralMessagesRepository = value
        }

    val parseBotCommandUseCase: ParseBotCommandUseCase
        get() = ParseBotCommandUseCase()

    val getEphemeralCommandBotIdUseCase: GetEphemeralCommandBotIdUseCase
        get() = GetEphemeralCommandBotIdUseCase(ephemeralMessagesRepository)

    val isEphemeralCommandUseCase: IsEphemeralCommandUseCase
        get() = IsEphemeralCommandUseCase(ephemeralMessagesRepository)

    val packEphemeralMessageIdUseCase: PackEphemeralMessageIdUseCase
        get() = PackEphemeralMessageIdUseCase()

    val unpackEphemeralMessageIdUseCase: UnpackEphemeralMessageIdUseCase
        get() = UnpackEphemeralMessageIdUseCase()

    val isEphemeralMessageIdUseCase: IsEphemeralMessageIdUseCase
        get() = IsEphemeralMessageIdUseCase()

    val putWelcomeAnchorBindingUseCase: PutWelcomeAnchorBindingUseCase
        get() = PutWelcomeAnchorBindingUseCase(ephemeralMessagesRepository)

    val removeWelcomeAnchorBindingUseCase: RemoveWelcomeAnchorBindingUseCase
        get() = RemoveWelcomeAnchorBindingUseCase(ephemeralMessagesRepository)

    val getWelcomeAnchorBindingsUseCase: GetWelcomeAnchorBindingsUseCase
        get() = GetWelcomeAnchorBindingsUseCase(ephemeralMessagesRepository)

    val clearAllWelcomeAnchorBindingsUseCase: ClearAllWelcomeAnchorBindingsUseCase
        get() = ClearAllWelcomeAnchorBindingsUseCase(ephemeralMessagesRepository)

    val observeEphemeralMessagesStateUseCase: ObserveEphemeralMessagesStateUseCase
        get() = ObserveEphemeralMessagesStateUseCase(ephemeralMessagesRepository)

    val getEphemeralMessagesStateUseCase: GetEphemeralMessagesStateUseCase
        get() = GetEphemeralMessagesStateUseCase(ephemeralMessagesRepository)

    private var cachedEphemeralMessagesViewModel: EphemeralMessagesViewModel? = null

    val ephemeralMessagesViewModel: EphemeralMessagesViewModel
        get() {
            var vm = cachedEphemeralMessagesViewModel
            if (vm == null) {
                vm = createEphemeralMessagesViewModel()
                cachedEphemeralMessagesViewModel = vm
            }
            return vm
        }

    fun createEphemeralMessagesViewModel(): EphemeralMessagesViewModel {
        return EphemeralMessagesViewModel(
            parseBotCommandUseCase = parseBotCommandUseCase,
            isEphemeralCommandUseCase = isEphemeralCommandUseCase,
            putWelcomeAnchorBindingUseCase = putWelcomeAnchorBindingUseCase,
            removeWelcomeAnchorBindingUseCase = removeWelcomeAnchorBindingUseCase,
            getWelcomeAnchorBindingsUseCase = getWelcomeAnchorBindingsUseCase,
            clearAllWelcomeAnchorBindingsUseCase = clearAllWelcomeAnchorBindingsUseCase,
            observeStateUseCase = observeEphemeralMessagesStateUseCase
        )
    }

    private var customBotKeyboardRepository: BotKeyboardRepository? = null

    var botKeyboardRepository: BotKeyboardRepository
        get() = customBotKeyboardRepository ?: LegacyBotKeyboardRepository(account)
        set(value) {
            customBotKeyboardRepository = value
        }

    val buildBotKeyboardLayoutUseCase: BuildBotKeyboardLayoutUseCase
        get() = BuildBotKeyboardLayoutUseCase()

    val checkIsForceReplyUseCase: CheckIsForceReplyUseCase
        get() = CheckIsForceReplyUseCase(botKeyboardRepository)

    val checkIsButtonWebViewUseCase: CheckIsButtonWebViewUseCase
        get() = CheckIsButtonWebViewUseCase(botKeyboardRepository)

    val resolveCustomButtonTypeUseCase: ResolveCustomButtonTypeUseCase
        get() = ResolveCustomButtonTypeUseCase()

    val getKeyboardForMessageUseCase: GetKeyboardForMessageUseCase
        get() = GetKeyboardForMessageUseCase(botKeyboardRepository)

    val setKeyboardForMessageUseCase: SetKeyboardForMessageUseCase
        get() = SetKeyboardForMessageUseCase(botKeyboardRepository)

    val removeKeyboardForMessageUseCase: RemoveKeyboardForMessageUseCase
        get() = RemoveKeyboardForMessageUseCase(botKeyboardRepository)

    val clearAllKeyboardsUseCase: ClearAllKeyboardsUseCase
        get() = ClearAllKeyboardsUseCase(botKeyboardRepository)

    val recordButtonPressedUseCase: RecordButtonPressedUseCase
        get() = RecordButtonPressedUseCase(botKeyboardRepository)

    val observeBotKeyboardStateUseCase: ObserveBotKeyboardStateUseCase
        get() = ObserveBotKeyboardStateUseCase(botKeyboardRepository)

    val getBotKeyboardStateUseCase: GetBotKeyboardStateUseCase
        get() = GetBotKeyboardStateUseCase(botKeyboardRepository)

    private var cachedBotKeyboardViewModel: BotKeyboardViewModel? = null

    val botKeyboardViewModel: BotKeyboardViewModel
        get() {
            var vm = cachedBotKeyboardViewModel
            if (vm == null) {
                vm = createBotKeyboardViewModel()
                cachedBotKeyboardViewModel = vm
            }
            return vm
        }

    fun createBotKeyboardViewModel(): BotKeyboardViewModel {
        return BotKeyboardViewModel(
            getKeyboardUseCase = getKeyboardForMessageUseCase,
            setKeyboardUseCase = setKeyboardForMessageUseCase,
            removeKeyboardUseCase = removeKeyboardForMessageUseCase,
            clearAllKeyboardsUseCase = clearAllKeyboardsUseCase,
            recordButtonPressedUseCase = recordButtonPressedUseCase,
            observeStateUseCase = observeBotKeyboardStateUseCase
        )
    }

    private var customTextHtmlRepository: org.telegram.messenger.feature.messaging.texthtml.domain.repository.TextHtmlRepository? = null

    var textHtmlRepository: org.telegram.messenger.feature.messaging.texthtml.domain.repository.TextHtmlRepository
        get() = customTextHtmlRepository ?: org.telegram.messenger.feature.messaging.texthtml.data.repository.LegacyTextHtmlRepository()
        set(value) {
            customTextHtmlRepository = value
        }

    val convertToHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ConvertToHtmlUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ConvertToHtmlUseCase(textHtmlRepository)

    val parseFromHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ParseFromHtmlUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ParseFromHtmlUseCase(textHtmlRepository)

    val escapeHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.EscapeHtmlUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.EscapeHtmlUseCase(textHtmlRepository)

    val unescapeHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.UnescapeHtmlUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.UnescapeHtmlUseCase(textHtmlRepository)

    val stripHtmlFormattingUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.StripHtmlFormattingUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.StripHtmlFormattingUseCase(textHtmlRepository)

    val extractHtmlSpansUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ExtractHtmlSpansUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ExtractHtmlSpansUseCase()

    val hasRichFormattingUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.HasRichFormattingUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.HasRichFormattingUseCase()

    val observeTextHtmlStateUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ObserveTextHtmlStateUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ObserveTextHtmlStateUseCase(textHtmlRepository)

    val clearTextHtmlStateUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ClearTextHtmlStateUseCase
        get() = org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ClearTextHtmlStateUseCase(textHtmlRepository)

    private var cachedTextHtmlViewModel: org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlViewModel? = null

    val textHtmlViewModel: org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlViewModel
        get() {
            var vm = cachedTextHtmlViewModel
            if (vm == null) {
                vm = createTextHtmlViewModel()
                cachedTextHtmlViewModel = vm
            }
            return vm
        }

    fun createTextHtmlViewModel(): org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlViewModel {
        return org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlViewModel(
            convertToHtmlUseCase = convertToHtmlUseCase,
            parseFromHtmlUseCase = parseFromHtmlUseCase,
            escapeHtmlUseCase = escapeHtmlUseCase,
            stripHtmlFormattingUseCase = stripHtmlFormattingUseCase,
            observeTextHtmlStateUseCase = observeTextHtmlStateUseCase,
            clearTextHtmlStateUseCase = clearTextHtmlStateUseCase
        )
    }

}
