#!/usr/bin/env python3
"""Train a tiny logistic model: Indian SMS is a real payment vs not.

Writes Kotlin weights the app loads at parse time. Features must stay in lockstep
with SmsRealityFeatures.kt.
"""

from __future__ import annotations

import re
from pathlib import Path

import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report
from sklearn.model_selection import train_test_split

# Name, regex-or-None, kind: body | sender | count
FEATURES: list[tuple[str, str | None, str]] = [
    ("debited", r"\bdebited\b", "body"),
    ("credited", r"\bcredited\b", "body"),
    ("spent", r"\bspent\b", "body"),
    ("purchase", r"\bpurchase\b", "body"),
    ("using_your", r"\busing your\b", "body"),
    ("payment_of", r"\bpayment of\b", "body"),
    ("withdrawn", r"\bwithdrawn\b", "body"),
    ("upi", r"\bupi\b", "body"),
    ("ref_utr", r"\b(upi\s*ref|utr|rrn|ref(?:erence)?(?:\s*no)?|txn(?:n)?(?:\s*id)?)\b", "body"),
    ("account_mask", r"(?:a/?c|acct|account).{0,12}(?:xx+|[x*]{2,})\d{2,}", "body"),
    ("not_you", r"\bnot you\b", "body"),
    ("available_limit", r"\b(?:available|avail|avl|avbl)\s+(?:credit\s+)?(?:limit|lim)\b", "body"),
    ("remaining_limit", r"\b(?:remaining|unused)\s+(?:credit\s+)?limit\b", "body"),
    ("avl_bal", r"\b(?:available|avail|avl|avbl)\s+bal(?:ance)?\b", "body"),
    ("outstanding", r"\boutstanding\b", "body"),
    ("due", r"\b(?:total|min(?:imum)?|amt|payment)\s+due\b|\bdue\s+(?:date|amt|amount)\b", "body"),
    ("statement", r"\bstatement\b", "body"),
    ("reward_points", r"\breward\s+points?\b", "body"),
    ("wallet", r"\bwallet\b", "body"),
    ("offer", r"\b(?:voucher|coupon|promo(?:tion|tional)?|offer)\b", "body"),
    ("cashback", r"\bcashback\b", "body"),
    ("will_be_credited", r"\bwill be credited\b", "body"),
    ("pre_approved", r"\bpre-?approved\b", "body"),
    ("otp", r"\botp\b", "body"),
    ("credit_card", r"\bcredit card\b", "body"),
    ("inr_rs", r"\b(?:inr|rs\.?|₹)\b", "body"),
    ("two_amounts", r"(?:(?:rs\.?|inr|₹|usd)\s*[\d,]+\.?\d*)", "count_ge_2"),
    ("sender_bank", None, "sender"),
    ("upi_path", r"\bupi/[a-z0-9]+/\d+", "body"),
    ("blockupi", r"\bblockupi\b", "body"),
    ("expiry_offer", r"\b(?:till|until|valid|expir)", "body"),
    ("limit_changed", r"\blimit\s+has been\b|\blimit\s+(?:increased|decreased|revised|enhanced)\b", "body"),
    ("overdue", r"\boverdue\b", "body"),
    ("usd", r"\b(?:usd|dollars?)\b", "body"),
    ("received_from", r"\breceived from\b", "body"),
    ("emi", r"\bemi\b", "body"),
    ("generated", r"\bgenerated\b", "body"),
    ("unlock_win", r"\b(?:unlock|win|grab|hurry)\b", "body"),
]

BANK_SENDER = re.compile(
    r"\b(?:ax-|vm-|vk-|ad-|jd-|bk-|hdfc|sbi|axis|icici|kotak|pnb|bob|yesbk|indus)\b",
    re.I,
)


def extract(body: str, sender: str = "") -> list[float]:
    text = body.lower()
    addr = (sender or "").lower()
    row: list[float] = []
    for _name, pattern, kind in FEATURES:
        if kind == "sender":
            row.append(1.0 if BANK_SENDER.search(addr) or BANK_SENDER.search(text) else 0.0)
        elif kind == "count_ge_2":
            row.append(1.0 if len(re.findall(pattern, text, flags=re.I)) >= 2 else 0.0)
        else:
            row.append(1.0 if re.search(pattern, text, flags=re.I) else 0.0)
    return row


