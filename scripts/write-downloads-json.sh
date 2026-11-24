#!/usr/bin/env bash
# Writes release/downloads.json: how many times each APK on GitHub Releases has been downloaded.
#
# The site cannot count its own copies (Pages serves static files and keeps no logs), so these are
# the GitHub Release downloads — the same files, by another door. The page says so. GitHub only
# exposes the numbers through its API, which allows 60 unauthenticated requests an hour per IP, so
# a scheduled workflow (download-stats.yml) collects them with the runner's token and commits this
# file; the site never calls the API from a visitor's browser.
#
# Usage: write-downloads-json.sh [output]     (GITHUB_REPOSITORY, RELEASES_JSON optional)
#   RELEASES_JSON=<file> reuses an already-fetched `gh api --paginate .../releases` dump.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/release/downloads.json}"
REPO="${GITHUB_REPOSITORY:-frhnkrnwn/SwadayaTV}"
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

if [ -n "${RELEASES_JSON:-}" ]; then
  cat "$RELEASES_JSON" >"$TMP"
else
  command -v gh >/dev/null || { echo "gh is needed to read the release counts" >&2; exit 1; }
  gh api --paginate "repos/$REPO/releases?per_page=100" >"$TMP"
fi

python3 - "$TMP" "$OUT" "$REPO" <<'PY'
import datetime
import json
import sys

src, out, repo = sys.argv[1:4]

# --paginate prints one JSON array per page, back to back.
with open(src, encoding="utf-8") as f:
    text = f.read()
decoder, at, releases = json.JSONDecoder(), 0, []
while at < len(text):
    while at < len(text) and text[at].isspace():
        at += 1
    if at >= len(text):
        break
    page, at = decoder.raw_decode(text, at)
    releases.extend(page if isinstance(page, list) else [page])

launcher = {"total": 0, "releases": 0}
app = {"total": 0, "releases": 0}
per_release = []
for release in releases:
    assets = [a for a in release.get("assets") or [] if str(a.get("name", "")).endswith(".apk")]
    if not assets:
        continue
    downloads = sum(int(a.get("download_count") or 0) for a in assets)
    is_app = any(str(a.get("name", "")).startswith("swadaya-app") for a in assets)
    bucket = app if is_app else launcher
    bucket["total"] += downloads
    bucket["releases"] += 1
    per_release.append({
        "tag": release.get("tag_name") or "",
        "kind": "app" if is_app else "launcher",
        "downloads": downloads,
        "publishedAt": (release.get("published_at") or "")[:10],
    })

per_release.sort(key=lambda e: (e["publishedAt"], e["tag"]), reverse=True)
document = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "source": "GitHub Releases asset download counts",
    "repository": repo,
    "launcher": launcher,
    "app": app,
    "releases": per_release,
}
with open(out, "w", encoding="utf-8") as f:
    json.dump(document, f, indent=2, ensure_ascii=False)
    f.write("\n")
print("%s: %d SwadayaTV downloads over %d releases, %d Swadaya App downloads"
      % (out, launcher["total"], launcher["releases"], app["total"]))
PY
