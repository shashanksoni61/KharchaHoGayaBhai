#!/usr/bin/env python3
"""Seed emulator inbox with Axis-style multi-line bank/UPI SMS.

Bodies match real alerts (amount, A/c, stamped date/time, UPI/P2M/ref/merchant).
By default seeds a fixed number of messages per month with a credit/debit split,
so the transaction list has realistic month-over-month history.

Defaults: May–Sep 2026, 30 messages/month (5 credit + 25 debit each).
September stamps are capped at --cutoff (today) so nothing lands in the future.

Usage:
  python3 scripts/seed_emulator_sms.py
  python3 scripts/seed_emulator_sms.py --months 2026-05 2026-06 --per-month 30 --credits 5
"""

from __future__ import annotations

import argparse
import calendar
import random
import socket
import time
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

IST = ZoneInfo("Asia/Kolkata")
DEFAULT_CUTOFF = datetime(2026, 9, 23, 11, 43, 0, tzinfo=IST)
DEFAULT_MONTHS = ["2026-05", "2026-06", "2026-07", "2026-08", "2026-09"]

MERCHANTS = [
    "SHREE MEDICAL STORE", "SWIGGY", "ZOMATO", "AMAZON PAY", "FLIPKART",
    "UBER INDIA", "IRCTC RAIL", "BIGBASKET", "BLINKIT", "MYNTRA",
    "BOOKMYSHOW", "OLA CABS", "ZEPTO", "DMART INDIA", "RELIANCE JIO",
    "BPCL PETROL", "INDANE GAS", "NETFLIX",
]
CREDIT_SOURCES = ["SALARY CREDIT", "REFUND AMAZON", "INTEREST PAID", "UPI RECEIVED", "CASHBACK"]
SENDERS = ["AX-AXISBK", "AX-AXISBK", "AX-AXISBK", "VM-HDFCBK", "VK-SBIINB", "JD-ICICIB", "AX-KOTAKB"]
ACCOUNTS = ["XX2073", "XX1234", "XX9876", "XX4521", "XX7788"]


def fmt_amount(amount: float) -> str:
    whole, frac = f"{amount:.2f}".split(".")
    w = int(whole)
    return f"{w:,}.{frac}" if w >= 1000 else f"{w}.{frac}"


def random_datetime_in_month(year: int, month: int, cutoff: datetime, rng: random.Random) -> datetime:
    last_day = calendar.monthrange(year, month)[1]
    start = datetime(year, month, 1, 0, 0, 0, tzinfo=IST)
    end = datetime(year, month, last_day, 23, 59, 59, tzinfo=IST)
    if end > cutoff:
        end = cutoff
    span = int((end - start).total_seconds())
    if span <= 0:
        return start
    return start + timedelta(seconds=rng.randint(0, span))


def make_message(when: datetime, is_credit: bool, rng: random.Random) -> tuple[str, str]:
    amount = round(rng.uniform(10, 8500), 2)
    amt = fmt_amount(amount)
    ref = f"{rng.randint(10**11, 10**12 - 1)}"
    sender = rng.choice(SENDERS)
    acct = rng.choice(ACCOUNTS)
    stamp = when.strftime("%d-%m-%y, %H:%M:%S")
    if is_credit:
        merchant = rng.choice(CREDIT_SOURCES)
        verb, kind = "credited", "P2A"
    else:
        merchant = rng.choice(MERCHANTS)
        verb, kind = "debited", "P2M"
    body = (
        f"INR {amt} {verb} "
        f"A/c no. {acct} "
        f"{stamp} "
        f"UPI/{kind}/{ref}/{merchant} "
        f"Not you? SMS BLOCKUPI Cust ID to 919951860002 "
        f"Axis Bank"
    )
    return sender, body


def build_plan(months: list[str], per_month: int, credits: int, cutoff: datetime,
               rng: random.Random) -> list[tuple[datetime, bool]]:
    plan: list[tuple[datetime, bool]] = []
    for m in months:
        year, month = map(int, m.split("-"))
        flags = [True] * credits + [False] * (per_month - credits)
        rng.shuffle(flags)
        for is_credit in flags:
            plan.append((random_datetime_in_month(year, month, cutoff, rng), is_credit))
    # Deliver oldest first so inbox ids increase with time.
    plan.sort(key=lambda t: t[0])
    return plan


def read_ok(sock: socket.socket, timeout: float = 5.0) -> str:
    sock.settimeout(timeout)
    buf = b""
    while True:
        chunk = sock.recv(4096)
        if not chunk:
            break
        buf += chunk
        if b"\nOK" in buf or buf.strip().endswith(b"OK") or b"\nKO" in buf:
            break
    return buf.decode("utf-8", errors="replace")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--months", nargs="+", default=DEFAULT_MONTHS, help="YYYY-MM list")
    parser.add_argument("--per-month", type=int, default=30)
    parser.add_argument("--credits", type=int, default=5, help="Credit messages per month")
    parser.add_argument("--cutoff", default=DEFAULT_CUTOFF.strftime("%Y-%m-%dT%H:%M:%S"),
                        help="Latest allowed stamp (ISO, IST)")
    parser.add_argument("--port", type=int, default=5554)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    if args.credits > args.per_month:
        raise SystemExit("--credits cannot exceed --per-month")
    cutoff = datetime.fromisoformat(args.cutoff).replace(tzinfo=IST)
    rng = random.Random(args.seed)
    plan = build_plan(args.months, args.per_month, args.credits, cutoff, rng)

    token = (Path.home() / ".emulator_console_auth_token").read_text().strip()
    sock = socket.create_connection(("127.0.0.1", args.port), timeout=10)
    print(sock.recv(4096).decode(errors="replace").splitlines()[0])
    sock.sendall(f"auth {token}\n".encode())
    if "OK" not in read_ok(sock):
        raise SystemExit("emulator console auth failed")

    credits = debits = 0
    t0 = time.time()
    for i, (when, is_credit) in enumerate(plan, start=1):
        sender, body = make_message(when, is_credit, rng)
        credits += is_credit
        debits += not is_credit
        sock.sendall(f"sms send {sender} {body}\n".encode())
        if "OK" not in read_ok(sock, timeout=15.0):
            raise SystemExit(f"sms send failed at {i}")
        if i % 30 == 0:
            print(f"queued {i}/{len(plan)}")

    sock.sendall(b"quit\n")
    sock.close()
    print(
        f"Queued {len(plan)} SMS across {len(args.months)} months "
        f"(credits={credits}, debits={debits}) in {time.time() - t0:.1f}s.\n"
        f"Stamps span {plan[0][0].date()}..{plan[-1][0].date()} (<= {cutoff.isoformat()}).\n"
        "Delivery is async — wait a bit, then:\n"
        "  adb shell content query --uri content://sms/inbox --projection _id | grep -c '^Row:'"
    )


if __name__ == "__main__":
    main()
