#!/usr/bin/env python3
"""
Standalone Offline Admin Activation Key Generator for Lucky 2D Android App.
Uses HMAC-SHA256(Device_ID, SHARED_SECRET) to derive 12-char keys: XXXX-XXXX-XXXX
"""

import hashlib
import hmac
import re
import sys

SHARED_SECRET = "LUCKY2D-OFFLINE-MASTER-SECRET-2026-KEY"


def normalize_device_id(device_id: str) -> str:
    cleaned = re.sub(r"[^A-Z0-9]", "", device_id.strip().upper())
    return cleaned


def generate_activation_key(device_id: str) -> str:
    normalized_id = normalize_device_id(device_id)
    if not normalized_id:
        raise ValueError("Device ID cannot be empty.")

    digest = hmac.new(
        SHARED_SECRET.encode("utf-8"),
        normalized_id.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest().upper()

    raw12 = digest[:12]
    return f"{raw12[0:4]}-{raw12[4:8]}-{raw12[8:12]}"


if __name__ == "__main__":
    if len(sys.argv) > 1:
        dev_id = sys.argv[1]
    else:
        dev_id = input("Enter Client Device ID: ")

    try:
        key = generate_activation_key(dev_id)
        print(f"\nNormalized Device ID : {normalize_device_id(dev_id)}")
        print(f"Activation Key       : {key}\n")
    except ValueError as err:
        print(f"Error: {err}")