def examples() -> tuple[list[tuple[str, str]], list[int]]:
    """label 1 = not a payment, 0 = real payment."""
    real: list[tuple[str, str]] = [
        ("AX-AXISBK", "INR 30.00 debited A/c no. XX2073 22-09-26, 20:41:05 UPI/P2M/347484353597/SHREE MEDICAL STORE Not you? SMS BLOCKUPI Axis Bank"),
        ("HDFCBK", "HDFC Bank: Rs.250.00 debited from a/c XX1234 on 15-09-26 to VPA swiggy@paytm UPI Ref 376598505975. Not you? Call 18002586161"),
        ("VM-HDFCBK", "Your a/c XX9876 is credited with INR 5,000.00 on 15-09-2026 by UPI Ref No. 451236987410"),
        ("AX-AXISBK", "INR 1,000.00 credited A/c no. XX2073 21-09-26, 09:15:00 UPI/P2A/333333333333/SALARY CREDIT Axis Bank"),
        ("VK-SBIINB", "SBI: Debited INR 120.00 on 15Sep26 to ZOMATO. UPI:432109876543. A/c X1234"),
        ("JD-ICICIB", "Rs.1,499.00 spent on your ICICI Bank Credit Card XX7788 at ZOMATO. Avl limit Rs.80,000.00"),
        ("HDFCBK", "Thank you for using your HDFC Bank Credit Card ending 1234 for Rs.25.00 at AMAZON on 23-09-26. Available limit: Rs 1,38,717.00"),
        ("AX-AXISBK", "Payment of USD 25.00 made on your Axis Bank Credit Card XX1234 at NETFLIX. Remaining limit for the card is 1,38,717 INR"),
        ("AX-AXISBK", "INR 150.50 debited A/c no. XX2073 22-09-26, 11:30:00 UPI/P2M/222222222222/STORE TWO Axis Bank"),
        ("VM-HDFCBK", "Rs 540.00 withdrawn from a/c XX4521 at ATM. Avl bal Rs 12,200. Not you?"),
        ("BK-KOTAKB", "INR 2,199.00 debited from A/c XX7788 UPI/P2M/998877665544/NETFLIX Kotak Bank"),
        ("AD-SBIPAY", "Rs.89.00 paid to VPA blinkit@ybl UPI Ref 112233445566 A/c XX1234"),
        ("AX-AXISBK", "INR 8,500.00 credited A/c no. XX2073 UPI/P2A/556677889900/SALARY CREDIT"),
        ("VK-SBIINB", "Your a/c XX2073 is credited with INR 250.00 by refund UPI Ref 778899001122"),
        ("JD-ICICIB", "ICICI Bank: Rs 3,200.00 debited from a/c XX9876 towards IRCTC UPI 667788990011. Not you?"),
        ("HDFCBK", "Rs.75.00 spent on HDFC Bank Debit Card XX1234 at BIGBASKET. Avl bal Rs 4,100"),
        ("AX-KOTAKB", "INR 420.00 debited A/c XX4521 01-08-26, 19:02:11 UPI/P2M/121212121212/UBER INDIA"),
        ("VM-HDFCBK", "Thank you for using your card ending 4521 for Rs.1,250.00 at FLIPKART"),
        ("JD-ICICIB", "Purchase of INR 699.00 on Card XX7788 at MYNTRA. Not you? Call 1800"),
        ("AX-AXISBK", "INR 49.00 debited A/c no. XX1234 UPI/P2M/343434343434/ZEPTO Axis Bank"),
        ("VK-SBIINB", "SBI: Rs 2,000 sent to VPA friend@oksbi UPI Ref 909090909090 A/c XX2073"),
        ("AD-IOBCRD", "Rs.1,100.00 spent on your Credit Card XX8899 at BOOKMYSHOW"),
        ("HDFCBK", "HDFC Bank: INR 15,000.00 credited to a/c XX1234 salary NEFT UTR 123456789012"),
        ("AX-AXISBK", "INR 60.00 debited A/c XX2073 UPI/P2M/454545454545/BPCL PETROL Not you? BLOCKUPI"),
        ("VM-YESBK", "Rs 350.00 debited from a/c XX7788 to VPA zomato@paytm UPI Ref 565656565656"),
    ]
    not_real: list[tuple[str, str]] = [
        ("AX-AXISBK", "Your Axis Bank Credit Card XX1234 available limit is INR 1,38,717.00"),
        ("HDFCBK", "Your HDFC Bank Credit Card XX1234 Total Due: Rs 12,000.00. Min Due: Rs 600.00. Due Date 05-10-26"),
        ("HDFCBK", "Your HDFC Bank Credit Card credit limit has been increased to Rs 2,00,000"),
        ("LM-LENSKT", "Lenskart is eager to serve you, Rs. 1000 credited in your wallet till 8 Dec. Shop now to unlock your reward."),
        ("VM-HDFCBK", "Your remaining credit limit for the card is 1,38,717 INR"),
        ("AX-AXISBK", "Statement is generated for your credit card. Total amount due Rs 18,450. Pay by 05-10-26"),
        ("JD-ICICIB", "ICICI Bank: Min amount due Rs 1,200. Outstanding Rs 24,000. Ignore if paid."),
        ("AD-REWARD", "You have 2500 reward points worth Rs 625. Redeem now."),
        ("LM-AMAZON", "Flat Rs 200 cashback. Offer valid till 30 Sep. Hurry up!"),
        ("VK-SBIINB", "Your OTP for login is 482910. Do not share with anyone."),
        ("AX-AXISBK", "Credit limit has been revised to Rs 3,00,000 on card XX1234"),
        ("HDFCBK", "Payment due of Rs 9,800 on your credit card. Pay to avoid late fee."),
        ("LM-SWIGGY", "Grab Rs 150 off. Coupon SWIGGY150. Limited period offer."),
        ("JD-ICICIB", "Your card XX7788 unused limit is Rs 80,000"),
        ("VM-HDFCBK", "EMI of Rs 4,500 is due on 02-10-26 for your loan account"),
        ("AX-KOTAKB", "Pre-approved personal loan of Rs 2,00,000. Click to apply."),
        ("LM-PAYTM", "Rs 50 will be credited to your wallet. Valid until 8 Dec."),
        ("AD-BILL", "Your electricity bill of Rs 1,340 is generated. Due date 12 Oct."),
        ("VM-HDFCBK", "Card XX1234 is blocked. Available limit Rs 0. Call 1800 if not you."),
        ("AX-AXISBK", "Outstanding amount Rs 32,100. Overdue. Pay immediately."),
        ("LM-MYNTRA", "Unlock Rs 500. Exclusive offer. Don't miss."),
        ("JD-ICICIB", "Available credit limit Rs 1,10,000 on ICICI Bank Credit Card XX7788"),
        ("HDFCBK", "Avl lim Rs.1,38,717.00 on your HDFC Bank Credit Card"),
        ("VK-SBIINB", "Reward points 1200 credited. Expiring soon."),
        ("LM-PHONEP", "Win Rs 1000 cashback. Promotional offer. Terms apply."),
        ("AX-AXISBK", "Your Axis Bank Credit Card statement generated. Amt due Rs 7,200."),
        ("AD-INSURE", "Policy premium Rs 8,900 due on 15-10-26. This is a reminder."),
        ("VM-KOTAKB", "Unused credit limit on card XX4521 is INR 95,000"),
        ("LM-ZOMATO", "Voucher of Rs 200 in your wallet till 20 Oct. Shop now."),
        ("JD-ICICIB", "Minimum due Rs 800. Payment due date 07-10-26."),
    ]
    # Repeat with light wording jitter so the hyperplane is not brittle.
    extra_real = [
        (s, b.replace("INR", "Rs.")) for s, b in real
    ]
    extra_fake = [
        (s, b.replace("Rs ", "INR ").replace("Rs.", "INR ")) for s, b in not_real
    ]
    texts = real + extra_real + not_real + extra_fake
    labels = [0] * (len(real) + len(extra_real)) + [1] * (len(not_real) + len(extra_fake))
    return texts, labels


