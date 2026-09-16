package org.telegram.messenger;

import java.util.ArrayList;
import java.util.Collections;

import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.ui.Components.SwipeGestureSettingsView;

/**
 * Controller extracted from MessagesController to modularize Dialog Filters / Chat Folders management.
 * Preserves full upstream logic while bridging into modern clean architecture (FoldersRepository).
 */
public class DialogFiltersController extends BaseController {

    private static final DialogFiltersController[] instances = new DialogFiltersController[UserConfig.MAX_ACCOUNT_COUNT];

    public static DialogFiltersController getInstance(int accountNum) {
        DialogFiltersController local = instances[accountNum];
        if (local == null) {
            synchronized (DialogFiltersController.class) {
                local = instances[accountNum];
                if (local == null) {
                    local = new DialogFiltersController(accountNum);
                    instances[accountNum] = local;
                }
            }
        }
        return local;
    }

    public DialogFiltersController(int accountNum) {
        super(accountNum);
    }

    /**
     * Strangler hook: returns the modern FoldersRepository instance for the given account.
     */
    public static FoldersRepository getFoldersRepository(int account) {
        return org.telegram.messenger.core.di.AccountFeatureContainer.Companion.get(account).getMessaging().getFoldersRepository();
    }

    public FoldersRepository getFoldersRepository() {
        return getFoldersRepository(currentAccount);
    }

    public void loadSuggestedFilters() {
        MessagesController mc = getMessagesController();
        if (mc.loadingSuggestedFilters) {
            return;
        }
        mc.loadingSuggestedFilters = true;

        TLRPC.TL_messages_getSuggestedDialogFilters req = new TLRPC.TL_messages_getSuggestedDialogFilters();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            mc.loadingSuggestedFilters = false;
            mc.suggestedFilters.clear();
            if (response instanceof Vector) {
                mc.suggestedFilters.addAll(((Vector<TLRPC.TL_dialogFilterSuggested>) response).objects);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.suggestedFiltersLoaded);
        }));
    }

    public void loadRemoteFilters(boolean force) {
        loadRemoteFilters(force, null);
    }

    public void loadRemoteFilters(boolean force, Utilities.Callback<Boolean> whenDone) {
        MessagesController mc = getMessagesController();
        if (whenDone != null) {
            mc.onLoadedRemoteFilters = whenDone;
        }
        if (mc.loadingRemoteFilters || !getUserConfig().isClientActivated() || !force && getUserConfig().filtersLoaded) {
            return;
        }
        if (force) {
            getUserConfig().filtersLoaded = false;
            getUserConfig().saveConfig(false);
        }
        TLRPC.TL_messages_getDialogFilters req = new TLRPC.TL_messages_getDialogFilters();
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof Vector) {
                ArrayList<TLRPC.DialogFilter> filters = new ArrayList<>();
                Vector vector = (Vector) response;
                for (int i = 0; i < vector.objects.size(); ++i) {
                    filters.add((TLRPC.DialogFilter) vector.objects.get(i));
                }
                getMessagesStorage().checkLoadedRemoteFilters(filters, () -> {
                    if (mc.onLoadedRemoteFilters != null) {
                        mc.onLoadedRemoteFilters.run(true);
                        mc.onLoadedRemoteFilters = null;
                    }
                });
            } else if (response instanceof TLRPC.TL_messages_dialogFilters) {
                TLRPC.TL_messages_dialogFilters res = (TLRPC.TL_messages_dialogFilters) response;
                if (mc.folderTags != res.tags_enabled) {
                    mc.setFolderTags(res.tags_enabled);
                    AndroidUtilities.runOnUIThread(() -> {
                        getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                    });
                }
                getMessagesStorage().checkLoadedRemoteFilters(res.filters, () -> {
                    if (mc.onLoadedRemoteFilters != null) {
                        mc.onLoadedRemoteFilters.run(true);
                        mc.onLoadedRemoteFilters = null;
                    }
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    mc.loadingRemoteFilters = false;
                    if (mc.onLoadedRemoteFilters != null) {
                        mc.onLoadedRemoteFilters.run(false);
                        mc.onLoadedRemoteFilters = null;
                    }
                });
            }
        });
    }

    public void selectDialogFilter(MessagesController.DialogFilter filter, int index) {
        MessagesController mc = getMessagesController();
        if (mc.selectedDialogFilter[index] == filter) {
            return;
        }
        MessagesController.DialogFilter prevFilter = mc.selectedDialogFilter[index];
        mc.selectedDialogFilter[index] = filter;
        if (mc.selectedDialogFilter[index == 0 ? 1 : 0] == filter) {
            mc.selectedDialogFilter[index == 0 ? 1 : 0] = null;
        }
        if (mc.selectedDialogFilter[index] == null) {
            if (prevFilter != null) {
                prevFilter.dialogs.clear();
                prevFilter.dialogsForward.clear();
            }
        } else {
            mc.sortDialogs(null);
        }
    }

    public void onFilterUpdate(MessagesController.DialogFilter filter) {
        MessagesController mc = getMessagesController();
        for (int a = 0; a < 2; a++) {
            if (mc.selectedDialogFilter[a] == filter) {
                mc.sortDialogs(null);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
                break;
            }
        }
    }

    public void addFilter(MessagesController.DialogFilter filter, boolean atBegin) {
        MessagesController mc = getMessagesController();
        if (atBegin) {
            int order = 254;
            for (int a = 0, N = mc.dialogFilters.size(); a < N; a++) {
                order = Math.min(order, mc.dialogFilters.get(a).order);
            }
            filter.order = order - 1;
            if (mc.dialogFilters.get(0).isDefault()) {
                mc.dialogFilters.add(1, filter);
            } else {
                mc.dialogFilters.add(0, filter);
            }
        } else {
            int order = 0;
            for (int a = 0, N = mc.dialogFilters.size(); a < N; a++) {
                order = Math.max(order, mc.dialogFilters.get(a).order);
            }
            filter.order = order + 1;
            mc.dialogFilters.add(filter);
        }
        mc.dialogFiltersById.put(filter.id, filter);
        if (mc.dialogFilters.size() == 1 && SharedConfig.getChatSwipeAction(currentAccount) != SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS) {
            SharedConfig.updateChatListSwipeSetting(SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS);
        }
        lockFiltersInternal();
    }

    public void removeFilter(MessagesController.DialogFilter filter) {
        MessagesController mc = getMessagesController();
        mc.dialogFilters.remove(filter);
        mc.dialogFiltersById.remove(filter.id);
        getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }

    public void lockFiltersInternal() {
        MessagesController mc = getMessagesController();
        boolean changed = false;
        if (!getUserConfig().isPremium() && mc.dialogFilters.size() - 1 > mc.dialogFiltersLimitDefault) {
            int n = mc.dialogFilters.size() - 1 - mc.dialogFiltersLimitDefault;
            ArrayList<MessagesController.DialogFilter> filtersSortedById = new ArrayList<>(mc.dialogFilters);
            Collections.reverse(filtersSortedById);
            for (int i = 0; i < filtersSortedById.size(); i++) {
                if (i < n) {
                    if (!filtersSortedById.get(i).locked) {
                        changed = true;
                    }
                    filtersSortedById.get(i).locked = true;
                } else {
                    if (filtersSortedById.get(i).locked) {
                        changed = true;
                    }
                    filtersSortedById.get(i).locked = false;
                }
            }
        }
        if (changed) {
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
        }
    }
}
