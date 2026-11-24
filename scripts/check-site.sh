#!/usr/bin/env bash
# Post-deploy smoke test: does the published site actually hand out what the repository says it
# does? A download site fails silently — the button still looks fine while it serves a 404 or the
# previous build — so every deploy checks the live pages, both APKs and the version files.
#
# Every request carries a unique query so GitHub's CDN (max-age=600) can't answer with the build
# before this one, and the whole check retries for a few minutes because a deploy is visible at
# different edges at slightly different times.
#
# Two deploys can be in flight at once (Build publishes the new APK, Pages publishes a docs-only
# push). The repository this script runs in is one revision; the site is whichever deploy landed
# last. So the rule is one-directional:
#   - the site is OLDER than this commit  -> still rolling out, retry, then fail;
#   - the site is NEWER than this commit  -> a later deploy won, which is fine: check that the
#     site is internally consistent (its version.json describes the APK it serves) and move on.
# That keeps a race red-free without ever accepting a site that serves an APK its own version
# file doesn't describe.
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
note() { echo "  note $*"; }

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

# json_get <file> <key>: one top-level value, empty when missing or the file isn't JSON.
json_get() {
  python3 - "$1" "$2" <<'PY'
import json, sys
try:
    with open(sys.argv[1], encoding="utf-8") as f:
        value = json.load(f).get(sys.argv[2])
except Exception:
    sys.exit(1)
print("" if value is None else value)
PY
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

# check_apk <path relative to the site>: the file is there, and served as an APK.
check_apk() {
  local path="$1" dest="$TMP/$(basename "$1")" code type
  code="$(fetch "$path" "$dest")" || { fail "$path -> HTTP $code"; return 1; }
  ok "$path ($(wc -c <"$dest" | tr -d ' ') bytes)"
  type="$(curl -sSIL --max-time 60 "$BASE_URL/$path?smoke=$BUST" | tr -d '\r' |
    awk 'tolower($1) == "content-type:" { $1 = ""; print }' | tail -1 | sed 's/^ *//')"
  case "$type" in
    application/vnd.android.package-archive*) ok "$path content-type is $type" ;;
    *) fail "$path content-type is '${type:-none}', not an APK type" ;;
  esac
  return 0
}

# described <json file> <apk file> <label>: the JSON the updater reads describes the APK the
# button hands out — the one thing that must hold whichever deploy landed last.
described() {
  local json="$1" apk="$2" label="$3" out
  if out="$(python3 - "$json" "$apk" "$label" <<'PY' 2>&1
import hashlib
import json
import sys

json_path, apk, label = sys.argv[1:4]
try:
    with open(json_path, encoding="utf-8") as f:
        data = json.load(f)
except ValueError as e:
    print("not valid JSON (%s)" % e)
    sys.exit(1)

with open(apk, "rb") as f:
    blob = f.read()
sha, size = hashlib.sha256(blob).hexdigest(), len(blob)
name = str(data.get("apkName") or apk.rsplit("/", 1)[-1])
url = str(data.get("apkUrl", ""))
problems = []
if data.get("apkSha256") != sha:
    problems.append("apkSha256 %s\u2026 is not the served APK's %s\u2026"
                    % (str(data.get("apkSha256"))[:12], sha[:12]))
if int(data.get("apkSize") or 0) != size:
    problems.append("apkSize %s is not the served APK's %s" % (data.get("apkSize"), size))
if not url.startswith("https://") or not url.endswith("/" + name):
    problems.append("apkUrl %r does not point at release/%s" % (url, name))
if problems:
    for p in problems:
        print(p)
    sys.exit(1)
print("%s describes the served %s (%s\u2026, %s bytes)"
      % (label, name, sha[:12], size))
PY
  )"; then
    ok "$out"
    return 0
  fi
  while IFS= read -r line; do
    [ -n "$line" ] && fail "$label: $line"
  done <<<"$out"
  return 1
}

# same_as_local <local file> <served file> <label>: byte-for-byte, only when the site is this
# commit's build. Under a race with a newer deploy the served file is a different (newer) one.
same_as_local() {
  local local_file="$1" served="$2" label="$3" want got
  [ -f "$local_file" ] || { fail "$label -> no $local_file in the repository to compare with"; return; }
  want="$(sha256sum "$local_file" | cut -d' ' -f1)"
  got="$(sha256sum "$served" | cut -d' ' -f1)"
  if [ "$want" != "$got" ]; then
    fail "$label -> served sha256 ${got:0:12}… but this commit has ${want:0:12}…"
  else
    ok "$label matches this commit (${got:0:12}…, $(wc -c <"$served" | tr -d ' ') bytes)"
  fi
}

# version_name <gradle file>: the versionName a build would carry, for the same check.
version_name() {
  grep -o "versionName '[^']*'" "$1" | head -1 | cut -d"'" -f2
}