def main() -> None:
    texts, labels = examples()
    x = np.array([extract(body, sender) for sender, body in texts], dtype=np.float32)
    y = np.array(labels, dtype=np.int32)
    x_train, x_test, y_train, y_test = train_test_split(
        x, y, test_size=0.25, random_state=7, stratify=y,
    )
    model = LogisticRegression(max_iter=400, C=1.5, solver="lbfgs")
    model.fit(x_train, y_train)
    print(classification_report(y_test, model.predict(x_test), target_names=["real", "not_real"]))
    print("full-set accuracy", model.score(x, y))

    weights = model.coef_[0].astype(float)
    bias = float(model.intercept_[0])
    names = [name for name, _, _ in FEATURES]
    out = Path(__file__).resolve().parents[1] / (
        "app/src/main/java/com/shashanksoni/kharchahogayabhai/sms/SmsRealityModelWeights.kt"
    )
    weight_lines = ",\n        ".join(f"{w:.8f}f" for w in weights)
    name_lines = ",\n        ".join(f"\"{n}\"" for n in names)
    out.write_text(
        f"""package com.shashanksoni.kharchahogayabhai.sms

/** Generated by scripts/train_sms_reality_model.py. Do not edit by hand. */
internal object SmsRealityModelWeights {{
    const val BIAS: Float = {bias:.8f}f
    const val NOT_A_PAYMENT_THRESHOLD: Float = 0.72f
    val WEIGHTS: FloatArray = floatArrayOf(
        {weight_lines},
    )
    val FEATURE_NAMES: Array<String> = arrayOf(
        {name_lines},
    )
}}
""",
        encoding="utf-8",
    )
    print(f"wrote {out} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
