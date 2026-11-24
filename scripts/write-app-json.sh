#!/usr/bin/env bash
# release/app.json: the phone app the site's "Swadaya App" button hands out, so the page can state
# its version, size and SHA-256 without anyone copying numbers by hand.
#
# Usage: write-app-json.sh <versionName> <versionCode> <apk>
set -euo pipefail

VERSION="${1:?versionName}"
CODE="${2:?versionCode}"
APK="${3:?path to the built APK}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RELEASE="$ROOT/release"
BASE_URL="${SITE_URL:-https://frhnkrnwn.github.io/SwadayaTV}"

[ -f "$APK" ] || { echo "no APK at $APK" >&2; exit 1; }
APK_SIZE="$(wc -c <"$APK" | tr -d ' ')"
APK_SHA="$(sha256sum "$APK" | cut -d' ' -f1)"

python3 - "$RELEASE/app.json" "$VERSION" "$CODE" "$BASE_URL" "$APK_SIZE" "$APK_SHA" <<'PY'
import json
import sys
from datetime import date

path, version, code, base, size, sha = sys.argv[1:7]
entry = {
    "versionName": version,
    "versionCode": int(code),
    "releaseDate": date.today().isoformat(),
    "apkUrl": base.rstrip("/") + "/release/swadaya-app.apk",
    "apkName": "swadaya-app.apk",
    "apkSize": int(size),
    "apkSha256": sha,
}
with open(path, "w", encoding="utf-8") as f:
    json.dump(entry, f, indent=2, ensure_ascii=False)
    f.write("\n")
print("app.json: %s, %s bytes, sha256 %s" % (version, size, sha[:12]))
PY
