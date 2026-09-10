package org.telegram.messenger.feature.business.payments.presentation

sealed class PaymentsEvent {
    object RefreshAll : PaymentsEvent()
    object RefreshBalance : PaymentsEvent()
    object RefreshTransactions : PaymentsEvent()
    object RefreshSubscriptions : PaymentsEvent()
}
