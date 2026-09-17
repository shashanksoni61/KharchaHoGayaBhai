package com.shashanksoni.kharchahogayabhai.domain.model

/** How the money moved. [UNKNOWN] is used when a source does not say. */
enum class PaymentMethod {
    UPI,
    CARD,
    ATM,
    NET_BANKING,
    IMPS,
    NEFT,
    RTGS,
    AUTO_DEBIT,
    WALLET,
    CASH,
    CHEQUE,
    UNKNOWN,
}
