package com.shashanksoni.kharchahogayabhai.ui.util

import androidx.annotation.StringRes
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource

/**
 * Display labels for domain enums. Enum names are storage identifiers, never
 * shown to users, so the two can change independently.
 */
@get:StringRes
val TransactionSource.labelRes: Int
    get() = when (this) {
        TransactionSource.SMS -> R.string.source_sms
        TransactionSource.CSV -> R.string.source_csv
        TransactionSource.PDF -> R.string.source_pdf
        TransactionSource.MANUAL -> R.string.source_manual
    }

@get:StringRes
val PaymentMethod.labelRes: Int
    get() = when (this) {
        PaymentMethod.UPI -> R.string.payment_method_upi
        PaymentMethod.CARD -> R.string.payment_method_card
        PaymentMethod.ATM -> R.string.payment_method_atm
        PaymentMethod.NET_BANKING -> R.string.payment_method_net_banking
        PaymentMethod.IMPS -> R.string.payment_method_imps
        PaymentMethod.NEFT -> R.string.payment_method_neft
        PaymentMethod.RTGS -> R.string.payment_method_rtgs
        PaymentMethod.AUTO_DEBIT -> R.string.payment_method_auto_debit
        PaymentMethod.WALLET -> R.string.payment_method_wallet
        PaymentMethod.CASH -> R.string.payment_method_cash
        PaymentMethod.CHEQUE -> R.string.payment_method_cheque
        PaymentMethod.UNKNOWN -> R.string.payment_method_unknown
    }
