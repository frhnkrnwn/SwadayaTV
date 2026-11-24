#!/usr/bin/env bash
# One file describes the launcher the download button hands out.
#
# The site serves release/version.json and release/tvhome-latest.apk from the same commit, and the
# launcher's updater reads that JSON instead of api.github.com: the GitHub API allows 60 requests an
# hour per IP, and a shared network (a school, a boarding house) uses them up, so updates used to
# fail there for no visible reason. The same file also feeds the "What's new" page.
#
# Usage: write-version-json.sh <versionName> <versionCode> <build> <tag> <apk>
# The changelog comes from release/notes/<versionName>.txt when that file exists, so the text is
# written once by hand and never regenerated from commits.
set -euo pipefail

VERSION="${1:?versionName}"
CODE="${2:?versionCode}"
BUILD="${3:?build number}"
TAG="${4:?release tag}"
APK="${5:?path to the built APK}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RELEASE="$ROOT/release"
BASE_URL="${SITE_URL:-https://frhnkrnwn.github.io/SwadayaTV}"

[ -f "$APK" ] || { echo "no APK at $APK" >&2; exit 1; }
APK_SIZE="$(wc -c <"$APK" | tr -d ' ')"
APK_SHA="$(sha256sum "$APK" | cut -d' ' -f1)"

NOTES=""
NOTES_FILE="$RELEASE/notes/$VERSION.txt"
if [ -f "$NOTES_FILE" ]; then
  NOTES="$(tr '\n' ' ' <"$NOTES_FILE" | sed -e 's/[[:space:]]\+/ /g' -e 's/^ //' -e 's/ $//')"
fi

mkdir -p "$RELEASE"
python3 - "$RELEASE/version.json" "$RELEASE/releases.json" \
  "$VERSION" "$CODE" "$BUILD" "$TAG" "$BASE_URL" "$APK_SIZE" "$APK_SHA" "$NOTES" <<'PY'
import json
import sys
from datetime import date

ver_path, list_path, version, code, build, tag, base, size, sha, notes = sys.argv[1:11]
base = base.rstrip("/")
today = date.today().isoformat()


def load(path, default):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return default


previous = load(ver_path, {})
if not notes:
    # No notes for this version yet: keep the last text only when the version didn't change, so a
    # new version never shows the previous one's changelog as if it were its own.
    notes = previous.get("changelog", "") if previous.get("versionName") == version else ""

apk_url = base + "/release/tvhome-latest.apk"
entry = {
    "versionName": version,
    "versionCode": int(code),
    "build": int(build),
    "tag": tag,
    "releaseDate": today,
    "apkUrl": apk_url,
    "apkName": "tvhome-latest.apk",
    "apkSize": int(size),
    "apkSha256": sha,
    "changelog": notes,
}
current = dict(entry, notesUrl=base + "/whats-new.html")
with open(ver_path, "w", encoding="utf-8") as f:
    json.dump(current, f, indent=2, ensure_ascii=False)
    f.write("\n")

history = load(list_path, [])
if not isinstance(history, list):
    history = []
history = [e for e in history if e.get("tag") != tag]
history.insert(0, entry)
del history[40:]
with open(list_path, "w", encoding="utf-8") as f:
    json.dump(history, f, indent=2, ensure_ascii=False)
    f.write("\n")

print("version.json: %s build %s, %s bytes, sha256 %s" % (version, build, size, sha[:12]))
PY
