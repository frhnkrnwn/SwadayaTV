#!/usr/bin/env bash
# Post-deploy smoke test: does the published site actually hand out what the repository says it
# does? A download site fails silently — the button still looks fine while it serves a 404 or the
# previous build — so every deploy checks the live pages, both APKs and the version files.
#
# Every request carries a unique query so GitHub's CDN (max-age=600) can't answer with the build
# before this one, and the whole check retries for a few minutes because a deploy is visible at
# different edges at slightly different times.
#
# Usage: check-site.sh            (BASE_URL, SMOKE_BUST, SMOKE_ATTEMPTS, SMOKE_DELAY optional)
set -uo pipefail

BASE_URL="${BASE_URL:-https://frhnkrnwn.github.io/SwadayaTV}"
BASE_URL="${BASE_URL%/}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUST="${SMOKE_BUST:-$(date +%s)-$$}"
ATTEMPTS="${SMOKE_ATTEMPTS:-7}"
DELAY="${SMOKE_DELAY:-15}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

FAILED=0
fail() { echo "  FAIL $*" >&2; FAILED=$((FAILED + 1)); }
ok() { echo "  ok   $*"; }

# fetch <path> <destination>: downloads one URL, printing the HTTP status. Retries on a bad status.
fetch() {
  local path="$1" dest="$2" url code
  url="$BASE_URL/$path"
  case "$url" in *\?*) url="$url&smoke=$BUST" ;; *) url="$url?smoke=$BUST" ;; esac
  for _ in 1 2 3; do
    code="$(curl -sSL --max-time 60 -o "$dest" -w '%{http_code}' "$url" || echo 000)"
    [ "$code" = "200" ] && { echo 200; return 0; }
    sleep 5
  done
  echo "$code"
  return 1
}

check_page() {
  local path="$1" body="$TMP/$(echo "$1" | tr '/?' '__')" code size
  code="$(fetch "$path" "$body")" || { fail "$path -> HTTP $code"; return; }
  size="$(wc -c <"$body" | tr -d ' ')"
  # A page that answers with a stub is a failure; small assets (robots.txt) are legitimately tiny.
  local minimum=1
  case "$path" in *.html) minimum=500 ;; esac
  if [ "$size" -lt "$minimum" ]; then
    fail "$path -> HTTP 200 but only $size bytes"
  else
    ok "$path ($size bytes)"
  fi
}

# check_apk <path relative to the site> <file in the repository>
check_apk() {
  local path="$1" local_file="$2" dest="$TMP/$(basename "$1")" code want got type
  code="$(fetch "$path" "$dest")" || { fail "$path -> HTTP $code"; return; }
  [ -f "$local_file" ] || { fail "$path -> no $local_file in the repository to compare with"; return; }
  want="$(sha256sum "$local_file" | cut -d' ' -f1)"
  got="$(sha256sum "$dest" | cut -d' ' -f1)"
  if [ "$want" != "$got" ]; then
    fail "$path -> served sha256 ${got:0:12}… but the repository has ${want:0:12}…"
  elif [ "$(wc -c <"$dest" | tr -d ' ')" != "$(wc -c <"$local_file" | tr -d ' ')" ]; then
    fail "$path -> size differs from the repository"
  else
    ok "$path matches the repository (${got:0:12}…, $(wc -c <"$dest" | tr -d ' ') bytes)"
  fi

  type="$(curl -sSIL --max-time 60 "$BASE_URL/$path?smoke=$BUST" | tr -d '\r' |
    awk 'tolower($1) == "content-type:" { $1 = ""; print }' | tail -1 | sed 's/^ *//')"
  case "$type" in
    application/vnd.android.package-archive*) ok "$path content-type is $type" ;;
    *) fail "$path content-type is '${type:-none}', not an APK type" ;;
  esac
}

