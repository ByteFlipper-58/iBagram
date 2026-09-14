package org.telegram.ui;

public interface MainTabsActivityController {
    void setTabsVisible(boolean visible);

    static org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository getMainTabsRepository(int account) {
        try {
            return org.telegram.messenger.core.di.AccountFeatureContainer.get(account).getSystem().getMainTabsRepository();
        } catch (Throwable ignore) {
            return null;
        }
    }

    static org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository getMainTabsRepository() {
        return getMainTabsRepository(org.telegram.messenger.UserConfig.selectedAccount);
    }
}
