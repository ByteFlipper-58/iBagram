package org.telegram.ui.Components.inset;

import org.telegram.messenger.AndroidUtilities;

public interface WindowInsetsInAppController {

    default void requestInAppKeyboardHeightIncludeNavbar(int inAppKeyboardHeight) {
        if (inAppKeyboardHeight > 0) {
            requestInAppKeyboardHeight(inAppKeyboardHeight + AndroidUtilities.navigationBarHeight);
        } else {
            resetInAppKeyboardHeight(true);
        }
    }

    void requestInAppKeyboardHeight(int inAppKeyboardHeight);
    void resetInAppKeyboardHeight(boolean waitKeyboardOpen);

    static org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository getKeyboardInsetsRepository(int account) {
        try {
            return org.telegram.messenger.core.di.AccountFeatureContainer.get(account).getSystem().getKeyboardInsetsRepository();
        } catch (Throwable ignore) {
            return null;
        }
    }

    static org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository getKeyboardInsetsRepository() {
        return getKeyboardInsetsRepository(org.telegram.messenger.UserConfig.selectedAccount);
    }
}
