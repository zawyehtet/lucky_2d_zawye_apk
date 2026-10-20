#!/usr/bin/env python3
"""
Standalone Offline Admin Activation Key Generator
Uses HMAC-SHA256(Device_ID, "Lucky3D_Super_Secret_Key_2026!#") -> XXXX-XXXX-XXXX
"""

import hashlib
import hmac
import re
import sys

SHARED_SECRET = "Lucky3D_Super_Secret_Key_2026!#"


def normalize_device_id(device_id: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", device_id.strip().upper())


def generate_activation_key(device_id: str, secret: str = SHARED_SECRET) -> str:
    normalized_id = normalize_device_id(device_id)
    if not normalized_id:
        raise ValueError("Device ID cannot be empty.")

    digest = hmac.new(
        secret.encode("utf-8"),
        normalized_id.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest().upper()

    raw12 = digest[:12]
    return f"{raw12[0:4]}-{raw12[4:8]}-{raw12[8:12]}"


if __name__ == "__main__":
    dev_id = sys.argv[1] if len(sys.argv) > 1 else input("Enter Client Device ID: ")
    print("Activation Key:", generate_activation_key(dev_id))
