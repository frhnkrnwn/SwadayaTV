#!/usr/bin/env bash
# Collects exactly what the published site serves into one folder. Every workflow that deploys
# Pages runs this, so the three of them can never drift apart: the pages, the stylesheet, the
# screenshots and guides in docs/, the APKs and their JSON in release/, and the tiny TV page.
#
# Usage: collect-site.sh [output-folder]     (default: _site)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-_site}"
case "$OUT" in
  "" | "/" | "." | ".." ) echo "refusing to write into '$OUT'" >&2; exit 1 ;;
esac
case "$OUT" in
  /*) ;;                      # absolute path, taken as given
  *) OUT="$ROOT/$OUT" ;;      # anything else is relative to the repository
esac

rm -rf "$OUT"
mkdir -p "$OUT"

cp "$ROOT"/*.html "$OUT"/
for f in site.css og.jpg sitemap.xml robots.txt; do
  [ -f "$ROOT/$f" ] && cp "$ROOT/$f" "$OUT"/
done
cp -r "$ROOT/docs" "$ROOT/release" "$OUT"/
[ -d "$ROOT/tv" ] && cp -r "$ROOT/tv" "$OUT"/

echo "collected $(find "$OUT" -type f | wc -l | tr -d ' ') files into $OUT"
