#!/usr/bin/env python3
"""Seed emulator inbox with Axis-style multi-line bank/UPI SMS.

Bodies match real alerts (amount, A/c, stamped date/time, UPI/P2M/ref/merchant).
Transaction stamps are at or before 2026-09-22 11:43 Asia/Kolkata.

Usage:
  python3 scripts/seed_emulator_sms.py [--count 1000] [--port 5554]
"""

from __future__ import annotations

import argparse
import random
import socket
import time
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

IST = ZoneInfo("Asia/Kolkata")
CUTOFF = datetime(2026, 9, 22, 11, 43, 0, tzinfo=IST)

MERCHANTS = [
    "SHREE MEDICAL STORE",
    "SWIGGY",
    "ZOMATO",
    "AMAZON PAY",
    "FLIPKART",
    "UBER INDIA",
    "IRCTC RAIL",
    "BIGBASKET",
    "BLINKIT",
    "MYNTRA",
    "BOOKMYSHOW",
    "OLA CABS",
    "ZEPTO",
    "DMART INDIA",
    "RELIANCE JIO",
    "BPCL PETROL",
    "INDANE GAS",
    "NETFLIX",
]
SENDERS = [
    "AX-AXISBK",
    "AX-AXISBK",
    "AX-AXISBK",  # same sender often has many prior alerts
    "VM-HDFCBK",
    "VK-SBIINB",
    "JD-ICICIB",
    "AX-KOTAKB",
]
ACCOUNTS = ["XX2073", "XX1234", "XX9876", "XX4521", "XX7788"]
UPI_KINDS = ["P2M", "P2M", "P2M", "P2A"]


def fmt_amount(amount: float) -> str:
    whole, frac = f"{amount:.2f}".split(".")
    w = int(whole)
    if w >= 1000:
        return f"{w:,}.{frac}"
    return f"{w}.{frac}"


def make_message(i: int, rng: random.Random) -> tuple[str, str]:
    is_credit = i % 5 == 0
    amount = round(rng.uniform(10, 8500), 2)
    amt = fmt_amount(amount)
    ref = f"{rng.randint(10**11, 10**12 - 1)}"
    merchant = rng.choice(MERCHANTS)
    sender = rng.choice(SENDERS)
    acct = rng.choice(ACCOUNTS)
    kind = "P2A" if is_credit else rng.choice(UPI_KINDS)

    # Spread over ~90 days ending at CUTOFF (inclusive).
    seconds_back = rng.randint(0, 90 * 24 * 3600)
    when = CUTOFF - timedelta(seconds=seconds_back)
    if when > CUTOFF:
        when = CUTOFF
    stamp = when.strftime("%d-%m-%y, %H:%M:%S")

    verb = "credited" if is_credit else "debited"
    # Single-line body (emulator console is newline-sensitive); parser accepts spaces.
    body = (
        f"INR {amt} {verb} "
        f"A/c no. {acct} "
        f"{stamp} "
        f"UPI/{kind}/{ref}/{merchant} "
        f"Not you? SMS BLOCKUPI Cust ID to 919951860002 "
        f"Axis Bank"
    )
    return sender, body


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
    parser.add_argument("--count", type=int, default=1000)
    parser.add_argument("--port", type=int, default=5554)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    token = (Path.home() / ".emulator_console_auth_token").read_text().strip()
    rng = random.Random(args.seed)

    sock = socket.create_connection(("127.0.0.1", args.port), timeout=10)
    print(sock.recv(4096).decode(errors="replace").splitlines()[0])
    sock.sendall(f"auth {token}\n".encode())
    if "OK" not in read_ok(sock):
        raise SystemExit("emulator console auth failed")

    credits = debits = 0
    t0 = time.time()
    for i in range(1, args.count + 1):
        sender, body = make_message(i, rng)
        if "credited" in body:
            credits += 1
        else:
            debits += 1
        sock.sendall(f"sms send {sender} {body}\n".encode())
        if "OK" not in read_ok(sock, timeout=15.0):
            raise SystemExit(f"sms send failed at {i}")
        if i % 100 == 0:
            print(f"queued {i}/{args.count}")

    sock.sendall(b"quit\n")
    sock.close()
    print(
        f"Queued {args.count} SMS (credits={credits}, debits={debits}) "
        f"in {time.time() - t0:.1f}s.\n"
        f"Body timestamps <= {CUTOFF.isoformat()}. Delivery is async — wait a few minutes,\n"
        "then: adb shell content query --uri content://sms/inbox --projection _id | grep -c '^Row:'"
    )


if __name__ == "__main__":
    main()