# check_installs <apk file> <json path on the site> <build.gradle>
# The JSON the app's updater reads has to describe the file the button hands out, otherwise an
# update installs a build the user never chose.
check_installs() {
  local apk="$1" json_path="$2" gradle="$3" json out
  json="$TMP/$(basename "$2")"
  local code
  code="$(fetch "$json_path" "$json")" || { fail "$json_path -> HTTP $code"; return; }
  [ -f "$apk" ] || { fail "$json_path -> no local $apk to compare with"; return; }

  if out="$(python3 - "$json" "$apk" "$gradle" <<'PY' 2>&1
import hashlib
import json
import re
import sys

json_path, apk, gradle = sys.argv[1:4]
name = apk.rsplit("/", 1)[-1]
try:
    with open(json_path, encoding="utf-8") as f:
        data = json.load(f)
except ValueError as e:
    print("not valid JSON (%s)" % e)
    sys.exit(1)

with open(apk, "rb") as f:
    blob = f.read()
sha, size = hashlib.sha256(blob).hexdigest(), len(blob)
problems = []
if data.get("apkSha256") != sha:
    problems.append("apkSha256 %s\u2026 is not the APK's %s\u2026"
                    % (str(data.get("apkSha256"))[:12], sha[:12]))
if int(data.get("apkSize") or 0) != size:
    problems.append("apkSize %s is not %s" % (data.get("apkSize"), size))
if not str(data.get("apkUrl", "")).startswith("https://") or \
        not str(data.get("apkUrl", "")).endswith("/" + name):
    problems.append("apkUrl %r does not point at release/%s" % (data.get("apkUrl"), name))
# version.json names the release tag its netengine-*.so files live under; app.json has no release.
if "tag" in data and not str(data.get("tag", "")).startswith("v"):
    problems.append("tag %r is missing or malformed" % data.get("tag"))
if gradle:
    with open(gradle, encoding="utf-8") as f:
        source = f.read()
    found = re.search(r"versionName\s+'([^']+)'", source)
    if not found:
        problems.append("no versionName in %s" % gradle)
    elif data.get("versionName") != found.group(1):
        problems.append("versionName %s is not %s in %s"
                        % (data.get("versionName"), found.group(1), gradle))
if problems:
    for p in problems:
        print(p)
    sys.exit(1)
print("%s describes %s (%s\u2026, %s bytes)"
      % (json_path, data.get("versionName"), sha[:12], size))
PY
  )"; then
    ok "$out"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && fail "$(basename "$json_path"): $line"
    done <<<"$out"
  fi
}

echo "Checking $BASE_URL"

# Pages first, then the files the pages load: a stylesheet or script left out of the artifact is
# exactly the kind of breakage this check exists for.
PAGES="index.html id.html docs/index.html docs/id.html whats-new.html id-whats-new.html help.html id-help.html tv/index.html site.css site.js sitemap.xml robots.txt"
for attempt in $(seq 1 "$ATTEMPTS"); do
  FAILED=0
  echo "Attempt $attempt of $ATTEMPTS"
  for page in $PAGES; do check_page "$page"; done
  check_apk "release/tvhome-latest.apk" "$ROOT/release/tvhome-latest.apk"
  check_apk "release/swadaya-app.apk" "$ROOT/release/swadaya-app.apk"
  if [ -f "$ROOT/release/tvhome-previous.apk" ]; then
    check_apk "release/tvhome-previous.apk" "$ROOT/release/tvhome-previous.apk"
  fi
  check_installs "$ROOT/release/tvhome-latest.apk" "release/version.json" \
    "$ROOT/launcher/app/build.gradle"
  check_installs "$ROOT/release/swadaya-app.apk" "release/app.json" \
    "$ROOT/phone-app/app/build.gradle"

  if [ "$FAILED" -eq 0 ]; then
    echo "Site is healthy: every page, APK and version file matches this commit."
    exit 0
  fi
  if [ "$attempt" -lt "$ATTEMPTS" ]; then
    echo "Waiting ${DELAY}s for the deploy to settle…"
    sleep "$DELAY"
  fi
done

echo "$FAILED check(s) still failing after $ATTEMPTS attempts." >&2
exit 1