echo "Checking $BASE_URL"

# Pages first, then the files the pages load: a stylesheet or script left out of the artifact is
# exactly the kind of breakage this check exists for.
PAGES="index.html id.html docs/index.html docs/id.html whats-new.html id-whats-new.html help.html id-help.html tv/index.html site.css site.js sitemap.xml robots.txt"
for attempt in $(seq 1 "$ATTEMPTS"); do
  FAILED=0
  echo "Attempt $attempt of $ATTEMPTS"
  for page in $PAGES; do check_page "$page"; done
  # Written by the nightly Download stats workflow; a repository that has not run it yet has no
  # file to compare against, and the page copes without one (the static text stays).
  [ -f "$ROOT/release/downloads.json" ] && check_page "release/downloads.json"

  local_build="$(json_get "$ROOT/release/version.json" build)" || local_build=""
  local_app_version="$(json_get "$ROOT/release/app.json" versionName)" || local_app_version=""
  ahead=0

  if check_page "release/version.json" >/dev/null; then
    served_json="$TMP/version.json"
    fetch "release/version.json" "$served_json" >/dev/null
    served_build="$(json_get "$served_json" build)" || served_build=""
    served_name="$(json_get "$served_json" apkName)" || served_name=""
    served_version="$(json_get "$served_json" versionName)" || served_version=""
    [ -n "$served_name" ] || served_name="tvhome-latest.apk"

    if [ -z "$served_build" ] || ! [ "$served_build" -ge 0 ] 2>/dev/null; then
      fail "release/version.json has no usable build number"
    elif [ -n "$local_build" ] && [ "$served_build" -lt "$local_build" ]; then
      fail "site still serves build $served_build while this commit publishes build $local_build"
    elif [ -n "$local_build" ] && [ "$served_build" -gt "$local_build" ]; then
      ahead=1
      note "site serves build $served_build, newer than this commit's $local_build (a later deploy won)"
    fi

    if check_apk "release/$served_name"; then
      described "$served_json" "$TMP/$served_name" "release/version.json"
      if [ "$ahead" -eq 0 ]; then
        same_as_local "$ROOT/release/tvhome-latest.apk" "$TMP/$served_name" "release/$served_name"
        gradle_name="$(version_name "$ROOT/launcher/app/build.gradle")"
        if [ -n "$served_version" ] && [ "$served_version" != "$gradle_name" ]; then
          fail "version.json says $served_version but launcher/app/build.gradle says $gradle_name"
        else
          ok "version.json names version $served_version"
        fi
      fi
    fi

    # The previous build is the one-tap rollback beside the newest APK; it does not have its own
    # version file, so it is checked for presence and (when the site is this commit) for content.
    if [ -f "$ROOT/release/tvhome-previous.apk" ]; then
      if check_apk "release/tvhome-previous.apk" && [ "$ahead" -eq 0 ]; then
        same_as_local "$ROOT/release/tvhome-previous.apk" "$TMP/tvhome-previous.apk" \
          "release/tvhome-previous.apk"
      fi
    else
      note "no release/tvhome-previous.apk in this commit; skipping the rollback copy"
    fi
  fi

  # The phone app has its own version file and its own release workflow, so it is compared by
  # version rather than by build number.
  if check_page "release/app.json" >/dev/null; then
    served_app_json="$TMP/app.json"
    fetch "release/app.json" "$served_app_json" >/dev/null
    served_app="$(json_get "$served_app_json" apkName)" || served_app=""
    served_app_version="$(json_get "$served_app_json" versionName)" || served_app_version=""
    [ -n "$served_app" ] || served_app="swadaya-app.apk"
    if check_apk "release/$served_app"; then
      if described "$served_app_json" "$TMP/$served_app" "release/app.json"; then
        if [ -n "$local_app_version" ] && [ -n "$served_app_version" ]; then
          case "$(printf '%s\n%s\n' "$local_app_version" "$served_app_version" | sort -V | tail -1)" in
            "$local_app_version")
              same_as_local "$ROOT/release/swadaya-app.apk" "$TMP/$served_app" "release/$served_app" ;;
            *)
              note "site serves Swadaya App $served_app_version, newer than this commit's $local_app_version" ;;
          esac
        fi
      fi
    fi
  fi

  if [ "$FAILED" -eq 0 ]; then
    if [ "$ahead" -eq 1 ]; then
      echo "Site is healthy: a newer build (a later deploy) is live, and every JSON describes the APK it serves."
    else
      echo "Site is healthy: every page, APK and version file matches this commit."
    fi
    exit 0
  fi
  if [ "$attempt" -lt "$ATTEMPTS" ]; then
    echo "Waiting ${DELAY}s for the deploy to settle…"
    sleep "$DELAY"
  fi
done

echo "$FAILED check(s) still failing after $ATTEMPTS attempts." >&2
exit 1
