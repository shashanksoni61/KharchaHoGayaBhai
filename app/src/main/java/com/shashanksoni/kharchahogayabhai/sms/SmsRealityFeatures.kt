package com.shashanksoni.kharchahogayabhai.sms

/**
 * Hand-built SMS features for [SmsRealityClassifier].
 *
 * Keep the regexes in lockstep with `scripts/train_sms_reality_model.py`.
 */
internal object SmsRealityFeatures {
    fun extract(body: String, address: String?): FloatArray {
        val text = body.lowercase()
        val addr = address.orEmpty().lowercase()
        return floatArrayOf(
            flag(text, DEBITED),
            flag(text, CREDITED),
            flag(text, SPENT),
            flag(text, PURCHASE),
            flag(text, USING_YOUR),
            flag(text, PAYMENT_OF),
            flag(text, WITHDRAWN),
            flag(text, UPI),
            flag(text, REF_UTR),
            flag(text, ACCOUNT_MASK),
            flag(text, NOT_YOU),
            flag(text, AVAILABLE_LIMIT),
            flag(text, REMAINING_LIMIT),
            flag(text, AVL_BAL),
            flag(text, OUTSTANDING),
            flag(text, DUE),
            flag(text, STATEMENT),
            flag(text, REWARD_POINTS),
            flag(text, WALLET),
            flag(text, OFFER),
            flag(text, CASHBACK),
            flag(text, WILL_BE_CREDITED),
            flag(text, PRE_APPROVED),
            flag(text, OTP),
            flag(text, CREDIT_CARD),
            flag(text, INR_RS),
            if (TWO_AMOUNTS.findAll(text).count() >= 2) 1f else 0f,
            if (BANK_SENDER.containsMatchIn(addr) || BANK_SENDER.containsMatchIn(text)) 1f else 0f,
            flag(text, UPI_PATH),
            flag(text, BLOCKUPI),
            flag(text, EXPIRY_OFFER),
            flag(text, LIMIT_CHANGED),
            flag(text, OVERDUE),
            flag(text, USD),
            flag(text, RECEIVED_FROM),
            flag(text, EMI),
            flag(text, GENERATED),
            flag(text, UNLOCK_WIN),
        )
    }

    private fun flag(text: String, pattern: Regex): Float =
        if (pattern.containsMatchIn(text)) 1f else 0f

    private val DEBITED = Regex("""\bdebited\b""")
    private val CREDITED = Regex("""\bcredited\b""")
    private val SPENT = Regex("""\bspent\b""")
    private val PURCHASE = Regex("""\bpurchase\b""")
    private val USING_YOUR = Regex("""\busing your\b""")
    private val PAYMENT_OF = Regex("""\bpayment of\b""")
    private val WITHDRAWN = Regex("""\bwithdrawn\b""")
    private val UPI = Regex("""\bupi\b""")
    private val REF_UTR = Regex(
        """\b(upi\s*ref|utr|rrn|ref(?:erence)?(?:\s*no)?|txn(?:n)?(?:\s*id)?)\b""",
    )
    private val ACCOUNT_MASK = Regex(
        """(?:a/?c|acct|account).{0,12}(?:xx+|[x*]{2,})\d{2,}""",
    )
    private val NOT_YOU = Regex("""\bnot you\b""")
    private val AVAILABLE_LIMIT = Regex(
        """\b(?:available|avail|avl|avbl)\s+(?:credit\s+)?(?:limit|lim)\b""",
    )
    private val REMAINING_LIMIT = Regex(
        """\b(?:remaining|unused)\s+(?:credit\s+)?limit\b""",
    )
    private val AVL_BAL = Regex(
        """\b(?:available|avail|avl|avbl)\s+bal(?:ance)?\b""",
    )
    private val OUTSTANDING = Regex("""\boutstanding\b""")
    private val DUE = Regex(
        """\b(?:total|min(?:imum)?|amt|payment)\s+due\b|\bdue\s+(?:date|amt|amount)\b""",
    )
    private val STATEMENT = Regex("""\bstatement\b""")
    private val REWARD_POINTS = Regex("""\breward\s+points?\b""")
    private val WALLET = Regex("""\bwallet\b""")
    private val OFFER = Regex("""\b(?:voucher|coupon|promo(?:tion|tional)?|offer)\b""")
    private val CASHBACK = Regex("""\bcashback\b""")
    private val WILL_BE_CREDITED = Regex("""\bwill be credited\b""")
    private val PRE_APPROVED = Regex("""\bpre-?approved\b""")
    private val OTP = Regex("""\botp\b""")
    private val CREDIT_CARD = Regex("""\bcredit card\b""")
    private val INR_RS = Regex("""\b(?:inr|rs\.?|₹)\b""")
    private val TWO_AMOUNTS = Regex("""(?:(?:rs\.?|inr|₹|usd)\s*[\d,]+\.?\d*)""")
    private val BANK_SENDER = Regex(
        """\b(?:ax-|vm-|vk-|ad-|jd-|bk-|hdfc|sbi|axis|icici|kotak|pnb|bob|yesbk|indus)\b""",
    )
    private val UPI_PATH = Regex("""\bupi/[a-z0-9]+/\d+""")
    private val BLOCKUPI = Regex("""\bblockupi\b""")
    private val EXPIRY_OFFER = Regex("""\b(?:till|until|valid|expir)""")
    private val LIMIT_CHANGED = Regex(
        """\blimit\s+has been\b|\blimit\s+(?:increased|decreased|revised|enhanced)\b""",
    )
    private val OVERDUE = Regex("""\boverdue\b""")
    private val USD = Regex("""\b(?:usd|dollars?)\b""")
    private val RECEIVED_FROM = Regex("""\breceived from\b""")
    private val EMI = Regex("""\bemi\b""")
    private val GENERATED = Regex("""\bgenerated\b""")
    private val UNLOCK_WIN = Regex("""\b(?:unlock|win|grab|hurry)\b""")
}
