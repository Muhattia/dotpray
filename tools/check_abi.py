#!/usr/bin/env python3
"""Check the APK's native-ABI footprint.

dotpray is pure Java, so the APK carries no lib/<abi>/*.so entries and the
same file runs on 32-bit and 64-bit devices. This guard makes that explicit,
and fails the build if native code is ever added without both halves of an
architecture family.
"""
import sys
import zipfile

# 32-bit ABI -> required 64-bit partner, and the reverse.
PAIRS = {
    "armeabi-v7a": "arm64-v8a",
    "arm64-v8a": "armeabi-v7a",
    "x86": "x86_64",
    "x86_64": "x86",
}


def main(path):
    with zipfile.ZipFile(path) as apk:
        abis = sorted({
            name.split("/")[1]
            for name in apk.namelist()
            if name.startswith("lib/") and name.endswith(".so")
        })

    if not abis:
        print("  universal: no native libraries (32-bit and 64-bit safe)")
        return 0

    print("  native libraries: " + ", ".join(abis))
    for abi in abis:
        if abi not in PAIRS:
            print("  ✗ unsupported ABI: " + abi)
            return 1
        if PAIRS[abi] not in abis:
            print("  ✗ " + abi + " is present without its "
                  + PAIRS[abi] + " counterpart")
            return 1
    print("  both 32-bit and 64-bit variants are present")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: check_abi.py <apk>", file=sys.stderr)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
